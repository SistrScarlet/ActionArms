package net.sistr.actionarms.client.key;

import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.sistr.actionarms.entity.util.KeyInputManager;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class AAKeys {
    private static final Map<KeyInputManager.Key, KeyBinding> KEY_BINDINGS = new HashMap<>();
    private static final String KEY_CATEGORY = "key.actionarms.category";
    public static final KeyBinding KEY_FIRE = new KeyBinding(
            "key.actionarms.fire",
            InputUtil.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_LEFT,
            KEY_CATEGORY
    );
    public static final KeyBinding KEY_AIM = new KeyBinding(
            "key.actionarms.aim",
            InputUtil.Type.MOUSE,
            GLFW.GLFW_MOUSE_BUTTON_RIGHT,
            KEY_CATEGORY
    );
    public static final KeyBinding KEY_RELOAD = new KeyBinding(
            "key.actionarms.reload",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_R,
            KEY_CATEGORY
    );
    public static final KeyBinding KEY_COCKING = new KeyBinding(
            "key.actionarms.cocking",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_C,
            KEY_CATEGORY
    );

    public static void init() {
        setKey(AAKeys::register,
                KeyRecord.of(KeyInputManager.Key.FIRE, KEY_FIRE),
                KeyRecord.of(KeyInputManager.Key.COCK, KEY_COCKING),
                KeyRecord.of(KeyInputManager.Key.RELOAD, KEY_RELOAD),
                KeyRecord.of(KeyInputManager.Key.AIM, KEY_AIM)
        );
        setKey(AAKeys::setConflictInGameKeys, KEY_RELOAD, KEY_COCKING);
        setKey(AAKeys::setConflictMouseKeys, KEY_FIRE, KEY_AIM);
    }

    public static Map<KeyInputManager.Key, KeyBinding> getKeyBindings() {
        return KEY_BINDINGS;
    }

    private static void setKey(Consumer<KeyBinding> consumer, KeyBinding... keys) {
        for (KeyBinding key : keys) {
            consumer.accept(key);
        }
    }

    private static void setKey(Consumer<KeyRecord> consumer, KeyRecord... keys) {
        for (KeyRecord key : keys) {
            consumer.accept(key);
        }
    }

    private static void register(KeyRecord record) {
        KeyMappingRegistry.register(record.keyBinding);
        KEY_BINDINGS.put(record.key, record.keyBinding);
    }

    private static void setConflictInGameKeys(KeyBinding keyBinding) {
        KeyRegisterCallback.setConflictInGameKeys(keyBinding);
    }

    private static void setConflictMouseKeys(KeyBinding keyBinding) {
        KeyRegisterCallback.setConflictMouseKeys(keyBinding);
    }

    private record KeyRecord(KeyInputManager.Key key, KeyBinding keyBinding) {
        private static KeyRecord of(KeyInputManager.Key key, KeyBinding keyBinding) {
            return new KeyRecord(key, keyBinding);
        }
    }
}
