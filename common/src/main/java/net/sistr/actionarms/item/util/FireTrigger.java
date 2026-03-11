package net.sistr.actionarms.item.util;

import net.sistr.actionarms.item.component.LeverActionGunComponent;
import net.sistr.actionarms.item.data.BulletData;

public interface FireTrigger {

    boolean pullTrigger(
            LeverActionPlaySoundContext playSoundContext,
            AnimationContext animationContext,
            FireStartContext fireContext);

    boolean canPullTrigger();

    interface FireStartContext {
        void fire(LeverActionGunComponent gun, BulletData bullet);
    }
}
