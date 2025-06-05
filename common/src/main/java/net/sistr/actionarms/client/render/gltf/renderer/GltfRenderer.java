package net.sistr.actionarms.client.render.gltf.renderer;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.client.render.gltf.data.ComputedBoneMatricesData;
import net.sistr.actionarms.client.render.gltf.data.ComputedVertexData;
import net.sistr.actionarms.client.render.gltf.data.ProcessedGltfModel;
import net.sistr.actionarms.client.render.gltf.data.ProcessedMesh;
import net.sistr.actionarms.client.render.gltf.processor.AnimationProcessor;
import net.sistr.actionarms.client.render.gltf.processor.VertexProcessor;

/**
 * glTFレンダリングクラス
 */
public class GltfRenderer {
    private final ProcessedGltfModel model;
    private final VertexProcessor vertexProcessor;
    private final AnimationProcessor animationController;

    public GltfRenderer(ProcessedGltfModel model) {
        this.model = model;
        this.vertexProcessor = new VertexProcessor();
        this.animationController = new AnimationProcessor();
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

        // 各メッシュを描画（純粋関数的）
        for (ProcessedMesh mesh : model.getMeshes()) {
            renderMesh(matrixStack, vertexConsumerProvider, mesh, context);
        }
    }

    /**
     * 単一メッシュの描画
     */
    private void renderMesh(MatrixStack matrixStack,
                            VertexConsumerProvider vertexConsumerProvider,
                            ProcessedMesh mesh,
                            RenderingContext context) {
        RenderLayer renderLayer = getRenderLayer(mesh, context);
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(renderLayer);

        ComputedBoneMatricesData boneMatrices = null;
        if (context.animations().length > 0) {
            boneMatrices = this.animationController.getBoneMatrices(context, mesh.getSkin(), model);
        }

        float[] morphWeights = null;

        ComputedVertexData vertexData = vertexProcessor.computeVertices(mesh, context, boneMatrices, morphWeights);

        switch (mesh.getDrawingMode()) {
            case TRIANGLES:
                renderTriangles(mesh, vertexData, matrixStack, vertexConsumer, context);
                break;
            default:
                break;
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
     * 三角形の描画
     */
    private void renderTriangles(ProcessedMesh mesh, ComputedVertexData vertexData,
                                 MatrixStack matrixStack, VertexConsumer vertexConsumer,
                                 RenderingContext context) {
        int[] indices = mesh.getIndices();
        if (indices != null) {
            // インデックスバッファを使用
            for (int i = 0; i < indices.length; i += 3) {
                // 通常の三角形順序
                for (int j = 0; j < 3; j++) {
                    int vertexIndex = indices[i + j];
                    renderVertex(vertexIndex, vertexData, mesh, matrixStack,
                            vertexConsumer, context);
                }
                renderVertex(indices[i + 2], vertexData, mesh, matrixStack,
                        vertexConsumer, context);
            }
        } else {
            // 直接頂点を描画
            int vertexCount = mesh.getVertexCount();
            for (int i = 0; i < vertexCount; i += 3) {
                for (int j = 0; j < 3; j++) {
                    if (i + j < vertexCount) {
                        renderVertex(i + j, vertexData, mesh, matrixStack,
                                vertexConsumer, context);
                    }
                }
            }
        }
    }

    /**
     * 単一頂点の描画
     */
    private void renderVertex(int vertexIndex, ComputedVertexData vertexData, ProcessedMesh mesh,
                              MatrixStack matrixStack, VertexConsumer vertexConsumer,
                              RenderingContext context) {

        float[] finalPositions = vertexData.getFinalPositions();
        float[] finalNormals = vertexData.getFinalNormals();
        float[] uvs = mesh.getUvsRaw();

        // 配列の境界チェック
        if (vertexIndex < 0 ||
                vertexIndex * 3 + 2 >= finalPositions.length ||
                vertexIndex * 3 + 2 >= finalNormals.length ||
                vertexIndex * 2 + 1 >= uvs.length) {
            return; // 無効なインデックス
        }

        // 最終的な頂点位置
        float x = finalPositions[vertexIndex * 3];
        float y = finalPositions[vertexIndex * 3 + 1];
        float z = finalPositions[vertexIndex * 3 + 2];

        // 法線
        float nx = finalNormals[vertexIndex * 3];
        float ny = finalNormals[vertexIndex * 3 + 1];
        float nz = finalNormals[vertexIndex * 3 + 2];

        // UV座標
        float u = uvs[vertexIndex * 2];
        float v = uvs[vertexIndex * 2 + 1];

        // Minecraftの頂点フォーマットに出力
        vertexConsumer.vertex(matrixStack.peek().getPositionMatrix(), x, y, z)
                .color(255, 255, 255, 255)
                .texture(u, v)
                .overlay(context.overlay())
                .light(context.light())
                .normal(matrixStack.peek().getNormalMatrix(), nx, ny, nz)
                .next();
    }

    /**
     * レンダリング可能かどうかをチェック
     */
    public boolean canRender(RenderingContext context) {
        return context != null && model != null && !model.getMeshes().isEmpty();
    }
}
