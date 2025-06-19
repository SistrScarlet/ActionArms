package net.sistr.actionarms.client.key.forge;

import net.minecraft.client.option.KeyBinding;
import net.minecraftforge.client.settings.IKeyConflictContext;
import net.minecraftforge.client.settings.KeyConflictContext;

public class KeyRegisterCallbackImpl {
    public static void setConflictInGameKeys(KeyBinding keyBinding) {
        keyBinding.setKeyConflictContext(KeyConflictContext.IN_GAME);
    }

    public static void setConflictMouseKeys(KeyBinding keyBinding) {
        keyBinding.setKeyConflictContext(new IKeyConflictContext() {
            @Override
            public boolean isActive() {
                return KeyConflictContext.IN_GAME.isActive();
            }

            @Override
            public boolean conflicts(IKeyConflictContext iKeyConflictContext) {
                return true;
            }
        });
    }
}
