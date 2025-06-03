package net.sistr.actionarms.client.render.gltf;


import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.sistr.actionarms.ActionArms;

public class GltfRenderer {
    private final ProcessedGltfModel model;
    private final GltfAnimationController animationController;
    private final VertexProcessor vertexProcessor;
    private RenderingContext lastContext;

    public GltfRenderer(ProcessedGltfModel model) {
        this.model = model;
        this.animationController = new GltfAnimationController(model);
        this.vertexProcessor = new VertexProcessor();

        // マテリアル別レンダーレイヤーの事前作成
        // initializeMaterialRenderLayers();
        this.animationController.playAnimation("cocking");
    }

    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider,
                       int packedLight, int packedOverlay, float delta) {

        // レンダリングコンテキストの作成
        RenderingContext context = createRenderingContext(delta);

        // アニメーション状態の更新（フレーム間で状態が変わった場合のみ）
        if (shouldUpdateAnimation(context)) {
            animationController.update(context);
            lastContext = context;
        }

        // 各メッシュを描画
        for (ProcessedMesh mesh : model.getMeshes()) {
            renderMesh(matrixStack, vertexConsumerProvider, mesh, context,
                    packedLight, packedOverlay);
        }
    }

    private RenderingContext createRenderingContext(float delta) {
        RenderingContext context = new RenderingContext();
        context.setCustomProperty("delta", delta);
        context.setCustomProperty("frame_time", System.currentTimeMillis());
        return context;
    }

    private boolean shouldUpdateAnimation(RenderingContext context) {
        if (lastContext == null) return true;

        // フレーム時間の変化をチェック
        long currentTime = (Long) context.getCustomProperty("frame_time");
        long lastTime = (Long) lastContext.getCustomProperty("frame_time");

        return currentTime != lastTime;
    }

    private void renderMesh(MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider,
                            ProcessedMesh mesh, RenderingContext context, int packedLight, int packedOverlay) {
        // レンダーレイヤーの取得
        RenderLayer renderLayer = RenderLayer.getEntityCutout(new Identifier(ActionArms.MOD_ID, "textures/test/texture.png")); // materialRenderLayers.get(mesh.getMaterialIndex());
        VertexConsumer vertexConsumer = vertexConsumerProvider.getBuffer(renderLayer);

        // 頂点データの取得（キャッシュ利用）
        ComputedVertexData vertexData = vertexProcessor.computeVertices(mesh, context);

        // 描画モードに応じた描画
        switch (mesh.getDrawingMode()) {
            case TRIANGLES:
                renderTriangles(mesh, vertexData, matrixStack, vertexConsumer, packedLight, packedOverlay);
                break;
            case TRIANGLE_STRIP:
                renderTriangleStrip(mesh, vertexData, matrixStack, vertexConsumer, packedLight, packedOverlay);
                break;
            case POINTS:
                renderPoints(mesh, vertexData, matrixStack, vertexConsumer, packedLight, packedOverlay);
                break;
            case LINES:
                renderLines(mesh, vertexData, matrixStack, vertexConsumer, packedLight, packedOverlay);
                break;
            default:
                // 未対応の描画モードはTRIANGLESとして扱う
                renderTriangles(mesh, vertexData, matrixStack, vertexConsumer, packedLight, packedOverlay);
        }
    }

    private void renderTriangles(ProcessedMesh mesh, ComputedVertexData vertexData,
                                 MatrixStack matrixStack, VertexConsumer vertexConsumer,
                                 int packedLight, int packedOverlay) {
        int[] indices = mesh.getIndices();
        if (indices != null) {
            // インデックスバッファを使用
            for (int i = 0; i < indices.length; i += 3) {
                for (int j = 0; j < 3; j++) {
                    int vertexIndex = indices[i + j];
                    renderVertex(vertexIndex, vertexData, mesh, matrixStack,
                            vertexConsumer, packedLight, packedOverlay);
                }
                // 仮
                int vertexIndex = indices[i + 2];
                renderVertex(vertexIndex, vertexData, mesh, matrixStack,
                        vertexConsumer, packedLight, packedOverlay);
            }
        } else {
            // 直接頂点を描画
            int vertexCount = mesh.getVertexCount();
            for (int i = 0; i < vertexCount; i += 3) {
                for (int j = 0; j < 3; j++) {
                    renderVertex(i + j, vertexData, mesh, matrixStack,
                            vertexConsumer, packedLight, packedOverlay);
                }
                // 仮
                renderVertex(i + 2, vertexData, mesh, matrixStack,
                        vertexConsumer, packedLight, packedOverlay);
            }
        }
    }

    private void renderTriangleStrip(ProcessedMesh mesh, ComputedVertexData vertexData,
                                     MatrixStack matrixStack, VertexConsumer vertexConsumer,
                                     int packedLight, int packedOverlay) {
        int[] indices = mesh.getIndices();
        int count = indices != null ? indices.length : mesh.getVertexCount();

        for (int i = 0; i < count - 2; i++) {
            // 三角形ストリップの順序に注意
            int[] triIndices = {i, i + 1, i + 2};
            if (i % 2 == 1) {
                // 奇数番目の三角形は頂点順序を反転
                triIndices = new int[]{i, i + 2, i + 1};
            }

            for (int vertexIndex : triIndices) {
                int actualIndex = indices != null ? indices[vertexIndex] : vertexIndex;
                renderVertex(actualIndex, vertexData, mesh, matrixStack,
                        vertexConsumer, packedLight, packedOverlay);
            }
        }
    }

    private void renderPoints(ProcessedMesh mesh, ComputedVertexData vertexData,
                              MatrixStack matrixStack, VertexConsumer vertexConsumer,
                              int packedLight, int packedOverlay) {
        // 点の描画（実際にはMinecraftでは点単体は描画しにくいので小さな四角形で代用）
        int[] indices = mesh.getIndices();
        int count = indices != null ? indices.length : mesh.getVertexCount();

        for (int i = 0; i < count; i++) {
            int vertexIndex = indices != null ? indices[i] : i;
            // 点として小さな四角形を描画（実装は省略）
        }
    }

    private void renderLines(ProcessedMesh mesh, ComputedVertexData vertexData,
                             MatrixStack matrixStack, VertexConsumer vertexConsumer,
                             int packedLight, int packedOverlay) {
        // 線の描画（Minecraftでは線も細い四角形で表現）
        int[] indices = mesh.getIndices();
        int count = indices != null ? indices.length : mesh.getVertexCount();

        for (int i = 0; i < count; i += 2) {
            if (i + 1 < count) {
                int index1 = indices != null ? indices[i] : i;
                int index2 = indices != null ? indices[i + 1] : i + 1;
                // 線として細い四角形を描画（実装は省略）
            }
        }
    }

    private void renderVertex(int vertexIndex, ComputedVertexData vertexData, ProcessedMesh mesh,
                              MatrixStack matrixStack, VertexConsumer vertexConsumer,
                              int packedLight, int packedOverlay) {

        float[] finalPositions = vertexData.getFinalPositions();
        float[] finalNormals = vertexData.getFinalNormals();
        float[] uvs = mesh.getUvs();

        // 配列の境界チェック
        if (vertexIndex * 3 + 2 >= finalPositions.length ||
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
                .overlay(packedOverlay)
                .light(packedLight)
                .normal(matrixStack.peek().getNormalMatrix(), nx, ny, nz)
                .next();
    }
}
