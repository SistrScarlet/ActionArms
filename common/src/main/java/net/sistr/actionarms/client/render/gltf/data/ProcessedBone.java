package net.sistr.actionarms.client.render.gltf.data;

import net.sistr.actionarms.client.render.gltf.util.Values;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

/**
 * 処理済みボーンデータ
 */
public final class ProcessedBone {
    private final int index;
    private final String name;
    private final ProcessedBone parent;
    private final List<ProcessedBone> children;
    private final Values.Position translation;
    private final Values.Rotation rotation;
    private final Values.Scale scale;
    private final Values.Matrix4x4 inverseBindMatrix;

    // hashCodeの循環参照を避けるためindex基準で計算
    private final int cachedHashCode;

    public ProcessedBone(
            int index,
            String name,
            ProcessedBone parent,
            List<ProcessedBone> children,
            Values.Position translation,
            Values.Rotation rotation,
            Values.Scale scale,
            Values.Matrix4x4 inverseBindMatrix
    ) {
        this.index = index;
        this.name = name != null ? name : "Bone_" + index;
        this.parent = parent;
        this.children = children != null ?
                children :
                new ArrayList<>();
        this.translation = translation != null ? translation : Values.ZERO_POSITION;
        this.rotation = rotation != null ? rotation : Values.IDENTITY_ROTATION;
        this.scale = scale != null ? scale : Values.UNIT_SCALE;
        this.inverseBindMatrix = inverseBindMatrix != null ?
                inverseBindMatrix : Values.Matrix4x4.identity();

        // indexベースのハッシュコード（循環参照を回避）
        this.cachedHashCode = Objects.hash(index, this.name);
    }

    // アクセサメソッド
    public int index() {
        return index;
    }

    public String name() {
        return name;
    }

    public ProcessedBone parent() {
        return parent;
    }

    public List<ProcessedBone> children() {
        return children;
    }

    public Values.Position translation() {
        return translation;
    }

    public Values.Rotation rotation() {
        return rotation;
    }

    public Values.Scale scale() {
        return scale;
    }

    public Values.Matrix4x4 inverseBindMatrix() {
        return inverseBindMatrix;
    }

    /**
     * 子ボーンリストを取得（既に不変）
     */
    public List<ProcessedBone> getChildren() {
        return children;
    }

    /**
     * 平行移動ベクトルを取得
     */
    public Vector3f getTranslation() {
        return translation.toVector3f();
    }

    /**
     * 回転クォータニオンを取得
     */
    public Quaternionf getRotation() {
        return rotation.toQuaternionf();
    }

    /**
     * スケールベクトルを取得
     */
    public Vector3f getScale() {
        return scale.toVector3f();
    }

    /**
     * 逆バインド行列を取得
     */
    public Matrix4f getInverseBindMatrix() {
        return inverseBindMatrix.toMatrix4f();
    }

    /**
     * 深度を取得
     */
    public int getDepth() {
        Set<ProcessedBone> visited = new HashSet<>();
        int depth = 0;
        ProcessedBone current = parent;

        while (current != null) {
            if (visited.contains(current)) {
                return -1; // 循環参照検出
            }
            visited.add(current);
            depth++;
            current = current.parent();
        }

        return depth;
    }

    /**
     * indexベースのequals（循環参照を回避）
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ProcessedBone other)) return false;

        // indexが同じなら同一のボーンとみなす
        return this.index == other.index &&
                Objects.equals(this.name, other.name);
    }

    /**
     * indexベースのhashCode（循環参照を回避）
     */
    @Override
    public int hashCode() {
        return cachedHashCode;
    }

    @Override
    public @NotNull String toString() {
        return String.format("Bone[%d: %s, children=%d, depth=%d]",
                index, name, children.size(), getDepth());
    }

    // Builder クラスは同じ...
    public static class Builder {
        private final int index;
        private final String name;
        private final List<ProcessedBone.Builder> childrenList = new ArrayList<>();
        private Values.Position translation = Values.ZERO_POSITION;
        private Values.Rotation rotation = Values.IDENTITY_ROTATION;
        private Values.Scale scale = Values.UNIT_SCALE;
        private Values.Matrix4x4 inverseBindMatrix = Values.Matrix4x4.identity();

        public Builder(int index, String name) {
            this.index = index;
            this.name = name;
        }

        public Builder addChild(ProcessedBone.Builder child) {
            if (child != null && !childrenList.contains(child)) {
                childrenList.add(child);
            }
            return this;
        }

        public Builder translation(float x, float y, float z) {
            translation = new Values.Position(x, y, z);
            return this;
        }

        public Builder rotation(float x, float y, float z, float w) {
            rotation = new Values.Rotation(x, y, z, w);
            return this;
        }

        public Builder scale(float x, float y, float z) {
            scale = new Values.Scale(x, y, z);
            return this;
        }

        public Builder inverseBindMatrix(Matrix4f matrix) {
            if (matrix != null) {
                this.inverseBindMatrix = Values.Matrix4x4.from(matrix);
            }
            return this;
        }

        public ProcessedBone build(@Nullable ProcessedBone parent, ProcessedBone[] bones) {
            var bone = new ProcessedBone(
                    index,
                    name,
                    parent,
                    new ArrayList<>(childrenList.size()),
                    translation,
                    rotation,
                    scale,
                    inverseBindMatrix
            );
            bones[this.index] = bone;

            childrenList.forEach(child -> bone.children.add(child.build(bone, bones)));

            return bone;
        }
    }
}