package net.sistr.actionarms.client.render.gltf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * AccessorDataベースの効率的なメッシュデータ管理クラス
 * ProcessedVertexの配列ではなく、属性別のAccessorDataを直接保持してメモリ効率を向上
 */
public class ProcessedMesh {
    private final String name;
    private final Map<String, AccessorData> attributeData;
    private final AccessorData indexData;
    private final List<MorphTarget> morphTargets;
    private final ProcessedSkin skin;
    private final int materialIndex;
    private final DrawingMode drawingMode;
    private final int primitiveIndex;

    /**
     * AccessorDataベースのコンストラクタ（推奨）
     */
    public ProcessedMesh(String name, Map<String, AccessorData> attributeData, AccessorData indexData,
                         List<MorphTarget> morphTargets, ProcessedSkin skin, int materialIndex,
                         DrawingMode drawingMode, int primitiveIndex) {
        this.name = name != null ? name : "Mesh";
        this.attributeData = new HashMap<>(attributeData);
        this.indexData = indexData;
        this.morphTargets = new ArrayList<>(morphTargets);
        this.skin = skin;
        this.materialIndex = materialIndex;
        this.drawingMode = drawingMode != null ? drawingMode : DrawingMode.TRIANGLES;
        this.primitiveIndex = primitiveIndex;
        
        validateMesh();
    }

    /**
     * 簡単なコンストラクタ
     */
    public ProcessedMesh(String name) {
        this.name = name != null ? name : "Mesh";
        this.attributeData = new HashMap<>();
        this.indexData = null;
        this.morphTargets = new ArrayList<>();
        this.skin = null;
        this.materialIndex = -1;
        this.drawingMode = DrawingMode.TRIANGLES;
        this.primitiveIndex = -1;
    }

    /**
     * メッシュデータの整合性チェック
     */
    private void validateMesh() {
        if (!attributeData.containsKey("POSITION")) {
            throw new IllegalArgumentException("POSITION attribute is required for mesh: " + name);
        }

        AccessorData positionData = attributeData.get("POSITION");
        int expectedVertexCount = positionData.getElementCount();

        // すべての属性データが同じ要素数を持つかチェック
        for (Map.Entry<String, AccessorData> entry : attributeData.entrySet()) {
            String attrName = entry.getKey();
            AccessorData data = entry.getValue();
            
            if (data.getElementCount() != expectedVertexCount) {
                throw new IllegalArgumentException(
                    String.format("Attribute %s has %d elements, expected %d for mesh %s",
                                attrName, data.getElementCount(), expectedVertexCount, name));
            }
        }
    }

    // 基本情報
    public String getName() { return name; }
    public DrawingMode getDrawingMode() { return drawingMode; }
    public int getPrimitiveIndex() { return primitiveIndex; }

    /**
     * 頂点数を取得（POSITION属性の要素数）
     */
    public int getVertexCount() {
        AccessorData positionData = attributeData.get("POSITION");
        return positionData != null ? positionData.getElementCount() : 0;
    }

    /**
     * 三角形数を取得
     */
    public int getTriangleCount() {
        if (drawingMode != DrawingMode.TRIANGLES) return 0;
        
        if (indexData != null) {
            return indexData.getElementCount() / 3;
        } else {
            return getVertexCount() / 3;
        }
    }

    // 属性データアクセス
    public Map<String, AccessorData> getAttributeData() { 
        return new HashMap<>(attributeData); 
    }

    public AccessorData getAttributeData(String attributeName) {
        return attributeData.get(attributeName);
    }

    public Set<String> getAttributeNames() {
        return attributeData.keySet();
    }

    public boolean hasAttribute(String attributeName) {
        return attributeData.containsKey(attributeName) && 
               attributeData.get(attributeName).isValid();
    }

    // 主要属性の便利メソッド
    public AccessorData getPositions() { return getAttributeData("POSITION"); }
    public AccessorData getNormals() { return getAttributeData("NORMAL"); }
    public AccessorData getUvs() { return getAttributeData("TEXCOORD_0"); }
    public AccessorData getUvs1() { return getAttributeData("TEXCOORD_1"); }
    public AccessorData getColors() { return getAttributeData("COLOR_0"); }
    public AccessorData getBoneIndices() { return getAttributeData("JOINTS_0"); }
    public AccessorData getBoneWeights() { return getAttributeData("WEIGHTS_0"); }

    // 存在チェック便利メソッド
    public boolean hasPositions() { return hasAttribute("POSITION"); }
    public boolean hasNormals() { return hasAttribute("NORMAL"); }
    public boolean hasUvs() { return hasAttribute("TEXCOORD_0"); }
    public boolean hasColors() { return hasAttribute("COLOR_0"); }
    public boolean hasSkinning() { 
        return hasAttribute("JOINTS_0") && hasAttribute("WEIGHTS_0"); 
    }

    // インデックスデータ
    public AccessorData getIndexData() { return indexData; }
    public boolean hasIndices() { return indexData != null && indexData.isValid(); }

    /**
     * インデックス配列を取得（便利メソッド）
     */
    public int[] getIndices() {
        if (!hasIndices()) return null;
        return indexData.getIntDataReadOnly();
    }

    // モーフターゲット
    public List<MorphTarget> getMorphTargets() { return new ArrayList<>(morphTargets); }
    public int getMorphTargetCount() { return morphTargets.size(); }
    public boolean hasMorphTargets() { return !morphTargets.isEmpty(); }

    // スキニング
    public ProcessedSkin getSkin() { return skin; }

    // マテリアル
    public int getMaterialIndex() { return materialIndex; }

    /**
     * 生の位置配列を取得（読み取り専用）
     * パフォーマンス重視の場合のみ使用
     */
    public float[] getPositionsRaw() {
        AccessorData posData = getPositions();
        return posData != null ? posData.getFloatDataReadOnly() : null;
    }

    /**
     * 生の法線配列を取得（読み取り専用）
     */
    public float[] getNormalsRaw() {
        AccessorData normalData = getNormals();
        return normalData != null ? normalData.getFloatDataReadOnly() : null;
    }

    /**
     * 生のUV配列を取得（読み取り専用）
     */
    public float[] getUvsRaw() {
        AccessorData uvData = getUvs();
        return uvData != null ? uvData.getFloatDataReadOnly() : null;
    }

    /**
     * 生のボーンインデックス配列を取得（読み取り専用）
     */
    public int[] getBoneIndicesRaw() {
        AccessorData boneIndicesData = getBoneIndices();
        return boneIndicesData != null ? boneIndicesData.getIntDataReadOnly() : null;
    }

    /**
     * 生のボーンウェイト配列を取得（読み取り専用）
     */
    public float[] getBoneWeightsRaw() {
        AccessorData boneWeightsData = getBoneWeights();
        return boneWeightsData != null ? boneWeightsData.getFloatDataReadOnly() : null;
    }

    /**
     * 特定の頂点の位置を取得
     */
    public float[] getVertexPosition(int vertexIndex) {
        AccessorData posData = getPositions();
        if (posData == null || vertexIndex < 0 || vertexIndex >= posData.getElementCount()) {
            return new float[]{0, 0, 0};
        }
        return posData.getFloatElement(vertexIndex);
    }

    /**
     * 特定の頂点の法線を取得
     */
    public float[] getVertexNormal(int vertexIndex) {
        AccessorData normalData = getNormals();
        if (normalData == null || vertexIndex < 0 || vertexIndex >= normalData.getElementCount()) {
            return new float[]{0, 1, 0}; // デフォルト上向き法線
        }
        return normalData.getFloatElement(vertexIndex);
    }

    /**
     * 特定の頂点のUVを取得
     */
    public float[] getVertexUv(int vertexIndex) {
        AccessorData uvData = getUvs();
        if (uvData == null || vertexIndex < 0 || vertexIndex >= uvData.getElementCount()) {
            return new float[]{0, 0};
        }
        return uvData.getFloatElement(vertexIndex);
    }

    /**
     * メモリ使用量を計算
     */
    public int getMemoryUsage() {
        int total = 0;
        
        // 属性データ
        for (AccessorData data : attributeData.values()) {
            total += data.getMemoryUsage();
        }
        
        // インデックスデータ
        if (indexData != null) {
            total += indexData.getMemoryUsage();
        }
        
        // モーフターゲット
        for (MorphTarget morphTarget : morphTargets) {
            total += morphTarget.getMemoryUsage();
        }
        
        return total;
    }

    /**
     * デバッグ情報を取得
     */
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("ProcessedMesh[%s: %d vertices, mode=%s, primitive=%d, material=%d",
                               name, getVertexCount(), drawingMode.name(), primitiveIndex, materialIndex));
        
        sb.append(", attributes=[");
        attributeData.keySet().forEach(attr -> sb.append(attr).append(" "));
        sb.append("]");
        
        if (hasIndices()) {
            sb.append(String.format(", indices=%d", indexData.getElementCount()));
        }
        
        if (hasMorphTargets()) {
            sb.append(String.format(", morphs=%d", getMorphTargetCount()));
        }
        
        if (skin != null) {
            sb.append(String.format(", skin=%s", skin.getName()));
        }
        
        sb.append(String.format(", memory=%d bytes]", getMemoryUsage()));
        
        return sb.toString();
    }

    @Override
    public String toString() {
        return getDebugInfo();
    }
}
