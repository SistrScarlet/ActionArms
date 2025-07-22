package net.sistr.actionarms.item.util;

import net.sistr.actionarms.item.component.LeverActionGunComponent;
import net.sistr.actionarms.item.data.BulletData;

public interface FireTrigger {

    boolean trigger(LeverActionPlaySoundContext playSoundContext, AnimationContext animationContext, FireStartContext fireContext);

    boolean canTrigger();

    interface FireStartContext {
        void fire(LeverActionGunComponent gun, BulletData bullet);
    }

}
