package net.sistr.actionarms.client.render.gltf.data;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * テクスチャ設定を表すレコードクラス
 */
public record TextureSettings(
        Map<String, Identifier> textureMap,              // "texture_map": テクスチャファイル名 → リソースパス
        Map<String, String> dynamicTextures            // "dynamic_textures": テクスチャファイル名 → コンテキスト名
) {
    /**
     * ビルダーパターンでTextureSettingsを構築
     */
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<String, Identifier> textureMap = new HashMap<>();
        private final Map<String, String> dynamicTextures = new HashMap<>();

        /**
         * テクスチャマッピングを設定
         *
         * @param textureMap テクスチャファイル名 → リソースパス
         */
        public Builder textureMap(Map<String, Identifier> textureMap) {
            this.textureMap.clear();
            this.textureMap.putAll(textureMap);
            return this;
        }

        /**
         * 動的テクスチャを設定
         *
         * @param dynamicTextures テクスチャファイル名 → コンテキスト名
         */
        public Builder dynamicTextures(Map<String, String> dynamicTextures) {
            this.dynamicTextures.clear();
            this.dynamicTextures.putAll(dynamicTextures);
            return this;
        }

        public TextureSettings build() {
            return new TextureSettings(
                    Map.copyOf(textureMap),
                    Map.copyOf(dynamicTextures)
            );
        }
    }
}