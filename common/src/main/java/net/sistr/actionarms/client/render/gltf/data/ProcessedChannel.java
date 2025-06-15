package net.sistr.actionarms.client.render.gltf.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 変換済みアニメーションチャンネルrecordクラス（不変設計）
 * キーフレーム補間とアニメーション値計算を管理
 */
public record ProcessedChannel(
        String targetNode,
        String targetPath,         // "translation", "rotation", "scale", "weights"
        List<ProcessedKeyframe> keyframes,
        InterpolationMode interpolationMode,
        float duration
) {

    /**
     * 補間モードの定義
     */
    public enum InterpolationMode {
        LINEAR,
        STEP,
        CUBICSPLINE
    }

    /**
     * バリデーション付きコンストラクタ
     */
    public ProcessedChannel {
        // ターゲットノードの妥当性チェック
        if (targetNode == null || targetNode.trim().isEmpty()) {
            throw new IllegalArgumentException("Target node cannot be null or empty");
        }

        // ターゲットパスの妥当性チェック
        if (targetPath == null || targetPath.trim().isEmpty()) {
            throw new IllegalArgumentException("Target path cannot be null or empty");
        }

        // キーフレームリストの防御的コピーと不変化
        if (keyframes == null) {
            keyframes = Collections.emptyList();
        } else {
            keyframes = Collections.unmodifiableList(List.copyOf(keyframes));
        }

        // 補間モードのデフォルト値設定
        if (interpolationMode == null) {
            interpolationMode = InterpolationMode.LINEAR;
        }

        // 期間の計算（引数として渡されているが、検証のため再計算）
        float calculatedDuration = calculateDuration(keyframes);
        if (Math.abs(duration - calculatedDuration) > 0.001f) {
            // 渡された期間と計算された期間が異なる場合は計算値を使用
            duration = calculatedDuration;
        }
    }

    /**
     * 期間自動計算コンストラクタ（推奨）
     */
    public ProcessedChannel(String targetNode, String targetPath,
                            List<ProcessedKeyframe> keyframes, InterpolationMode interpolationMode) {
        this(targetNode, targetPath, keyframes, interpolationMode, calculateDuration(keyframes));
    }

    // === 基本情報アクセサー ===

    public int getKeyframeCount() {
        return keyframes.size();
    }

    public boolean hasKeyframes() {
        return !keyframes.isEmpty();
    }


    public boolean isValid() {
        return hasKeyframes() && duration > 0;
    }

// === キーフレーム検索・アクセス ===
//

    /**
     * 最初のキーフレームを取得
     */
    @Nullable
    public ProcessedKeyframe getFirstKeyframe() {
        return hasKeyframes() ? keyframes.get(0) : null;
    }

    /**
     * 最後のキーフレームを取得
     */
    @Nullable
    public ProcessedKeyframe getLastKeyframe() {
        return hasKeyframes() ? keyframes.get(keyframes.size() - 1) : null;
    }

    /**
     * 指定されたインデックスのキーフレームを取得
     */
    @Nullable
    public ProcessedKeyframe getKeyframe(int index) {
        return (index >= 0 && index < keyframes.size()) ? keyframes.get(index) : null;
    }

    // === アニメーション値計算（コア機能） ===

    /**
     * 指定時間での値を取得（補間込み）
     */
    @Nullable
    public Object getValueAt(float time) {
        if (!hasKeyframes()) return null;

        // 時間に対応するキーフレーム区間を探す
        int keyIndex = findKeyframeIndex(time);

        if (keyIndex >= keyframes.size() - 1) {
            // 最後のキーフレーム
            return keyframes.get(keyframes.size() - 1).value();
        }
        ProcessedKeyframe currentKey = keyframes.get(keyIndex);
        ProcessedKeyframe nextKey = keyframes.get(keyIndex + 1);

        // 補間の実行
        return interpolateValue(currentKey, nextKey, time);
    }

    /**
     * 時間に対応するキーフレームインデックスを検索
     */
    private int findKeyframeIndex(float time) {
        for (int i = 0; i < keyframes.size() - 1; i++) {
            ProcessedKeyframe current = keyframes.get(i);
            ProcessedKeyframe next = keyframes.get(i + 1);

            if (time >= current.time() && time <= next.time()) {
                return i;
            }
        }
        return keyframes.size() - 1;
    }

    /**
     * 補間値計算
     */
    private Object interpolateValue(ProcessedKeyframe key1, ProcessedKeyframe key2, float time) {
        float t1 = key1.time();
        float t2 = key2.time();

        if (Math.abs(t2 - t1) < 0.0001f) {
            return key1.value();
        }

        float factor = (time - t1) / (t2 - t1);
        factor = Math.max(0, Math.min(1, factor)); // クランプ

        return switch (interpolationMode) {
            case STEP -> key1.value(); // ステップ補間は前の値をそのまま使用
            case LINEAR -> interpolateLinear(key1.value(), key2.value(), factor);
            case CUBICSPLINE -> interpolateLinear(key1.value(), key2.value(), factor); // 簡略化
        };
    }

    /**
     * 線形補間計算
     */
    private Object interpolateLinear(Object value1, Object value2, float factor) {
        if (value1 instanceof Vector3f v1 && value2 instanceof Vector3f v2) {
            // Vector3fの線形補間
            return new Vector3f(v1).lerp(v2, factor);
        } else if (value1 instanceof Quaternionf q1 && value2 instanceof Quaternionf q2) {
            // Quaternionの球面線形補間
            return new Quaternionf(q1).slerp(q2, factor);
        } else if (value1 instanceof Float f1 && value2 instanceof Float f2) {
            // floatの線形補間
            return f1 + (f2 - f1) * factor;
        } else if (value1 instanceof float[] arr1 && value2 instanceof float[] arr2) {
            // float配列の線形補間（モーフウェイト用）
            if (arr1.length != arr2.length) return arr1;

            float[] result = new float[arr1.length];
            for (int i = 0; i < result.length; i++) {
                result[i] = arr1[i] + (arr2[i] - arr1[i]) * factor;
            }
            return result;
        }

        // 未対応の型は最初の値を返す
        return value1;
    }

    // === 統計・計算メソッド ===

    /**
     * 期間を計算する静的メソッド
     */
    private static float calculateDuration(List<ProcessedKeyframe> keyframes) {
        if (keyframes == null || keyframes.isEmpty()) return 0;

        float maxTime = 0;
        for (ProcessedKeyframe keyframe : keyframes) {
            maxTime = Math.max(maxTime, keyframe.time());
        }
        return maxTime;
    }

    /**
     * 値の型を取得（デバッグ用）
     */
    public String getValueType() {
        if (!hasKeyframes()) return "None";

        Object value = keyframes.get(0).value();
        return value != null ? value.getClass().getSimpleName() : "Null";
    }

    /**
     * 詳細な統計情報を取得
     */
    public ChannelStats getStats() {
        return new ChannelStats(
                getKeyframeCount(),
                duration,
                getValueType(),
                interpolationMode,
                isValid()
        );
    }

    /**
     * デバッグ情報を取得
     */
    public String getDebugInfo() {
        return String.format("ProcessedChannel[%s.%s: %d keys, %.3fs, %s, %s]",
                targetNode, targetPath, getKeyframeCount(), duration, getValueType(), interpolationMode);
    }

    @Override
    public @NotNull String toString() {
        return String.format("Channel[%s.%s: %d keys, %.2fs, %s]",
                targetNode, targetPath, getKeyframeCount(), duration, interpolationMode);
    }

    /**
     * チャンネル統計情報を保持するrecord
     */
    public record ChannelStats(
            int keyframeCount,
            float duration,
            String valueType,
            InterpolationMode interpolationMode,
            boolean isValid
    ) {
    }

    /**
     * ビルダーパターンの実装
     */
    public static class Builder {
        private String targetNode;
        private String targetPath;
        private final List<ProcessedKeyframe> keyframes = new ArrayList<>(10);
        private InterpolationMode interpolationMode = InterpolationMode.LINEAR;

        public Builder targetNode(String targetNode) {
            this.targetNode = targetNode;
            return this;
        }

        public Builder targetPath(String targetPath) {
            this.targetPath = targetPath;
            return this;
        }

        public Builder keyframes(List<ProcessedKeyframe> keyframes) {
            this.keyframes.clear();
            this.keyframes.addAll(keyframes);
            return this;
        }

        public Builder interpolationMode(InterpolationMode interpolationMode) {
            this.interpolationMode = interpolationMode;
            return this;
        }

        public ProcessedChannel build() {
            return new ProcessedChannel(targetNode, targetPath, keyframes, interpolationMode);
        }
    }

    /**
     * 新しいBuilderインスタンスを作成
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 既存のProcessedChannelをベースにした新しいBuilderを作成
     */
    public Builder toBuilder() {
        return builder()
                .targetNode(targetNode)
                .targetPath(targetPath)
                .keyframes(keyframes)
                .interpolationMode(interpolationMode);
    }
}
