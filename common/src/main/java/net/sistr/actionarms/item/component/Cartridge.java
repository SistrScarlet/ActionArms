package net.sistr.actionarms.item.component;

import net.minecraft.nbt.NbtCompound;
import net.sistr.actionarms.item.component.registry.GunComponentTypes;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class Cartridge {
    @Nullable
    private BulletComponent bullet;

    public Cartridge(@Nullable BulletComponent bullet) {
        this.bullet = bullet;
    }

    public Optional<BulletComponent> getBullet() {
        return Optional.ofNullable(bullet);
    }

    public void setBullet(@Nullable BulletComponent bullet) {
        this.bullet = bullet;
    }

    public boolean canShoot() {
        return bullet != null;
    }

    public boolean isEmpty() {
        return bullet == null;
    }

    public void read(NbtCompound nbt) {
        if (nbt.contains("bullet")) {
            var bulletNbt = nbt.getCompound("bullet");
            this.bullet = GunComponentTypes.MEDIUM_CALIBER_BULLET.get();
            this.bullet.read(bulletNbt);
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
