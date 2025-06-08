package net.sistr.actionarms.item.component;

import net.minecraft.nbt.NbtCompound;
import net.sistr.actionarms.component.LeverActionGun;
import net.sistr.actionarms.component.LeverActionGunType;
import net.sistr.actionarms.component.MagazineType;

public class LeverActionGunComponent implements IItemComponent {
    private static final LeverActionGunType GUN_TYPE = new LeverActionGunType("lever_action",
            0.1f, 0.2f, 0.1f, 0.4f,
            0.1f, 0.1f, 0.1f, 0.1f,
            1);
    private static final MagazineType MAGAZINE_TYPE = new MagazineType("lever_action", 10, bullet -> true);
    public LeverActionGun leverActionGunItem;

    @Override
    public void read(NbtCompound nbt) {
        leverActionGunItem = new LeverActionGun(GUN_TYPE, MAGAZINE_TYPE);
        leverActionGunItem.read(nbt);
    }

    @Override
    public void write(NbtCompound nbt) {
        leverActionGunItem.write(nbt);
    }
}
