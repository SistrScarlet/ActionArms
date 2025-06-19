package net.sistr.actionarms.entity.util;

public interface IKeyInputManager {

    void tick();

    void input(KeyInputManager.Key key, boolean isPress);

    boolean isPress(KeyInputManager.Key key);

    boolean isPressPrev(KeyInputManager.Key key, int prev);

    boolean isTurnPress(KeyInputManager.Key key);

    boolean isTurnRelease(KeyInputManager.Key key);

    boolean isTurnPressPrev(KeyInputManager.Key key, int prev);

    boolean isTurnReleasePrev(KeyInputManager.Key key, int prev);

    boolean isTurnPressWithin(KeyInputManager.Key key, int nTicks);

    boolean isTurnReleaseWithin(KeyInputManager.Key key, int nTicks);

    void killTurnPressWithin(KeyInputManager.Key key, int nTicks);

}
