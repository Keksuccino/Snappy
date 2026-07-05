package de.keksuccino.snappy.preview;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.PrimitiveTopology;
import com.mojang.blaze3d.ProjectionType;
import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.RenderPipelines;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector4f;

import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;

public final class PreviewCubeMapRenderer implements AutoCloseable {

    private static final float PROJECTION_Z_NEAR = 0.05F;
    private static final float PROJECTION_Z_FAR = 10.0F;
    private static final float PROJECTION_FOV = 85.0F;
    private static final Vector4f CLEAR_COLOR = new Vector4f(0.0F, 0.0F, 0.0F, 1.0F);
    public static final float DEFAULT_ROT_X_IN_DEGREES = 10.0F;

    private final int defaultWidth;
    private final int defaultHeight;
    private final Projection projection = new Projection();
    @Nullable
    private ProjectionMatrixBuffer projectionMatrixBuffer;
    @Nullable
    private GpuBuffer vertexBuffer;
    @Nullable
    private RenderTarget target;

    public PreviewCubeMapRenderer(int width, int height) {
        this.defaultWidth = Math.max(1, width);
        this.defaultHeight = Math.max(1, height);
    }

    public void render(@NotNull PreviewCubeMapTexture texture, float rotYInDegrees) {
        this.render(texture, this.defaultWidth, this.defaultHeight, DEFAULT_ROT_X_IN_DEGREES, rotYInDegrees);
    }

    public void render(@NotNull PreviewCubeMapTexture texture, float rotXInDegrees, float rotYInDegrees) {
        this.render(texture, this.defaultWidth, this.defaultHeight, rotXInDegrees, rotYInDegrees);
    }

    public void render(@NotNull PreviewCubeMapTexture texture, int width, int height, float rotXInDegrees, float rotYInDegrees) {
        RenderTarget renderTarget = this.getOrCreateTarget(width, height);
        this.projection.setupPerspective(PROJECTION_Z_NEAR, PROJECTION_Z_FAR, PROJECTION_FOV, renderTarget.width, renderTarget.height);

        RenderSystem.backupProjectionMatrix();
        try {
            RenderSystem.setProjectionMatrix(this.getOrCreateProjectionMatrixBuffer().getBuffer(this.projection), ProjectionType.PERSPECTIVE);
            RenderPipeline renderPipeline = RenderPipelines.PANORAMA;
            RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
            GpuBuffer indexBuffer = indices.getBuffer(36);
            Matrix4fStack modelViewStack = RenderSystem.getModelViewStack();
            modelViewStack.pushMatrix();
            GpuBufferSlice dynamicTransforms;
            try {
                modelViewStack.rotationX((float) Math.PI);
                modelViewStack.rotateX(rotXInDegrees * (float) (Math.PI / 180.0));
                modelViewStack.rotateY(rotYInDegrees * (float) (Math.PI / 180.0));
                dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(new Matrix4f(modelViewStack));
            } finally {
                modelViewStack.popMatrix();
            }

            try (RenderPass renderPass = RenderSystem.getDevice()
                    .createCommandEncoder()
                    .createRenderPass(
                            () -> "Snappy panorama screenshot preview",
                            Objects.requireNonNull(renderTarget.getColorTextureView()),
                            Optional.of(CLEAR_COLOR),
                            null,
                            OptionalDouble.empty()
                    )) {
                renderPass.setPipeline(renderPipeline);
                RenderSystem.bindDefaultUniforms(renderPass);
                renderPass.setVertexBuffer(0, this.getOrCreateVertexBuffer().slice());
                renderPass.setIndexBuffer(indexBuffer, indices.type());
                renderPass.setUniform("DynamicTransforms", dynamicTransforms);
                renderPass.bindTexture("Sampler0", texture.getTextureView(), texture.getSampler());
                renderPass.drawIndexed(36, 1, 0, 0, 0);
            }
        } finally {
            RenderSystem.restoreProjectionMatrix();
        }
    }

    @NotNull
    public GpuTextureView getColorTextureView() {
        RenderTarget renderTarget = this.target;
        if (renderTarget == null) {
            renderTarget = this.getOrCreateTarget(this.defaultWidth, this.defaultHeight);
        }
        return Objects.requireNonNull(renderTarget.getColorTextureView());
    }

    @NotNull
    private RenderTarget getOrCreateTarget(int width, int height) {
        int targetWidth = Math.max(1, width);
        int targetHeight = Math.max(1, height);
        RenderTarget renderTarget = this.target;
        if (renderTarget == null) {
            renderTarget = new TextureTarget("Snappy Panorama Preview", targetWidth, targetHeight, false, GpuFormat.RGBA8_UNORM);
            this.target = renderTarget;
        } else if (renderTarget.width != targetWidth || renderTarget.height != targetHeight) {
            renderTarget.resize(targetWidth, targetHeight);
        }
        return renderTarget;
    }

    @NotNull
    private ProjectionMatrixBuffer getOrCreateProjectionMatrixBuffer() {
        ProjectionMatrixBuffer buffer = this.projectionMatrixBuffer;
        if (buffer == null) {
            buffer = new ProjectionMatrixBuffer("snappy_preview_cubemap");
            this.projectionMatrixBuffer = buffer;
        }
        return buffer;
    }

    @NotNull
    private GpuBuffer getOrCreateVertexBuffer() {
        GpuBuffer buffer = this.vertexBuffer;
        if (buffer == null) {
            buffer = initializeVertices();
            this.vertexBuffer = buffer;
        }
        return buffer;
    }

    @NotNull
    private static GpuBuffer initializeVertices() {
        try (ByteBufferBuilder byteBufferBuilder = ByteBufferBuilder.exactlySized(DefaultVertexFormat.POSITION.getVertexSize() * 4 * 6)) {
            BufferBuilder bufferBuilder = new BufferBuilder(byteBufferBuilder, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION);
            bufferBuilder.addVertex(-1.0F, -1.0F, 1.0F);
            bufferBuilder.addVertex(-1.0F, 1.0F, 1.0F);
            bufferBuilder.addVertex(1.0F, 1.0F, 1.0F);
            bufferBuilder.addVertex(1.0F, -1.0F, 1.0F);
            bufferBuilder.addVertex(1.0F, -1.0F, 1.0F);
            bufferBuilder.addVertex(1.0F, 1.0F, 1.0F);
            bufferBuilder.addVertex(1.0F, 1.0F, -1.0F);
            bufferBuilder.addVertex(1.0F, -1.0F, -1.0F);
            bufferBuilder.addVertex(1.0F, -1.0F, -1.0F);
            bufferBuilder.addVertex(1.0F, 1.0F, -1.0F);
            bufferBuilder.addVertex(-1.0F, 1.0F, -1.0F);
            bufferBuilder.addVertex(-1.0F, -1.0F, -1.0F);
            bufferBuilder.addVertex(-1.0F, -1.0F, -1.0F);
            bufferBuilder.addVertex(-1.0F, 1.0F, -1.0F);
            bufferBuilder.addVertex(-1.0F, 1.0F, 1.0F);
            bufferBuilder.addVertex(-1.0F, -1.0F, 1.0F);
            bufferBuilder.addVertex(-1.0F, -1.0F, -1.0F);
            bufferBuilder.addVertex(-1.0F, -1.0F, 1.0F);
            bufferBuilder.addVertex(1.0F, -1.0F, 1.0F);
            bufferBuilder.addVertex(1.0F, -1.0F, -1.0F);
            bufferBuilder.addVertex(-1.0F, 1.0F, 1.0F);
            bufferBuilder.addVertex(-1.0F, 1.0F, -1.0F);
            bufferBuilder.addVertex(1.0F, 1.0F, -1.0F);
            bufferBuilder.addVertex(1.0F, 1.0F, 1.0F);

            try (MeshData meshData = bufferBuilder.buildOrThrow()) {
                return RenderSystem.getDevice().createBuffer(() -> "Snappy panorama preview cubemap vertices", 32, meshData.vertexBuffer());
            }
        }
    }

    @Override
    public void close() {
        RenderTarget renderTarget = this.target;
        this.target = null;
        if (renderTarget != null) {
            renderTarget.destroyBuffers();
        }

        GpuBuffer buffer = this.vertexBuffer;
        this.vertexBuffer = null;
        if (buffer != null) {
            buffer.close();
        }

        ProjectionMatrixBuffer projectionBuffer = this.projectionMatrixBuffer;
        this.projectionMatrixBuffer = null;
        if (projectionBuffer != null) {
            projectionBuffer.close();
        }
    }

}
