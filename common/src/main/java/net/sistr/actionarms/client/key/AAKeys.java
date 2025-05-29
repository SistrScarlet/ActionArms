package net.sistr.actionarms.client.key;

import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.function.Consumer;

public class AAKeys {
    public static final String KEY_CATEGORY = "key.actionarms.category";
    public static final KeyBinding KEY_SHOOT = new KeyBinding(
            "key.actionarms.shoot",
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
        setKey(AAKeys::register, KEY_SHOOT, KEY_AIM, KEY_RELOAD, KEY_COCKING);
        setKey(AAKeys::setConflictInGameKeys, KEY_RELOAD, KEY_COCKING);
        setKey(AAKeys::setConflictMouseKeys, KEY_SHOOT, KEY_AIM);
    }

    private static void setKey(Consumer<KeyBinding> consumer, KeyBinding... keyBindings) {
        for (KeyBinding keyBinding : keyBindings) {
            consumer.accept(keyBinding);
        }
    }

    private static void register(KeyBinding keyBinding) {
        KeyMappingRegistry.register(keyBinding);
    }

    private static void setConflictInGameKeys(KeyBinding keyBinding) {
        KeyRegisterCallback.setConflictInGameKeys(keyBinding);
    }

    private static void setConflictMouseKeys(KeyBinding keyBinding) {
        KeyRegisterCallback.setConflictMouseKeys(keyBinding);
    }
}
