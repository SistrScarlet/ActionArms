package net.sistr.actionarms.item.component;

import java.util.List;
import java.util.function.Predicate;

public interface Reloadable {

    boolean reload(AnimationContext animationContext);

    boolean canReload();

    boolean shouldReload();

    interface ReloadTickContext {
        List<BulletComponent> popBullets(Predicate<BulletComponent> predicate, int count);

        void returnBullets(List<BulletComponent> bullets);
    }

}
