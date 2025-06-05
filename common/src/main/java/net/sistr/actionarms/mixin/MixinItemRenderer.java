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
import net.sistr.actionarms.client.render.gltf.*;
import net.sistr.actionarms.client.render.gltf.renderer.GltfRenderer;
import net.sistr.actionarms.client.render.gltf.renderer.RenderingContext;
import net.sistr.actionarms.item.util.GLTFModelItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

            var renderingContext = RenderingContext.builder()
                    .tickDelta(tickDelta)
                    .light(light)
                    .overlay(overlay)
                    .addAnimationState(new RenderingContext.AnimationState("cocking", (entity.age + tickDelta) * 0.05f, 1, 1.0f))
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
        return !stack.isEmpty() && stack.getItem() instanceof GLTFModelItem;
    }

}
