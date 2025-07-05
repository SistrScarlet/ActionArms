package net.sistr.actionarms.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.world.World;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.client.render.gltf.renderer.GltfObjectRenderer;
import net.sistr.actionarms.client.render.gltf.manager.GltfObjectRendererRegistry;
import net.sistr.actionarms.item.util.GlftModelItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(ItemRenderer.class)
public class MixinItemRenderer {

    /**
     * プレイヤーやモブが手に持った時の描画
     * 新しいレンダラーレジストリシステムを使用
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
        // glTFモデルアイテムかチェック
        if (!(stack.getItem() instanceof GlftModelItem)) {
            return;
        }

        var id = Registries.ITEM.getId(stack.getItem());

        // レンダラーIDを取得してレンダラーを取得
        Optional<GltfObjectRenderer<ItemStack>> renderer =
                GltfObjectRendererRegistry.INSTANCE.getRenderer(id);

        if (renderer.isPresent()) {
            ci.cancel(); // 元の描画をキャンセル

            matrices.push();
            try {
                renderer.get().render(stack, renderMode, matrices, vertexConsumers,
                        entity, world, light, overlay, MinecraftClient.getInstance().getTickDelta());
            } catch (Exception e) {
                ActionArms.LOGGER.error("Error during glTF item rendering: {}", e.getMessage(), e);
            } finally {
                matrices.pop();
            }
        }
    }
}
