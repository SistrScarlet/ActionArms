package net.sistr.actionarms.client.render.gltf.data;

import org.jetbrains.annotations.Nullable;

/**
 * glTFアクセサから抽出されたプリミティブ配列データを保持するクラス
 * 元のAccessorModelは保持せず、必要なデータのみを効率的に管理する
 */
public class AccessorData {
    // 基本情報
    private final String id;
    private final AccessorDataType dataType;
    private final int elementCount;

    // データ本体（どちらか一方のみnull以外）
    private final float @Nullable [] floatData;
    private final int @Nullable [] intData;

    // メタデータ
    private final boolean normalized;
    private final AccessorSignature signature;
    private final int stride;

    /**
     * float型データ用コンストラクタ
     */
    public AccessorData(@Nullable String id, AccessorDataType dataType, int elementCount,
                        float[] floatData, boolean normalized, @Nullable AccessorSignature signature) {
        this.id = id != null ? id : "AccessorData_" + System.identityHashCode(this);
        this.dataType = dataType;
        this.elementCount = elementCount;
        this.floatData = floatData.clone(); // 防御的コピー
        this.intData = null;
        this.normalized = normalized;
        this.signature = signature;
        this.stride = dataType.getComponentCount();

        validateData();
    }

    /**
     * int型データ用コンストラクタ
     */
    public AccessorData(@Nullable String id, AccessorDataType dataType, int elementCount,
                        int[] intData, boolean normalized, @Nullable AccessorSignature signature) {
        this.id = id != null ? id : "AccessorData_" + System.identityHashCode(this);
        this.dataType = dataType;
        this.elementCount = elementCount;
        this.floatData = null;
        this.intData = intData.clone(); // 防御的コピー
        this.normalized = normalized;
        this.signature = signature;
        this.stride = dataType.getComponentCount();

        validateData();
    }

    /**
     * データの整合性検証
     */
    private void validateData() {
        if (elementCount <= 0) {
            throw new IllegalArgumentException("Element count must be positive: " + elementCount);
        }

        if (!isValid()) {
            throw new IllegalArgumentException("Invalid accessor data: " + getDebugInfo());
        }

        // データ型とJava配列型の整合性チェック
        if (dataType.isFloatType() && floatData == null) {
            throw new IllegalArgumentException("Float data expected but not provided for " + dataType);
        }
        if (dataType.isIntType() && intData == null) {
            throw new IllegalArgumentException("Int data expected but not provided for " + dataType);
        }
    }

    /**
     * データの有効性をチェック
     */
    public boolean isValid() {
        if (elementCount <= 0) return false;
        if (floatData == null && intData == null) return false;
        if (floatData != null && intData != null) return false; // 両方あるのは無効

        int expectedLength = getExpectedDataLength();
        int actualLength = getActualDataLength();

        return actualLength == expectedLength;
    }

    /**
     * 期待されるデータ配列の長さ
     */
    private int getExpectedDataLength() {
        return elementCount * dataType.getComponentCount();
    }

    /**
     * 実際のデータ配列の長さ
     */
    private int getActualDataLength() {
        if (floatData != null) return floatData.length;
        if (intData != null) return intData.length;
        return 0;
    }

    /**
     * メモリ使用量をバイト単位で計算
     */
    public int getMemoryUsage() {
        if (floatData != null) return floatData.length * 4; // float = 4bytes
        if (intData != null) return intData.length * 4;     // int = 4bytes
        return 0;
    }

    /**
     * デバッグ用情報文字列
     */
    public String getDebugInfo() {
        return String.format("AccessorData[id=%s, type=%s, elements=%d, stride=%d, normalized=%s, memory=%d bytes, valid=%s]",
                id, dataType, elementCount, stride, normalized, getMemoryUsage(), isValid());
    }

    /**
     * 指定されたインデックスの要素を取得（float配列用）
     *
     * @param elementIndex   要素のインデックス（0 ≤ index < elementCount）
     * @param componentIndex コンポーネントのインデックス（0 ≤ index < componentCount）
     */
    public float getFloat(int elementIndex, int componentIndex) {
        if (floatData == null) {
            throw new IllegalStateException("This accessor data does not contain float data");
        }
        validateIndex(elementIndex, componentIndex);
        return floatData[elementIndex * stride + componentIndex];
    }

    /**
     * 指定されたインデックスの要素を取得（int配列用）
     */
    public int getInt(int elementIndex, int componentIndex) {
        if (intData == null) {
            throw new IllegalStateException("This accessor data does not contain int data");
        }
        validateIndex(elementIndex, componentIndex);
        return intData[elementIndex * stride + componentIndex];
    }

    /**
     * 指定された要素の全コンポーネントを配列で取得（float用）
     */
    public float[] getFloatElement(int elementIndex) {
        if (floatData == null) {
            throw new IllegalStateException("This accessor data does not contain float data");
        }
        if (elementIndex < 0 || elementIndex >= elementCount) {
            throw new IndexOutOfBoundsException("Element index out of bounds: " + elementIndex);
        }

        float[] result = new float[dataType.getComponentCount()];
        System.arraycopy(floatData, elementIndex * stride, result, 0, dataType.getComponentCount());
        return result;
    }

    /**
     * 指定された要素の全コンポーネントを配列で取得（int用）
     */
    public int[] getIntElement(int elementIndex) {
        if (intData == null) {
            throw new IllegalStateException("This accessor data does not contain int data");
        }
        if (elementIndex < 0 || elementIndex >= elementCount) {
            throw new IndexOutOfBoundsException("Element index out of bounds: " + elementIndex);
        }

        int[] result = new int[dataType.getComponentCount()];
        System.arraycopy(intData, elementIndex * stride, result, 0, dataType.getComponentCount());
        return result;
    }

    /**
     * インデックスの有効性をチェック
     */
    private void validateIndex(int elementIndex, int componentIndex) {
        if (elementIndex < 0 || elementIndex >= elementCount) {
            throw new IndexOutOfBoundsException("Element index out of bounds: " + elementIndex);
        }
        if (componentIndex < 0 || componentIndex >= dataType.getComponentCount()) {
            throw new IndexOutOfBoundsException("Component index out of bounds: " + componentIndex);
        }
    }

    // Getter methods
    public String getId() {
        return id;
    }

    public AccessorDataType getDataType() {
        return dataType;
    }

    public int getElementCount() {
        return elementCount;
    }

    public boolean isNormalized() {
        return normalized;
    }

    @Nullable
    public AccessorSignature getSignature() {
        return signature;
    }

    public int getStride() {
        return stride;
    }

    /**
     * 生のfloat配列を取得（読み取り専用）
     * 注意: 返される配列は防御的コピーではないため、変更してはいけない
     */
    public float[] getFloatDataReadOnly() {
        assert floatData != null;
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return floatData;
    }

    /**
     * 生のint配列を取得（読み取り専用）
     * 注意: 返される配列は防御的コピーではないため、変更してはいけない
     */
    public int[] getIntDataReadOnly() {
        assert intData != null;
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return intData;
    }

    /**
     * float配列の防御的コピーを取得
     */
    public float @Nullable [] copyFloatData() {
        return floatData != null ? floatData.clone() : null;
    }

    /**
     * int配列の防御的コピーを取得
     */
    public int @Nullable [] copyIntData() {
        return intData != null ? intData.clone() : null;
    }

    @Override
    public String toString() {
        return getDebugInfo();
    }
}