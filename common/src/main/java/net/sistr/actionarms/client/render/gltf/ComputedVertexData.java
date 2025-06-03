package net.sistr.actionarms.client.render.gltf;

/**
 * 計算済み頂点データクラス - AccessorDataシステムと連携する効率的な設計
 * モーフターゲットとスキニング適用後の最終頂点データを保持
 */
public class ComputedVertexData {
    private final float[] finalPositions;
    private final float[] finalNormals;
    private final float[] uvCoordinates;
    private final int vertexCount;

    /**
     * 完全なコンストラクタ
     */
    public ComputedVertexData(float[] finalPositions, float[] finalNormals, float[] uvCoordinates) {
        this.finalPositions = finalPositions != null ? finalPositions.clone() : null;
        this.finalNormals = finalNormals != null ? finalNormals.clone() : null;
        this.uvCoordinates = uvCoordinates != null ? uvCoordinates.clone() : null;
        
        // 頂点数の計算（位置データから算出）
        this.vertexCount = finalPositions != null ? finalPositions.length / 3 : 0;
        
        validateData();
    }

    /**
     * 位置と法線のみのコンストラクタ
     */
    public ComputedVertexData(float[] finalPositions, float[] finalNormals) {
        this(finalPositions, finalNormals, null);
    }

    /**
     * 位置のみのコンストラクタ
     */
    public ComputedVertexData(float[] finalPositions) {
        this(finalPositions, null, null);
    }

    /**
     * データの整合性チェック
     */
    private void validateData() {
        if (finalPositions == null) {
            throw new IllegalArgumentException("Final positions cannot be null");
        }
        
        if (finalPositions.length % 3 != 0) {
            throw new IllegalArgumentException("Position array length must be divisible by 3");
        }
        
        if (finalNormals != null && finalNormals.length != finalPositions.length) {
            throw new IllegalArgumentException("Normal array length must match position array length");
        }
        
        if (uvCoordinates != null && uvCoordinates.length != (finalPositions.length / 3) * 2) {
            throw new IllegalArgumentException("UV array length must be 2/3 of position array length");
        }
    }

    // 基本情報
    public int getVertexCount() { return vertexCount; }
    public boolean hasNormals() { return finalNormals != null; }
    public boolean hasUvCoordinates() { return uvCoordinates != null; }

    // データ取得（防御的コピー）
    public float[] getFinalPositions() { 
        return finalPositions != null ? finalPositions.clone() : null; 
    }
    
    public float[] getFinalNormals() { 
        return finalNormals != null ? finalNormals.clone() : null; 
    }
    
    public float[] getUvCoordinates() { 
        return uvCoordinates != null ? uvCoordinates.clone() : null; 
    }

    // データ取得（読み取り専用、パフォーマンス重視）
    public float[] getFinalPositionsReadOnly() { return finalPositions; }
    public float[] getFinalNormalsReadOnly() { return finalNormals; }
    public float[] getUvCoordinatesReadOnly() { return uvCoordinates; }

    /**
     * 特定の頂点の位置を取得
     */
    public float[] getVertexPosition(int vertexIndex) {
        if (vertexIndex < 0 || vertexIndex >= vertexCount) {
            throw new IndexOutOfBoundsException("Vertex index out of bounds: " + vertexIndex);
        }
        
        int baseIndex = vertexIndex * 3;
        return new float[] {
            finalPositions[baseIndex],
            finalPositions[baseIndex + 1],
            finalPositions[baseIndex + 2]
        };
    }

    /**
     * 特定の頂点の法線を取得
     */
    public float[] getVertexNormal(int vertexIndex) {
        if (!hasNormals()) {
            return new float[]{0, 1, 0}; // デフォルト上向き法線
        }
        
        if (vertexIndex < 0 || vertexIndex >= vertexCount) {
            throw new IndexOutOfBoundsException("Vertex index out of bounds: " + vertexIndex);
        }
        
        int baseIndex = vertexIndex * 3;
        return new float[] {
            finalNormals[baseIndex],
            finalNormals[baseIndex + 1],
            finalNormals[baseIndex + 2]
        };
    }

    /**
     * 特定の頂点のUV座標を取得
     */
    public float[] getVertexUv(int vertexIndex) {
        if (!hasUvCoordinates()) {
            return new float[]{0, 0}; // デフォルトUV
        }
        
        if (vertexIndex < 0 || vertexIndex >= vertexCount) {
            throw new IndexOutOfBoundsException("Vertex index out of bounds: " + vertexIndex);
        }
        
        int baseIndex = vertexIndex * 2;
        return new float[] {
            uvCoordinates[baseIndex],
            uvCoordinates[baseIndex + 1]
        };
    }

    /**
     * 境界ボックスを計算
     */
    public BoundingBox calculateBoundingBox() {
        if (vertexCount == 0) {
            return new BoundingBox(0, 0, 0, 0, 0, 0);
        }
        
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        
        for (int i = 0; i < finalPositions.length; i += 3) {
            float x = finalPositions[i];
            float y = finalPositions[i + 1];
            float z = finalPositions[i + 2];
            
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }
        
        return new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }

    /**
     * メモリ使用量を計算
     */
    public int getMemoryUsage() {
        int total = 0;
        if (finalPositions != null) total += finalPositions.length * 4; // float = 4 bytes
        if (finalNormals != null) total += finalNormals.length * 4;
        if (uvCoordinates != null) total += uvCoordinates.length * 4;
        return total;
    }

    /**
     * デバッグ情報を取得
     */
    public String getDebugInfo() {
        return String.format("ComputedVertexData[vertices=%d, normals=%s, uvs=%s, memory=%d bytes]",
                           vertexCount, hasNormals() ? "yes" : "no", 
                           hasUvCoordinates() ? "yes" : "no", getMemoryUsage());
    }

    @Override
    public String toString() {
        return getDebugInfo();
    }

    /**
     * 境界ボックスクラス
     */
    public static class BoundingBox {
        public final float minX, minY, minZ;
        public final float maxX, maxY, maxZ;

        public BoundingBox(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }

        public float getWidth() { return maxX - minX; }
        public float getHeight() { return maxY - minY; }
        public float getDepth() { return maxZ - minZ; }

        public float[] getCenter() {
            return new float[]{
                (minX + maxX) / 2,
                (minY + maxY) / 2,
                (minZ + maxZ) / 2
            };
        }

        public float[] getSize() {
            return new float[]{getWidth(), getHeight(), getDepth()};
        }

        @Override
        public String toString() {
            return String.format("BoundingBox[min=(%.2f,%.2f,%.2f), max=(%.2f,%.2f,%.2f), size=(%.2f,%.2f,%.2f)]",
                               minX, minY, minZ, maxX, maxY, maxZ, getWidth(), getHeight(), getDepth());
        }
    }
}
