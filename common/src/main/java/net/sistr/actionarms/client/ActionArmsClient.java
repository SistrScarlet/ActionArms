package net.sistr.actionarms.client;

import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.minecraft.item.ItemStack;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.client.key.AAKeys;
import net.sistr.actionarms.client.key.ClientAimManager;
import net.sistr.actionarms.client.key.ClientKeyInputManager;
import net.sistr.actionarms.client.render.entity.BulletEntityRenderer;
import net.sistr.actionarms.client.render.gltf.data.ModelMetadata;
import net.sistr.actionarms.client.render.gltf.data.ProcessedGltfModel;
import net.sistr.actionarms.client.render.gltf.manager.GltfMetadataManager;
import net.sistr.actionarms.client.render.gltf.manager.GltfModelManager;
import net.sistr.actionarms.client.render.gltf.manager.GltfObjectRendererRegistry;
import net.sistr.actionarms.client.render.gltf.manager.ItemAnimationManager;
import net.sistr.actionarms.client.render.gltf.renderer.ActionArmsItemRenderer;
import net.sistr.actionarms.client.render.gltf.renderer.GltfObjectRenderer;
import net.sistr.actionarms.client.render.hud.AAHudRenderer;
import net.sistr.actionarms.client.render.hud.ClientHudManager;
import net.sistr.actionarms.setup.Registration;

public class ActionArmsClient {
    public static void init() {
        ClientTickEvent.CLIENT_PRE.register(mc -> {
            if (mc.world == null) return;
            ClientKeyInputManager.INSTANCE.preTick();
            ClientAimManager.INSTANCE.preTick();
            ClientHudManager.INSTANCE.preTick();
        });
        ClientTickEvent.CLIENT_POST.register(mc -> {
            if (mc.world == null) return;
            ItemAnimationManager.INSTANCE.tick(1f / 20f);
        });
        ClientGuiEvent.RENDER_HUD.register(AAHudRenderer.INSTANCE::render);
    }

    public static void preInit() {
        AAKeys.init();
        ReloadListenerRegistry.register(ResourceType.CLIENT_RESOURCES, GltfModelManager.INSTANCE);
        ReloadListenerRegistry.register(ResourceType.CLIENT_RESOURCES, GltfMetadataManager.INSTANCE);
        ReloadListenerRegistry.register(ResourceType.CLIENT_RESOURCES, GltfObjectRendererRegistry.INSTANCE);
        EntityRendererRegistry.register(Registration.BULLET_ENTITY, BulletEntityRenderer::new);
        registerGltfItem(new Identifier(ActionArms.MOD_ID, "m1873"), ActionArmsItemRenderer::new);
    }

    private static void registerGltfItem(Identifier id, GltfItemFactory<ItemStack> factory) {
        GltfObjectRendererRegistry.INSTANCE.registerRenderer(id,
                (context) -> {
                    var metadata = context.metadataManager().getModelMetadata(id).orElseThrow();
                    var model = context.modelManager().getLoadedModel(metadata.modelPath()).orElseThrow();
                    return factory.create(model, metadata);
                }
        );
    }

    @FunctionalInterface
    private interface GltfItemFactory<T> {
        GltfObjectRenderer<T> create(ProcessedGltfModel model, ModelMetadata metadata);
    }

}
