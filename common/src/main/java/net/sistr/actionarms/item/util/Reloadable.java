package net.sistr.actionarms.item.util;

import net.sistr.actionarms.item.data.BulletData;

import java.util.List;
import java.util.function.Predicate;

public interface Reloadable {

    boolean reload(LeverActionPlaySoundContext playSoundContext, AnimationContext animationContext, ReloadStartContext context);

    boolean canReload(ReloadStartContext context);

    boolean shouldReload();

    interface ReloadStartContext {
        boolean hasBullet(Predicate<BulletData> predicate);
    }

    interface ReloadTickContext {
        List<BulletData> popBullets(Predicate<BulletData> predicate, int count);

        void returnBullets(List<BulletData> bullets);
    }

}
