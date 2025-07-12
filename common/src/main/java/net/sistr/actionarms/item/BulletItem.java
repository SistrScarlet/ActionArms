package net.sistr.actionarms.item;

import net.minecraft.item.Item;
import net.sistr.actionarms.item.component.BulletDataType;

import java.util.function.Supplier;

public class BulletItem extends Item {
    private final Supplier<BulletDataType> componentSupplier;

    public BulletItem(Settings settings, Supplier<BulletDataType> componentSupplier) {
        super(settings);
        this.componentSupplier = componentSupplier;
    }

    public Supplier<BulletDataType> getComponentSupplier() {
        return this.componentSupplier;
    }

}
