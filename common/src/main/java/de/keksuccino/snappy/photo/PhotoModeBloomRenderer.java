package de.keksuccino.snappy.photo;

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
import de.keksuccino.snappy.Snappy;
import de.keksuccino.snappy.client.render.ShaderEffectPass;
import de.keksuccino.snappy.client.render.config.BloomConfig;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector4fc;

import java.util.Optional;
import java.util.OptionalDouble;

final class PhotoModeBloomRenderer {

    private static final Identifier PREFILTER_PIPELINE_ID = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "pipeline/photo_bloom_prefilter");
    private static final Identifier DOWNSAMPLE_PIPELINE_ID = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "pipeline/photo_bloom_downsample");
    private static final Identifier BLUR_HORIZONTAL_PIPELINE_ID = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "pipeline/photo_bloom_blur_horizontal");
    private static final Identifier BLUR_VERTICAL_PIPELINE_ID = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "pipeline/photo_bloom_blur_vertical");
    private static final Identifier COMPOSITE_PIPELINE_ID = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "pipeline/photo_bloom_composite");
    private static final Identifier COPY_PIPELINE_ID = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "pipeline/photo_bloom_copy");
    private static final Identifier PREFILTER_SHADER_ID = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "post/bloom_prefilter");
    private static final Identifier DOWNSAMPLE_SHADER_ID = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "post/bloom_downsample");
    private static final Identifier BLUR_HORIZONTAL_SHADER_ID = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "post/bloom_blur_horizontal");
    private static final Identifier BLUR_VERTICAL_SHADER_ID = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "post/bloom_blur_vertical");
    private static final Identifier COMPOSITE_SHADER_ID = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "post/bloom_composite");
    private static final Identifier COPY_SHADER_ID = ShaderEffectPass.COPY_SHADER_ID;
    private static final Identifier SCREEN_QUAD_SHADER_ID = ShaderEffectPass.SCREEN_QUAD_SHADER_ID;
    private static final int SAMPLER_INFO_SIZE = ShaderEffectPass.SAMPLER_INFO_SIZE;
    private static final int COMPOSITE_SAMPLER_INFO_SIZE = new Std140SizeCalculator().putVec2().putVec2().putVec2().putVec2().get();
    private static final int BLOOM_CONFIG_SIZE = new Std140SizeCalculator().putVec4().get();
    private static final Vector4fc CLEAR_COLOR = ShaderEffectPass.CLEAR_COLOR;
    private static final RenderPipeline PREFILTER_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(PREFILTER_PIPELINE_ID)
            .withVertexShader(SCREEN_QUAD_SHADER_ID)
            .withFragmentShader(PREFILTER_SHADER_ID)
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withSampler("InSampler")
                    .withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER)
                    .withUniform("BloomConfig", UniformType.UNIFORM_BUFFER)
                    .build())
            .build();
    private static final RenderPipeline DOWNSAMPLE_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(DOWNSAMPLE_PIPELINE_ID)
            .withVertexShader(SCREEN_QUAD_SHADER_ID)
            .withFragmentShader(DOWNSAMPLE_SHADER_ID)
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withSampler("InSampler")
                    .withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER)
                    .build())
            .build();
    private static final RenderPipeline BLUR_HORIZONTAL_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(BLUR_HORIZONTAL_PIPELINE_ID)
            .withVertexShader(SCREEN_QUAD_SHADER_ID)
            .withFragmentShader(BLUR_HORIZONTAL_SHADER_ID)
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withSampler("InSampler")
                    .withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER)
                    .build())
            .build();
    private static final RenderPipeline BLUR_VERTICAL_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(BLUR_VERTICAL_PIPELINE_ID)
            .withVertexShader(SCREEN_QUAD_SHADER_ID)
            .withFragmentShader(BLUR_VERTICAL_SHADER_ID)
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withSampler("InSampler")
                    .withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER)
                    .build())
            .build();
    private static final RenderPipeline COMPOSITE_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(COMPOSITE_PIPELINE_ID)
            .withVertexShader(SCREEN_QUAD_SHADER_ID)
            .withFragmentShader(COMPOSITE_SHADER_ID)
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withSampler("SceneSampler")
                    .withSampler("BloomNearSampler")
                    .withSampler("BloomWideSampler")
                    .withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER)
                    .withUniform("BloomConfig", UniformType.UNIFORM_BUFFER)
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

    private PhotoModeBloomRenderer() {
    }

    static void process(
            @NotNull RenderTarget mainRenderTarget,
            @NotNull GraphicsResourceAllocator resourceAllocator,
            @NotNull BloomConfig config
    ) {
        GpuTextureView mainColor = mainRenderTarget.getColorTextureView();
        if (mainColor == null || mainRenderTarget.width <= 0 || mainRenderTarget.height <= 0 || !ensurePipelinesAvailable()) {
            return;
        }

        int halfWidth = Math.max(1, mainRenderTarget.width / 2);
        int halfHeight = Math.max(1, mainRenderTarget.height / 2);
        int quarterWidth = Math.max(1, mainRenderTarget.width / 4);
        int quarterHeight = Math.max(1, mainRenderTarget.height / 4);

        Resources renderResources = resources();
        writeBloomConfig(renderResources, config);
        FrameGraphBuilder frame = new FrameGraphBuilder();
        ResourceHandle<RenderTarget> main = frame.importExternal("snappy main", mainRenderTarget);
        ResourceHandle<RenderTarget> halfPrefilterTarget = frame.createInternal(
                "snappy bloom half prefilter",
                descriptor(halfWidth, halfHeight)
        );
        ResourceHandle<RenderTarget> halfBlurTarget = frame.createInternal(
                "snappy bloom half blur",
                descriptor(halfWidth, halfHeight)
        );
        ResourceHandle<RenderTarget> halfBloomTarget = frame.createInternal(
                "snappy bloom half",
                descriptor(halfWidth, halfHeight)
        );
        ResourceHandle<RenderTarget> quarterPrefilterTarget = frame.createInternal(
                "snappy bloom quarter prefilter",
                descriptor(quarterWidth, quarterHeight)
        );
        ResourceHandle<RenderTarget> quarterBlurTarget = frame.createInternal(
                "snappy bloom quarter blur",
                descriptor(quarterWidth, quarterHeight)
        );
        ResourceHandle<RenderTarget> quarterBloomTarget = frame.createInternal(
                "snappy bloom quarter",
                descriptor(quarterWidth, quarterHeight)
        );
        ResourceHandle<RenderTarget> compositeTarget = frame.createInternal(
                "snappy bloom composite",
                descriptor(mainRenderTarget.width, mainRenderTarget.height)
        );

        FramePass prefilterPass = frame.addPass("snappy_bloom_prefilter");
        prefilterPass.reads(main);
        ResourceHandle<RenderTarget> halfPrefilterOutput = prefilterPass.readsAndWrites(halfPrefilterTarget);
        prefilterPass.executes(() -> drawSingleInputPass(
                renderResources,
                renderResources.prefilterSamplerInfoBuffer,
                PREFILTER_PIPELINE,
                main.get(),
                halfPrefilterOutput.get(),
                "Snappy photo bloom prefilter",
                true
        ));

        FramePass halfBlurHorizontalPass = frame.addPass("snappy_bloom_half_blur_horizontal");
        halfBlurHorizontalPass.reads(halfPrefilterOutput);
        ResourceHandle<RenderTarget> halfBlurOutput = halfBlurHorizontalPass.readsAndWrites(halfBlurTarget);
        halfBlurHorizontalPass.executes(() -> drawSingleInputPass(
                renderResources,
                renderResources.halfBlurHorizontalSamplerInfoBuffer,
                BLUR_HORIZONTAL_PIPELINE,
                halfPrefilterOutput.get(),
                halfBlurOutput.get(),
                "Snappy photo bloom half horizontal",
                false
        ));

        FramePass halfBlurVerticalPass = frame.addPass("snappy_bloom_half_blur_vertical");
        halfBlurVerticalPass.reads(halfBlurOutput);
        ResourceHandle<RenderTarget> halfBloomOutput = halfBlurVerticalPass.readsAndWrites(halfBloomTarget);
        halfBlurVerticalPass.executes(() -> drawSingleInputPass(
                renderResources,
                renderResources.halfBlurVerticalSamplerInfoBuffer,
                BLUR_VERTICAL_PIPELINE,
                halfBlurOutput.get(),
                halfBloomOutput.get(),
                "Snappy photo bloom half vertical",
                false
        ));

        FramePass downsamplePass = frame.addPass("snappy_bloom_downsample");
        downsamplePass.reads(halfBloomOutput);
        ResourceHandle<RenderTarget> quarterPrefilterOutput = downsamplePass.readsAndWrites(quarterPrefilterTarget);
        downsamplePass.executes(() -> drawSingleInputPass(
                renderResources,
                renderResources.downsampleSamplerInfoBuffer,
                DOWNSAMPLE_PIPELINE,
                halfBloomOutput.get(),
                quarterPrefilterOutput.get(),
                "Snappy photo bloom downsample",
                false
        ));

        FramePass quarterBlurHorizontalPass = frame.addPass("snappy_bloom_quarter_blur_horizontal");
        quarterBlurHorizontalPass.reads(quarterPrefilterOutput);
        ResourceHandle<RenderTarget> quarterBlurOutput = quarterBlurHorizontalPass.readsAndWrites(quarterBlurTarget);
        quarterBlurHorizontalPass.executes(() -> drawSingleInputPass(
                renderResources,
                renderResources.quarterBlurHorizontalSamplerInfoBuffer,
                BLUR_HORIZONTAL_PIPELINE,
                quarterPrefilterOutput.get(),
                quarterBlurOutput.get(),
                "Snappy photo bloom quarter horizontal",
                false
        ));

        FramePass quarterBlurVerticalPass = frame.addPass("snappy_bloom_quarter_blur_vertical");
        quarterBlurVerticalPass.reads(quarterBlurOutput);
        ResourceHandle<RenderTarget> quarterBloomOutput = quarterBlurVerticalPass.readsAndWrites(quarterBloomTarget);
        quarterBlurVerticalPass.executes(() -> drawSingleInputPass(
                renderResources,
                renderResources.quarterBlurVerticalSamplerInfoBuffer,
                BLUR_VERTICAL_PIPELINE,
                quarterBlurOutput.get(),
                quarterBloomOutput.get(),
                "Snappy photo bloom quarter vertical",
                false
        ));

        FramePass compositePass = frame.addPass("snappy_bloom_composite");
        compositePass.reads(main);
        compositePass.reads(halfBloomOutput);
        compositePass.reads(quarterBloomOutput);
        ResourceHandle<RenderTarget> compositeOutput = compositePass.readsAndWrites(compositeTarget);
        compositePass.executes(() -> drawCompositePass(renderResources, main.get(), halfBloomOutput.get(), quarterBloomOutput.get(), compositeOutput.get()));

        FramePass copyPass = frame.addPass("snappy_bloom_copy");
        copyPass.reads(compositeOutput);
        ResourceHandle<RenderTarget> mainOutput = copyPass.readsAndWrites(main);
        copyPass.executes(() -> drawCopyPass(renderResources, compositeOutput.get(), mainOutput.get()));

        try {
            frame.execute(resourceAllocator);
        } finally {
            renderResources.bloomConfigBuffer.rotate();
        }
    }

    static void close() {
        if (resources != null) {
            resources.close();
            resources = null;
        }
    }

    private static boolean ensurePipelinesAvailable() {
        return ShaderEffectPass.allPipelinesAvailable(
                PREFILTER_PIPELINE,
                DOWNSAMPLE_PIPELINE,
                BLUR_HORIZONTAL_PIPELINE,
                BLUR_VERTICAL_PIPELINE,
                COMPOSITE_PIPELINE,
                COPY_PIPELINE
        );
    }

    @NotNull
    private static RenderTargetDescriptor descriptor(int width, int height) {
        return new RenderTargetDescriptor(width, height, false, CLEAR_COLOR, GpuFormat.RGBA8_UNORM);
    }

    private static void writeBloomConfig(@NotNull Resources renderResources, @NotNull BloomConfig config) {
        float amount = config.intensity();
        float threshold = 0.82F - amount * 0.24F;
        float knee = 0.18F + amount * 0.10F;
        float compositeGain = 0.42F + amount * 1.08F;
        try (GpuBufferSlice.MappedView view = renderResources.bloomConfigBuffer.currentBuffer().map(false, true)) {
            Std140Builder builder = Std140Builder.intoBuffer(view.data());
            builder.putVec4(amount, threshold, knee, compositeGain);
        }
    }

    private static void writeSamplerInfo(
            @NotNull MappableRingBuffer samplerInfoBuffer,
            @NotNull RenderTarget output,
            @NotNull RenderTarget input
    ) {
        try (GpuBufferSlice.MappedView view = samplerInfoBuffer.currentBuffer().map(false, true)) {
            Std140Builder builder = Std140Builder.intoBuffer(view.data());
            builder.putVec2(output.width, output.height);
            builder.putVec2(input.width, input.height);
        }
    }

    private static void writeCompositeSamplerInfo(
            @NotNull Resources renderResources,
            @NotNull RenderTarget output,
            @NotNull RenderTarget scene,
            @NotNull RenderTarget nearBloom,
            @NotNull RenderTarget wideBloom
    ) {
        try (GpuBufferSlice.MappedView view = renderResources.compositeSamplerInfoBuffer.currentBuffer().map(false, true)) {
            Std140Builder builder = Std140Builder.intoBuffer(view.data());
            builder.putVec2(output.width, output.height);
            builder.putVec2(scene.width, scene.height);
            builder.putVec2(nearBloom.width, nearBloom.height);
            builder.putVec2(wideBloom.width, wideBloom.height);
        }
    }

    private static void drawSingleInputPass(
            @NotNull Resources renderResources,
            @NotNull MappableRingBuffer samplerInfoBuffer,
            @NotNull RenderPipeline pipeline,
            @NotNull RenderTarget source,
            @NotNull RenderTarget output,
            @NotNull String name,
            boolean bindBloomConfig
    ) {
        GpuTextureView sourceColor = source.getColorTextureView();
        GpuTextureView outputColor = output.getColorTextureView();
        if (sourceColor == null || outputColor == null) {
            return;
        }

        writeSamplerInfo(samplerInfoBuffer, output, source);
        POST_PROJECTION.setSize(output.width, output.height);
        GpuBufferSlice projectionBuffer = renderResources.postProjectionMatrixBuffer.getBuffer(POST_PROJECTION);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projectionBuffer, ProjectionType.ORTHOGRAPHIC);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        try (RenderPass renderPass = commandEncoder.createRenderPass(
                () -> name,
                outputColor,
                Optional.empty(),
                null,
                OptionalDouble.empty()
        )) {
            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("SamplerInfo", samplerInfoBuffer.currentBuffer());
            if (bindBloomConfig) {
                renderPass.setUniform("BloomConfig", renderResources.bloomConfigBuffer.currentBuffer());
            }
            renderPass.bindTexture("InSampler", sourceColor, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
            renderPass.draw(3, 1, 0, 0);
        } finally {
            samplerInfoBuffer.rotate();
            RenderSystem.restoreProjectionMatrix();
        }
    }

    private static void drawCompositePass(
            @NotNull Resources renderResources,
            @NotNull RenderTarget scene,
            @NotNull RenderTarget nearBloom,
            @NotNull RenderTarget wideBloom,
            @NotNull RenderTarget output
    ) {
        GpuTextureView sceneColor = scene.getColorTextureView();
        GpuTextureView nearBloomColor = nearBloom.getColorTextureView();
        GpuTextureView wideBloomColor = wideBloom.getColorTextureView();
        GpuTextureView outputColor = output.getColorTextureView();
        if (sceneColor == null || nearBloomColor == null || wideBloomColor == null || outputColor == null) {
            return;
        }

        writeCompositeSamplerInfo(renderResources, output, scene, nearBloom, wideBloom);
        POST_PROJECTION.setSize(output.width, output.height);
        GpuBufferSlice projectionBuffer = renderResources.postProjectionMatrixBuffer.getBuffer(POST_PROJECTION);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projectionBuffer, ProjectionType.ORTHOGRAPHIC);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        SamplerCache samplerCache = RenderSystem.getSamplerCache();
        try (RenderPass renderPass = commandEncoder.createRenderPass(
                () -> "Snappy photo bloom composite",
                outputColor,
                Optional.empty(),
                null,
                OptionalDouble.empty()
        )) {
            renderPass.setPipeline(COMPOSITE_PIPELINE);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("SamplerInfo", renderResources.compositeSamplerInfoBuffer.currentBuffer());
            renderPass.setUniform("BloomConfig", renderResources.bloomConfigBuffer.currentBuffer());
            renderPass.bindTexture("SceneSampler", sceneColor, samplerCache.getClampToEdge(FilterMode.NEAREST));
            renderPass.bindTexture("BloomNearSampler", nearBloomColor, samplerCache.getClampToEdge(FilterMode.LINEAR));
            renderPass.bindTexture("BloomWideSampler", wideBloomColor, samplerCache.getClampToEdge(FilterMode.LINEAR));
            renderPass.draw(3, 1, 0, 0);
        } finally {
            renderResources.compositeSamplerInfoBuffer.rotate();
            RenderSystem.restoreProjectionMatrix();
        }
    }

    private static void drawCopyPass(@NotNull Resources renderResources, @NotNull RenderTarget source, @NotNull RenderTarget output) {
        GpuTextureView sourceColor = source.getColorTextureView();
        GpuTextureView outputColor = output.getColorTextureView();
        if (sourceColor == null || outputColor == null) {
            return;
        }

        writeSamplerInfo(renderResources.copySamplerInfoBuffer, output, source);
        POST_PROJECTION.setSize(output.width, output.height);
        GpuBufferSlice projectionBuffer = renderResources.postProjectionMatrixBuffer.getBuffer(POST_PROJECTION);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projectionBuffer, ProjectionType.ORTHOGRAPHIC);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        try (RenderPass renderPass = commandEncoder.createRenderPass(
                () -> "Snappy photo bloom copy",
                outputColor,
                Optional.empty(),
                null,
                OptionalDouble.empty()
        )) {
            renderPass.setPipeline(COPY_PIPELINE);
            RenderSystem.bindDefaultUniforms(renderPass);
            renderPass.setUniform("SamplerInfo", renderResources.copySamplerInfoBuffer.currentBuffer());
            renderPass.bindTexture("InSampler", sourceColor, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
            renderPass.draw(3, 1, 0, 0);
        } finally {
            renderResources.copySamplerInfoBuffer.rotate();
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

        private final ProjectionMatrixBuffer postProjectionMatrixBuffer = new ProjectionMatrixBuffer("snappy photo bloom");
        private final MappableRingBuffer prefilterSamplerInfoBuffer = samplerInfoBuffer("prefilter");
        private final MappableRingBuffer halfBlurHorizontalSamplerInfoBuffer = samplerInfoBuffer("half horizontal");
        private final MappableRingBuffer halfBlurVerticalSamplerInfoBuffer = samplerInfoBuffer("half vertical");
        private final MappableRingBuffer downsampleSamplerInfoBuffer = samplerInfoBuffer("downsample");
        private final MappableRingBuffer quarterBlurHorizontalSamplerInfoBuffer = samplerInfoBuffer("quarter horizontal");
        private final MappableRingBuffer quarterBlurVerticalSamplerInfoBuffer = samplerInfoBuffer("quarter vertical");
        private final MappableRingBuffer copySamplerInfoBuffer = samplerInfoBuffer("copy");
        private final MappableRingBuffer compositeSamplerInfoBuffer = new MappableRingBuffer(() -> "Snappy photo bloom Composite SamplerInfo", 130, COMPOSITE_SAMPLER_INFO_SIZE);
        private final MappableRingBuffer bloomConfigBuffer = new MappableRingBuffer(() -> "Snappy photo bloom Config", 130, BLOOM_CONFIG_SIZE);

        @Override
        public void close() {
            this.postProjectionMatrixBuffer.close();
            this.prefilterSamplerInfoBuffer.close();
            this.halfBlurHorizontalSamplerInfoBuffer.close();
            this.halfBlurVerticalSamplerInfoBuffer.close();
            this.downsampleSamplerInfoBuffer.close();
            this.quarterBlurHorizontalSamplerInfoBuffer.close();
            this.quarterBlurVerticalSamplerInfoBuffer.close();
            this.copySamplerInfoBuffer.close();
            this.compositeSamplerInfoBuffer.close();
            this.bloomConfigBuffer.close();
        }

        @NotNull
        private static MappableRingBuffer samplerInfoBuffer(@NotNull String passName) {
            return new MappableRingBuffer(() -> "Snappy photo bloom " + passName + " SamplerInfo", 130, SAMPLER_INFO_SIZE);
        }

    }

}
