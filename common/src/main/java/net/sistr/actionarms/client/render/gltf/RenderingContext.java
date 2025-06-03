package net.sistr.actionarms.client.render.gltf;

import org.joml.Matrix4f;
import java.util.HashMap;
import java.util.Map;

public class RenderingContext {
    private Matrix4f[] boneMatrices;
    private float[] morphWeights;
    private float animationTime;
    private Map<String, Object> customProperties;
    
    public RenderingContext() {
        this.customProperties = new HashMap<>();
    }
    
    // ボーン行列
    public Matrix4f[] getBoneMatrices() { return boneMatrices; }
    public void setBoneMatrices(Matrix4f[] boneMatrices) { this.boneMatrices = boneMatrices; }
    
    // モーフウェイト
    public float[] getMorphWeights() { return morphWeights; }
    public void setMorphWeights(float[] morphWeights) { this.morphWeights = morphWeights; }
    
    // アニメーション時間
    public float getAnimationTime() { return animationTime; }
    public void setAnimationTime(float animationTime) { this.animationTime = animationTime; }
    
    // カスタムプロパティ
    public Map<String, Object> getCustomProperties() { return customProperties; }
    public void setCustomProperty(String key, Object value) { customProperties.put(key, value); }
    public Object getCustomProperty(String key) { return customProperties.get(key); }
}
