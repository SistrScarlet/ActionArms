package net.sistr.actionarms.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.sistr.actionarms.entity.util.HasAimManager;
import net.sistr.actionarms.item.util.GlftModelItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(HeldItemRenderer.class)
public class MixinHeldItemRenderer {

    @Shadow
    private ItemStack mainHand;

    @Shadow
    private ItemStack offHand;

    @Inject(method = "applyEquipOffset", at = @At("HEAD"), cancellable = true)
    private void onApplyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress, CallbackInfo ci) {
        var player = MinecraftClient.getInstance().player;
        if (player == null) {
            return;
        }
        if (actionArms$isHoldingGltfItem(arm)) {
            ci.cancel();
            int x = arm == Arm.RIGHT ? 1 : -1;
            int y = 1;
            if (((HasAimManager) player).actionArms$getAimManager().isAiming()) {
                x = 0;
                y = 0;
            }
            equipProgress = 0;
            //matrices.translate(i * 0.56f, -0.52f + equipProgress * -0.6f, -0.72f);
            matrices.translate(x * 0.56f, y * -0.52f, -0.72f);
        }
    }

    @Inject(method = "applySwingOffset", at = @At("HEAD"), cancellable = true)
    private void onApplySwingOffset(MatrixStack matrices, Arm arm, float swingProgress, CallbackInfo ci) {
        if (actionArms$isHoldingGltfItem(arm)) {
            ci.cancel();
        }
    }

    @ModifyArgs(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider$Immediate;Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderFirstPersonItem(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/util/Hand;FLnet/minecraft/item/ItemStack;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
    private void modifySwingProgressArg(Args args) {
        Hand hand = args.get(3);
        if (actionArms$isHoldingGltfItem(hand)) {
            args.set(4, 0f);
        }
    }

    @Unique
    private boolean actionArms$isHoldingGltfItem(Hand hand) {
        var player = MinecraftClient.getInstance().player;
        if (player == null) {
            return false;
        }
        return actionArms$isHoldingGltfItem(hand == Hand.MAIN_HAND ? this.mainHand : this.offHand);
    }

    @Unique
    private boolean actionArms$isHoldingGltfItem(Arm arm) {
        var player = MinecraftClient.getInstance().player;
        if (player == null) {
            return false;
        }
        return actionArms$isHoldingGltfItem(player.getMainArm() == arm ? this.mainHand : this.offHand);
    }

    @Unique
    private boolean actionArms$isHoldingGltfItem(ItemStack stack) {
        return stack.getItem() instanceof GlftModelItem;
    }

}
