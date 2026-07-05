package de.keksuccino.panoramica.photo;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.framegraph.FrameGraphBuilder;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.resource.ResourceHandle;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.systems.SamplerCache;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import de.keksuccino.panoramica.Panoramica;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4f;
import org.joml.Vector4fc;

import java.util.Optional;
import java.util.OptionalDouble;

final class PhotoModeColorAdjustmentRenderer {

    private static final Identifier ADJUSTMENT_PIPELINE_ID = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "pipeline/photo_color_adjustments");
    private static final Identifier COPY_PIPELINE_ID = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "pipeline/photo_color_adjustments_copy");
    private static final Identifier ADJUSTMENT_SHADER_ID = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "post/color_adjustments");
    private static final Identifier COPY_SHADER_ID = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "post/copy");
    private static final Identifier SCREEN_QUAD_SHADER_ID = Identifier.withDefaultNamespace("core/screenquad");
    private static final int SAMPLER_INFO_SIZE = new Std140SizeCalculator().putVec2().putVec2().get();
    private static final int ADJUSTMENT_CONFIG_SIZE = new Std140SizeCalculator().putVec4().get();
    private static final Vector4fc CLEAR_COLOR = new Vector4f(0.0F, 0.0F, 0.0F, 0.0F);
    private static final RenderPipeline ADJUSTMENT_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(ADJUSTMENT_PIPELINE_ID)
            .withVertexShader(SCREEN_QUAD_SHADER_ID)
            .withFragmentShader(ADJUSTMENT_SHADER_ID)
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withSampler("InSampler")
                    .withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER)
                    .withUniform("ColorAdjustmentConfig", UniformType.UNIFORM_BUFFER)
                    .build())
            .build();
    private static final RenderPipeline COPY_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(COPY_PIPELINE_ID)
            .withVertexShader(SCREEN_QUAD_SHADER_ID)
            .withFragmentShader(COPY_SHADER_ID)
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withSampler("InSampler")
                    .withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER)
                    .build())
            .build();
    private static final Projection POST_PROJECTION = new Projection();
    @Nullable
    private static Resources resources;

    static {
        POST_PROJECTION.setupOrtho(0.1F, 1000.0F, 1.0F, 1.0F, false);
    }

    private PhotoModeColorAdjustmentRenderer() {
    }

    static void process(
            @NotNull RenderTarget mainRenderTarget,
            @NotNull GraphicsResourceAllocator resourceAllocator,
            @NotNull PhotoModeManager.Session session
    ) {
        GpuTextureView mainColor = mainRenderTarget.getColorTextureView();
        if (mainColor == null || mainRenderTarget.width <= 0 || mainRenderTarget.height <= 0 || !ensurePipelinesAvailable()) {
            return;
        }

        Resources renderResources = resources();
        writeUniforms(renderResources, mainRenderTarget, session);
        FrameGraphBuilder frame = new FrameGraphBuilder();
        ResourceHandle<RenderTarget> main = frame.importExternal("panoramica main", mainRenderTarget);
        ResourceHandle<RenderTarget> adjustedTarget = frame.createInternal(
                "panoramica color adjustments",
                new RenderTargetDescriptor(mainRenderTarget.width, mainRenderTarget.height, false, CLEAR_COLOR, GpuFormat.RGBA8_UNORM)
        );

        FramePass adjustmentPass = frame.addPass("panoramica_color_adjustments");
        adjustmentPass.reads(main);
        ResourceHandle<RenderTarget> adjustedOutput = adjustmentPass.readsAndWrites(adjustedTarget);
        adjustmentPass.executes(() -> drawAdjustmentPass(renderResources, main.get(), adjustedOutput.get()));

        FramePass copyPass = frame.addPass("panoramica_color_adjustments_copy");
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
        return RenderSystem.getDevice().precompilePipeline(ADJUSTMENT_PIPELINE).isValid()
                && RenderSystem.getDevice().precompilePipeline(COPY_PIPELINE).isValid();
    }

    private static void writeUniforms(
            @NotNull Resources renderResources,
            @NotNull RenderTarget mainRenderTarget,
            @NotNull PhotoModeManager.Session session
    ) {
        try (GpuBufferSlice.MappedView view = renderResources.samplerInfoBuffer.currentBuffer().map(false, true)) {
            Std140Builder builder = Std140Builder.intoBuffer(view.data());
            builder.putVec2(mainRenderTarget.width, mainRenderTarget.height);
            builder.putVec2(mainRenderTarget.width, mainRenderTarget.height);
        }

        try (GpuBufferSlice.MappedView view = renderResources.adjustmentConfigBuffer.currentBuffer().map(false, true)) {
            Std140Builder builder = Std140Builder.intoBuffer(view.data());
            builder.putVec4(session.saturation(), session.contrast(), session.overexposure(), session.gamma());
        }
    }

    private static void drawAdjustmentPass(@NotNull Resources renderResources, @NotNull RenderTarget source, @NotNull RenderTarget output) {
        GpuTextureView sourceColor = source.getColorTextureView();
        GpuTextureView outputColor = output.getColorTextureView();
        if (sourceColor == null || outputColor == null) {
            return;
        }

        POST_PROJECTION.setSize(output.width, output.height);
        GpuBufferSlice projectionBuffer = renderResources.postProjectionMatrixBuffer.getBuffer(POST_PROJECTION);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projectionBuffer, ProjectionType.ORTHOGRAPHIC);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        SamplerCache samplerCache = RenderSystem.getSamplerCache();
        try (RenderPass renderPass = commandEncoder.createRenderPass(
                () -> "Panoramica photo color adjustments",
                outputColor,
                Optional.empty(),
                null,
                OptionalDouble.empty()
        )) {
            renderPass.setPipeline(ADJUSTMENT_PIPELINE);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("SamplerInfo", renderResources.samplerInfoBuffer.currentBuffer());
            renderPass.setUniform("ColorAdjustmentConfig", renderResources.adjustmentConfigBuffer.currentBuffer());
            renderPass.bindTexture("InSampler", sourceColor, samplerCache.getClampToEdge(FilterMode.NEAREST));
            renderPass.draw(3, 1, 0, 0);
        } finally {
            RenderSystem.restoreProjectionMatrix();
        }
    }

    private static void drawCopyPass(@NotNull Resources renderResources, @NotNull RenderTarget source, @NotNull RenderTarget output) {
        GpuTextureView sourceColor = source.getColorTextureView();
        GpuTextureView outputColor = output.getColorTextureView();
        if (sourceColor == null || outputColor == null) {
            return;
        }

        POST_PROJECTION.setSize(output.width, output.height);
        GpuBufferSlice projectionBuffer = renderResources.postProjectionMatrixBuffer.getBuffer(POST_PROJECTION);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projectionBuffer, ProjectionType.ORTHOGRAPHIC);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        try (RenderPass renderPass = commandEncoder.createRenderPass(
                () -> "Panoramica photo color adjustments copy",
                outputColor,
                Optional.empty(),
                null,
                OptionalDouble.empty()
        )) {
            renderPass.setPipeline(COPY_PIPELINE);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("SamplerInfo", renderResources.samplerInfoBuffer.currentBuffer());
            renderPass.bindTexture("InSampler", sourceColor, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            renderPass.draw(3, 1, 0, 0);
        } finally {
            RenderSystem.restoreProjectionMatrix();
        }
    }

    @NotNull
    private static Resources resources() {
        if (resources == null) {
            resources = new Resources();
        }
        return resources;
    }

    private static final class Resources implements AutoCloseable {

        private final ProjectionMatrixBuffer postProjectionMatrixBuffer = new ProjectionMatrixBuffer("panoramica photo color adjustments");
        private final MappableRingBuffer samplerInfoBuffer = new MappableRingBuffer(() -> "Panoramica photo color adjustments SamplerInfo", 130, SAMPLER_INFO_SIZE);
        private final MappableRingBuffer adjustmentConfigBuffer = new MappableRingBuffer(() -> "Panoramica photo color adjustments Config", 130, ADJUSTMENT_CONFIG_SIZE);

        @Override
        public void close() {
            this.postProjectionMatrixBuffer.close();
            this.samplerInfoBuffer.close();
            this.adjustmentConfigBuffer.close();
        }

    }

}
