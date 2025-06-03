package net.sistr.actionarms.client.render.gltf;

public class ComputedVertexData {
    private final float[] finalPositions;
    private final float[] finalNormals;
    
    public ComputedVertexData(float[] finalPositions, float[] finalNormals) {
        this.finalPositions = finalPositions;
        this.finalNormals = finalNormals;
    }
    
    public float[] getFinalPositions() { return finalPositions; }
    public float[] getFinalNormals() { return finalNormals; }
}
