package net.sistr.actionarms.client.render.gltf;

import org.joml.Vector3f;

public class MorphTarget {
    private final String name;
    private final float[] positionDeltas;
    private final float[] normalDeltas;
    private final float[] uvDeltas;
    
    public MorphTarget(String name) {
        this.name = name != null ? name : "MorphTarget";
        this.positionDeltas = null;
        this.normalDeltas = null;
        this.uvDeltas = null;
    }
    
    public MorphTarget(String name, float[] positionDeltas, float[] normalDeltas, float[] uvDeltas) {
        this.name = name != null ? name : "MorphTarget";
        this.positionDeltas = positionDeltas != null ? positionDeltas.clone() : null;
        this.normalDeltas = normalDeltas != null ? normalDeltas.clone() : null;
        this.uvDeltas = uvDeltas != null ? uvDeltas.clone() : null;
    }
    
    public String getName() { return name; }
    
    public float[] getPositionDeltas() { 
        return positionDeltas != null ? positionDeltas.clone() : null; 
    }
    
    public float[] getNormalDeltas() { 
        return normalDeltas != null ? normalDeltas.clone() : null; 
    }
    
    public float[] getUvDeltas() { 
        return uvDeltas != null ? uvDeltas.clone() : null; 
    }
    
    public boolean hasPositionDeltas() { return positionDeltas != null; }
    public boolean hasNormalDeltas() { return normalDeltas != null; }
    public boolean hasUvDeltas() { return uvDeltas != null; }
    
    // 特定の頂点の差分を取得
    public Vector3f getPositionDelta(int vertexIndex) {
        if (positionDeltas == null || vertexIndex * 3 + 2 >= positionDeltas.length) {
            return new Vector3f(0, 0, 0);
        }
        
        return new Vector3f(
            positionDeltas[vertexIndex * 3],
            positionDeltas[vertexIndex * 3 + 1],
            positionDeltas[vertexIndex * 3 + 2]
        );
    }
    
    public Vector3f getNormalDelta(int vertexIndex) {
        if (normalDeltas == null || vertexIndex * 3 + 2 >= normalDeltas.length) {
            return new Vector3f(0, 0, 0);
        }
        
        return new Vector3f(
            normalDeltas[vertexIndex * 3],
            normalDeltas[vertexIndex * 3 + 1],
            normalDeltas[vertexIndex * 3 + 2]
        );
    }
    
    @Override
    public String toString() {
        return String.format("MorphTarget[%s: pos=%s, normal=%s, uv=%s]", 
                name, 
                hasPositionDeltas() ? "yes" : "no",
                hasNormalDeltas() ? "yes" : "no", 
                hasUvDeltas() ? "yes" : "no");
    }
}
