package net.sistr.actionarms.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.sistr.actionarms.item.util.GlftModelItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public class MixinHeldItemRenderer {

    @Inject(method = "applyEquipOffset", at = @At("HEAD"), cancellable = true)
    private void onApplyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress, CallbackInfo ci) {
        var player = MinecraftClient.getInstance().player;
        if (player != null
                && player.getMainArm() == arm
                && player.getStackInHand(player.getMainArm() == arm ? Hand.MAIN_HAND : Hand.OFF_HAND)
                .getItem() instanceof GlftModelItem) {
            ci.cancel();
            int i = arm == Arm.RIGHT ? 1 : -1;
            equipProgress = 0;
            //matrices.translate(i * 0.56f, -0.52f + equipProgress * -0.6f, -0.72f);
            matrices.translate(i * 0.56f, 0, -0.72f);
        }
    }

    @Inject(method = "applySwingOffset", at = @At("HEAD"), cancellable = true)
    private void onApplySwingOffset(MatrixStack matrices, Arm arm, float swingProgress, CallbackInfo ci) {
        var player = MinecraftClient.getInstance().player;
        if (player != null
                && player.getMainArm() == arm
                && player.getStackInHand(player.getMainArm() == arm ? Hand.MAIN_HAND : Hand.OFF_HAND)
                .getItem() instanceof GlftModelItem) {
            ci.cancel();
        }
    }

}
