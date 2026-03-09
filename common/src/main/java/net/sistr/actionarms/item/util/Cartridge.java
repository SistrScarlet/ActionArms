package net.sistr.actionarms.item.util;

import java.util.Optional;
import net.minecraft.nbt.NbtCompound;
import net.sistr.actionarms.item.data.AADataRegistry;
import net.sistr.actionarms.item.data.BulletData;
import org.jetbrains.annotations.Nullable;

public class Cartridge {
    @Nullable private BulletData bullet;

    public Cartridge(@Nullable BulletData bullet) {
        this.bullet = bullet;
    }

    public Optional<BulletData> getBullet() {
        return Optional.ofNullable(bullet);
    }

    public void setBullet(@Nullable BulletData bullet) {
        this.bullet = bullet;
    }

    public boolean canShoot() {
        return bullet != null;
    }

    public boolean isEmpty() {
        return bullet == null;
    }

    public void read(NbtCompound nbt) {
        this.bullet = null;
        AADataRegistry.read(BulletData.class, nbt, "bullet").ifPresent(this::setBullet);
    }

    public void write(NbtCompound nbt) {
        AADataRegistry.write(this.bullet, nbt, "bullet");
    }
}
