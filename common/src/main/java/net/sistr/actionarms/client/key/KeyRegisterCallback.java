package net.sistr.actionarms.client.key;

import dev.architectury.injectables.annotations.ExpectPlatform;
import net.minecraft.client.option.KeyBinding;

public class KeyRegisterCallback {
    @ExpectPlatform
    public static void setConflictInGameKeys(KeyBinding keyBinding) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void setConflictMouseKeys(KeyBinding keyBinding) {
        throw new AssertionError();
    }
}
