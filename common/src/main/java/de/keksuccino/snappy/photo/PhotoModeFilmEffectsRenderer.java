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
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import de.keksuccino.snappy.Snappy;
import de.keksuccino.snappy.client.render.ShaderEffectPass;
import de.keksuccino.snappy.client.render.config.FilmEffectsConfig;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class PhotoModeFilmEffectsRenderer {

    private static final Identifier FILM_EFFECTS_PIPELINE_ID = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "pipeline/photo_film_effects");
    private static final Identifier COPY_PIPELINE_ID = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "pipeline/photo_film_effects_copy");
    private static final Identifier FILM_EFFECTS_SHADER_ID = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "post/film_effects");
    private static final int FILM_EFFECTS_CONFIG_SIZE = new Std140SizeCalculator().putVec4().get();
    private static final RenderPipeline FILM_EFFECTS_PIPELINE = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
            .withLocation(FILM_EFFECTS_PIPELINE_ID)
            .withVertexShader(ShaderEffectPass.SCREEN_QUAD_SHADER_ID)
            .withFragmentShader(FILM_EFFECTS_SHADER_ID)
            .withBindGroupLayout(BindGroupLayout.builder()
                    .withSampler("InSampler")
                    .withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER)
                    .withUniform("FilmEffectsConfig", UniformType.UNIFORM_BUFFER)
                    .build())
            .build();
    private static final RenderPipeline COPY_PIPELINE = ShaderEffectPass.buildCopyPipeline(COPY_PIPELINE_ID);
    @Nullable
    private static Resources resources;

    private PhotoModeFilmEffectsRenderer() {
    }

    static void process(
            @NotNull RenderTarget mainRenderTarget,
            @NotNull GraphicsResourceAllocator resourceAllocator,
            @NotNull FilmEffectsConfig config
    ) {
        if (!config.active() || !ShaderEffectPass.hasUsableColorTarget(mainRenderTarget) || !ensurePipelinesAvailable()) {
            return;
        }

        Resources renderResources = resources();
        FrameGraphBuilder frame = new FrameGraphBuilder();
        ResourceHandle<RenderTarget> main = frame.importExternal("snappy main", mainRenderTarget);
        ResourceHandle<RenderTarget> filmEffectsTarget = frame.createInternal(
                "snappy film effects",
                ShaderEffectPass.colorTargetDescriptor(mainRenderTarget)
        );

        FramePass filmEffectsPass = frame.addPass("snappy_film_effects");
        filmEffectsPass.reads(main);
        ResourceHandle<RenderTarget> filmEffectsOutput = filmEffectsPass.readsAndWrites(filmEffectsTarget);
        filmEffectsPass.executes(() -> {
            writeUniforms(renderResources, main.get(), filmEffectsOutput.get(), config);
            drawFilmEffectsPass(renderResources, main.get(), filmEffectsOutput.get());
        });

        FramePass copyPass = frame.addPass("snappy_film_effects_copy");
        copyPass.reads(filmEffectsOutput);
        ResourceHandle<RenderTarget> mainOutput = copyPass.readsAndWrites(main);
        copyPass.executes(() -> drawCopyPass(renderResources, filmEffectsOutput.get(), mainOutput.get()));

        try {
            frame.execute(resourceAllocator);
        } finally {
            renderResources.samplerInfoBuffer.rotate();
            renderResources.filmEffectsConfigBuffer.rotate();
        }
    }

    static void close() {
        if (resources != null) {
            resources.close();
            resources = null;
        }
    }

    private static boolean ensurePipelinesAvailable() {
        return ShaderEffectPass.allPipelinesAvailable(FILM_EFFECTS_PIPELINE, COPY_PIPELINE);
    }

    private static void writeUniforms(
            @NotNull Resources renderResources,
            @NotNull RenderTarget source,
            @NotNull RenderTarget output,
            @NotNull FilmEffectsConfig config
    ) {
        ShaderEffectPass.writeSamplerInfo(renderResources.samplerInfoBuffer, source, output);

        float grainSeed = Math.floorMod(System.nanoTime(), 4096L) / 4096.0F;
        try (GpuBufferSlice.MappedView view = renderResources.filmEffectsConfigBuffer.currentBuffer().map(false, true)) {
            Std140Builder builder = Std140Builder.intoBuffer(view.data());
            builder.putVec4(config.filmGrain(), config.chromaticAberration(), grainSeed, 0.0F);
        }
    }

    private static void drawFilmEffectsPass(@NotNull Resources renderResources, @NotNull RenderTarget source, @NotNull RenderTarget output) {
        GpuTextureView sourceColor = source.getColorTextureView();
        GpuTextureView outputColor = output.getColorTextureView();
        if (sourceColor == null || outputColor == null) {
            return;
        }

        ShaderEffectPass.drawScreenQuad("Snappy photo film effects", renderResources.postProjectionMatrixBuffer, FILM_EFFECTS_PIPELINE, output, renderPass -> {
            renderPass.setUniform("SamplerInfo", renderResources.samplerInfoBuffer.currentBuffer());
            renderPass.setUniform("FilmEffectsConfig", renderResources.filmEffectsConfigBuffer.currentBuffer());
            renderPass.bindTexture("InSampler", sourceColor, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
        });
    }

    private static void drawCopyPass(@NotNull Resources renderResources, @NotNull RenderTarget source, @NotNull RenderTarget output) {
        ShaderEffectPass.drawCopyPass(
                "Snappy photo film effects copy",
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

        private final ProjectionMatrixBuffer postProjectionMatrixBuffer = new ProjectionMatrixBuffer("snappy photo film effects");
        private final MappableRingBuffer samplerInfoBuffer = new MappableRingBuffer(() -> "Snappy photo film effects SamplerInfo", 130, ShaderEffectPass.SAMPLER_INFO_SIZE);
        private final MappableRingBuffer filmEffectsConfigBuffer = new MappableRingBuffer(() -> "Snappy photo film effects Config", 130, FILM_EFFECTS_CONFIG_SIZE);

        @Override
        public void close() {
            this.postProjectionMatrixBuffer.close();
            this.samplerInfoBuffer.close();
            this.filmEffectsConfigBuffer.close();
        }

    }

}
