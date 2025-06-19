package net.sistr.actionarms.client.render.gltf.renderer;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.client.render.gltf.data.ProcessedGltfModel;
import net.sistr.actionarms.client.render.gltf.data.ProcessedMesh;
import net.sistr.actionarms.client.render.gltf.processor.DirectProcessor;
import net.sistr.actionarms.client.render.gltf.util.DrawingMode;
import org.jetbrains.annotations.Nullable;

/**
 * glTFレンダリングクラス
 */
@SuppressWarnings("MethodParameterNamingConvention")
public class GltfRenderer {
    private final ProcessedGltfModel model;
    private final DirectProcessor directProcessor;

    public GltfRenderer(ProcessedGltfModel model) {
        this.model = model;
        this.directProcessor = new DirectProcessor();
    }

    /**
     * @param matrixStack            変換行列スタック
     * @param vertexConsumerProvider 頂点コンシューマープロバイダー
     * @param context                レンダリング状態を含むコンテキスト
     */
    public void render(MatrixStack matrixStack,
                       VertexConsumerProvider vertexConsumerProvider,
                       RenderingContext context) {
        if (!canRender(context)) {
            return;
        }

        renderDirect(matrixStack, vertexConsumerProvider, context);
    }

    private void renderDirect(MatrixStack matrixStack,
                              VertexConsumerProvider vertexConsumerProvider,
                              RenderingContext context) {
        for (ProcessedMesh mesh : model.meshes()) {
            RenderLayer renderLayer = getRenderLayer(context, mesh);
            VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(renderLayer);

            // DirectProcessorで中間オブジェクトなしで直接描画
            directProcessor.renderMeshDirect(mesh, context, model, matrixStack, vertexConsumer);
        }
    }

    /**
     * レンダーレイヤーの決定
     */
    private RenderLayer getRenderLayer(RenderingContext context, ProcessedMesh mesh) {
        // 現在はデフォルトテクスチャを使用
        if (mesh.drawingMode() == DrawingMode.TRIANGLES) {
            var material = mesh.getMaterial();
            var texture = material.baseColorTexture();
            if ("texture.png".equals(texture)) {
                return GltfRenderLayer.getEntityCutoutTriangle(new Identifier(ActionArms.MOD_ID,
                        "textures/test/texture.png"));
            } if ("bullet.png".equals(texture)) {
                return GltfRenderLayer.getEntityCutoutTriangle(new Identifier(ActionArms.MOD_ID,
                        "textures/test/bullet.png"));
            } else if ("skin_alex.png".equals(texture)) {
                if (context.entity() != null && context.entity() instanceof AbstractClientPlayerEntity player) {
                    var skinTexture = player.getSkinTexture();
                    return GltfRenderLayer.getEntityTranslucentTriangle(skinTexture, true);
                }
                return GltfRenderLayer.getEntityTranslucentTriangle(new Identifier(ActionArms.MOD_ID,
                        "textures/test/skin_alex.png"), true);
            }
            throw new IllegalArgumentException("そのテクスチャ無いよー");
        } else {
            throw new IllegalArgumentException("三角形描画以外は対応していません。drawingMode: " + mesh.drawingMode());
        }
    }

    /**
     * レンダリング可能かどうかをチェック
     */
    public boolean canRender(@Nullable RenderingContext context) {
        return context != null && model != null && !model.meshes().isEmpty();
    }

}
