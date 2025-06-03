package net.sistr.actionarms.client.render.gltf;

public class ProcessedKeyframe {
    private final float time;
    private final Object value;
    private final Object inTangent;  // キュービックスプライン用
    private final Object outTangent; // キュービックスプライン用
    
    // 基本コンストラクタ（線形・ステップ補間用）
    public ProcessedKeyframe(float time, Object value) {
        this.time = time;
        this.value = value;
        this.inTangent = null;
        this.outTangent = null;
    }
    
    // キュービックスプライン用コンストラクタ
    public ProcessedKeyframe(float time, Object value, Object inTangent, Object outTangent) {
        this.time = time;
        this.value = value;
        this.inTangent = inTangent;
        this.outTangent = outTangent;
    }
    
    public float getTime() { return time; }
    public Object getValue() { return value; }
    public Object getInTangent() { return inTangent; }
    public Object getOutTangent() { return outTangent; }
    
    public boolean hasSplineTangents() {
        return inTangent != null && outTangent != null;
    }
    
    @Override
    public String toString() {
        return String.format("Keyframe[t=%.3f, value=%s]", time, value.toString());
    }
}
