package net.sistr.actionarms.client.render.gltf.renderer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

/** レンダリングに必要な全ての状態を保持するコンテキスト イミュータブルな設計により副作用を防ぐ */
public record RenderingContext(
        float tickDelta,
        int light,
        int overlay,
        AnimationLayer[] layers,
        @Nullable Entity entity,
        List<String> hideBones // 隠蔽対象ボーン名のリスト
        ) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private float tickDelta;
        private int light;
        private int overlay;
        private final List<AnimationLayer> layers = new ArrayList<>();
        @Nullable Entity entity;
        private final List<String> hideBones = new ArrayList<>();

        public Builder tickDelta(float tickDelta) {
            this.tickDelta = tickDelta;
            return this;
        }

        public Builder light(int light) {
            this.light = light;
            return this;
        }

        public Builder overlay(int overlay) {
            this.overlay = overlay;
            return this;
        }

        public Builder addLayer(AnimationLayer layer) {
            if (layer != null) {
                this.layers.add(layer);
            }
            return this;
        }

        public Builder entity(@Nullable Entity entity) {
            this.entity = entity;
            return this;
        }

        public Builder hideBones(List<String> hideBones) {
            this.hideBones.clear();
            this.hideBones.addAll(hideBones);
            return this;
        }

        public Builder addHideBones(List<String> hideBones) {
            this.hideBones.addAll(hideBones);
            return this;
        }

        public RenderingContext build() {
            layers.sort(Comparator.comparingInt(AnimationLayer::priority));
            return new RenderingContext(
                    tickDelta,
                    light,
                    overlay,
                    layers.toArray(new AnimationLayer[0]),
                    entity,
                    List.copyOf(hideBones));
        }
    }
}
