package net.sistr.actionarms.client.render.gltf.data;

import org.joml.Matrix4f;

public class ComputedBoneMatricesData {
    private final Matrix4f[] matrices;

    public ComputedBoneMatricesData(Matrix4f[] matrices) {
        this.matrices = matrices;
    }

    public Matrix4f[] getMatrices() {
        return matrices;
    }
}
