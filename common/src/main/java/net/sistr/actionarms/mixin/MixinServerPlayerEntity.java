package net.sistr.actionarms.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.sistr.actionarms.entity.util.GunController;
import net.sistr.actionarms.entity.util.HasGunController;
import net.sistr.actionarms.entity.util.HasKeyInputManager;
import net.sistr.actionarms.entity.util.KeyInputManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(ServerPlayerEntity.class)
public abstract class MixinServerPlayerEntity extends PlayerEntity implements HasKeyInputManager, HasGunController {
    @Unique
    private final KeyInputManager actionArms$keyInputManager = new KeyInputManager();
    @Unique
    private final GunController actionArms$gunController
            = new GunController(
                    (ServerPlayerEntity) (Object) this,
            actionArms$keyInputManager,
            this::getItems
            );

    public MixinServerPlayerEntity(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Override
    public KeyInputManager actionArms$getKeyInputManager() {
        return this.actionArms$keyInputManager;
    }

    @Override
    public GunController actionArms$getGunController() {
        return this.actionArms$gunController;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        this.actionArms$gunController.tick();
    }

    @Unique
    private List<ItemStack> getItems() {
        var items = new ArrayList<ItemStack>();

        var inv = this.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            var stack = inv.getStack(i);
            if (stack.isEmpty()) {
                continue;
            }
            items.add(stack);
        }
        return items;
    }
}
