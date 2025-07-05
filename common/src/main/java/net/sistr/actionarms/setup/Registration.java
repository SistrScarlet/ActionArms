package net.sistr.actionarms.setup;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.entity.BulletEntity;
import net.sistr.actionarms.item.BulletItem;
import net.sistr.actionarms.item.LeverActionGunItem;
import net.sistr.actionarms.item.component.registry.GunComponentTypes;

public class Registration {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ActionArms.MOD_ID, RegistryKeys.ITEM);
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(ActionArms.MOD_ID, RegistryKeys.ENTITY_TYPE);
    private static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ActionArms.MOD_ID, RegistryKeys.SOUND_EVENT);

    public static void init() {
        ITEMS.register();
        ENTITY_TYPES.register();
        SOUND_EVENTS.register();
    }

    public static final RegistrySupplier<BulletItem> MEDIUM_CALIBER_BULLET = ITEMS.register(
            "medium_caliber_bullet", () -> new BulletItem(
                    new Item.Settings()
                            .arch$tab(ItemGroups.COMBAT),
                    GunComponentTypes.MEDIUM_CALIBER_BULLET)
    );

    public static final RegistrySupplier<Item> M1873 = ITEMS.register(
            "m1873",
            () -> new LeverActionGunItem(
                    new Item.Settings()
                            .maxDamage(256)
                            .arch$tab(ItemGroups.COMBAT),
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

    public static final RegistrySupplier<SoundEvent> RIFLE_SHOT_SOUND = registerS("item.gun.rifle.shot");
    public static final RegistrySupplier<SoundEvent> RIFLE_DRY_FIRE_SOUND = registerS("item.gun.rifle.dry_fire");
    public static final RegistrySupplier<SoundEvent> RIFLE_LOAD_BULLET_SOUND = registerS("item.gun.rifle.load_bullet");
    public static final RegistrySupplier<SoundEvent> RIFLE_COCK_SOUND = registerS("item.gun.rifle.cock");

    private static RegistrySupplier<SoundEvent> registerS(String id) {
        return SOUND_EVENTS.register(id, () -> SoundEvent.of(new Identifier(ActionArms.MOD_ID, id)));
    }

    public static final RegistryKey<DamageType> BULLET_DAMAGE_TYPE
            = RegistryKey.of(RegistryKeys.DAMAGE_TYPE, new Identifier(ActionArms.MOD_ID, "bullet"));
}
