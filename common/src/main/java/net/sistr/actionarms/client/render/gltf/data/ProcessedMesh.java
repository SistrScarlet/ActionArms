package net.sistr.actionarms.client.render.gltf.data;

import net.sistr.actionarms.client.render.gltf.util.DrawingMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * メッシュデータ管理クラス
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
    public String getName() {
        return name;
    }

    public DrawingMode getDrawingMode() {
        return drawingMode;
    }

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

    public AccessorData getAttributeData(String attributeName) {
        return attributeData.get(attributeName);
    }

    public boolean hasAttribute(String attributeName) {
        return attributeData.containsKey(attributeName) &&
                attributeData.get(attributeName).isValid();
    }

    // 主要属性の便利メソッド
    public AccessorData getPositions() {
        return getAttributeData("POSITION");
    }

    public AccessorData getNormals() {
        return getAttributeData("NORMAL");
    }

    public AccessorData getUvs() {
        return getAttributeData("TEXCOORD_0");
    }

    public AccessorData getBoneIndices() {
        return getAttributeData("JOINTS_0");
    }

    public AccessorData getBoneWeights() {
        return getAttributeData("WEIGHTS_0");
    }

    // 存在チェック便利メソッド
    public boolean hasPositions() {
        return hasAttribute("POSITION");
    }

    public boolean hasSkinning() {
        return hasAttribute("JOINTS_0") && hasAttribute("WEIGHTS_0");
    }

    public boolean hasIndices() {
        return indexData != null && indexData.isValid();
    }

    /**
     * インデックス配列を取得（便利メソッド）
     */
    public int[] getIndices() {
        if (!hasIndices()) return null;
        return indexData.getIntDataReadOnly();
    }

    // モーフターゲット
    public List<MorphTarget> getMorphTargets() {
        return new ArrayList<>(morphTargets);
    }

    public int getMorphTargetCount() {
        return morphTargets.size();
    }

    public boolean hasMorphTargets() {
        return !morphTargets.isEmpty();
    }

    // スキニング
    public ProcessedSkin getSkin() {
        return skin;
    }

    /**
     * 生のUV配列を取得（読み取り専用）
     */
    public float[] getUvsRaw() {
        AccessorData uvData = getUvs();
        return uvData != null ? uvData.getFloatDataReadOnly() : null;
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
