package net.sistr.actionarms.setup;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.item.LeverActionGunItem;
import net.sistr.actionarms.item.component.registry.GunComponentTypes;

public class Registration {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ActionArms.MOD_ID, RegistryKeys.ITEM);

    public static void init() {
        ITEMS.register();
    }

    public static final RegistrySupplier<Item> TEST_LEVER_ACTION_GUN = ITEMS.register(
            "test_lever_action_gun",
            () -> new LeverActionGunItem(
                    new Item.Settings().maxCount(1),
                    GunComponentTypes.M1873
            )
    );

}
