package net.sistr.actionarms.item.component.registry;

import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.item.component.BulletDataType;
import net.sistr.actionarms.item.component.LeverActionGunDataType;
import net.sistr.actionarms.item.component.MagazineDataType;

public class GunDataTypes {
    public static final BulletDataType MEDIUM_CALIBER_BULLET = new BulletDataType(
            ActionArms.getConfig().game.medium_caliber_bullet_damage,
            ActionArms.getConfig().game.medium_caliber_bullet_headshot_damage
    );
    public static final MagazineDataType LEVER_ACTION_TUBE_MAGAZINE =
            new MagazineDataType(10, bullet -> true);
    public static final LeverActionGunDataType M1873 =
            new LeverActionGunDataType(0.3f,
                    0.2f, 0.2f, 0.1f, 0.2f,
                    0.05f, 0.05f, 0.05f, 1,
                    5.0f, 0.01f, 5.0f);
}
