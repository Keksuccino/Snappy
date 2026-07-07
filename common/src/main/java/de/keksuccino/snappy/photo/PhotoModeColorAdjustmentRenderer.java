package de.keksuccino.snappy.photo;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.shaders.UniformType;
import de.keksuccino.snappy.Snappy;
import de.keksuccino.snappy.client.render.ShaderEffectPass;
import de.keksuccino.snappy.client.render.config.ColorAdjustmentConfig;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class PhotoModeColorAdjustmentRenderer {

    private static final Identifier ADJUSTMENT_PIPELINE_ID = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "pipeline/photo_color_adjustments");
    private static final Identifier COPY_PIPELINE_ID = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "pipeline/photo_color_adjustments_copy");
    private static final Identifier ADJUSTMENT_SHADER_ID = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "post/color_adjustments");
    private static final int ADJUSTMENT_CONFIG_SIZE = new Std140SizeCalculator().putVec4().get();
    private static final RenderPipeline ADJUSTMENT_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(ADJUSTMENT_PIPELINE_ID)
            .withVertexShader(ShaderEffectPass.SCREEN_QUAD_SHADER_ID)
            .withFragmentShader(ADJUSTMENT_SHADER_ID)
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withSampler("InSampler")
                    .withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER)
                    .withUniform("ColorAdjustmentConfig", UniformType.UNIFORM_BUFFER)
                    .build())
            .build();
    private static final RenderPipeline COPY_PIPELINE = ShaderEffectPass.buildCopyPipeline(COPY_PIPELINE_ID);
    @Nullable
    private static Resources resources;

    private PhotoModeColorAdjustmentRenderer() {
    }

    static void process(
            @NotNull RenderTarget mainRenderTarget,
            @NotNull GraphicsResourceAllocator resourceAllocator,
            @NotNull ColorAdjustmentConfig config
    ) {
        if (!ShaderEffectPass.hasUsableColorTarget(mainRenderTarget) || !ensurePipelinesAvailable()) {
            return;
        }

        Resources renderResources = resources();
        FrameGraphBuilder frame = new FrameGraphBuilder();
        ResourceHandle<RenderTarget> main = frame.importExternal("snappy main", mainRenderTarget);
        ResourceHandle<RenderTarget> adjustedTarget = frame.createInternal(
                "snappy color adjustments",
                ShaderEffectPass.colorTargetDescriptor(mainRenderTarget)
        );

        FramePass adjustmentPass = frame.addPass("snappy_color_adjustments");
        adjustmentPass.reads(main);
        ResourceHandle<RenderTarget> adjustedOutput = adjustmentPass.readsAndWrites(adjustedTarget);
        adjustmentPass.executes(() -> {
            writeUniforms(renderResources, main.get(), adjustedOutput.get(), config);
            drawAdjustmentPass(renderResources, main.get(), adjustedOutput.get());
        });

        FramePass copyPass = frame.addPass("snappy_color_adjustments_copy");
        copyPass.reads(adjustedOutput);
        ResourceHandle<RenderTarget> mainOutput = copyPass.readsAndWrites(main);
        copyPass.executes(() -> drawCopyPass(renderResources, adjustedOutput.get(), mainOutput.get()));

        try {
            frame.execute(resourceAllocator);
        } finally {
            renderResources.samplerInfoBuffer.rotate();
            renderResources.adjustmentConfigBuffer.rotate();
        }
    }

    static void close() {
        if (resources != null) {
            resources.close();
            resources = null;
        }
    }

    private static boolean ensurePipelinesAvailable() {
        return ShaderEffectPass.allPipelinesAvailable(ADJUSTMENT_PIPELINE, COPY_PIPELINE);
    }

    private static void writeUniforms(
            @NotNull Resources renderResources,
            @NotNull RenderTarget source,
            @NotNull RenderTarget output,
            @NotNull ColorAdjustmentConfig config
    ) {
        ShaderEffectPass.writeSamplerInfo(renderResources.samplerInfoBuffer, source, output);

        try (GpuBufferSlice.MappedView view = renderResources.adjustmentConfigBuffer.currentBuffer().map(false, true)) {
            Std140Builder builder = Std140Builder.intoBuffer(view.data());
            builder.putVec4(config.saturation(), config.contrast(), config.overexposure(), config.gamma());
        }
    }

    private static void drawAdjustmentPass(@NotNull Resources renderResources, @NotNull RenderTarget source, @NotNull RenderTarget output) {
        ShaderEffectPass.drawSingleSamplerPass(
                "Snappy photo color adjustments",
                renderResources.postProjectionMatrixBuffer,
                renderResources.samplerInfoBuffer,
                ADJUSTMENT_PIPELINE,
                source,
                output,
                renderPass -> renderPass.setUniform("ColorAdjustmentConfig", renderResources.adjustmentConfigBuffer.currentBuffer())
        );
    }

    private static void drawCopyPass(@NotNull Resources renderResources, @NotNull RenderTarget source, @NotNull RenderTarget output) {
        ShaderEffectPass.drawCopyPass(
                "Snappy photo color adjustments copy",
                renderResources.postProjectionMatrixBuffer,
                renderResources.samplerInfoBuffer,
                COPY_PIPELINE,
                source,
                output
        );
    }

    @NotNull
    private static Resources resources() {
        if (resources == null) {
            resources = new Resources();
        }
        return resources;
    }

    private static final class Resources implements AutoCloseable {

        private final ProjectionMatrixBuffer postProjectionMatrixBuffer = new ProjectionMatrixBuffer("snappy photo color adjustments");
        private final MappableRingBuffer samplerInfoBuffer = new MappableRingBuffer(() -> "Snappy photo color adjustments SamplerInfo", 130, ShaderEffectPass.SAMPLER_INFO_SIZE);
        private final MappableRingBuffer adjustmentConfigBuffer = new MappableRingBuffer(() -> "Snappy photo color adjustments Config", 130, ADJUSTMENT_CONFIG_SIZE);

        @Override
        public void close() {
            this.postProjectionMatrixBuffer.close();
            this.samplerInfoBuffer.close();
            this.adjustmentConfigBuffer.close();
        }

    }

}
