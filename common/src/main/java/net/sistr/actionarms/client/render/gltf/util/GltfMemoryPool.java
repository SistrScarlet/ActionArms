package net.sistr.actionarms.client.render.gltf.util;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * glTF描画用のThreadLocalメモリプール
 * アロケーション削減とGC負荷軽減のための配列・オブジェクトプール
 */
public class GltfMemoryPool {

    // float配列プール
    private static final ThreadLocal<FloatArrayPool> FLOAT_POOL =
            ThreadLocal.withInitial(FloatArrayPool::new);

    // Matrix4f配列プール
    private static final ThreadLocal<MatrixArrayPool> MATRIX_POOL =
            ThreadLocal.withInitial(MatrixArrayPool::new);

    // int配列プール
    private static final ThreadLocal<IntArrayPool> INT_POOL =
            ThreadLocal.withInitial(IntArrayPool::new);

    /**
     * float配列を借用
     *
     * @param size 必要なサイズ
     * @return 再利用可能なfloat配列
     */
    public static float[] borrowFloatArray(int size) {
        return FLOAT_POOL.get().borrow(size);
    }

    /**
     * float配列を返却
     *
     * @param array 使用済み配列
     */
    public static void returnFloatArray(float @Nullable [] array) {
        if (array != null) {
            FLOAT_POOL.get().returnArray(array);
        }
    }

    /**
     * Matrix4f配列を借用
     *
     * @param size 必要なサイズ
     * @return 再利用可能なMatrix4f配列
     */
    public static Matrix4f[] borrowMatrixArray(int size) {
        return MATRIX_POOL.get().borrow(size);
    }

    /**
     * Matrix4f配列を返却
     *
     * @param array 使用済み配列
     */
    public static void returnMatrixArray(@Nullable Matrix4f[] array) {
        if (array != null) {
            MATRIX_POOL.get().returnArray(array);
        }
    }

    /**
     * int配列を借用
     *
     * @param size 必要なサイズ
     * @return 再利用可能なint配列
     */
    public static int[] borrowIntArray(int size) {
        return INT_POOL.get().borrow(size);
    }

    /**
     * int配列を返却
     *
     * @param array 使用済み配列
     */
    public static void returnIntArray(int @Nullable [] array) {
        if (array != null) {
            INT_POOL.get().returnArray(array);
        }
    }

    /**
     * プールの統計情報を取得（デバッグ用）
     */
    public static PoolStats getStats() {
        return new PoolStats(
                FLOAT_POOL.get().getStats(),
                MATRIX_POOL.get().getStats(),
                INT_POOL.get().getStats()
        );
    }

    /**
     * プールをクリア（メモリリーク防止用）
     */
    public static void clearPools() {
        FLOAT_POOL.get().clear();
        MATRIX_POOL.get().clear();
        INT_POOL.get().clear();
    }

    // float配列プール実装
    private static class FloatArrayPool {
        private final Map<Integer, Deque<float[]>> pools = new HashMap<>(10);
        private int borrowCount;
        private int returnCount;

        float[] borrow(int size) {
            borrowCount++;
            Deque<float[]> pool = pools.computeIfAbsent(size, k -> new ArrayDeque<>());
            if (pool.isEmpty()) {
                return new float[size];
            } else {
                float[] array = pool.pop();
                // 配列をクリア（前回の値を残さない）
                java.util.Arrays.fill(array, 0.0f);
                return array;
            }
        }

        void returnArray(float[] array) {
            returnCount++;
            Deque<float[]> pool = pools.computeIfAbsent(array.length, k -> new ArrayDeque<>());
            // プールサイズ制限（メモリリーク防止）
            if (pool.size() < 10) {
                pool.push(array);
            }
        }

        void clear() {
            pools.clear();
            borrowCount = 0;
            returnCount = 0;
        }

        ArrayPoolStats getStats() {
            int totalPooled = pools.values().stream().mapToInt(Deque::size).sum();
            return new ArrayPoolStats("float[]", borrowCount, returnCount, totalPooled);
        }
    }

    // Matrix4f配列プール実装
    private static class MatrixArrayPool {
        private final Map<Integer, ArrayDeque<Matrix4f[]>> pools = new HashMap<>(10);
        private int borrowCount = 0;
        private int returnCount = 0;

        Matrix4f[] borrow(int size) {
            borrowCount++;
            ArrayDeque<Matrix4f[]> pool = pools.computeIfAbsent(size, k -> new ArrayDeque<>(10));
            if (pool.isEmpty()) {
                Matrix4f[] array = new Matrix4f[size];
                // 新しいMatrix4fインスタンスで初期化
                for (int i = 0; i < size; i++) {
                    array[i] = new Matrix4f();
                }
                return array;
            } else {
                Matrix4f[] array = pool.pop();
                // 行列を単位行列にリセット
                for (Matrix4f matrix : array) {
                    if (matrix != null) {
                        matrix.identity();
                    }
                }
                return array;
            }
        }

        void returnArray(Matrix4f[] array) {
            returnCount++;
            Deque<Matrix4f[]> pool = pools.computeIfAbsent(array.length, k -> new ArrayDeque<>(10));
            // プールサイズ制限
            if (pool.size() < 5) { // Matrix4f配列は重いので少なめに制限
                pool.push(array);
            }
        }

        void clear() {
            pools.clear();
            borrowCount = 0;
            returnCount = 0;
        }

        ArrayPoolStats getStats() {
            int totalPooled = pools.values().stream().mapToInt(ArrayDeque::size).sum();
            return new ArrayPoolStats("Matrix4f[]", borrowCount, returnCount, totalPooled);
        }
    }

    // int配列プール実装
    private static class IntArrayPool {
        private final Map<Integer, ArrayDeque<int[]>> pools = new HashMap<>();
        private int borrowCount = 0;
        private int returnCount = 0;

        int[] borrow(int size) {
            borrowCount++;
            ArrayDeque<int[]> pool = pools.computeIfAbsent(size, k -> new ArrayDeque<>());
            if (pool.isEmpty()) {
                return new int[size];
            } else {
                int[] array = pool.pop();
                // 配列をクリア
                java.util.Arrays.fill(array, 0);
                return array;
            }
        }

        void returnArray(int[] array) {
            returnCount++;
            Deque<int[]> pool = pools.computeIfAbsent(array.length, k -> new ArrayDeque<>(10));
            // プールサイズ制限
            if (pool.size() < 10) {
                pool.push(array);
            }
        }

        void clear() {
            pools.clear();
            borrowCount = 0;
            returnCount = 0;
        }

        ArrayPoolStats getStats() {
            int totalPooled = pools.values().stream().mapToInt(ArrayDeque::size).sum();
            return new ArrayPoolStats("int[]", borrowCount, returnCount, totalPooled);
        }
    }

    // 統計情報用クラス
    public static class PoolStats {
        public final ArrayPoolStats floatArrayStats;
        public final ArrayPoolStats matrixArrayStats;
        public final ArrayPoolStats intArrayStats;

        PoolStats(ArrayPoolStats floatArrayStats, ArrayPoolStats matrixArrayStats, ArrayPoolStats intArrayStats) {
            this.floatArrayStats = floatArrayStats;
            this.matrixArrayStats = matrixArrayStats;
            this.intArrayStats = intArrayStats;
        }

        @Override
        public String toString() {
            return String.format("""
                            GltfMemoryPool Stats:
                            %s
                            %s
                            %s""",
                    floatArrayStats, matrixArrayStats, intArrayStats);
        }
    }

    public static class ArrayPoolStats {
        public final String arrayType;
        public final int borrowCount;
        public final int returnCount;
        public final int currentPooled;
        public final double hitRate;

        ArrayPoolStats(String arrayType, int borrowCount, int returnCount, int currentPooled) {
            this.arrayType = arrayType;
            this.borrowCount = borrowCount;
            this.returnCount = returnCount;
            this.currentPooled = currentPooled;
            this.hitRate = borrowCount > 0 ? (double) returnCount / borrowCount : 0.0;
        }

        @Override
        public String toString() {
            return String.format("%s: borrow=%d, return=%d, pooled=%d, hit_rate=%.2f%%",
                    arrayType, borrowCount, returnCount, currentPooled, hitRate * 100);
        }
    }
}
