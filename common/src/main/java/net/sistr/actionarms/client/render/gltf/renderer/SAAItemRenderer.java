package net.sistr.actionarms.client.render.gltf.renderer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.sistr.actionarms.client.render.gltf.data.ModelMetadata;
import net.sistr.actionarms.client.render.gltf.data.ProcessedGltfModel;
import net.sistr.actionarms.client.render.gltf.manager.ItemAnimationManager;
import net.sistr.actionarms.client.render.hud.ClientHudManager;
import net.sistr.actionarms.hud.SAAHudState;
import net.sistr.actionarms.item.ItemUniqueManager;
import net.sistr.actionarms.item.SAAGunItem;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

public class SAAItemRenderer extends ActionArmsItemRenderer {
    private static final int CYLINDER_CAPACITY = 6;
    private static final float STEP_ANGLE = (float) (2.0 * Math.PI / CYLINDER_CAPACITY);

    private final Map<UUID, CylinderState> cylinderStates = new HashMap<>();

    public SAAItemRenderer(ProcessedGltfModel model, ModelMetadata metadata) {
        super(model, metadata);
    }

    public void tickCylinderAnimation() {
        cylinderStates.values().forEach(CylinderState::tick);
        cylinderStates.values().removeIf(CylinderState::isExpired);
    }

    @Override
    protected List<AnimationLayer> createAnimationLayers(
            ItemStack stack, ModelTransformationMode mode, @Nullable LivingEntity entity) {
        if (!(stack.getItem() instanceof SAAGunItem)) {
            return List.of();
        }
        if (entity == null) {
            return List.of();
        }

        var layers = new ArrayList<AnimationLayer>();
        float tickDelta = MinecraftClient.getInstance().getTickDelta();
        float entityAge = entity.age * (1.0f / 20) + tickDelta;
        var uuid = ItemUniqueManager.INSTANCE.getOrSet(stack);

        Optional<SAAHudState> hudStateOpt =
                ClientHudManager.INSTANCE.getState("saa@" + uuid, SAAHudState::of);

        // Priority 10: 状態ポーズ
        boolean hammerCocked = hudStateOpt.map(SAAHudState::hammerCocked).orElse(false);
        layers.add(
                new AnimationLayer.Clip(
                        hammerCocked ? "hammerCocked" : "hammerNotCocked", entityAge, true, 10));

        boolean gateOpen = hudStateOpt.map(SAAHudState::gateOpen).orElse(false);
        layers.add(
                new AnimationLayer.Clip(gateOpen ? "gateOpen" : "gateClosed", entityAge, true, 10));

        // Priority 20: シリンダー回転 (Procedural)
        float cylinderAngle = getCylinderAngle(uuid, hudStateOpt, tickDelta);
        layers.add(
                new AnimationLayer.Procedural(
                        "cylinder",
                        trs -> {
                            var q = new Quaternionf().rotateY(cylinderAngle);
                            trs.setRotation(q);
                        },
                        20));

        // FPV 専用
        if (mode == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND) {
            float secondDelta = tickDelta * (1f / 20f);

            // Priority 0: アイドル
            layers.add(new AnimationLayer.Clip("idle", entityAge, true, 0));

            // Priority 30: ワンショット
            var itemStates = ItemAnimationManager.INSTANCE.getItemStateMap(stack);
            itemStates.values().stream()
                    .sorted(
                            Comparator.comparingDouble(ItemAnimationManager.State::seconds)
                                    .reversed())
                    .forEach(
                            state ->
                                    layers.add(
                                            new AnimationLayer.Clip(
                                                    state.id(),
                                                    state.seconds() + secondDelta,
                                                    false,
                                                    30)));
        }

        // hideBones: 弾丸・薬莢の表示切替
        // (getHideKeys ではなく createRenderContext で直接 Builder に追加)

        return layers;
    }

    @Override
    public RenderingContext.Builder createRenderContext(
            ItemStack stack, ModelTransformationMode mode, @Nullable LivingEntity entity) {
        var builder = super.createRenderContext(stack, mode, entity);

        // 弾丸・薬莢の表示/非表示
        if (stack.getItem() instanceof SAAGunItem) {
            var uuid = ItemUniqueManager.INSTANCE.getOrSet(stack);
            Optional<SAAHudState> hudStateOpt =
                    ClientHudManager.INSTANCE.getState("saa@" + uuid, SAAHudState::of);
            hudStateOpt.ifPresent(
                    hudState -> {
                        var states = hudState.chamberStates();
                        for (int i = 0; i < states.size(); i++) {
                            if (states.get(i) != SAAHudState.ChamberState.LOADED) {
                                builder.addHideBone("bullet_" + i);
                            }
                            if (states.get(i) != SAAHudState.ChamberState.SPENT) {
                                builder.addHideBone("cartridge_" + i);
                            }
                        }
                    });
        }

        return builder;
    }

    private float getCylinderAngle(UUID uuid, Optional<SAAHudState> hudStateOpt, float tickDelta) {
        var state = cylinderStates.computeIfAbsent(uuid, k -> new CylinderState());
        state.markUsed();

        if (hudStateOpt.isPresent()) {
            int firingIndex = hudStateOpt.get().firingIndex();
            state.updateTarget(firingIndex, CYLINDER_CAPACITY);
        }

        return state.getInterpolatedAngle(tickDelta);
    }

    static class CylinderState {
        private int prevFiringIndex = -1;
        private float prevRotation;
        private float currentRotation;
        private float targetRotation;
        private int unusedTicks;

        void updateTarget(int firingIndex, int capacity) {
            float stepAngle = (float) (2.0 * Math.PI / capacity);
            if (prevFiringIndex == -1) {
                prevFiringIndex = firingIndex;
                targetRotation = -stepAngle * firingIndex;
                prevRotation = targetRotation;
                currentRotation = targetRotation;
            } else if (firingIndex != prevFiringIndex) {
                int diff = firingIndex - prevFiringIndex;
                if (diff > capacity / 2) diff -= capacity;
                if (diff < -capacity / 2) diff += capacity;
                targetRotation += -stepAngle * diff;
                prevFiringIndex = firingIndex;
            }
        }

        void tick() {
            prevRotation = currentRotation;
            currentRotation = targetRotation;
            unusedTicks++;
        }

        float getInterpolatedAngle(float tickDelta) {
            return prevRotation + (currentRotation - prevRotation) * tickDelta;
        }

        void markUsed() {
            unusedTicks = 0;
        }

        boolean isExpired() {
            return unusedTicks > 100;
        }
    }
}
