package net.sistr.actionarms.mixin;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import net.sistr.actionarms.entity.util.AimManager;
import net.sistr.actionarms.entity.util.HasAimManager;
import net.sistr.actionarms.entity.util.IAimManager;
import net.sistr.actionarms.item.LeverActionGunItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class MixinPlayerEntity extends LivingEntity implements HasAimManager {
    @Unique
    private final AimManager actionArms$aimManager = new AimManager((PlayerEntity) (Object) this);

    protected MixinPlayerEntity(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        this.actionArms$aimManager.tick();
    }

    @Override
    public IAimManager actionArms$getAimManager() {
        return this.actionArms$aimManager;
    }

    @Override
    public float getHandSwingProgress(float tickDelta) {
        if (this.getMainHandStack().getItem() instanceof LeverActionGunItem) {
            return 0;
        }
        return super.getHandSwingProgress(tickDelta);
    }
}
