package net.sistr.actionarms.client.render.gltf.data;

import org.joml.Vector3f;

/**
 * モーフターゲットクラス - AccessorDataベースの効率的な設計
 * float[]配列の代わりにAccessorDataを直接保持してメモリ効率を向上
 */
public class MorphTarget {
    private final String name;
    private final AccessorData positionDeltas;
    private final AccessorData normalDeltas;
    private final AccessorData tangentDeltas;

    /**
     * AccessorDataを使用するコンストラクタ（推奨）
     */
    public MorphTarget(String name, AccessorData positionDeltas, AccessorData normalDeltas) {
        this(name, positionDeltas, normalDeltas, null);
    }

    /**
     * 完全なコンストラクタ
     */
    public MorphTarget(String name, AccessorData positionDeltas, AccessorData normalDeltas, AccessorData tangentDeltas) {
        this.name = name != null ? name : "MorphTarget";
        this.positionDeltas = positionDeltas;
        this.normalDeltas = normalDeltas;
        this.tangentDeltas = tangentDeltas;
    }

    /**
     * 空のモーフターゲット作成用
     */
    public MorphTarget(String name) {
        this(name, null, null, null);
    }

    // 基本情報
    public String getName() {
        return name;
    }

    // AccessorDataの直接取得
    public AccessorData getPositionDeltas() {
        return positionDeltas;
    }

    public AccessorData getNormalDeltas() {
        return normalDeltas;
    }

    public AccessorData getTangentDeltas() {
        return tangentDeltas;
    }

    // データ存在チェック
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
     * 要素数を取得（すべてのAccessorDataは同じ要素数を持つ必要がある）
     */
    public int getElementCount() {
        if (hasPositionDeltas()) return positionDeltas.getElementCount();
        if (hasNormalDeltas()) return normalDeltas.getElementCount();
        if (hasTangentDeltas()) return tangentDeltas.getElementCount();
        return 0;
    }

    /**
     * 特定の頂点の位置差分を取得
     */
    public Vector3f getPositionDelta(int vertexIndex) {
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
     * 位置差分の生配列を取得（読み取り専用）
     * 注意: パフォーマンス重視の場合のみ使用、通常はgetPositionDelta()を推奨
     */
    public float[] getPositionDeltasRaw() {
        return hasPositionDeltas() ? positionDeltas.getFloatDataReadOnly() : null;
    }

    /**
     * 法線差分の生配列を取得（読み取り専用）
     */
    public float[] getNormalDeltasRaw() {
        return hasNormalDeltas() ? normalDeltas.getFloatDataReadOnly() : null;
    }

    /**
     * タンジェント差分の生配列を取得（読み取り専用）
     */
    public float[] getTangentDeltasRaw() {
        return hasTangentDeltas() ? tangentDeltas.getFloatDataReadOnly() : null;
    }

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
     * デバッグ情報を取得
     */
    public String getDebugInfo() {
        return String.format("MorphTarget[%s: elements=%d, pos=%s, normal=%s, tangent=%s, memory=%d bytes]",
                name, getElementCount(),
                hasPositionDeltas() ? "yes" : "no",
                hasNormalDeltas() ? "yes" : "no",
                hasTangentDeltas() ? "yes" : "no",
                getMemoryUsage());
    }

    @Override
    public String toString() {
        return getDebugInfo();
    }
}
