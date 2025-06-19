package net.sistr.actionarms.item.component;

import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.world.World;
import net.sistr.actionarms.setup.Registration;

import java.util.function.Supplier;

public interface LeverActionPlaySoundContext {

    void playSound(Sound sound);

    enum Sound {
        CYCLE(Registration.RIFLE_COCK_SOUND, 0.5f, 1.0f),
        RELOAD(Registration.RIFLE_LOAD_BULLET_SOUND, 0.5f, 1.0f),
        FIRE(Registration.RIFLE_SHOT_SOUND, 2.0f, 1.0f),
        DRY_FIRE(Registration.RIFLE_DRY_FIRE_SOUND, 0.5f, 1.0f);
        private final Supplier<SoundEvent> soundEvent;
        private final float volume;
        private final float pitch;

        Sound(Supplier<SoundEvent> soundEvent, float volume, float pitch) {
            this.soundEvent = soundEvent;
            this.volume = volume;
            this.pitch = pitch;
        }

        public Supplier<SoundEvent> getSoundEvent() {
            return soundEvent;
        }

        public float getVolume() {
            return volume;
        }

        public float getPitch() {
            return pitch;
        }

        public void playSound(World world, Entity user, SoundCategory category) {
            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    soundEvent.get(), category, volume, pitch);
        }

    }

}
