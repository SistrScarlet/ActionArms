package net.sistr.actionarms.item.util;

import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import net.sistr.actionarms.item.component.SAAGunComponent;

public interface SAAPlaySoundContext extends SAAGunComponent.SoundContext {

    @Override
    void playSound(String sound);

    enum Sound {
        COCK(() -> SoundSuppliers.REVOLVER_COCK, 0.5f, 1.0f),
        FIRE(() -> SoundSuppliers.REVOLVER_SHOT, 2.0f, 1.0f),
        DRY_FIRE(() -> SoundSuppliers.REVOLVER_DRY_FIRE, 0.5f, 1.0f),
        LOAD_BULLET(() -> SoundSuppliers.REVOLVER_LOAD_BULLET, 0.5f, 1.0f),
        EJECT(() -> SoundSuppliers.REVOLVER_EJECT, 0.5f, 1.0f),
        GATE_OPEN(() -> SoundSuppliers.REVOLVER_GATE_OPEN, 0.5f, 1.0f),
        GATE_CLOSE(() -> SoundSuppliers.REVOLVER_GATE_CLOSE, 0.5f, 1.0f);

        private final Supplier<Supplier<SoundEvent>> soundEventSupplier;
        private final float volume;
        private final float pitch;

        Sound(Supplier<Supplier<SoundEvent>> soundEventSupplier, float volume, float pitch) {
            this.soundEventSupplier = soundEventSupplier;
            this.volume = volume;
            this.pitch = pitch;
        }

        public Supplier<SoundEvent> getSoundEvent() {
            return soundEventSupplier.get();
        }

        public float getVolume() {
            return volume;
        }

        public float getPitch() {
            return pitch;
        }

        public void playSound(World world, Entity user, SoundCategory category) {
            world.playSound(
                    null,
                    user.getX(),
                    user.getY(),
                    user.getZ(),
                    getSoundEvent().get(),
                    category,
                    volume,
                    pitch);
        }
    }

    Map<String, Sound> SOUND_MAP =
            Map.of(
                    "COCK", Sound.COCK,
                    "FIRE", Sound.FIRE,
                    "DRY_FIRE", Sound.DRY_FIRE,
                    "LOAD_BULLET", Sound.LOAD_BULLET,
                    "EJECT", Sound.EJECT,
                    "GATE_OPEN", Sound.GATE_OPEN,
                    "GATE_CLOSE", Sound.GATE_CLOSE);

    /**
     * Registration への参照を別クラスに隔離し、Sound enum の初期化時に Registration がロードされることを防ぐ。テスト環境では Registration
     * を初期化できないため必要。
     */
    final class SoundSuppliers {
        private SoundSuppliers() {}

        static final Supplier<SoundEvent> REVOLVER_COCK =
                net.sistr.actionarms.setup.Registration.REVOLVER_COCK_SOUND;
        static final Supplier<SoundEvent> REVOLVER_SHOT =
                net.sistr.actionarms.setup.Registration.REVOLVER_SHOT_SOUND;
        static final Supplier<SoundEvent> REVOLVER_DRY_FIRE =
                net.sistr.actionarms.setup.Registration.REVOLVER_DRY_FIRE_SOUND;
        static final Supplier<SoundEvent> REVOLVER_LOAD_BULLET =
                net.sistr.actionarms.setup.Registration.REVOLVER_LOAD_BULLET_SOUND;
        static final Supplier<SoundEvent> REVOLVER_EJECT =
                net.sistr.actionarms.setup.Registration.REVOLVER_EJECT_SOUND;
        static final Supplier<SoundEvent> REVOLVER_GATE_OPEN =
                net.sistr.actionarms.setup.Registration.REVOLVER_GATE_OPEN_SOUND;
        static final Supplier<SoundEvent> REVOLVER_GATE_CLOSE =
                net.sistr.actionarms.setup.Registration.REVOLVER_GATE_CLOSE_SOUND;
    }
}
