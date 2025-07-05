package net.sistr.actionarms.mixin;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.EntityList;
import net.sistr.actionarms.entity.util.EntityRecordManager;
import net.sistr.actionarms.entity.util.HasEntityRecordManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

@Mixin(ServerWorld.class)
public abstract class MixinServerWorld implements HasEntityRecordManager {
    @Shadow
    @Final
    private EntityList entityList;
    @Unique
    private final EntityRecordManager actionArms$entityRecordManager = new EntityRecordManager();

    @Override
    public EntityRecordManager actionArms$getEntityRecordManager() {
        return actionArms$entityRecordManager;
    }

    @Inject(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/EntityList;forEach(Ljava/util/function/Consumer;)V"))
    private void onTickEntityListForeach(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        this.actionArms$entityRecordManager.preWorldTick((ServerWorld) (Object) this, this.entityList);
    }


}
