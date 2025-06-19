package net.sistr.actionarms.client.key;

import net.sistr.actionarms.entity.util.KeyInputManager;
import net.sistr.actionarms.network.KeyInputPacket;

import java.util.HashMap;

public class ClientKeyInputManager {
    public static final ClientKeyInputManager INSTANCE = new ClientKeyInputManager();
    private final KeyInputManager keyInputManager = new KeyInputManager();

    public void preTick() {
        keyInputManager.tick();

        var changedKeys = new HashMap<KeyInputManager.Key, Boolean>();
        for (var entry : AAKeys.getKeyBindings().entrySet()) {
            var key = entry.getKey();
            var keyBinding = entry.getValue();
            keyInputManager.input(key, keyBinding.isPressed());
            if (keyInputManager.isTurnPress(key)) {
                changedKeys.put(key, true);
            } else if (keyInputManager.isTurnRelease(key)) {
                changedKeys.put(key, false);
            }
        }
        if (!changedKeys.isEmpty()) {
            KeyInputPacket.sendC2S(changedKeys);
        }
    }

    public KeyInputManager getKeyInputManager() {
        return keyInputManager;
    }
}
