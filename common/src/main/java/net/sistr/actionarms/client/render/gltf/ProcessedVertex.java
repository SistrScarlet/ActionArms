package net.sistr.actionarms.client.render.gltf;

public class ProcessedVertex {
    private float x, y, z;          // 位置
    private float nx, ny, nz;       // 法線
    private float u, v;             // UV座標
    private int[] boneIndices;      // ボーンインデックス（最大4つ）
    private float[] boneWeights;    // ボーンウェイト（最大4つ）
    private float[] morphDeltas;    // モーフターゲット用の差分データ
    
    public ProcessedVertex() {
        this.boneIndices = new int[4];
        this.boneWeights = new float[4];
    }
    
    public ProcessedVertex(float x, float y, float z, float nx, float ny, float nz, float u, float v) {
        this();
        setPosition(x, y, z);
        setNormal(nx, ny, nz);
        setUV(u, v);
    }
    
    // 位置
    public void setPosition(float x, float y, float z) {
        this.x = x; this.y = y; this.z = z;
    }
    
    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    
    // 法線
    public void setNormal(float nx, float ny, float nz) {
        this.nx = nx; this.ny = ny; this.nz = nz;
    }
    
    public float getNormalX() { return nx; }
    public float getNormalY() { return ny; }
    public float getNormalZ() { return nz; }
    
    // UV座標
    public void setUV(float u, float v) {
        this.u = u; this.v = v;
    }
    
    public float getU() { return u; }
    public float getV() { return v; }
    
    // スキニング情報
    public void setBoneData(int slot, int boneIndex, float weight) {
        if (slot >= 0 && slot < 4) {
            this.boneIndices[slot] = boneIndex;
            this.boneWeights[slot] = weight;
        }
    }
    
    public int getBoneIndex(int slot) {
        return slot >= 0 && slot < 4 ? boneIndices[slot] : -1;
    }
    
    public float getBoneWeight(int slot) {
        return slot >= 0 && slot < 4 ? boneWeights[slot] : 0.0f;
    }
    
    public int[] getBoneIndices() { return boneIndices.clone(); }
    public float[] getBoneWeights() { return boneWeights.clone(); }
    
    // ウェイトの正規化
    public void normalizeWeights() {
        float totalWeight = 0.0f;
        for (float weight : boneWeights) {
            totalWeight += weight;
        }
        
        if (totalWeight > 0.001f) {
            for (int i = 0; i < boneWeights.length; i++) {
                boneWeights[i] /= totalWeight;
            }
        }
    }
    
    // スキニングが有効かチェック
    public boolean hasSkinning() {
        for (float weight : boneWeights) {
            if (weight > 0.001f) return true;
        }
        return false;
    }
    
    @Override
    public String toString() {
        return String.format("Vertex[pos=(%.2f,%.2f,%.2f), normal=(%.2f,%.2f,%.2f), uv=(%.2f,%.2f)]",
                x, y, z, nx, ny, nz, u, v);
    }
}
