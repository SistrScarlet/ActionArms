package net.sistr.actionarms.item.component;

public interface CyclingLever {

    boolean cycle(LeverActionPlaySoundContext playSoundContext, AnimationContext context);

    boolean canCycle();

    boolean shouldCycle();

    boolean isHammerReady();

    boolean isLeverDown();

    interface CycleTickContext {
        void ejectCartridge(Cartridge cartridge);
    }

}
