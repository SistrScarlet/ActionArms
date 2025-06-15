package net.sistr.actionarms.client.render.gltf.data;

import de.javagl.jgltf.model.GltfModel;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 変換済みglTFモデルデータの統合管理recordクラス（マテリアル対応・不変設計）
 * ロード時に一度構築された後は変更されない設計
 */
public record ProcessedGltfModel(
        String name,
        List<ProcessedMesh> meshes,
        List<ProcessedSkin> skins,
        List<ProcessedMaterial> materials,
        Map<String, ProcessedAnimation> animations,
        @Deprecated GltfModel originalGltfModel // デバッグ用のため、将来削除する
) {

    /**
     * コンストラクタでのバリデーションと不変化
     */
    public ProcessedGltfModel {
        // 名前のデフォルト値設定
        if (name == null || name.trim().isEmpty()) {
            name = "GltfModel_" + System.identityHashCode(this);
        }

        // 防御的コピーによる不変化
        meshes = meshes != null ? List.copyOf(meshes) : Collections.emptyList();
        skins = skins != null ? List.copyOf(skins) : Collections.emptyList();
        materials = materials != null ? List.copyOf(materials) : Collections.emptyList();

        // アニメーションマップの構築（名前 -> アニメーション）
        if (animations == null) {
            animations = Collections.emptyMap();
        } else {
            // 不変化
            animations = Collections.unmodifiableMap(animations);
        }
    }

    /**
     * アニメーションリストを受け取るコンストラクタ（利便性のため）
     */
    public ProcessedGltfModel(String name, List<ProcessedMesh> meshes, List<ProcessedSkin> skins,
                              List<ProcessedMaterial> materials, List<ProcessedAnimation> animationList,
                              GltfModel originalGltfModel) {
        this(name, meshes, skins, materials,
                animationList != null ?
                        animationList.stream().collect(Collectors.toMap(ProcessedAnimation::name, a -> a)) :
                        Collections.emptyMap(),
                originalGltfModel);
    }

    // === アニメーション関連メソッド ===

    public Optional<ProcessedAnimation> getAnimation(String animationName) {
        return Optional.ofNullable(animations.get(animationName));
    }

    /**
     * ビルダーパターンの実装
     */
    public static class Builder {
        private String name;
        private final List<ProcessedMesh> meshes = new ArrayList<>(10);
        private final List<ProcessedSkin> skins = new ArrayList<>(10);
        private final List<ProcessedMaterial> materials = new ArrayList<>(10);
        private final List<ProcessedAnimation> animations = new ArrayList<>(10);
        private GltfModel originalGltfModel;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder addMesh(ProcessedMesh mesh) {
            this.meshes.add(mesh);
            return this;
        }

        public Builder addMeshes(List<ProcessedMesh> meshes) {
            this.meshes.addAll(meshes);
            return this;
        }

        public Builder addSkin(ProcessedSkin skin) {
            this.skins.add(skin);
            return this;
        }

        public Builder addSkins(List<ProcessedSkin> skins) {
            this.skins.addAll(skins);
            return this;
        }

        public Builder addMaterial(ProcessedMaterial material) {
            this.materials.add(material);
            return this;
        }

        public Builder addMaterials(List<ProcessedMaterial> materials) {
            this.materials.addAll(materials);
            return this;
        }

        public Builder addAnimation(ProcessedAnimation animation) {
            this.animations.add(animation);
            return this;
        }

        public Builder addAnimations(List<ProcessedAnimation> animations) {
            if (animations != null) {
                this.animations.addAll(animations);
            }
            return this;
        }

        public Builder originalGltfModel(GltfModel originalGltfModel) {
            this.originalGltfModel = originalGltfModel;
            return this;
        }

        public ProcessedGltfModel build() {
            return new ProcessedGltfModel(name, meshes, skins, materials, animations, originalGltfModel);
        }
    }

    /**
     * 新しいBuilderインスタンスを作成
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 既存のProcessedGltfModelをベースにした新しいBuilderを作成
     */
    public Builder toBuilder() {
        //noinspection deprecation
        return builder()
                .name(name)
                .addMeshes(meshes)
                .addSkins(skins)
                .addMaterials(materials)
                .addAnimations(new ArrayList<>(animations.values()))
                .originalGltfModel(originalGltfModel);
    }
}
