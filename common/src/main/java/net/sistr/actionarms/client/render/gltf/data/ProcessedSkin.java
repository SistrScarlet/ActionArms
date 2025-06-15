package net.sistr.actionarms.client.render.gltf.data;

import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * 変換済みスキンデータ管理recordクラス（不変設計）
 * ボーン階層とその関係性を管理
 */
public record ProcessedSkin(
        String name,
        List<ProcessedBone> bones,
        List<ProcessedBone> rootBones
) {

    /**
     * バリデーション付きコンストラクタ
     */
    public ProcessedSkin {
        // 名前のデフォルト値設定
        if (name == null || name.trim().isEmpty()) {
            name = "DefaultSkin_" + System.identityHashCode(this);
        }

        // ボーンリストの防御的コピーと不変化
        if (bones == null) {
            bones = Collections.emptyList();
        } else {
            bones = List.copyOf(bones);
        }

        // ルートボーンリストの防御的コピーと不変化
        if (rootBones == null) {
            rootBones = Collections.unmodifiableList(findRootBones(bones));
        } else {
            rootBones = List.copyOf(rootBones);
        }
    }

    // === 基本情報アクセサー ===

    public int getBoneCount() {
        return bones.size();
    }

    public int getRootBoneCount() {
        return rootBones.size();
    }

    public boolean hasBones() {
        return !bones.isEmpty();
    }

    public boolean hasRootBones() {
        return !rootBones.isEmpty();
    }

    // === ボーン検索メソッド ===

    /**
     * 名前でボーンを検索
     */
    public Optional<ProcessedBone> findBone(String boneName) {
        return bones.stream()
                .filter(bone -> Objects.equals(bone.name(), boneName))
                .findFirst();
    }

    /**
     * インデックスでボーンを検索
     */
    public Optional<ProcessedBone> findBone(int boneIndex) {
        return bones.stream()
                .filter(bone -> bone.index() == boneIndex)
                .findFirst();
    }

    /**
     * ボーンのインデックスを取得
     */
    public int getBoneIndex(ProcessedBone bone) {
        return bones.indexOf(bone);
    }

    /**
     * 指定されたボーンが含まれているかチェック
     */
    public boolean containsBone(ProcessedBone bone) {
        return bones.contains(bone);
    }

    /**
     * 指定された名前のボーンが含まれているかチェック
     */
    public boolean containsBone(String boneName) {
        return findBone(boneName).isPresent();
    }

    // === 階層構造分析メソッド ===

    /**
     * ルートボーンを自動検出する静的メソッド
     */
    private static List<ProcessedBone> findRootBones(List<ProcessedBone> bones) {
        if (bones == null || bones.isEmpty()) {
            return Collections.emptyList();
        }

        List<ProcessedBone> rootBones = new ArrayList<>();
        for (ProcessedBone bone : bones) {
            if (bone.parent() == null) {
                rootBones.add(bone);
            }
        }
        return rootBones;
    }

    /**
     * ボーン階層の検証
     */
    public void validateHierarchy() {
        for (ProcessedBone bone : bones) {
            // 循環参照のチェック
            if (hasCircularReference(bone)) {
                throw new IllegalStateException("invalid skin hierarchy: %s has circular reference: %s"
                        .formatted(this.name, bone.name()));
            }
        }
    }

    /**
     * 循環参照の検出
     */
    private boolean hasCircularReference(ProcessedBone bone) {
        Set<ProcessedBone> visited = new HashSet<>();
        ProcessedBone current = bone;

        while (current != null) {
            if (visited.contains(current)) {
                return true; // 循環参照発見
            }
            visited.add(current);
            current = current.parent();
        }

        return false;
    }

    /**
     * 最大の階層深度を取得
     */
    public int getMaxDepth() {
        return bones.stream()
                .mapToInt(ProcessedBone::getDepth)
                .max()
                .orElse(0);
    }

    @Override
    public @NotNull String toString() {
        return String.format("ProcessedSkin[%s: %d bones, %d roots, depth=%d]",
                name, getBoneCount(), getRootBoneCount(), getMaxDepth());
    }

    /**
     * スキン統計情報を保持するrecord
     */
    public record SkinStats(
            int boneCount,
            int rootBoneCount,
            int maxDepth,
            boolean validHierarchy,
            boolean hasCircularReference
    ) {
    }

    /**
     * ビルダーパターンの実装
     */
    public static class Builder {
        private String name;
        private final List<ProcessedBone> bones = new ArrayList<>(10);
        private final List<ProcessedBone> rootBones = new ArrayList<>(10);

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder addBone(ProcessedBone bone) {
            if (bone != null && !bones.contains(bone)) {
                bones.add(bone);
            }
            return this;
        }

        public Builder addBones(List<ProcessedBone> bones) {
            for (ProcessedBone bone : bones) {
                addBone(bone);
            }
            return this;
        }

        public Builder rootBones(List<ProcessedBone> rootBones) {
            this.rootBones.addAll(rootBones);
            return this;
        }

        public ProcessedSkin build() {
            var skin = new ProcessedSkin(name, bones, rootBones.isEmpty() ? findRootBones(bones) : rootBones);
            skin.validateHierarchy();
            return skin;
        }
    }

    /**
     * 新しいBuilderインスタンスを作成
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 既存のProcessedSkinをベースにした新しいBuilderを作成
     */
    public Builder toBuilder() {
        return builder()
                .name(name)
                .addBones(bones)
                .rootBones(rootBones);
    }
}
