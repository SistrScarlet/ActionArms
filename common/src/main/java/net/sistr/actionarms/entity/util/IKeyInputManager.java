package net.sistr.actionarms.entity.util;

public interface IKeyInputManager {

    void tick();

    void input(KeyInputManager.Key key, boolean isPress);

    boolean isPress(KeyInputManager.Key key);

    boolean isPressPrev(KeyInputManager.Key key);

    boolean isTurnPress(KeyInputManager.Key key);

    boolean isTurnRelease(KeyInputManager.Key key);

}
