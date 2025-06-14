package net.sistr.actionarms.entity.util;

import java.util.HashMap;
import java.util.Map;

public class KeyInputManager implements IKeyInputManager {
    private final Map<Key, InputLog> keyInputLogMap;

    public KeyInputManager() {
        keyInputLogMap = new HashMap<>();
        for (Key key : Key.values()) {
            keyInputLogMap.put(key, new InputLog());
        }
    }

    @Override
    public void tick() {
        for (InputLog log : keyInputLogMap.values()) {
            log.tick();
        }
    }

    @Override
    public void input(Key key, boolean isPress) {
        keyInputLogMap.get(key).input(isPress);
    }

    @Override
    public boolean isPress(Key key) {
        return keyInputLogMap.get(key).isPressed();
    }

    @Override
    public boolean isPressPrev(Key key) {
        return keyInputLogMap.get(key).isPressedPrev();
    }

    @Override
    public boolean isTurnPress(Key key) {
        return isPress(key) && !isPressPrev(key);
    }

    @Override
    public boolean isTurnRelease(Key key) {
        return !isPress(key) && isPressPrev(key);
    }

    public enum Key {
        FIRE,
        AIM,
        COCK,
        RELOAD;
    }

    public static class InputLog {
        private final boolean[] keyLog = new boolean[8];
        private int nowIndex;

        public void tick() {
            int prev = nowIndex;
            nowIndex = (nowIndex + 1) % keyLog.length;
            keyLog[nowIndex] = keyLog[prev];
        }

        public void input(boolean isPress) {
            keyLog[nowIndex] = isPress;
        }

        public boolean isPressed() {
            return keyLog[nowIndex];
        }

        public boolean isPressedPrev() {
            return keyLog[(nowIndex + keyLog.length - 1) % keyLog.length];
        }

    }
}
