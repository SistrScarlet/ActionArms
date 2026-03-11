package net.sistr.actionarms.item.util;

import java.util.List;
import java.util.function.Predicate;
import net.sistr.actionarms.item.data.BulletData;

public interface Reloadable {

    boolean loadBullet(
            LeverActionPlaySoundContext playSoundContext,
            AnimationContext animationContext,
            ReloadStartContext context);

    boolean canLoadBullet(ReloadStartContext context);

    boolean shouldLoadBullet();

    interface ReloadStartContext {
        boolean hasBullet(Predicate<BulletData> predicate);
    }

    interface ReloadTickContext {
        List<BulletData> popBullets(Predicate<BulletData> predicate, int count);

        void returnBullets(List<BulletData> bullets);
    }
}
