package net.sistr.actionarms.item.util;

public interface CyclingLever {

    boolean cycleLever(LeverActionPlaySoundContext playSoundContext, AnimationContext context);

    boolean canCycleLever();

    boolean shouldCycleLever();

    boolean isHammerReady();

    boolean isLeverDown();

    interface CycleTickContext {
        void ejectCartridge(Cartridge cartridge);
    }
}
