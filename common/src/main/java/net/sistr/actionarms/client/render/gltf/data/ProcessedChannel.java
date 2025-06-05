package net.sistr.actionarms.client.render.gltf.data;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public class ProcessedChannel {
    private final String targetNode;
    private final String targetPath; // "translation", "rotation", "scale", "weights"
    private final List<ProcessedKeyframe> keyframes;
    private final InterpolationMode interpolationMode;
    private final float duration;

    public enum InterpolationMode {
        LINEAR,
        STEP,
        CUBICSPLINE
    }

    public ProcessedChannel(String targetNode, String targetPath,
                            List<ProcessedKeyframe> keyframes, InterpolationMode interpolationMode) {
        this.targetNode = targetNode;
        this.targetPath = targetPath;
        this.keyframes = keyframes;
        this.interpolationMode = interpolationMode;
        this.duration = calculateDuration();
    }

    public String getTargetNode() {
        return targetNode;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public List<ProcessedKeyframe> getKeyframes() {
        return keyframes;
    }

    public InterpolationMode getInterpolationMode() {
        return interpolationMode;
    }

    public float getDuration() {
        return duration;
    }

    // 指定時間での値を取得（補間込み）
    public Object getValueAt(float time) {
        if (keyframes.isEmpty()) return null;

        // 時間に対応するキーフレーム区間を探す
        int keyIndex = findKeyframeIndex(time);

        if (keyIndex >= keyframes.size() - 1) {
            // 最後のキーフレーム
            return keyframes.get(keyframes.size() - 1).getValue();
        }

        ProcessedKeyframe currentKey = keyframes.get(keyIndex);
        ProcessedKeyframe nextKey = keyframes.get(keyIndex + 1);

        // 補間の実行
        return interpolateValue(currentKey, nextKey, time);
    }

    private int findKeyframeIndex(float time) {
        for (int i = 0; i < keyframes.size() - 1; i++) {
            ProcessedKeyframe current = keyframes.get(i);
            ProcessedKeyframe next = keyframes.get(i + 1);

            if (time >= current.getTime() && time <= next.getTime()) {
                return i;
            }
        }
        return keyframes.size() - 1;
    }

    private Object interpolateValue(ProcessedKeyframe key1, ProcessedKeyframe key2, float time) {
        float t1 = key1.getTime();
        float t2 = key2.getTime();

        if (Math.abs(t2 - t1) < 0.0001f) {
            return key1.getValue();
        }

        float factor = (time - t1) / (t2 - t1);
        factor = Math.max(0, Math.min(1, factor)); // クランプ

        switch (interpolationMode) {
            case STEP:
                return key1.getValue(); // ステップ補間は前の値をそのまま使用

            case LINEAR:
                return interpolateLinear(key1.getValue(), key2.getValue(), factor);

            case CUBICSPLINE:
                // キュービックスプライン補間（簡略化）
                return interpolateLinear(key1.getValue(), key2.getValue(), factor);

            default:
                return key1.getValue();
        }
    }

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

    private float calculateDuration() {
        if (keyframes.isEmpty()) return 0;

        float maxTime = 0;
        for (ProcessedKeyframe keyframe : keyframes) {
            maxTime = Math.max(maxTime, keyframe.getTime());
        }
        return maxTime;
    }

    @Override
    public String toString() {
        return String.format("Channel[%s.%s: %d keys, %.2fs, %s]",
                targetNode, targetPath, keyframes.size(), duration, interpolationMode);
    }
}
