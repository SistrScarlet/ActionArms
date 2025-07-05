package net.sistr.actionarms.client.render.gltf.renderer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.sistr.actionarms.client.render.gltf.data.ModelMetadata;
import net.sistr.actionarms.client.render.gltf.data.ProcessedGltfModel;
import net.sistr.actionarms.client.render.gltf.data.ProcessedMesh;
import net.sistr.actionarms.client.render.gltf.manager.ItemAnimationManager;
import net.sistr.actionarms.client.render.gltf.util.DrawingMode;
import net.sistr.actionarms.entity.util.HasAimManager;
import net.sistr.actionarms.item.LeverActionGunItem;
import net.sistr.actionarms.item.component.IItemComponent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * ActionArms用ItemStackレンダラー
 * LeverActionGunItem専用のglTFレンダリング実装
 */
public class ActionArmsItemRenderer extends GltfObjectRenderer<ItemStack> {

    public ActionArmsItemRenderer(ProcessedGltfModel model, ModelMetadata metadata) {
        super(model, metadata);
    }

    @Override
    public RenderingContext.Builder createRenderContext(ItemStack stack, ModelTransformationMode mode,
                                                        @Nullable LivingEntity entity) {

        RenderingContext.Builder builder = RenderingContext.builder();

        // アニメーション状態の設定
        List<RenderingContext.AnimationState> animationStates =
                createAnimationStates(stack, mode, entity);
        builder.addAnimationState(animationStates);

        return builder;
    }

    /**
     * 隠蔽コンテキストキーを取得
     */
    @Override
    protected List<String> getHideKeys(ModelTransformationMode mode, @Nullable LivingEntity entity) {
        var list = new ArrayList<String>(1);
        // FPV（一人称視点）の判定
        if (mode != ModelTransformationMode.FIRST_PERSON_RIGHT_HAND) {
            list.add("fpv");
        }

        return list;
    }

    /**
     * アニメーション状態を作成
     * 既存のMixinItemRenderer#getAnimationStatesロジックを移植
     */
    protected List<RenderingContext.AnimationState> createAnimationStates(
            ItemStack stack, ModelTransformationMode mode,
            @Nullable LivingEntity entity) {
        if (!(stack.getItem() instanceof LeverActionGunItem gunItem)) {
            return new ArrayList<>(0);
        }

        var states = new ArrayList<RenderingContext.AnimationState>(10);

        if (entity == null) {
            return states;
        }

        float tickDelta = MinecraftClient.getInstance().getTickDelta();

        // ガンコンポーネントの取得
        var gunComponent = IItemComponent.query(gunItem.getGunComponent(), stack, c -> c);

        // エイム状態の判定
        boolean isAiming = entity instanceof HasAimManager hasAim
                && hasAim.actionArms$getAimManager().isAiming();

        float entityAge = entity.age * (1.0f / 20) + tickDelta;

        // ハンマー状態のアニメーション
        if (gunComponent.isHammerReady()) {
            states.add(new RenderingContext.AnimationState("hammerReady", entityAge, true));
        } else {
            states.add(new RenderingContext.AnimationState("hammerNotReady", entityAge, true));
        }

        // レバー状態のアニメーション
        if (gunComponent.isLeverDown()) {
            states.add(new RenderingContext.AnimationState("leverDown", entityAge, true));
        } else {
            states.add(new RenderingContext.AnimationState("leverUp", entityAge, true));
        }

        // FPV専用のアニメーション
        if (mode == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND) {
            float secondDelta = tickDelta * (1f / 20f);

            // アイドルアニメーション
            states.add(new RenderingContext.AnimationState(
                    isAiming ? "idle_aiming" : "idle", entityAge, true));

            // アイテムアニメーションの追加
            var itemStates = ItemAnimationManager.INSTANCE.getItemStateMap(stack);
            itemStates.values().stream()
                    .sorted(Comparator.comparingDouble(ItemAnimationManager.State::seconds).reversed())
                    .forEach(state -> {
                        String animationId = state.id();
                        if (isAiming) {
                            animationId += "_aiming";
                        }
                        states.add(new RenderingContext.AnimationState(
                                animationId, state.seconds() + secondDelta, false));
                    });
        }

        return states;
    }

    /**
     * レンダーレイヤーの決定
     */
    @Override
    public RenderLayer getRenderLayer(RenderingContext context, ProcessedMesh mesh, ModelMetadata metadata) {
        if (mesh.drawingMode() != DrawingMode.TRIANGLES) {
            throw new IllegalArgumentException("三角形描画以外は対応していません。drawingMode: " + mesh.drawingMode());
        }

        var material = mesh.getMaterial();
        var textureFileName = material.baseColorTexture();

        var textureMap = metadata.textureSettings().textureMap();
        Identifier textureResource = textureMap.get(textureFileName);

        if (textureResource == null) {
            throw new IllegalArgumentException("テクスチャリソースが見つかりません: " + textureFileName);
        }

        // contexts_overridesでplayer_skinが設定されているテクスチャの特別処理
        if (metadata.textureSettings().dynamicTextures().containsKey(textureFileName)) {
            String contextName = metadata.textureSettings().dynamicTextures().get(textureFileName);
            if ("player_skin".equals(contextName)) {
                // プレイヤースキンの動的解決
                if (context.entity() instanceof AbstractClientPlayerEntity player) {
                    var skinTexture = player.getSkinTexture();
                    return GltfRenderLayer.getEntityTranslucentTriangle(skinTexture, true);
                }
                // プレイヤーが存在しない場合はデフォルトスキン
                return GltfRenderLayer.getEntityTranslucentTriangle(textureResource, true);
            }
        }

        // 通常のテクスチャ
        return GltfRenderLayer.getEntityCutoutTriangle(textureResource);
    }
}