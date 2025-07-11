package net.sistr.actionarms.client.render.gltf.renderer;

import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * レンダリングに必要な全ての状態を保持するコンテキスト
 * イミュータブルな設計により副作用を防ぐ
 */
public record RenderingContext(
        float tickDelta,
        int light,
        int overlay,
        AnimationState[] animations,
        @Nullable Entity entity,
        List<String> hideBones  // 隠蔽対象ボーン名のリスト
) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private float tickDelta;
        private int light;
        private int overlay;
        private final List<AnimationState> animations = new ArrayList<>();
        @Nullable Entity entity;
        private final List<String> hideBones = new ArrayList<>();  // 隠蔽対象ボーン名のリスト

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

        public Builder addAnimationState(AnimationState state) {
            if (state != null) {
                this.animations.add(state);
            }
            return this;
        }

        public Builder addAnimationState(@Nullable List<AnimationState> states) {
            if (states != null) {
                this.animations.addAll(states);
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
            return new RenderingContext(
                    tickDelta,
                    light,
                    overlay,
                    animations.toArray(new AnimationState[0]),
                    entity,
                    List.copyOf(hideBones)
            );
        }
    }

    public record AnimationState(String name, float seconds, boolean isLooping) {
    }
}
