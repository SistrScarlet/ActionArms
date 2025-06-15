package net.sistr.actionarms.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.client.render.gltf.GltfModelManager;
import net.sistr.actionarms.client.render.gltf.ItemAnimationManager;
import net.sistr.actionarms.client.render.gltf.renderer.GltfRenderer;
import net.sistr.actionarms.client.render.gltf.renderer.RenderingContext;
import net.sistr.actionarms.entity.util.HasAimManager;
import net.sistr.actionarms.item.LeverActionGunItem;
import net.sistr.actionarms.item.component.IItemComponent;
import net.sistr.actionarms.item.component.LeverActionGunComponent;
import net.sistr.actionarms.item.util.GlftModelItem;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer {

    /**
     * プレイヤーやモブが手に持った時の描画
     */
    @Inject(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderItemWithEntity(
            LivingEntity entity,
            ItemStack stack,
            ModelTransformationMode renderMode,
            boolean leftHanded,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            World world,
            int light,
            int overlay,
            int seed,
            CallbackInfo ci) {

        if (!actionArms$shouldRenderWithGltf(stack)) {
            return;
        }

        try {
            ci.cancel(); // 元の描画をキャンセル

            matrices.push();

            float tickDelta = MinecraftClient.getInstance().getTickDelta();

            var gunComponent = IItemComponent.query(((LeverActionGunItem) stack.getItem()).getGunComponent(),
                    stack, c -> c);
            var itemStates = ItemAnimationManager.INSTANCE.getItemStateMap(stack);

            boolean isFPV = renderMode == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND;
            var animationStates = getAnimationStates(entity, gunComponent, itemStates, tickDelta, isFPV);

            var renderingContext = RenderingContext.builder()
                    .tickDelta(tickDelta)
                    .light(light)
                    .overlay(overlay)
                    .addAnimationState(animationStates)
                    .fpv(isFPV)
                    .build();

            // レンダラーの取得または作成
            new GltfRenderer(GltfModelManager.INSTANCE.getModels().entrySet().stream().findFirst().orElseThrow().getValue())
                    .render(matrices, vertexConsumers, renderingContext);

            matrices.pop();

        } catch (Exception e) {
            ActionArms.LOGGER.error("Error during glTF item rendering with entity: {}", e.getMessage(), e);
            matrices.pop(); // エラー時もスタックを戻す
        }
    }

    @Unique
    private static @NotNull ArrayList<RenderingContext.AnimationState> getAnimationStates(
            LivingEntity entity, LeverActionGunComponent component,
            Map<String, ItemAnimationManager.State> itemStates, float tickDelta, boolean isFPV) {
        var states = new ArrayList<RenderingContext.AnimationState>();

        boolean isAiming = entity instanceof HasAimManager hasAimManager
                && hasAimManager.actionArms$getAimManager().isAiming();
        float entityAge = entity.age * (1f / 20f) + tickDelta;

        if (component.isHammerReady()) {
            states.add(new RenderingContext.AnimationState(
                    "hammerReady",
                    entityAge,
                    true));
        } else {
            states.add(new RenderingContext.AnimationState(
                    "hammerNotReady",
                    entityAge,
                    true));
        }
        if (component.isLeverDown()) {
            states.add(new RenderingContext.AnimationState(
                    "leverDown",
                    entityAge,
                    true));
        } else {
            states.add(new RenderingContext.AnimationState(
                    "leverUp",
                    entityAge,
                    true));
        }

        if (!isFPV) {
            return states;
        }

        float secondDelta = tickDelta * (1f / 20f);
        states.add(new RenderingContext.AnimationState(
                isAiming ? "idle_aiming" : "idle",
                entityAge,
                true));
        itemStates.values().stream()
                .sorted(Comparator.comparingDouble(ItemAnimationManager.State::seconds).reversed())
                .forEach(state -> {
                    var id = state.id();
                    if (isAiming) {
                        id += "_aiming";
                    }
                    states.add(new RenderingContext.AnimationState(
                            id, state.seconds() + secondDelta, false));
                });
        return states;
    }

    /**
     * 通常描画（インベントリ、ドロップ等）
     */
    @Inject(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderItemStandalone(
            ItemStack stack,
            ModelTransformationMode renderMode,
            boolean leftHanded,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay,
            BakedModel model,
            CallbackInfo ci) {
        // 一旦何もしない
    }

    /**
     * glTFでレンダリングするべきかどうかの判定
     */
    @Unique
    private boolean actionArms$shouldRenderWithGltf(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof GlftModelItem;
    }

}
