package net.sistr.actionarms.component;

import java.util.List;
import java.util.function.Predicate;

public interface Reloadable {

    boolean reload(ReloadStartContext context);

    boolean canReload();

    boolean shouldReload();

    boolean isReloading();

    float getReloadingTime();

    interface ReloadTickContext {
        List<Bullet> popBullets(Predicate<Bullet> predicate, int count);

        void returnBullets(List<Bullet> bullets);
    }

    interface ReloadStartContext {
        void reload(float seconds);
    }

}
