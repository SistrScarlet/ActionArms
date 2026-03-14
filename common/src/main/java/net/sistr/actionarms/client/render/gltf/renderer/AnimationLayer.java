package net.sistr.actionarms.client.render.gltf.renderer;

public sealed interface AnimationLayer permits AnimationLayer.Clip, AnimationLayer.Procedural {

    int priority();

    record Clip(String animationName, float seconds, boolean looping, int priority)
            implements AnimationLayer {}

    record Procedural(String boneName, BoneTRSSupplier trsSupplier, int priority)
            implements AnimationLayer {}
}
