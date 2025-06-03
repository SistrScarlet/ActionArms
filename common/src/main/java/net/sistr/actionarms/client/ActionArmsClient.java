package net.sistr.actionarms.client;

import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.resource.ResourceType;
import net.sistr.actionarms.client.key.AAKeys;
import net.sistr.actionarms.client.render.gltf.GLTFModelManager;

public class ActionArmsClient {
    public static void init() {
    }

    public static void preInit() {
        AAKeys.init();
        ReloadListenerRegistry.register(ResourceType.CLIENT_RESOURCES, GLTFModelManager.INSTANCE);
    }

}
