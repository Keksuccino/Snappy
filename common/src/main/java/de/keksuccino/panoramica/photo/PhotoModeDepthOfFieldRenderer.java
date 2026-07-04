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
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

import java.util.Optional;
import java.util.OptionalDouble;

final class PhotoModeDepthOfFieldRenderer {

    private static final Identifier DOF_PIPELINE_ID = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "pipeline/photo_depth_of_field");
    private static final Identifier COPY_PIPELINE_ID = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "pipeline/photo_depth_of_field_copy");
    private static final Identifier DOF_SHADER_ID = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "post/depth_of_field");
    private static final Identifier COPY_SHADER_ID = Identifier.fromNamespaceAndPath(Panoramica.MOD_ID, "post/copy");
    private static final Identifier SCREEN_QUAD_SHADER_ID = Identifier.withDefaultNamespace("core/screenquad");
    private static final int SAMPLER_INFO_SIZE = new Std140SizeCalculator().putVec2().putVec2().putVec2().get();
    private static final int DOF_CONFIG_SIZE = new Std140SizeCalculator().putMat4f().putVec4().putVec4().get();
    private static final float MAX_BLUR_PIXELS = 18.0F;
    private static final float BLUR_SCALE = 0.78F;
    private static final float FOREGROUND_BIAS = 1.20F;
    private static final Vector4fc CLEAR_COLOR = new Vector4f(0.0F, 0.0F, 0.0F, 0.0F);
    private static final RenderPipeline DOF_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(DOF_PIPELINE_ID)
            .withVertexShader(SCREEN_QUAD_SHADER_ID)
            .withFragmentShader(DOF_SHADER_ID)
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withSampler("ColorSampler")
                    .withSampler("DepthSampler")
                    .withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER)
                    .withUniform("DepthOfFieldConfig", UniformType.UNIFORM_BUFFER)
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
    private static final Matrix4f INVERSE_PROJECTION = new Matrix4f();
    @Nullable
    private static Resources resources;

    static {
        POST_PROJECTION.setupOrtho(0.1F, 1000.0F, 1.0F, 1.0F, false);
    }

    private PhotoModeDepthOfFieldRenderer() {
    }

    static void process(
            @NotNull RenderTarget mainRenderTarget,
            @NotNull GraphicsResourceAllocator resourceAllocator,
            @NotNull CameraRenderState cameraState,
            @NotNull Matrix4fc projectionMatrix,
            @NotNull PhotoModeManager.Session session
    ) {
        GpuTextureView mainColor = mainRenderTarget.getColorTextureView();
        GpuTextureView mainDepth = mainRenderTarget.getDepthTextureView();
        if (mainColor == null || mainDepth == null || mainRenderTarget.width <= 0 || mainRenderTarget.height <= 0 || !ensurePipelinesAvailable()) {
            return;
        }

        Resources renderResources = resources();
        writeUniforms(renderResources, mainRenderTarget, cameraState, projectionMatrix, session);
        FrameGraphBuilder frame = new FrameGraphBuilder();
        ResourceHandle<RenderTarget> main = frame.importExternal("panoramica main", mainRenderTarget);
        ResourceHandle<RenderTarget> dofTarget = frame.createInternal(
                "panoramica depth of field",
                new RenderTargetDescriptor(mainRenderTarget.width, mainRenderTarget.height, false, CLEAR_COLOR, GpuFormat.RGBA8_UNORM)
        );

        FramePass dofPass = frame.addPass("panoramica_depth_of_field");
        dofPass.reads(main);
        ResourceHandle<RenderTarget> dofOutput = dofPass.readsAndWrites(dofTarget);
        dofPass.executes(() -> drawDepthOfFieldPass(renderResources, main.get(), dofOutput.get()));

        FramePass copyPass = frame.addPass("panoramica_depth_of_field_copy");
        copyPass.reads(dofOutput);
        ResourceHandle<RenderTarget> mainOutput = copyPass.readsAndWrites(main);
        copyPass.executes(() -> drawCopyPass(renderResources, dofOutput.get(), mainOutput.get()));

        try {
            frame.execute(resourceAllocator);
        } finally {
            renderResources.samplerInfoBuffer.rotate();
            renderResources.dofConfigBuffer.rotate();
        }
    }

    static void close() {
        if (resources != null) {
            resources.close();
            resources = null;
        }
    }

    private static boolean ensurePipelinesAvailable() {
        return RenderSystem.getDevice().precompilePipeline(DOF_PIPELINE).isValid()
                && RenderSystem.getDevice().precompilePipeline(COPY_PIPELINE).isValid();
    }

    private static void writeUniforms(
            @NotNull Resources renderResources,
            @NotNull RenderTarget mainRenderTarget,
            @NotNull CameraRenderState cameraState,
            @NotNull Matrix4fc projectionMatrix,
            @NotNull PhotoModeManager.Session session
    ) {
        try (GpuBufferSlice.MappedView view = renderResources.samplerInfoBuffer.currentBuffer().map(false, true)) {
            Std140Builder builder = Std140Builder.intoBuffer(view.data());
            builder.putVec2(mainRenderTarget.width, mainRenderTarget.height);
            builder.putVec2(mainRenderTarget.width, mainRenderTarget.height);
            builder.putVec2(mainRenderTarget.width, mainRenderTarget.height);
        }

        INVERSE_PROJECTION.set(projectionMatrix).invert();
        try (GpuBufferSlice.MappedView view = renderResources.dofConfigBuffer.currentBuffer().map(false, true)) {
            Std140Builder builder = Std140Builder.intoBuffer(view.data());
            builder.putMat4f(INVERSE_PROJECTION);
            builder.putVec4(
                    session.depthOfFieldFocusDistance(),
                    session.depthOfFieldFocalLength(),
                    session.depthOfFieldAperture(),
                    MAX_BLUR_PIXELS
            );
            builder.putVec4(
                    Math.max(cameraState.depthFar, PhotoModeManager.DEPTH_OF_FIELD_FOCUS_DISTANCE_MAX),
                    RenderSystem.getDevice().getDeviceInfo().isZZeroToOne() ? 1.0F : 0.0F,
                    BLUR_SCALE,
                    FOREGROUND_BIAS
            );
        }
    }

    private static void drawDepthOfFieldPass(@NotNull Resources renderResources, @NotNull RenderTarget source, @NotNull RenderTarget output) {
        GpuTextureView sourceColor = source.getColorTextureView();
        GpuTextureView sourceDepth = source.getDepthTextureView();
        GpuTextureView outputColor = output.getColorTextureView();
        if (sourceColor == null || sourceDepth == null || outputColor == null) {
            return;
        }

        POST_PROJECTION.setSize(output.width, output.height);
        GpuBufferSlice projectionBuffer = renderResources.postProjectionMatrixBuffer.getBuffer(POST_PROJECTION);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projectionBuffer, ProjectionType.ORTHOGRAPHIC);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        SamplerCache samplerCache = RenderSystem.getSamplerCache();
        try (RenderPass renderPass = commandEncoder.createRenderPass(
                () -> "Panoramica photo depth of field",
                outputColor,
                Optional.empty(),
                null,
                OptionalDouble.empty()
        )) {
            renderPass.setPipeline(DOF_PIPELINE);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("SamplerInfo", renderResources.samplerInfoBuffer.currentBuffer());
            renderPass.setUniform("DepthOfFieldConfig", renderResources.dofConfigBuffer.currentBuffer());
            renderPass.bindTexture("ColorSampler", sourceColor, samplerCache.getClampToEdge(FilterMode.LINEAR));
            renderPass.bindTexture("DepthSampler", sourceDepth, samplerCache.getClampToEdge(FilterMode.NEAREST));
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
                () -> "Panoramica photo depth of field copy",
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

        private final ProjectionMatrixBuffer postProjectionMatrixBuffer = new ProjectionMatrixBuffer("panoramica photo dof");
        private final MappableRingBuffer samplerInfoBuffer = new MappableRingBuffer(() -> "Panoramica photo DOF SamplerInfo", 130, SAMPLER_INFO_SIZE);
        private final MappableRingBuffer dofConfigBuffer = new MappableRingBuffer(() -> "Panoramica photo DOF Config", 130, DOF_CONFIG_SIZE);

        @Override
        public void close() {
            this.postProjectionMatrixBuffer.close();
            this.samplerInfoBuffer.close();
            this.dofConfigBuffer.close();
        }

    }

}
