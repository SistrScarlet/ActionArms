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

    /** 弾を発射済みにし、弾データを返す。既に発射済みなら空を返す。 */
    public Optional<BulletData> spend() {
        var b = this.bullet;
        this.bullet = null;
        return Optional.ofNullable(b);
    }

    public boolean canShoot() {
        return bullet != null;
    }

    public boolean isEmpty() {
        return bullet == null;
    }

    public void read(NbtCompound nbt) {
        this.bullet = null;
        AADataRegistry.read(BulletData.class, nbt, "bullet").ifPresent(b -> this.bullet = b);
    }

    public void write(NbtCompound nbt) {
        AADataRegistry.write(this.bullet, nbt, "bullet");
    }
}
