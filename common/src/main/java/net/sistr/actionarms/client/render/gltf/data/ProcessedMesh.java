package net.sistr.actionarms.client.render.gltf.data;

import net.sistr.actionarms.client.render.gltf.util.DrawingMode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * メッシュデータ管理recordクラス（マテリアル参照対応・不変設計）
 * glTFプリミティブから変換されたメッシュ情報を保持
 */
public record ProcessedMesh(
        String name,
        Map<String, AccessorData> attributeData,
        AccessorData indexData,
        List<MorphTarget> morphTargets,
        @Nullable ProcessedSkin skin,
        @Nullable ProcessedMaterial material,
        DrawingMode drawingMode,
        int primitiveIndex
) {

    /**
     * コンストラクタでのバリデーションと不変化
     */
    public ProcessedMesh {
        // 名前のデフォルト値設定
        if (name == null || name.trim().isEmpty()) {
            name = "Mesh_" + primitiveIndex;
        }

        // 属性データの防御的コピーと不変化
        if (attributeData == null) {
            attributeData = Collections.emptyMap();
        } else {
            attributeData = Map.copyOf(attributeData);
        }

        // モーフターゲットの不変化
        if (morphTargets == null) {
            morphTargets = Collections.emptyList();
        } else {
            morphTargets = List.copyOf(morphTargets);
        }

        // 描画モードのデフォルト値設定
        if (drawingMode == null) {
            drawingMode = DrawingMode.TRIANGLES;
        }

        // メッシュデータの整合性チェック
        validateMesh(name, attributeData);
    }

    /**
     * メッシュデータの整合性チェック
     */
    private static void validateMesh(String name, Map<String, AccessorData> attributeData) {
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

    // === 基本情報アクセサー ===

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

    // === 属性データアクセサー ===

    @Nullable
    public AccessorData getAttributeData(String attributeName) {
        return attributeData.get(attributeName);
    }

    public boolean hasAttribute(String attributeName) {
        return attributeData.containsKey(attributeName) &&
                attributeData.get(attributeName).isValid();
    }

    // 主要属性の便利メソッド
    public AccessorData getPositions() {
        var positions = getAttributeData("POSITION");
        assert positions != null;
        return positions;
    }

    @Nullable
    public AccessorData getNormals() {
        return getAttributeData("NORMAL");
    }

    @Nullable
    public AccessorData getUvs() {
        return getAttributeData("TEXCOORD_0");
    }

    @Nullable
    public AccessorData getBoneIndices() {
        return getAttributeData("JOINTS_0");
    }

    @Nullable
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

    /**
     * インデックス配列を取得（便利メソッド）
     */
    public int[] getIndices() {
        assert indexData.getIntDataReadOnly() != null;
        return indexData.getIntDataReadOnly();
    }

    /**
     * 生のUV配列を取得（読み取り専用）
     */
    public float @Nullable [] getUvsRaw() {
        AccessorData uvData = getUvs();
        if (uvData == null) return null;
        return getUvs().getFloatDataReadOnly();
    }

    // === モーフターゲット ===

    public int getMorphTargetCount() {
        return morphTargets.size();
    }

    public boolean hasMorphTargets() {
        return !morphTargets.isEmpty();
    }

    // === マテリアル関連（新機能） ===

    /**
     * マテリアルを安全に取得
     * マテリアルが設定されていない場合はデフォルトマテリアルを返す
     */
    public ProcessedMaterial getMaterial() {
        return material != null ? material : ProcessedMaterial.createDefault();
    }

    /**
     * マテリアルが設定されているかチェック
     */
    public boolean hasMaterial() {
        return material != null;
    }

    /**
     * マテリアルの名前を取得（デバッグ用）
     */
    public String getMaterialName() {
        return hasMaterial() ? material.name() : "DefaultMaterial";
    }

    // === スキニング ===

    public boolean hasSkin() {
        return skin != null;
    }

    @Nullable
    public String getSkinName() {
        return hasSkin() ? skin.name() : null;
    }

    // === 統計・計算メソッド ===

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
     * 詳細な統計情報を取得
     */
    public MeshStats getStats() {
        return new MeshStats(
                getVertexCount(),
                getTriangleCount(),
                getMorphTargetCount(),
                attributeData.size(),
                indexData.getElementCount(),
                hasSkinning(),
                hasMaterial(),
                getMemoryUsage()
        );
    }

    /**
     * デバッグ情報を取得
     */
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("ProcessedMesh[%s: %d vertices, mode=%s, primitive=%d",
                name, getVertexCount(), drawingMode.name(), primitiveIndex));

        sb.append(String.format(", material=%s", getMaterialName()));

        sb.append(", attributes=[");
        attributeData.keySet().forEach(attr -> sb.append(attr).append(" "));
        sb.append("]");

        sb.append(String.format(", indices=%d", indexData.getElementCount()));

        if (hasMorphTargets()) {
            sb.append(String.format(", morphs=%d", getMorphTargetCount()));
        }

        if (hasSkin()) {
            sb.append(String.format(", skin=%s", getSkinName()));
        }

        sb.append(String.format(", memory=%d bytes]", getMemoryUsage()));

        return sb.toString();
    }

    @Override
    public @NotNull String toString() {
        return String.format("ProcessedMesh[%s: %d vertices, %s, %s]",
                name, getVertexCount(), drawingMode.name(), getMaterialName());
    }

    /**
     * メッシュ統計情報を保持するrecord
     */
    public record MeshStats(
            int vertexCount,
            int triangleCount,
            int morphTargetCount,
            int attributeCount,
            int indexCount,
            boolean hasSkinning,
            boolean hasMaterial,
            int memoryUsage
    ) {
    }

    /**
     * ビルダーパターンの実装
     */
    public static class Builder {
        private String name;
        private final Map<String, AccessorData> attributeData = new HashMap<>(5);
        private AccessorData indexData;
        private List<MorphTarget> morphTargets = new ArrayList<>();
        private @Nullable ProcessedSkin skin;
        private @Nullable ProcessedMaterial material;
        private DrawingMode drawingMode = DrawingMode.TRIANGLES;
        private int primitiveIndex;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder attributeData(Map<String, AccessorData> attributeData) {
            this.attributeData.clear();
            this.attributeData.putAll(attributeData);
            return this;
        }

        public Builder addAttribute(String name, AccessorData data) {
            this.attributeData.put(name, data);
            return this;
        }

        public Builder indexData(AccessorData indexData) {
            this.indexData = indexData;
            return this;
        }

        public Builder morphTargets(List<MorphTarget> morphTargets) {
            this.morphTargets = morphTargets != null ? List.copyOf(morphTargets) : List.of();
            return this;
        }

        public Builder skin(@Nullable ProcessedSkin skin) {
            this.skin = skin;
            return this;
        }

        public Builder material(@Nullable ProcessedMaterial material) {
            this.material = material;
            return this;
        }

        public Builder drawingMode(DrawingMode drawingMode) {
            this.drawingMode = drawingMode;
            return this;
        }

        public Builder primitiveIndex(int primitiveIndex) {
            this.primitiveIndex = primitiveIndex;
            return this;
        }

        public ProcessedMesh build() {
            return new ProcessedMesh(name, attributeData, indexData, morphTargets, skin,
                    material, drawingMode, primitiveIndex);
        }
    }

    /**
     * 新しいBuilderインスタンスを作成
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 既存のProcessedMeshをベースにした新しいBuilderを作成
     */
    public Builder toBuilder() {
        return builder()
                .name(name)
                .attributeData(attributeData)
                .indexData(indexData)
                .morphTargets(morphTargets)
                .skin(skin)
                .material(material)
                .drawingMode(drawingMode)
                .primitiveIndex(primitiveIndex);
    }
}
