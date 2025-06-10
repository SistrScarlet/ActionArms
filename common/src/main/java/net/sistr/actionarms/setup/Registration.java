package net.sistr.actionarms.setup;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.entity.BulletEntity;
import net.sistr.actionarms.item.LeverActionGunItem;
import net.sistr.actionarms.item.component.registry.GunComponentTypes;

public class Registration {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ActionArms.MOD_ID, RegistryKeys.ITEM);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ActionArms.MOD_ID, RegistryKeys.ENTITY_TYPE);

    public static void init() {
        ITEMS.register();
        ENTITY_TYPES.register();
    }

    public static final RegistrySupplier<Item> TEST_LEVER_ACTION_GUN = ITEMS.register(
            "test_lever_action_gun",
            () -> new LeverActionGunItem(
                    new Item.Settings().maxCount(1),
                    GunComponentTypes.M1873
            )
    );

    public static final RegistrySupplier<EntityType<BulletEntity>> BULLET_ENTITY = ENTITY_TYPES.register(
            "bullet",
            () -> EntityType.Builder.create(BulletEntity::new, SpawnGroup.MISC)
                    .setDimensions(0.05f, 0.05f)
                    .maxTrackingRange(4)
                    .trackingTickInterval(20)
                    .build("bullet")
    );

}
