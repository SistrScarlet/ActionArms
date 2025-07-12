package net.sistr.actionarms.item.component;

import net.minecraft.nbt.NbtCompound;
import net.sistr.actionarms.item.component.registry.GunDataTypes;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

public class Cartridge {
    @Nullable
    private BulletDataType bullet;

    public Cartridge(@Nullable BulletDataType bullet) {
        this.bullet = bullet;
    }

    public Optional<BulletDataType> getBullet() {
        return Optional.ofNullable(bullet);
    }

    public void setBullet(@Nullable BulletDataType bullet) {
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
            this.bullet = GunDataTypes.MEDIUM_CALIBER_BULLET;
        }
    }

    public void write(NbtCompound nbt) {
        if (this.bullet != null) {
            var bulletNbt = new NbtCompound();
            nbt.put("bullet", bulletNbt);
        }
    }

}
