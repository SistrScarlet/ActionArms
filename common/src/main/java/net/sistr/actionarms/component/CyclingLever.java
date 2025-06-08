package net.sistr.actionarms.component;

public interface CyclingLever {

    boolean cycle(CycleStartContext context);

    boolean canCycle();

    boolean shouldCycle();

    boolean isHammerReady();

    boolean isLeverDown();

    boolean isCycling();

    float getCyclingTime();

    interface CycleTickContext {
        void ejectCartridge(Cartridge cartridge);
    }

    interface CycleStartContext {
        void cycle(float seconds);
    }

}
