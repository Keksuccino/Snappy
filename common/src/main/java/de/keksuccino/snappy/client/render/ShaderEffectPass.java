package de.keksuccino.snappy.client.render;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.buffers.Std140SizeCalculator;
import com.mojang.blaze3d.pipeline.BindGroupLayout;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.resource.RenderTargetDescriptor;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTextureView;
import de.keksuccino.snappy.Snappy;
import net.minecraft.client.renderer.MappableRingBuffer;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector4f;
import org.joml.Vector4fc;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.function.Consumer;

public final class ShaderEffectPass {

    public static final Identifier COPY_SHADER_ID = Identifier.fromNamespaceAndPath(Snappy.MOD_ID, "post/copy");
    public static final Identifier SCREEN_QUAD_SHADER_ID = Identifier.withDefaultNamespace("core/screenquad");
    public static final int SAMPLER_INFO_SIZE = new Std140SizeCalculator().putVec2().putVec2().get();
    public static final Vector4fc CLEAR_COLOR = new Vector4f(0.0F, 0.0F, 0.0F, 0.0F);

    private static final Projection POST_PROJECTION = new Projection();

    static {
        POST_PROJECTION.setupOrtho(0.1F, 1000.0F, 1.0F, 1.0F, false);
    }

    private ShaderEffectPass() {
    }

    @NotNull
    public static RenderPipeline buildCopyPipeline(@NotNull Identifier pipelineId) {
        return RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                .withLocation(pipelineId)
                .withVertexShader(SCREEN_QUAD_SHADER_ID)
                .withFragmentShader(COPY_SHADER_ID)
                .withBindGroupLayout(BindGroupLayout.builder()
                        .withSampler("InSampler")
                        .withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER)
                        .build())
                .build();
    }

    @NotNull
    public static RenderTargetDescriptor colorTargetDescriptor(@NotNull RenderTarget source) {
        return new RenderTargetDescriptor(source.width, source.height, false, CLEAR_COLOR, GpuFormat.RGBA8_UNORM);
    }

    public static boolean hasUsableColorTarget(@NotNull RenderTarget target) {
        return target.getColorTextureView() != null && target.width > 0 && target.height > 0;
    }

    public static boolean allPipelinesAvailable(RenderPipeline @NotNull ... pipelines) {
        for (RenderPipeline pipeline : pipelines) {
            if (!RenderSystem.getDevice().precompilePipeline(pipeline).isValid()) {
                return false;
            }
        }
        return true;
    }

    public static void writeSamplerInfo(@NotNull MappableRingBuffer samplerInfoBuffer, @NotNull RenderTarget source, @NotNull RenderTarget output) {
        try (GpuBufferSlice.MappedView view = samplerInfoBuffer.currentBuffer().map(false, true)) {
            Std140Builder builder = Std140Builder.intoBuffer(view.data());
            builder.putVec2(source.width, source.height);
            builder.putVec2(output.width, output.height);
        }
    }

    public static void drawScreenQuad(
            @NotNull String label,
            @NotNull ProjectionMatrixBuffer projectionMatrixBuffer,
            @NotNull RenderPipeline pipeline,
            @NotNull RenderTarget output,
            @NotNull Consumer<RenderPass> uniformAndTextureBinder
    ) {
        GpuTextureView outputColor = output.getColorTextureView();
        if (outputColor == null) {
            return;
        }

        POST_PROJECTION.setSize(output.width, output.height);
        GpuBufferSlice projectionBuffer = projectionMatrixBuffer.getBuffer(POST_PROJECTION);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(projectionBuffer, ProjectionType.ORTHOGRAPHIC);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();
        try (RenderPass renderPass = commandEncoder.createRenderPass(
                () -> label,
                outputColor,
                Optional.empty(),
                null,
                OptionalDouble.empty()
        )) {
            renderPass.setPipeline(pipeline);
            RenderSystem.bindDefaultUniforms(renderPass);
            uniformAndTextureBinder.accept(renderPass);
            renderPass.draw(3, 1, 0, 0);
        } finally {
            RenderSystem.restoreProjectionMatrix();
        }
    }

    public static void drawSingleSamplerPass(
            @NotNull String label,
            @NotNull ProjectionMatrixBuffer projectionMatrixBuffer,
            @NotNull MappableRingBuffer samplerInfoBuffer,
            @NotNull RenderPipeline pipeline,
            @NotNull RenderTarget source,
            @NotNull RenderTarget output,
            @NotNull Consumer<RenderPass> customUniformBinder
    ) {
        GpuTextureView sourceColor = source.getColorTextureView();
        GpuTextureView outputColor = output.getColorTextureView();
        if (sourceColor == null || outputColor == null) {
            return;
        }

        drawScreenQuad(label, projectionMatrixBuffer, pipeline, output, renderPass -> {
            renderPass.setUniform("SamplerInfo", samplerInfoBuffer.currentBuffer());
            customUniformBinder.accept(renderPass);
            renderPass.bindTexture("InSampler", sourceColor, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
        });
    }

    public static void drawCopyPass(
            @NotNull String label,
            @NotNull ProjectionMatrixBuffer projectionMatrixBuffer,
            @NotNull MappableRingBuffer samplerInfoBuffer,
            @NotNull RenderPipeline copyPipeline,
            @NotNull RenderTarget source,
            @NotNull RenderTarget output
    ) {
        drawSingleSamplerPass(label, projectionMatrixBuffer, samplerInfoBuffer, copyPipeline, source, output, renderPass -> {
        });
    }

}
