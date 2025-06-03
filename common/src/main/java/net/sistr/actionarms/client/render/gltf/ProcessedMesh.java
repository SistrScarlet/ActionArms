package net.sistr.actionarms.client.render.gltf;

import java.util.ArrayList;
import java.util.List;

public class ProcessedMesh {
    private final String name;
    private final List<ProcessedVertex> vertices;
    private final int[] indices;
    private final List<MorphTarget> morphTargets;
    private final ProcessedSkin skin;
    private final int materialIndex;
    private final DrawingMode drawingMode;
    private final int primitiveIndex;

    // キャッシュ用の配列（パフォーマンス最適化）
    private float[] cachedPositions;
    private float[] cachedNormals;
    private float[] cachedUVs;
    private int[] cachedBoneIndices;
    private float[] cachedBoneWeights;
    private boolean cacheValid;

    public ProcessedMesh(String name) {
        this.name = name != null ? name : "Mesh";
        this.drawingMode = DrawingMode.TRIANGLES;
        this.vertices = new ArrayList<>();
        this.indices = null;
        this.morphTargets = new ArrayList<>();
        this.skin = null;
        this.materialIndex = -1;
        this.cacheValid = false;
        this.primitiveIndex = -1;
    }

    public ProcessedMesh(String name, List<ProcessedVertex> vertices, int[] indices,
                         List<MorphTarget> morphTargets, ProcessedSkin skin, int materialIndex,
                         DrawingMode drawingMode, int primitiveIndex) {
        this.name = name != null ? name : "Mesh";
        this.vertices = new ArrayList<>(vertices);
        this.indices = indices != null ? indices.clone() : null;
        this.morphTargets = new ArrayList<>(morphTargets);
        this.skin = skin;
        this.materialIndex = materialIndex;
        this.drawingMode = drawingMode;
        this.cacheValid = false;
        this.primitiveIndex = primitiveIndex;
    }

    // 新しいゲッター
    public DrawingMode getDrawingMode() {
        return drawingMode;
    }

    public int getPrimitiveIndex() {
        return primitiveIndex;
    }

    // 基本情報
    public String getName() {
        return name;
    }

    public int getVertexCount() {
        return vertices.size();
    }

    public int getTriangleCount() {
        return drawingMode == DrawingMode.TRIANGLES ?
                (indices != null ? indices.length / 3 : vertices.size() / 3) : 0;
    }

    // 頂点アクセス
    public List<ProcessedVertex> getVertices() {
        return new ArrayList<>(vertices);
    }

    public ProcessedVertex getVertex(int index) {
        return index >= 0 && index < vertices.size() ? vertices.get(index) : null;
    }

    // インデックス
    public int[] getIndices() {
        return indices != null ? indices.clone() : null;
    }

    // モーフターゲット
    public List<MorphTarget> getMorphTargets() {
        return new ArrayList<>(morphTargets);
    }

    public int getMorphTargetCount() {
        return morphTargets.size();
    }

    // スキニング
    public ProcessedSkin getSkin() {
        return skin;
    }

    public boolean hasSkinning() {
        return skin != null;
    }

    // マテリアル
    public int getMaterialIndex() {
        return materialIndex;
    }

    // キャッシュされた配列の取得（パフォーマンス最適化）
    public float[] getPositions() {
        updateCache();
        return cachedPositions.clone();
    }

    public float[] getNormals() {
        updateCache();
        return cachedNormals.clone();
    }

    public float[] getUvs() {
        updateCache();
        return cachedUVs.clone();
    }

    public int[] getBoneIndices() {
        updateCache();
        return cachedBoneIndices != null ? cachedBoneIndices.clone() : null;
    }

    public float[] getBoneWeights() {
        updateCache();
        return cachedBoneWeights != null ? cachedBoneWeights.clone() : null;
    }

    private void updateCache() {
        if (cacheValid) return;

        int vertexCount = vertices.size();
        cachedPositions = new float[vertexCount * 3];
        cachedNormals = new float[vertexCount * 3];
        cachedUVs = new float[vertexCount * 2];

        boolean hasSkinningData = vertices.stream().anyMatch(ProcessedVertex::hasSkinning);
        if (hasSkinningData) {
            cachedBoneIndices = new int[vertexCount * 4];
            cachedBoneWeights = new float[vertexCount * 4];
        }

        for (int i = 0; i < vertexCount; i++) {
            ProcessedVertex vertex = vertices.get(i);

            // 位置
            cachedPositions[i * 3] = vertex.getX();
            cachedPositions[i * 3 + 1] = vertex.getY();
            cachedPositions[i * 3 + 2] = vertex.getZ();

            // 法線
            cachedNormals[i * 3] = vertex.getNormalX();
            cachedNormals[i * 3 + 1] = vertex.getNormalY();
            cachedNormals[i * 3 + 2] = vertex.getNormalZ();

            // UV
            cachedUVs[i * 2] = vertex.getU();
            cachedUVs[i * 2 + 1] = vertex.getV();

            // スキニングデータ
            if (hasSkinningData) {
                System.arraycopy(vertex.getBoneIndices(), 0, cachedBoneIndices, i * 4, 4);
                System.arraycopy(vertex.getBoneWeights(), 0, cachedBoneWeights, i * 4, 4);
            }
        }

        cacheValid = true;
    }

    // キャッシュの無効化
    public void invalidateCache() {
        cacheValid = false;
    }

    @Override
    public String toString() {
        return String.format("Mesh[%s: %d vertices, mode=%s, primitive=%d, material=%d, skin=%s]",
                name, getVertexCount(), drawingMode.name(), primitiveIndex, materialIndex,
                hasSkinning() ? skin.getName() : "none");
    }
}
