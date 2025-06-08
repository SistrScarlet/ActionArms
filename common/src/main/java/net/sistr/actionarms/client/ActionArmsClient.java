package net.sistr.actionarms.client;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.resource.ResourceType;
import net.sistr.actionarms.client.key.AAKeys;
import net.sistr.actionarms.client.render.gltf.GltfModelManager;
import net.sistr.actionarms.client.render.gltf.ItemAnimationManager;
import net.sistr.actionarms.item.ItemUniqueManager;

public class ActionArmsClient {
    public static void init() {
        ClientTickEvent.CLIENT_POST.register(mc -> {
            if (mc.world == null) return;
            ItemAnimationManager.INSTANCE.tick(1f / 20f);
        });
    }

    public static void preInit() {
        AAKeys.init();
        ReloadListenerRegistry.register(ResourceType.CLIENT_RESOURCES, GltfModelManager.INSTANCE);
    }

}
