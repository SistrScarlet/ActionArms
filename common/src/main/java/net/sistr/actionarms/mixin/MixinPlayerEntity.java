package net.sistr.actionarms.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.sistr.actionarms.entity.util.AimManager;
import net.sistr.actionarms.entity.util.HasAimManager;
import net.sistr.actionarms.entity.util.IAimManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class MixinPlayerEntity implements HasAimManager {
    @Unique
    private final AimManager actionArms$aimManager = new AimManager((PlayerEntity) (Object) this);

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        this.actionArms$aimManager.tick();
    }

    @Override
    public IAimManager actionArms$getAimManager() {
        return this.actionArms$aimManager;
    }
}
