package net.sistr.actionarms.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.sistr.actionarms.client.render.gltf.GLTFModelManager;
import net.sistr.actionarms.client.render.gltf.GltfRenderer;
import net.sistr.actionarms.item.util.GLTFModelItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer {

    // プレイヤーやモブが手に持った時の描画
    @Inject(
            method = "renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/world/World;III)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderItem(
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
        if (!stack.isEmpty() && stack.getItem() instanceof GLTFModelItem) {
            ci.cancel();
            matrices.push();
            GLTFModelManager.INSTANCE.getModels().forEach((id, modelData) -> {
                var renderer = new GltfRenderer(modelData);
                renderer.render(matrices, vertexConsumers, light, overlay, entity.age * 0.01f);
            });
            matrices.pop();
        }
    }

    // 通常描画
    @Inject(
            method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/render/model/json/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IILnet/minecraft/client/render/model/BakedModel;)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderItem(
            ItemStack stack,
            ModelTransformationMode renderMode,
            boolean leftHanded,
            MatrixStack matrices,
            VertexConsumerProvider vertexConsumers,
            int light,
            int overlay,
            BakedModel model,
            CallbackInfo ci) {
        if (!stack.isEmpty() && stack.getItem() instanceof GLTFModelItem) {
            ci.cancel();
            matrices.push();
            GLTFModelManager.INSTANCE.getModels().forEach((id, modelData) -> {
                var renderer = new GltfRenderer(modelData);
                //renderer.render(matrices, vertexConsumers, light, overlay, 0);
            });
            matrices.pop();
        }
    }

}
