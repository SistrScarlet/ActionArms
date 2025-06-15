package net.sistr.actionarms.mixin;

import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class MixinItemStack {

    @Inject(method = "canCombine", at = @At("HEAD"))
    private static void onCanCombine(ItemStack stack, ItemStack otherStack, CallbackInfoReturnable<Boolean> cir) {
        if (stack.getNbt() != null && stack.getNbt().isEmpty()) {
            stack.setNbt(null);
        }
        if (otherStack.getNbt() != null && otherStack.getNbt().isEmpty()) {
            otherStack.setNbt(null);
        }
    }

}
