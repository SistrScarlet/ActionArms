package net.sistr.actionarms.item.component;

public interface FireTrigger {

    boolean trigger(LeverActionPlaySoundContext playSoundContext, AnimationContext animationContext, FireStartContext fireContext);

    boolean canTrigger();

    interface FireStartContext {
        void fire(BulletComponent bullet);
    }

}
