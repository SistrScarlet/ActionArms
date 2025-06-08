package net.sistr.actionarms.component;

import net.minecraft.nbt.NbtCompound;

public record Bullet(BulletType type) {
    public static Bullet read(NbtCompound nbt) {
        return new Bullet(BulletType.DEFAULT_TYPE);
    }

    public void write(NbtCompound nbt) {

    }
}
