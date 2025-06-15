package net.sistr.actionarms.client.render.gltf.data;

import de.javagl.jgltf.model.*;
import de.javagl.jgltf.model.AccessorData;
import net.sistr.actionarms.ActionArms;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AccessorDataのキャッシュ管理クラス
 * 同じアクセサデータの重複作成を防ぎ、メモリ効率を向上させる
 */
public class AccessorDataCache {
    private final Map<AccessorSignature, net.sistr.actionarms.client.render.gltf.data.AccessorData> cache;
    private long totalMemoryUsage;
    private int hitCount;
    private int missCount;

    public AccessorDataCache() {
        this.cache = new ConcurrentHashMap<>(); // スレッドセーフ
    }

    /**
     * アクセサデータを取得または作成
     * キャッシュにある場合は既存のものを返し、ない場合は新規作成してキャッシュに保存
     */
    public net.sistr.actionarms.client.render.gltf.data.AccessorData getOrCreate(
            AccessorModel accessor, AccessorDataType expectedType) {
        return getOrCreate(accessor, expectedType, null);
    }

    /**
     * アクセサデータを取得または作成（ID指定版）
     */
    public net.sistr.actionarms.client.render.gltf.data.AccessorData getOrCreate(
            AccessorModel accessor, AccessorDataType expectedType, @Nullable String customId) {

        AccessorSignature signature = createSignature(accessor);

        @Nullable net.sistr.actionarms.client.render.gltf.data.AccessorData cached = cache.get(signature);
        if (cached != null) {
            hitCount++;
            ActionArms.LOGGER.debug("Cache hit for accessor: {}", customId != null ? customId : "unnamed");
            return cached;
        }

        missCount++;
        ActionArms.LOGGER.debug("Cache miss for accessor: {}, extracting data...",
                customId != null ? customId : "unnamed");

        net.sistr.actionarms.client.render.gltf.data.AccessorData newData
                = extractAccessorData(accessor, expectedType, customId, signature);
        cache.put(signature, newData);
        totalMemoryUsage += newData.getMemoryUsage();

        ActionArms.LOGGER.debug("Created new AccessorData: {}", newData.getDebugInfo());
        return newData;
    }

    /**
     * AccessorModelからAccessorSignatureを作成
     */
    private AccessorSignature createSignature(AccessorModel accessor) {
        // BufferViewのハッシュ値を計算（BufferView自体のidentityHashCode）
        int bufferViewHash = accessor.getBufferViewModel() != null ?
                System.identityHashCode(accessor.getBufferViewModel()) : 0;

        return new AccessorSignature(
                bufferViewHash,
                accessor.getByteOffset(),
                accessor.getComponentType(),
                accessor.getCount(),
                accessor.getElementType().name(),
                accessor.isNormalized()
        );
    }

    /**
     * AccessorModelからAccessorDataを抽出
     */
    private net.sistr.actionarms.client.render.gltf.data.AccessorData extractAccessorData(
            AccessorModel accessor, AccessorDataType expectedType,
            @Nullable String customId, AccessorSignature signature) {

        String id = customId != null ? customId :
                "Accessor_" + accessor.getElementType().name() + "_" + accessor.getCount();

        int elementCount = accessor.getCount();
        boolean normalized = accessor.isNormalized();

        // データ型に応じて抽出
        if (expectedType.isFloatType()) {
            float[] data = extractFloatArray(accessor, expectedType.getComponentCount());
            return new net.sistr.actionarms.client.render.gltf.data.AccessorData(
                    id, expectedType, elementCount, data, normalized, signature);
        } else {
            int[] data = extractIntArray(accessor, expectedType.getComponentCount());
            return new net.sistr.actionarms.client.render.gltf.data.AccessorData(
                    id, expectedType, elementCount, data, normalized, signature);
        }
    }

    /**
     * float配列の抽出
     */
    private float[] extractFloatArray(AccessorModel accessor, int componentCount) {
        AccessorData accessorData = accessor.getAccessorData();
        if (!(accessorData instanceof AccessorFloatData floatData)) {
            throw new IllegalArgumentException("Expected float data but got: " + accessorData.getClass().getSimpleName());
        }

        int elementCount = accessor.getCount();
        float[] result = new float[elementCount * componentCount];

        for (int i = 0; i < elementCount; i++) {
            for (int j = 0; j < componentCount; j++) {
                result[i * componentCount + j] = floatData.get(i, j);
            }
        }

        return result;
    }

    /**
     * int配列の抽出
     */
    private int[] extractIntArray(AccessorModel accessor, int componentCount) {
        AccessorData accessorData = accessor.getAccessorData();
        int elementCount = accessor.getCount();
        int[] result = new int[elementCount * componentCount];

        if (accessorData instanceof AccessorIntData intData) {
            for (int i = 0; i < elementCount; i++) {
                for (int j = 0; j < componentCount; j++) {
                    result[i * componentCount + j] = intData.get(i, j);
                }
            }
        } else if (accessorData instanceof AccessorShortData shortData) {
            for (int i = 0; i < elementCount; i++) {
                for (int j = 0; j < componentCount; j++) {
                    result[i * componentCount + j] = shortData.getInt(i, j);
                }
            }
        } else {
            throw new IllegalArgumentException("Expected int or short data but got: " + accessorData.getClass().getSimpleName());
        }

        return result;
    }

    /**
     * キャッシュをクリア
     */
    public void clear() {
        cache.clear();
        totalMemoryUsage = 0;
        hitCount = 0;
        missCount = 0;
        ActionArms.LOGGER.info("AccessorDataCache cleared");
    }

    /**
     * 特定のシグネチャをキャッシュから削除
     */
    public boolean remove(AccessorSignature signature) {
        @Nullable net.sistr.actionarms.client.render.gltf.data.AccessorData removed = cache.remove(signature);
        if (removed != null) {
            totalMemoryUsage -= removed.getMemoryUsage();
            return true;
        }
        return false;
    }

    /**
     * キャッシュ統計情報を取得
     */
    public CacheStats getStats() {
        return new CacheStats(cache.size(), totalMemoryUsage, hitCount, missCount);
    }

    /**
     * キャッシュ統計情報クラス
     */
    public record CacheStats(int entryCount, long totalMemoryUsage, int hitCount, int missCount) {
        private static final int K = 1024;

        public int getTotalRequests() {
            return hitCount + missCount;
        }

        public double getHitRate() {
            int total = getTotalRequests();
            return total > 0 ? (double) hitCount / total : 0.0;
        }

        public String getMemoryUsageFormatted() {
            if (totalMemoryUsage < K) return totalMemoryUsage + " B";
            if (totalMemoryUsage < K * K) {
                return String.format("%.1f KB", (double) totalMemoryUsage / K);
            }
            return String.format("%.1f MB", (double) totalMemoryUsage / (K * K));
        }

        @Override
        public String toString() {
            return String.format("CacheStats[entries=%d, memory=%s, requests=%d, hitRate=%.1f%%]",
                    entryCount, getMemoryUsageFormatted(), getTotalRequests(), getHitRate() * 100);
        }
    }

    /**
     * デバッグ用：キャッシュの内容を表示
     */
    public void printCacheContents() {
        ActionArms.LOGGER.info("=== AccessorDataCache Contents ===");
        ActionArms.LOGGER.info("Stats: {}", getStats());

        cache.forEach((signature, data) -> {
            ActionArms.LOGGER.info("  {} -> {}", signature, data.getDebugInfo());
        });

        ActionArms.LOGGER.info("=== End Cache Contents ===");
    }
}