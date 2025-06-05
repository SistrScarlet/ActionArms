package net.sistr.actionarms.client.render.gltf.data;

/**
 * glTFアクセサのデータ型定義
 * 各属性に対応するコンポーネント数とJavaデータ型を管理
 */
public enum AccessorDataType {
    POSITION(3, float.class, "VEC3"),
    NORMAL(3, float.class, "VEC3"),
    TANGENT(4, float.class, "VEC4"),
    UV_0(2, float.class, "VEC2"),
    UV_1(2, float.class, "VEC2"),
    COLOR_0(4, float.class, "VEC4"),
    BONE_INDICES(4, int.class, "VEC4"),
    BONE_WEIGHTS(4, float.class, "VEC4"),
    MORPH_POSITION(3, float.class, "VEC3"),
    MORPH_NORMAL(3, float.class, "VEC3"),
    MORPH_TANGENT(3, float.class, "VEC3"),
    SCALAR_FLOAT(1, float.class, "SCALAR"),
    SCALAR_INT(1, int.class, "SCALAR");

    private final int componentCount;
    private final Class<?> javaType;
    private final String gltfType;

    AccessorDataType(int componentCount, Class<?> javaType, String gltfType) {
        this.componentCount = componentCount;
        this.javaType = javaType;
        this.gltfType = gltfType;
    }

    public int getComponentCount() {
        return componentCount;
    }

    public Class<?> getJavaType() {
        return javaType;
    }

    public String getGltfType() {
        return gltfType;
    }

    public boolean isFloatType() {
        return javaType == float.class;
    }

    public boolean isIntType() {
        return javaType == int.class;
    }

    /**
     * glTF属性名からAccessorDataTypeを推定
     */
    public static AccessorDataType fromAttributeName(String attributeName) {
        switch (attributeName) {
            case "POSITION":
                return POSITION;
            case "NORMAL":
                return NORMAL;
            case "TANGENT":
                return TANGENT;
            case "TEXCOORD_0":
                return UV_0;
            case "TEXCOORD_1":
                return UV_1;
            case "COLOR_0":
                return COLOR_0;
            case "JOINTS_0":
                return BONE_INDICES;
            case "WEIGHTS_0":
                return BONE_WEIGHTS;
            default:
                // モーフターゲット属性の場合
                if (attributeName.startsWith("POSITION")) return MORPH_POSITION;
                if (attributeName.startsWith("NORMAL")) return MORPH_NORMAL;
                if (attributeName.startsWith("TANGENT")) return MORPH_TANGENT;

                // デフォルトはスカラー（型は文脈によって決まる）
                return SCALAR_FLOAT;
        }
    }

    /**
     * バイト単位での要素サイズを計算
     */
    public int getElementSizeInBytes() {
        int typeSize = isFloatType() ? 4 : 4; // float=4bytes, int=4bytes
        return componentCount * typeSize;
    }
}