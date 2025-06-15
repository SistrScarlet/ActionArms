package net.sistr.actionarms.client.render.gltf.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

/**
 * モーフターゲットrecordクラス（AccessorDataベース・不変設計）
 * float[]配列の代わりにAccessorDataを直接保持してメモリ効率を向上
 */
public record MorphTarget(
        String name,
        @Nullable AccessorData positionDeltas,
        @Nullable AccessorData normalDeltas,
        @Nullable AccessorData tangentDeltas
) {

    /**
     * バリデーション付きコンストラクタ
     */
    public MorphTarget {
        // 名前のデフォルト値設定
        if (name == null || name.trim().isEmpty()) {
            name = "MorphTarget_" + System.identityHashCode(this);
        }
    }

    /**
     * 位置と法線のみのコンストラクタ（推奨）
     */
    public MorphTarget(String name, @Nullable AccessorData positionDeltas, @Nullable AccessorData normalDeltas) {
        this(name, positionDeltas, normalDeltas, null);
    }

    /**
     * 名前のみのコンストラクタ（空のモーフターゲット作成用）
     */
    public MorphTarget(String name) {
        this(name, null, null, null);
    }

    // === データ存在チェック ===

    public boolean hasPositionDeltas() {
        return positionDeltas != null && positionDeltas.isValid();
    }

    public boolean hasNormalDeltas() {
        return normalDeltas != null && normalDeltas.isValid();
    }

    public boolean hasTangentDeltas() {
        return tangentDeltas != null && tangentDeltas.isValid();
    }

    /**
     * 何らかのデルタデータを持っているかチェック
     */
    public boolean hasAnyDeltas() {
        return hasPositionDeltas() || hasNormalDeltas() || hasTangentDeltas();
    }

    /**
     * すべてのデルタデータを持っているかチェック
     */
    public boolean hasAllDeltas() {
        return hasPositionDeltas() && hasNormalDeltas() && hasTangentDeltas();
    }

    // === 要素数取得 ===

    /**
     * 要素数を取得（すべてのAccessorDataは同じ要素数を持つ必要がある）
     */
    @SuppressWarnings("DataFlowIssue")
    public int getElementCount() {
        if (hasPositionDeltas()) return positionDeltas.getElementCount();
        if (hasNormalDeltas()) return normalDeltas.getElementCount();
        if (hasTangentDeltas()) return tangentDeltas.getElementCount();
        return 0;
    }

    // === 個別頂点データアクセス ===

    /**
     * 特定の頂点の位置差分を取得
     */
    public Vector3f getPositionDelta(int vertexIndex) {
        //noinspection DataFlowIssue
        if (!hasPositionDeltas() || vertexIndex < 0 || vertexIndex >= positionDeltas.getElementCount()) {
            return new Vector3f(0, 0, 0);
        }

        return new Vector3f(
                positionDeltas.getFloat(vertexIndex, 0),
                positionDeltas.getFloat(vertexIndex, 1),
                positionDeltas.getFloat(vertexIndex, 2)
        );
    }

    /**
     * 特定の頂点の法線差分を取得
     */
    public Vector3f getNormalDelta(int vertexIndex) {
        //noinspection DataFlowIssue
        if (!hasNormalDeltas() || vertexIndex < 0 || vertexIndex >= normalDeltas.getElementCount()) {
            return new Vector3f(0, 0, 0);
        }

        return new Vector3f(
                normalDeltas.getFloat(vertexIndex, 0),
                normalDeltas.getFloat(vertexIndex, 1),
                normalDeltas.getFloat(vertexIndex, 2)
        );
    }

    /**
     * 特定の頂点のタンジェント差分を取得
     */
    public Vector3f getTangentDelta(int vertexIndex) {
        //noinspection DataFlowIssue
        if (!hasTangentDeltas() || vertexIndex < 0 || vertexIndex >= tangentDeltas.getElementCount()) {
            return new Vector3f(0, 0, 0);
        }

        return new Vector3f(
                tangentDeltas.getFloat(vertexIndex, 0),
                tangentDeltas.getFloat(vertexIndex, 1),
                tangentDeltas.getFloat(vertexIndex, 2)
        );
    }

    // === 生配列アクセス（パフォーマンス重視の場合のみ使用） ===

    /**
     * 位置差分の生配列を取得（読み取り専用）
     * 注意: パフォーマンス重視の場合のみ使用、通常はgetPositionDelta()を推奨
     */
    public float @Nullable [] getPositionDeltasRaw() {
        //noinspection DataFlowIssue
        return hasPositionDeltas() ? positionDeltas.getFloatDataReadOnly() : null;
    }

    /**
     * 法線差分の生配列を取得（読み取り専用）
     */
    public float @Nullable [] getNormalDeltasRaw() {
        //noinspection DataFlowIssue
        return hasNormalDeltas() ? normalDeltas.getFloatDataReadOnly() : null;
    }

    /**
     * タンジェント差分の生配列を取得（読み取り専用）
     */
    public float @Nullable [] getTangentDeltasRaw() {
        //noinspection DataFlowIssue
        return hasTangentDeltas() ? tangentDeltas.getFloatDataReadOnly() : null;
    }

    // === 統計・計算メソッド ===

    /**
     * メモリ使用量を計算
     */
    public int getMemoryUsage() {
        int total = 0;
        if (positionDeltas != null) total += positionDeltas.getMemoryUsage();
        if (normalDeltas != null) total += normalDeltas.getMemoryUsage();
        if (tangentDeltas != null) total += tangentDeltas.getMemoryUsage();
        return total;
    }

    /**
     * 保持しているデータ型のリストを取得
     */
    public java.util.List<String> getDataTypes() {
        java.util.List<String> types = new java.util.ArrayList<>();
        if (hasPositionDeltas()) types.add("Position");
        if (hasNormalDeltas()) types.add("Normal");
        if (hasTangentDeltas()) types.add("Tangent");
        return types;
    }

    /**
     * 詳細な統計情報を取得
     */
    public MorphTargetStats getStats() {
        return new MorphTargetStats(
                getElementCount(),
                hasPositionDeltas(),
                hasNormalDeltas(),
                hasTangentDeltas(),
                getMemoryUsage()
        );
    }

    /**
     * デバッグ情報を取得
     */
    public String getDebugInfo() {
        return String.format("MorphTarget[%s: elements=%d, types=%s, memory=%d bytes]",
                name, getElementCount(), getDataTypes(), getMemoryUsage());
    }

    @Override
    public @NotNull String toString() {
        return String.format("MorphTarget[%s: %d elements, %s]",
                name, getElementCount(), getDataTypes());
    }

    /**
     * モーフターゲット統計情報を保持するrecord
     */
    public record MorphTargetStats(
            int elementCount,
            boolean hasPositionDeltas,
            boolean hasNormalDeltas,
            boolean hasTangentDeltas,
            int memoryUsage
    ) {
    }

    /**
     * ビルダーパターンの実装
     */
    public static class Builder {
        private String name;
        private @Nullable AccessorData positionDeltas;
        private @Nullable AccessorData normalDeltas;
        private @Nullable AccessorData tangentDeltas;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder positionDeltas(@Nullable AccessorData positionDeltas) {
            this.positionDeltas = positionDeltas;
            return this;
        }

        public Builder normalDeltas(@Nullable AccessorData normalDeltas) {
            this.normalDeltas = normalDeltas;
            return this;
        }

        public Builder tangentDeltas(@Nullable AccessorData tangentDeltas) {
            this.tangentDeltas = tangentDeltas;
            return this;
        }

        public MorphTarget build() {
            return new MorphTarget(name, positionDeltas, normalDeltas, tangentDeltas);
        }
    }

    /**
     * 新しいBuilderインスタンスを作成
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 既存のMorphTargetをベースにした新しいBuilderを作成
     */
    public Builder toBuilder() {
        return builder()
                .name(name)
                .positionDeltas(positionDeltas)
                .normalDeltas(normalDeltas)
                .tangentDeltas(tangentDeltas);
    }
}
