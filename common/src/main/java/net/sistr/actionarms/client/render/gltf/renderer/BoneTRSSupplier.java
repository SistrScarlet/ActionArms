package net.sistr.actionarms.client.render.gltf.renderer;

@FunctionalInterface
public interface BoneTRSSupplier {
    void apply(BoneTRS trs);
}
