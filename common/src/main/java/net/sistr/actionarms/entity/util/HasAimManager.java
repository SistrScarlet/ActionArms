package net.sistr.actionarms.entity.util;

import java.util.Optional;

public interface HasAimManager {
    IAimManager actionArms$getAimManager();

    static Optional<IAimManager> get(Object user) {
        return Optional.ofNullable(user instanceof HasAimManager manager ? manager.actionArms$getAimManager() : null);
    }
}
