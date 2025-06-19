package net.sistr.actionarms.item;

import net.minecraft.item.Item;
import net.sistr.actionarms.item.component.BulletComponent;

import java.util.function.Supplier;

public class BulletItem extends Item {
    private final Supplier<BulletComponent> componentSupplier;

    public BulletItem(Settings settings, Supplier<BulletComponent> componentSupplier) {
        super(settings);
        this.componentSupplier = componentSupplier;
    }

    public Supplier<BulletComponent> getComponentSupplier() {
        return this.componentSupplier;
    }

}
