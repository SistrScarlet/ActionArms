package net.sistr.actionarms.item.util;

import java.util.function.Supplier;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;

public interface LeverActionPlaySoundContext {

    void playSound(Sound sound);

    enum Sound {
        CYCLE(() -> SoundSuppliers.RIFLE_COCK, 0.5f, 1.0f),
        RELOAD(() -> SoundSuppliers.RIFLE_LOAD_BULLET, 0.5f, 1.0f),
        FIRE(() -> SoundSuppliers.RIFLE_SHOT, 2.0f, 1.0f),
        DRY_FIRE(() -> SoundSuppliers.RIFLE_DRY_FIRE, 0.5f, 1.0f);

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

    /**
     * Registration への参照を別クラスに隔離し、Sound enum の初期化時に Registration がロードされることを防ぐ。テスト環境では Registration
     * を初期化できないため必要。
     */
    final class SoundSuppliers {
        private SoundSuppliers() {}

        static final Supplier<SoundEvent> RIFLE_COCK =
                net.sistr.actionarms.setup.Registration.RIFLE_COCK_SOUND;
        static final Supplier<SoundEvent> RIFLE_LOAD_BULLET =
                net.sistr.actionarms.setup.Registration.RIFLE_LOAD_BULLET_SOUND;
        static final Supplier<SoundEvent> RIFLE_SHOT =
                net.sistr.actionarms.setup.Registration.RIFLE_SHOT_SOUND;
        static final Supplier<SoundEvent> RIFLE_DRY_FIRE =
                net.sistr.actionarms.setup.Registration.RIFLE_DRY_FIRE_SOUND;
    }
}
