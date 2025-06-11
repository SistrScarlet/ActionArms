package net.sistr.actionarms.client;

import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.minecraft.resource.ResourceType;
import net.sistr.actionarms.client.key.AAKeys;
import net.sistr.actionarms.client.key.ClientKeyInputManager;
import net.sistr.actionarms.client.render.entity.BulletEntityRenderer;
import net.sistr.actionarms.client.render.gltf.GltfModelManager;
import net.sistr.actionarms.client.render.gltf.ItemAnimationManager;
import net.sistr.actionarms.setup.Registration;

public class ActionArmsClient {
    public static void init() {
        ClientTickEvent.CLIENT_PRE.register(mc -> {
            if (mc.world == null) return;
            ClientKeyInputManager.INSTANCE.preTick();
        });
        ClientTickEvent.CLIENT_POST.register(mc -> {
            if (mc.world == null) return;
            ItemAnimationManager.INSTANCE.tick(1f / 20f);
            ClientKeyInputManager.INSTANCE.postTick();
        });
    }

    public static void preInit() {
        AAKeys.init();
        ReloadListenerRegistry.register(ResourceType.CLIENT_RESOURCES, GltfModelManager.INSTANCE);
        EntityRendererRegistry.register(Registration.BULLET_ENTITY, BulletEntityRenderer::new);
    }

}
