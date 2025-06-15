package net.sistr.actionarms.hud;

import net.minecraft.nbt.NbtCompound;

public record BulletHitHudState(boolean kill) {

    public static BulletHitHudState of(boolean kill) {
        return new BulletHitHudState(kill);
    }

    public static BulletHitHudState of(NbtCompound nbt) {
        return new BulletHitHudState(nbt.getBoolean("kill"));
    }

    public NbtCompound write() {
        var nbt = new NbtCompound();
        nbt.putBoolean("kill", this.kill);
        return nbt;
    }

}
