package net.sistr.actionarms.client.render.gltf.renderer;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.world.World;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.client.render.gltf.data.ModelMetadata;
import net.sistr.actionarms.client.render.gltf.data.ProcessedGltfModel;
import net.sistr.actionarms.client.render.gltf.data.ProcessedMesh;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * glTFオブジェクトレンダラーの基底クラス
 * Genericsを使用して様々なオブジェクト型に対応
 *
 * @param <T> レンダリング対象のオブジェクト型
 */
public abstract class GltfObjectRenderer<T> {
    private final GltfMeshRenderer gltfMeshRenderer;
    protected final ProcessedGltfModel model;
    protected final ModelMetadata metadata;

    protected GltfObjectRenderer(ProcessedGltfModel model, ModelMetadata metadata) {
        this.model = model;
        this.metadata = metadata;
        this.gltfMeshRenderer = new GltfMeshRenderer();
    }

    // ========== 継承クラスで実装必須の抽象メソッド ==========

    /**
     * オブジェクトとレンダリング情報からRenderingContext.Builderを作成
     *
     * @param object レンダリング対象オブジェクト
     * @param mode   レンダリングモード
     * @param entity エンティティ（オプション）
     * @return RenderingContext.Builder
     */
    public abstract RenderingContext.Builder createRenderContext(T object, ModelTransformationMode mode,
                                                                 @Nullable LivingEntity entity);

    /**
     * レンダーレイヤーの決定 (継承先で実装)
     * 各オブジェクトタイプ固有のテクスチャマッピングロジックが必要
     */
    protected abstract RenderLayer getRenderLayer(RenderingContext context, ProcessedMesh mesh, ModelMetadata metadata);

    protected abstract List<String> getHideKeys(ModelTransformationMode mode, @Nullable LivingEntity entity);

    // ========== 共通レンダリングロジック ==========

    /**
     * オブジェクトをレンダリング
     */
    public void render(T object, ModelTransformationMode mode,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       @Nullable LivingEntity entity, World world, int light, int overlay, float tickDelta) {
        try {
            var contextBuilder = createRenderContext(object, mode, entity);
            contextBuilder.light(light)
                    .overlay(overlay)
                    .entity(entity)
                    .tickDelta(tickDelta);

            List<String> hideKeys = getHideKeys(mode, entity);
            for (String hideKey : hideKeys) {
                var hideBones = metadata.hideBoneKeys().get(hideKey);
                contextBuilder.addHideBones(hideBones);
            }

            var context = contextBuilder.build();

            // レンダリング実行
            if (canRenderGltfModel(context, model)) {
                for (ProcessedMesh mesh : model.meshes()) {
                    RenderLayer renderLayer = getRenderLayer(context, mesh, metadata);
                    VertexConsumer vertexConsumer = vertexConsumers.getBuffer(renderLayer);

                    // DirectProcessorで中間オブジェクトなしで直接描画
                    gltfMeshRenderer.renderMesh(mesh, context, model, matrices, vertexConsumer);
                }
            }

        } catch (Exception e) {
            ActionArms.LOGGER.error("Error during glTF object rendering: {}", e.getMessage(), e);
        }
    }

    /**
     * glTFモデルのレンダリング可能かどうかをチェック
     */
    private boolean canRenderGltfModel(@Nullable RenderingContext context, @Nullable ProcessedGltfModel model) {
        return context != null && model != null && !model.meshes().isEmpty();
    }
}