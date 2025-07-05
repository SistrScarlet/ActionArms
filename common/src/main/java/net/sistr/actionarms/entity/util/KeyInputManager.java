package net.sistr.actionarms.entity.util;

import java.util.EnumMap;
import java.util.Map;

public class KeyInputManager implements IKeyInputManager {
    private static final int LOG_SIZE = 10;
    private final Map<Key, InputLog> keyInputLogMap;
    private final Map<Key, Integer> killedKeyMap;

    public KeyInputManager() {
        keyInputLogMap = new EnumMap<>(Key.class);
        for (Key key : Key.values()) {
            keyInputLogMap.put(key, new InputLog());
        }
        killedKeyMap = new EnumMap<>(Key.class);
        for (Key key : Key.values()) {
            killedKeyMap.put(key, LOG_SIZE);
        }
    }

    @Override
    public void tick() {
        for (InputLog log : keyInputLogMap.values()) {
            log.tick();
        }
        killedKeyMap.replaceAll((key, integer) -> Math.max(integer - 1, 0));
    }

    @Override
    public void input(Key key, boolean isPress) {
        keyInputLogMap.get(key).input(isPress);
    }

    @Override
    public boolean isPress(Key key) {
        return keyInputLogMap.get(key).isPressed();
    }

    public boolean isPressPrev(Key key, int prev) {
        return keyInputLogMap.get(key).isPressedPrev(prev);
    }

    @Override
    public boolean isTurnPress(Key key) {
        return isPress(key) && !isPressPrev(key, 1);
    }

    @Override
    public boolean isTurnRelease(Key key) {
        return !isPress(key) && isPressPrev(key, 1);
    }

    @Override
    public boolean isTurnPressPrev(Key key, int prev) {
        return isPressPrev(key, prev) && !isPressPrev(key, prev + 1);
    }

    @Override
    public boolean isTurnReleasePrev(Key key, int prev) {
        return !isPressPrev(key, prev) && isPressPrev(key, prev + 1);
    }

    @Override
    public boolean isTurnPressWithin(Key key, int nTicks) {
        int killed = killedKeyMap.get(key);
        for (int i = 0; i < nTicks - killed; i++) {
            if (isTurnPressPrev(key, i)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isTurnReleaseWithin(Key key, int nTicks) {
        for (int i = 0; i < nTicks; i++) {
            if (isTurnReleasePrev(key, i)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void killTurnPressWithin(Key key, int nTicks) {
        killedKeyMap.put(key, Math.max(nTicks, killedKeyMap.get(key)));
    }

    public enum Key {
        FIRE,
        AIM,
        COCK,
        RELOAD;
    }

    public static class InputLog {
        private final boolean[] keyLog = new boolean[LOG_SIZE];
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

        public boolean isPressedPrev(int prev) {
            return keyLog[(nowIndex + keyLog.length - prev) % keyLog.length];
        }

    }
}
