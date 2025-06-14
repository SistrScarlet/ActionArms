package net.sistr.actionarms.item.component.registry;

import net.sistr.actionarms.item.component.BulletDataType;
import net.sistr.actionarms.item.component.LeverActionGunDataType;
import net.sistr.actionarms.item.component.MagazineDataType;

public class GunDataTypes {
    public static final BulletDataType MIDDLE_CALIBER = new BulletDataType(9, 12);
    public static final MagazineDataType LEVER_ACTION_TUBE_MAGAZINE =
            new MagazineDataType(10, bullet -> true);
    public static final LeverActionGunDataType M1873 =
            new LeverActionGunDataType(0.15f,
                    0.2f, 0.2f, 0.05f, 0.1f,
                    0.05f, 0.05f, 0.05f, 1);
}
