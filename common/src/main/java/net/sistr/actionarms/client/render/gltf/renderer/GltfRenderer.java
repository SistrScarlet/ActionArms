package net.sistr.actionarms.client.render.gltf.renderer;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.client.render.gltf.data.ProcessedGltfModel;
import net.sistr.actionarms.client.render.gltf.data.ProcessedMesh;
import net.sistr.actionarms.client.render.gltf.processor.DirectProcessor;

/**
 * glTFレンダリングクラス
 * 効率的な描画のため複数の処理方式をサポート
 */
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
        for (ProcessedMesh mesh : model.getMeshes()) {
            RenderLayer renderLayer = getRenderLayer(mesh, context);
            VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(renderLayer);

            // DirectProcessorで中間オブジェクトなしで直接描画
            directProcessor.renderMeshDirect(mesh, context, model, matrixStack, vertexConsumer);
        }
    }

    /**
     * レンダーレイヤーの決定
     */
    private RenderLayer getRenderLayer(ProcessedMesh mesh, RenderingContext context) {
        // TODO: メッシュのマテリアル情報からレンダーレイヤーを決定
        // 現在はデフォルトテクスチャを使用
        return RenderLayer.getEntityCutout(new Identifier(ActionArms.MOD_ID,
                "textures/test/texture.png"));
    }

    /**
     * レンダリング可能かどうかをチェック
     */
    public boolean canRender(RenderingContext context) {
        return context != null && model != null && !model.getMeshes().isEmpty();
    }

    /**
     * パフォーマンス統計情報を取得（デバッグ用）
     */
    public String getPerformanceStats() {
        return String.format("GltfRenderer [Meshes: %d, Vertices: %d]",
                model.getMeshes().size(),
                model.getMeshes().stream().mapToInt(ProcessedMesh::getVertexCount).sum());
    }
}
