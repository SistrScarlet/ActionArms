package net.sistr.actionarms.item.component.registry;

import net.sistr.actionarms.item.component.BulletComponent;
import net.sistr.actionarms.item.component.LeverActionGunComponent;
import net.sistr.actionarms.item.component.MagazineComponent;

import java.util.function.Supplier;

public class GunComponentTypes {
    public static final Supplier<BulletComponent> MEDIUM_CALIBER_BULLET
            = () -> new BulletComponent(GunDataTypes.MEDIUM_CALIBER_BULLET);
    public static final Supplier<MagazineComponent> LEVER_ACTION_TUBE_MAGAZINE
            = () -> new MagazineComponent(GunDataTypes.LEVER_ACTION_TUBE_MAGAZINE);
    public static final Supplier<LeverActionGunComponent> M1873
            = () -> new LeverActionGunComponent(GunDataTypes.M1873, LEVER_ACTION_TUBE_MAGAZINE.get());
}
