package net.sistr.actionarms.client.render.gltf.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 変換済みキーフレームデータ管理recordクラス（不変設計）
 * アニメーションの時間とその時点での値を保持
 */
public record ProcessedKeyframe(
        float time,
        Object value,
        @Nullable Object inTangent,   // キュービックスプライン用
        @Nullable Object outTangent   // キュービックスプライン用
) {

    /**
     * 基本コンストラクタ（線形・ステップ補間用）
     */
    public ProcessedKeyframe(float time, Object value) {
        this(time, value, null, null);
    }

    /**
     * バリデーション付きコンストラクタ
     */
    public ProcessedKeyframe {
        // 時間の妥当性チェック
        if (time < 0) {
            throw new IllegalArgumentException("Time cannot be negative: " + time);
        }

        // 値の妥当性チェック
        if (value == null) {
            throw new IllegalArgumentException("Value cannot be null");
        }
    }

    /**
     * スプライン補間用のタンジェントを持っているかチェック
     */
    public boolean hasSplineTangents() {
        return inTangent != null && outTangent != null;
    }

    /**
     * タンジェントのいずれかを持っているかチェック
     */
    public boolean hasTangents() {
        return inTangent != null || outTangent != null;
    }

    /**
     * 値の型を取得（デバッグ用）
     */
    public String getValueType() {
        return value.getClass().getSimpleName();
    }

    /**
     * タンジェントの型を取得（デバッグ用）
     */
    public String getTangentType() {
        if (hasSplineTangents()) {
            return inTangent.getClass().getSimpleName();
        } else if (inTangent != null) {
            return "In:" + inTangent.getClass().getSimpleName();
        } else if (outTangent != null) {
            return "Out:" + outTangent.getClass().getSimpleName();
        }
        return "None";
    }

    /**
     * デバッグ用の詳細文字列を取得
     */
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder(100);
        sb.append(String.format("ProcessedKeyframe[time=%.3f, valueType=%s", time, getValueType()));

        if (hasTangents()) {
            sb.append(String.format(", tangents=%s", getTangentType()));
        }

        sb.append(String.format(", value=%s]", value.toString()));

        return sb.toString();
    }

    @Override
    public @NotNull String toString() {
        return String.format("Keyframe[t=%.3f, %s]", time, getValueType());
    }
}
