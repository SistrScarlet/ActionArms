package net.sistr.actionarms.client.render.gltf.renderer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.sistr.actionarms.client.render.gltf.data.ModelMetadata;
import net.sistr.actionarms.client.render.gltf.data.ProcessedGltfModel;
import net.sistr.actionarms.client.render.gltf.data.ProcessedMesh;
import net.sistr.actionarms.client.render.gltf.manager.ItemAnimationManager;
import net.sistr.actionarms.client.render.gltf.util.DrawingMode;
import net.sistr.actionarms.entity.util.HasAimManager;
import net.sistr.actionarms.item.LeverActionGunItem;
import net.sistr.actionarms.item.component.IComponent;
import org.jetbrains.annotations.Nullable;

/** ActionArms用ItemStackレンダラー glTFレンダリング実装 */
public class ActionArmsItemRenderer extends GltfObjectRenderer<ItemStack> {

    public ActionArmsItemRenderer(ProcessedGltfModel model, ModelMetadata metadata) {
        super(model, metadata);
    }

    @Override
    public RenderingContext.Builder createRenderContext(
            ItemStack stack, ModelTransformationMode mode, @Nullable LivingEntity entity) {
        RenderingContext.Builder builder = RenderingContext.builder();
        for (var layer : createAnimationLayers(stack, mode, entity)) {
            builder.addLayer(layer);
        }
        return builder;
    }

    /** 隠蔽コンテキストキーを取得 */
    @Override
    protected List<String> getHideKeys(
            ModelTransformationMode mode, @Nullable LivingEntity entity) {
        var list = new ArrayList<String>(1);
        if (mode != ModelTransformationMode.FIRST_PERSON_RIGHT_HAND) {
            list.add("fpv");
        }
        return list;
    }

    protected List<AnimationLayer> createAnimationLayers(
            ItemStack stack, ModelTransformationMode mode, @Nullable LivingEntity entity) {
        if (!(stack.getItem() instanceof LeverActionGunItem gunItem)) {
            return List.of();
        }
        if (entity == null) {
            return List.of();
        }

        var layers = new ArrayList<AnimationLayer>();
        float tickDelta = MinecraftClient.getInstance().getTickDelta();
        var gunComponent = IComponent.query(gunItem.getGunComponent(), stack, c -> c);
        boolean isAiming =
                entity instanceof HasAimManager hasAim
                        && hasAim.actionArms$getAimManager().isAiming();
        float entityAge = entity.age * (1.0f / 20) + tickDelta;

        // Priority 10: 状態ポーズ
        layers.add(
                new AnimationLayer.Clip(
                        gunComponent.isHammerReady() ? "hammerReady" : "hammerNotReady",
                        entityAge,
                        true,
                        10));
        layers.add(
                new AnimationLayer.Clip(
                        gunComponent.isLeverDown() ? "leverDown" : "leverUp", entityAge, true, 10));

        // FPV 専用
        if (mode == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND) {
            float secondDelta = tickDelta * (1f / 20f);

            // Priority 0: アイドル
            layers.add(
                    new AnimationLayer.Clip(isAiming ? "idle_aiming" : "idle", entityAge, true, 0));

            // Priority 30: ワンショット
            var itemStates = ItemAnimationManager.INSTANCE.getItemStateMap(stack);
            itemStates.values().stream()
                    .sorted(
                            Comparator.comparingDouble(ItemAnimationManager.State::seconds)
                                    .reversed())
                    .forEach(
                            state -> {
                                String id = state.id();
                                if (isAiming) {
                                    id += "_aiming";
                                }
                                layers.add(
                                        new AnimationLayer.Clip(
                                                id, state.seconds() + secondDelta, false, 30));
                            });
        }

        return layers;
    }

    /** レンダーレイヤーの決定 */
    @Override
    public RenderLayer getRenderLayer(
            RenderingContext context, ProcessedMesh mesh, ModelMetadata metadata) {
        if (mesh.drawingMode() != DrawingMode.TRIANGLES) {
            throw new IllegalArgumentException(
                    "三角形描画以外は対応していません。drawingMode: " + mesh.drawingMode());
        }

        var material = mesh.getMaterial();
        var textureFileName = material.baseColorTexture();

        var textureMap = metadata.textureSettings().textureMap();
        Identifier textureResource = textureMap.get(textureFileName);

        if (textureResource == null) {
            throw new IllegalArgumentException("テクスチャリソースが見つかりません: " + textureFileName);
        }

        if (metadata.textureSettings().dynamicTextures().containsKey(textureFileName)) {
            String contextName = metadata.textureSettings().dynamicTextures().get(textureFileName);
            if ("player_skin".equals(contextName)) {
                if (context.entity() instanceof AbstractClientPlayerEntity player) {
                    var skinTexture = player.getSkinTexture();
                    return GltfRenderLayer.getEntityTranslucentTriangle(skinTexture, true);
                }
                return GltfRenderLayer.getEntityTranslucentTriangle(textureResource, true);
            }
        }

        return GltfRenderLayer.getEntityCutoutTriangle(textureResource);
    }
}
