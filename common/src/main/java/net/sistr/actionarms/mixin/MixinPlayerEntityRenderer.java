package net.sistr.actionarms.mixin;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.util.Arm;
import net.sistr.actionarms.item.LeverActionGunItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class MixinPlayerEntityRenderer
        extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    public MixinPlayerEntityRenderer(EntityRendererFactory.Context ctx, PlayerEntityModel<AbstractClientPlayerEntity> model, float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "setModelPose", at = @At("RETURN"))
    private void onSetModelPose(AbstractClientPlayerEntity player, CallbackInfo ci) {
        var model = this.getModel();
        if (player.getMainHandStack().getItem() instanceof LeverActionGunItem) {
            if (player.getMainArm() == Arm.RIGHT) {
                model.rightArmPose = BipedEntityModel.ArmPose.CROSSBOW_HOLD;
                model.leftArmPose = BipedEntityModel.ArmPose.EMPTY;
            } else {
                model.rightArmPose = BipedEntityModel.ArmPose.EMPTY;
                model.leftArmPose = BipedEntityModel.ArmPose.CROSSBOW_HOLD;
            }
        }
    }

}
