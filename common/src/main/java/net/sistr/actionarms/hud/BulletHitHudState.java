package net.sistr.actionarms.hud;

import net.minecraft.nbt.NbtCompound;

public record BulletHitHudState(State state) {

    public static BulletHitHudState of(State state) {
        return new BulletHitHudState(state);
    }

    public static BulletHitHudState of(NbtCompound nbt) {
        State state = State.values()[nbt.getByte("state")];
        return new BulletHitHudState(state);
    }

    public NbtCompound write() {
        var nbt = new NbtCompound();
        nbt.putByte("state", (byte) this.state.ordinal());
        return nbt;
    }

    public enum State {
        HIT(0xFF00FF00),
        HEADSHOT(0xFFFFFF00),
        KILL(0xFFFF0000);
        private final int color;

        State(int color) {
            this.color = color;
        }

        public int color() {
            return color;
        }

        public static State of(boolean kill, boolean headshot) {
            if (kill) {
                return KILL;
            } else if (headshot) {
                return HEADSHOT;
            } else {
                return HIT;
            }
        }
    }

}
