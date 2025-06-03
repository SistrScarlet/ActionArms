package net.sistr.actionarms.client.render.gltf;

import java.util.Objects;

/**
 * アクセサの内容を一意に識別するためのシグネチャ
 * 同じデータを持つアクセサは同じシグネチャを持ち、キャッシュで共有される
 */
public class AccessorSignature {
    private final int bufferViewHash;
    private final int byteOffset;
    private final int componentType;
    private final int count;
    private final String type;
    private final boolean normalized;
    private final int hashCode;

    public AccessorSignature(int bufferViewHash, int byteOffset, int componentType, 
                           int count, String type, boolean normalized) {
        this.bufferViewHash = bufferViewHash;
        this.byteOffset = byteOffset;
        this.componentType = componentType;
        this.count = count;
        this.type = type;
        this.normalized = normalized;
        this.hashCode = calculateHashCode();
    }

    private int calculateHashCode() {
        return Objects.hash(bufferViewHash, byteOffset, componentType, count, type, normalized);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        AccessorSignature that = (AccessorSignature) obj;
        return bufferViewHash == that.bufferViewHash &&
               byteOffset == that.byteOffset &&
               componentType == that.componentType &&
               count == that.count &&
               normalized == that.normalized &&
               Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return String.format("AccessorSignature[bufferView=%d, offset=%d, type=%s(%d), count=%d, normalized=%s]",
                           bufferViewHash, byteOffset, type, componentType, count, normalized);
    }

    // Getter methods
    public int getBufferViewHash() { return bufferViewHash; }
    public int getByteOffset() { return byteOffset; }
    public int getComponentType() { return componentType; }
    public int getCount() { return count; }
    public String getType() { return type; }
    public boolean isNormalized() { return normalized; }
}