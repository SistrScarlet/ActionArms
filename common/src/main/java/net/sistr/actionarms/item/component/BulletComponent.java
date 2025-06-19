package net.sistr.actionarms.item.component;

import net.minecraft.nbt.NbtCompound;

public class BulletComponent implements IItemComponent {
    private final BulletDataType bulletDataType;

    public BulletComponent(BulletDataType bulletDataType) {
        this.bulletDataType = bulletDataType;
    }

    @Override
    public void read(NbtCompound nbt) {

    }

    @Override
    public void write(NbtCompound nbt) {

    }

    public BulletDataType getBulletDataType() {
        return bulletDataType;
    }

    public BulletComponent copy() {
        return new BulletComponent(this.bulletDataType);
    }
}
