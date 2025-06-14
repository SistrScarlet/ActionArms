package net.sistr.actionarms.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.sistr.actionarms.client.key.ClientKeyInputManager;
import net.sistr.actionarms.entity.util.HasKeyInputManager;
import net.sistr.actionarms.entity.util.IKeyInputManager;
import net.sistr.actionarms.entity.util.KeyInputManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity implements HasKeyInputManager {
    @Unique
    private final KeyInputManager actionArms$keyInputManagerDummy = new KeyInputManager();

    @Override
    public IKeyInputManager actionArms$getKeyInputManager() {
        if (MinecraftClient.getInstance().player == (Object) this) {
            return ClientKeyInputManager.INSTANCE.getKeyInputManager();
        }
        return this.actionArms$keyInputManagerDummy;
    }
}
