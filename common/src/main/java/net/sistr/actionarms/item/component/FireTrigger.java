package net.sistr.actionarms.item.component;

public interface FireTrigger {

    boolean trigger(AnimationContext animationContext, FireStartContext context);

    boolean canTrigger();

    interface FireStartContext {
        void fire(BulletComponent bullet);
    }

}
