package net.sistr.actionarms.item;

import net.minecraft.item.Item;
import net.sistr.actionarms.item.data.BulletData;

public class BulletItem extends Item {
    private final BulletData bulletData;

    public BulletItem(Settings settings, BulletData bulletData) {
        super(settings);
        this.bulletData = bulletData;
    }

    public BulletData getBulletData() {
        return bulletData;
    }
}
