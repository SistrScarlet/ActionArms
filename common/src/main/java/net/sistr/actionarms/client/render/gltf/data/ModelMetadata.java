package net.sistr.actionarms.client.render.gltf.data;

import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Map;

/**
 * glTFモデルのメタデータを表すレコードクラス
 */
public record ModelMetadata(
        Identifier modelPath,                           // "model": MCパス形式のモデルパス
        Map<String, List<String>> hideBoneKeys,         // "hide_bone_keys": コンテキスト → 隠蔽対象ボーンリスト
        TextureSettings textureSettings                 // "texture_settings": テクスチャ設定
) {
    /**
     * ビルダーパターンでModelMetadataを構築
     *
     * @param modelPath モデルパス
     * @return ビルダーインスタンス
     */
    public static Builder builder(Identifier modelPath) {
        return new Builder(modelPath);
    }

    public static class Builder {
        private final Identifier modelPath;
        private int sceneIndex;
        private Map<String, List<String>> hideBoneKeys = Map.of();
        private TextureSettings textureSettings;

        private Builder(Identifier modelPath) {
            this.modelPath = modelPath;
        }

        /**
         * シーンインデックスを設定
         *
         * @param sceneIndex シーンインデックス
         */
        public Builder sceneIndex(int sceneIndex) {
            this.sceneIndex = sceneIndex;
            return this;
        }

        /**
         * 隠蔽ボーンキーを設定
         *
         * @param hideBoneKeys コンテキスト → 隠蔽対象ボーンリスト
         */
        public Builder hideBoneKeys(Map<String, List<String>> hideBoneKeys) {
            this.hideBoneKeys = Map.copyOf(hideBoneKeys);
            return this;
        }

        /**
         * テクスチャ設定を設定
         *
         * @param textureSettings テクスチャ設定
         */
        public Builder textureSettings(TextureSettings textureSettings) {
            this.textureSettings = textureSettings;
            return this;
        }

        public ModelMetadata build() {
            return new ModelMetadata(
                    new Identifier(modelPath.getNamespace(), modelPath.getPath() + "_scene" + sceneIndex),
                    hideBoneKeys, textureSettings
            );
        }
    }
}