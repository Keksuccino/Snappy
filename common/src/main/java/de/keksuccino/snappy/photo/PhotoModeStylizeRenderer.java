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
import de.keksuccino.snappy.client.render.config.StylizeConfig;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class PhotoModeStylizeRenderer {

    private static final Identifier STYLIZE_PIPELINE_ID = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "pipeline/photo_stylize");
    private static final Identifier COPY_PIPELINE_ID = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "pipeline/photo_stylize_copy");
    private static final Identifier STYLIZE_SHADER_ID = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "post/stylize");
    private static final int STYLIZE_CONFIG_SIZE = new Std140SizeCalculator().putVec4().get();
    private static final RenderPipeline STYLIZE_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(STYLIZE_PIPELINE_ID)
            .withVertexShader(ShaderEffectPass.SCREEN_QUAD_SHADER_ID)
            .withFragmentShader(STYLIZE_SHADER_ID)
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withSampler("InSampler")
                    .withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER)
                    .withUniform("StylizeConfig", UniformType.UNIFORM_BUFFER)
                    .build())
            .build();
    private static final RenderPipeline COPY_PIPELINE = ShaderEffectPass.buildCopyPipeline(COPY_PIPELINE_ID);
    @Nullable
    private static Resources resources;

    private PhotoModeStylizeRenderer() {
    }

    static void process(
            @NotNull RenderTarget mainRenderTarget,
            @NotNull GraphicsResourceAllocator resourceAllocator,
            @NotNull StylizeConfig config
    ) {
        if (!ShaderEffectPass.hasUsableColorTarget(mainRenderTarget) || !ensurePipelinesAvailable()) {
            return;
        }

        Resources renderResources = resources();
        FrameGraphBuilder frame = new FrameGraphBuilder();
        ResourceHandle<RenderTarget> main = frame.importExternal("snappy main", mainRenderTarget);
        ResourceHandle<RenderTarget> stylizedTarget = frame.createInternal(
                "snappy stylize",
                ShaderEffectPass.colorTargetDescriptor(mainRenderTarget)
        );

        FramePass stylizePass = frame.addPass("snappy_stylize");
        stylizePass.reads(main);
        ResourceHandle<RenderTarget> stylizedOutput = stylizePass.readsAndWrites(stylizedTarget);
        stylizePass.executes(() -> {
            writeUniforms(renderResources, main.get(), stylizedOutput.get(), config);
            drawStylizePass(renderResources, main.get(), stylizedOutput.get());
        });

        FramePass copyPass = frame.addPass("snappy_stylize_copy");
        copyPass.reads(stylizedOutput);
        ResourceHandle<RenderTarget> mainOutput = copyPass.readsAndWrites(main);
        copyPass.executes(() -> drawCopyPass(renderResources, stylizedOutput.get(), mainOutput.get()));

        try {
            frame.execute(resourceAllocator);
        } finally {
            renderResources.samplerInfoBuffer.rotate();
            renderResources.stylizeConfigBuffer.rotate();
        }
    }

    static void close() {
        if (resources != null) {
            resources.close();
            resources = null;
        }
    }

    private static boolean ensurePipelinesAvailable() {
        return ShaderEffectPass.allPipelinesAvailable(STYLIZE_PIPELINE, COPY_PIPELINE);
    }

    private static void writeUniforms(
            @NotNull Resources renderResources,
            @NotNull RenderTarget source,
            @NotNull RenderTarget output,
            @NotNull StylizeConfig config
    ) {
        ShaderEffectPass.writeSamplerInfo(renderResources.samplerInfoBuffer, source, output);

        try (GpuBufferSlice.MappedView view = renderResources.stylizeConfigBuffer.currentBuffer().map(false, true)) {
            Std140Builder builder = Std140Builder.intoBuffer(view.data());
            builder.putVec4(config.shaderIndex(), 0.0F, 0.0F, 0.0F);
        }
    }

    private static void drawStylizePass(@NotNull Resources renderResources, @NotNull RenderTarget source, @NotNull RenderTarget output) {
        ShaderEffectPass.drawScreenQuad(
                "Snappy photo stylize",
                renderResources.postProjectionMatrixBuffer,
                renderResources.samplerInfoBuffer,
                STYLIZE_PIPELINE,
                source,
                output,
                renderPass -> renderPass.setUniform("StylizeConfig", renderResources.stylizeConfigBuffer.currentBuffer())
        );
    }

    private static void drawCopyPass(@NotNull Resources renderResources, @NotNull RenderTarget source, @NotNull RenderTarget output) {
        ShaderEffectPass.drawCopyPass(
                "Snappy photo stylize copy",
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

        private final ProjectionMatrixBuffer postProjectionMatrixBuffer = new ProjectionMatrixBuffer("snappy photo stylize");
        private final MappableRingBuffer samplerInfoBuffer = new MappableRingBuffer(() -> "Snappy photo stylize SamplerInfo", 130, ShaderEffectPass.SAMPLER_INFO_SIZE);
        private final MappableRingBuffer stylizeConfigBuffer = new MappableRingBuffer(() -> "Snappy photo stylize Config", 130, STYLIZE_CONFIG_SIZE);

        @Override
        public void close() {
            this.postProjectionMatrixBuffer.close();
            this.samplerInfoBuffer.close();
            this.stylizeConfigBuffer.close();
        }

    }

}
