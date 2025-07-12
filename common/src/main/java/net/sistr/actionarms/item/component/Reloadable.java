package net.sistr.actionarms.item.component;

import java.util.List;
import java.util.function.Predicate;

public interface Reloadable {

    boolean reload(LeverActionPlaySoundContext playSoundContext, AnimationContext animationContext, ReloadStartContext context);

    boolean canReload(ReloadStartContext context);

    boolean shouldReload();

    interface ReloadStartContext {
        boolean hasBullet(Predicate<BulletDataType> predicate);
    }

    interface ReloadTickContext {
        List<BulletDataType> popBullets(Predicate<BulletDataType> predicate, int count);

        void returnBullets(List<BulletDataType> bullets);
    }

}
