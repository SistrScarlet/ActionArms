package net.sistr.actionarms.component;

import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class Cartridge {
    @Nullable
    private Bullet bullet;

    public Cartridge(@Nullable Bullet bullet) {
        this.bullet = bullet;
    }

    public Optional<Bullet> getBullet() {
        return Optional.ofNullable(bullet);
    }

    public void setBullet(@Nullable Bullet bullet) {
        this.bullet = bullet;
    }

    public boolean isInBullet() {
        return bullet != null;
    }

    public boolean canShoot() {
        return bullet != null;
    }

    public void read(NbtCompound nbt) {
        if (nbt.contains("bullet")) {
            this.bullet = Bullet.read(nbt.getCompound("bullet"));
        }
    }

    public void write(NbtCompound nbt) {
        if (this.bullet != null) {
            var bulletNbt = new NbtCompound();
            this.bullet.write(bulletNbt);
            nbt.put("bullet", bulletNbt);
        }
    }

}
