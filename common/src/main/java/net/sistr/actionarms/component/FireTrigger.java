package net.sistr.actionarms.component;

public interface FireTrigger {

    boolean trigger(FireStartContext context);

    boolean canTrigger();

    boolean isFiring();

    float getFiringTime();

    interface FireStartContext {
        void fire(Bullet bullet);
    }

}
