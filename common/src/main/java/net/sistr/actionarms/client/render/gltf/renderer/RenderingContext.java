package net.sistr.actionarms.client.render.gltf.renderer;

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
        AnimationState[] animations) {

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        float tickDelta;
        int light;
        int overlay;
        List<AnimationState> animations = new ArrayList<>();

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
            this.animations.add(state);
            return this;
        }

        public RenderingContext build() {
            return new RenderingContext(
                    tickDelta,
                    light,
                    overlay,
                    animations.toArray(new AnimationState[0])
            );
        }
    }

    public record AnimationState(String name, float seconds) {
    }
}
