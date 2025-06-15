package net.sistr.actionarms.client.render.gltf.data;

import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * 変換済みマテリアルデータ管理クラス（Record版）
 * glTFのMaterialModelから変換されたマテリアル情報を保持
 */
public record ProcessedMaterial(
        String name,
        @Nullable String baseColorTexture,
        @Nullable String metallicRoughnessTexture,
        @Nullable String normalTexture,
        @Nullable String occlusionTexture,
        @Nullable String emissiveTexture,
        float[] baseColorFactor,    // RGBA [r, g, b, a]
        float[] emissiveFactor,     // RGB [r, g, b]
        float metallicFactor,
        float roughnessFactor,
        float normalScale,
        float occlusionStrength,
        String alphaMode,           // "OPAQUE", "MASK", "BLEND"
        float alphaCutoff,
        boolean doubleSided
) {

    /**
     * コンストラクタでのバリデーション
     */
    public ProcessedMaterial {
        // 名前のデフォルト値設定
        if (name == null || name.trim().isEmpty()) {
            name = "Material_" + System.identityHashCode(this);
        }

        // 配列の防御的コピーとバリデーション
        if (baseColorFactor == null) {
            baseColorFactor = new float[]{1.0f, 1.0f, 1.0f, 1.0f}; // デフォルト：白
        } else {
            if (baseColorFactor.length != 4) {
                throw new IllegalArgumentException("baseColorFactor must have 4 components (RGBA)");
            }
            baseColorFactor = Arrays.copyOf(baseColorFactor, 4);
            // RGBA値を0-1の範囲にクランプ
            for (int i = 0; i < 4; i++) {
                baseColorFactor[i] = Math.max(0.0f, Math.min(1.0f, baseColorFactor[i]));
            }
        }

        if (emissiveFactor == null) {
            emissiveFactor = new float[]{0.0f, 0.0f, 0.0f}; // デフォルト：黒（発光なし）
        } else {
            if (emissiveFactor.length != 3) {
                throw new IllegalArgumentException("emissiveFactor must have 3 components (RGB)");
            }
            emissiveFactor = Arrays.copyOf(emissiveFactor, 3);
            // RGB値を0-1の範囲にクランプ
            for (int i = 0; i < 3; i++) {
                emissiveFactor[i] = Math.max(0.0f, Math.min(1.0f, emissiveFactor[i]));
            }
        }

        // 係数値のバリデーション
        metallicFactor = Math.max(0.0f, Math.min(1.0f, metallicFactor));
        roughnessFactor = Math.max(0.0f, Math.min(1.0f, roughnessFactor));
        normalScale = Math.max(0.0f, normalScale);
        occlusionStrength = Math.max(0.0f, Math.min(1.0f, occlusionStrength));
        alphaCutoff = Math.max(0.0f, Math.min(1.0f, alphaCutoff));

        // アルファモードのバリデーション
        if (alphaMode == null) {
            alphaMode = "OPAQUE";
        } else if (!alphaMode.equals("OPAQUE") && !alphaMode.equals("MASK") && !alphaMode.equals("BLEND")) {
            alphaMode = "OPAQUE"; // 無効な値の場合はデフォルトに戻す
        }
    }

    /**
     * テクスチャを持っているかチェック
     */
    public boolean hasBaseColorTexture() {
        return baseColorTexture != null && !baseColorTexture.trim().isEmpty();
    }

    public boolean hasMetallicRoughnessTexture() {
        return metallicRoughnessTexture != null && !metallicRoughnessTexture.trim().isEmpty();
    }

    public boolean hasNormalTexture() {
        return normalTexture != null && !normalTexture.trim().isEmpty();
    }

    public boolean hasOcclusionTexture() {
        return occlusionTexture != null && !occlusionTexture.trim().isEmpty();
    }

    public boolean hasEmissiveTexture() {
        return emissiveTexture != null && !emissiveTexture.trim().isEmpty();
    }

    /**
     * 透明度を使用するかチェック
     */
    public boolean usesTransparency() {
        return "BLEND".equals(alphaMode) ||
                ("MASK".equals(alphaMode) && alphaCutoff < 1.0f) ||
                baseColorFactor[3] < 1.0f; // アルファチャンネルが1未満
    }

    /**
     * 発光するかチェック
     */
    public boolean isEmissive() {
        return hasEmissiveTexture() ||
                (emissiveFactor[0] > 0.0f || emissiveFactor[1] > 0.0f || emissiveFactor[2] > 0.0f);
    }

    /**
     * デバッグ情報を取得
     */
    public String getDebugInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("ProcessedMaterial[%s", name));

        if (hasBaseColorTexture()) {
            sb.append(", baseColor=").append(baseColorTexture);
        }
        if (hasMetallicRoughnessTexture()) {
            sb.append(", metallicRoughness=").append(metallicRoughnessTexture);
        }
        if (hasNormalTexture()) {
            sb.append(", normal=").append(normalTexture);
        }
        if (hasOcclusionTexture()) {
            sb.append(", occlusion=").append(occlusionTexture);
        }
        if (hasEmissiveTexture()) {
            sb.append(", emissive=").append(emissiveTexture);
        }

        sb.append(String.format(", factors[base=%.2f,%.2f,%.2f,%.2f, metallic=%.2f, roughness=%.2f]",
                baseColorFactor[0], baseColorFactor[1], baseColorFactor[2], baseColorFactor[3],
                metallicFactor, roughnessFactor));

        sb.append(String.format(", alpha=%s", alphaMode));
        if ("MASK".equals(alphaMode)) {
            sb.append(String.format("(%.2f)", alphaCutoff));
        }

        if (doubleSided) {
            sb.append(", doubleSided");
        }

        sb.append("]");
        return sb.toString();
    }

    /**
     * ビルダーパターンの実装
     */
    public static class Builder {
        private String name;
        private @Nullable String baseColorTexture;
        private @Nullable String metallicRoughnessTexture;
        private @Nullable String normalTexture;
        private @Nullable String occlusionTexture;
        private @Nullable String emissiveTexture;
        private float[] baseColorFactor = {1.0f, 1.0f, 1.0f, 1.0f}; // デフォルト：白
        private float[] emissiveFactor = {0.0f, 0.0f, 0.0f}; // デフォルト：黒
        private float metallicFactor = 1.0f;
        private float roughnessFactor = 1.0f;
        private float normalScale = 1.0f;
        private float occlusionStrength = 1.0f;
        private String alphaMode = "OPAQUE";
        private float alphaCutoff = 0.5f;
        private boolean doubleSided = false;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder baseColorTexture(@Nullable String baseColorTexture) {
            this.baseColorTexture = baseColorTexture;
            return this;
        }

        @SuppressWarnings("MethodParameterNamingConvention")
        public Builder metallicRoughnessTexture(@Nullable String metallicRoughnessTexture) {
            this.metallicRoughnessTexture = metallicRoughnessTexture;
            return this;
        }

        public Builder normalTexture(@Nullable String normalTexture) {
            this.normalTexture = normalTexture;
            return this;
        }

        public Builder occlusionTexture(@Nullable String occlusionTexture) {
            this.occlusionTexture = occlusionTexture;
            return this;
        }

        public Builder emissiveTexture(@Nullable String emissiveTexture) {
            this.emissiveTexture = emissiveTexture;
            return this;
        }

        public Builder baseColorFactor(float r, float g, float b, float a) {
            this.baseColorFactor = new float[]{r, g, b, a};
            return this;
        }

        public Builder baseColorFactor(float[] rgba) {
            if (rgba != null && rgba.length == 4) {
                this.baseColorFactor = Arrays.copyOf(rgba, 4);
            }
            return this;
        }

        public Builder emissiveFactor(float r, float g, float b) {
            this.emissiveFactor = new float[]{r, g, b};
            return this;
        }

        public Builder emissiveFactor(float[] rgb) {
            if (rgb != null && rgb.length == 3) {
                this.emissiveFactor = Arrays.copyOf(rgb, 3);
            }
            return this;
        }

        public Builder metallicFactor(float metallicFactor) {
            this.metallicFactor = metallicFactor;
            return this;
        }

        public Builder roughnessFactor(float roughnessFactor) {
            this.roughnessFactor = roughnessFactor;
            return this;
        }

        public Builder normalScale(float normalScale) {
            this.normalScale = normalScale;
            return this;
        }

        public Builder occlusionStrength(float occlusionStrength) {
            this.occlusionStrength = occlusionStrength;
            return this;
        }

        public Builder alphaMode(String alphaMode) {
            this.alphaMode = alphaMode;
            return this;
        }

        public Builder alphaCutoff(float alphaCutoff) {
            this.alphaCutoff = alphaCutoff;
            return this;
        }

        public Builder doubleSided(boolean doubleSided) {
            this.doubleSided = doubleSided;
            return this;
        }

        /**
         * ProcessedMaterialインスタンスを構築
         */
        public ProcessedMaterial build() {
            return new ProcessedMaterial(
                    name, baseColorTexture, metallicRoughnessTexture, normalTexture,
                    occlusionTexture, emissiveTexture, baseColorFactor, emissiveFactor,
                    metallicFactor, roughnessFactor, normalScale, occlusionStrength,
                    alphaMode, alphaCutoff, doubleSided
            );
        }
    }

    /**
     * 新しいBuilderインスタンスを作成
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * デフォルトマテリアルを作成
     */
    public static ProcessedMaterial createDefault() {
        return builder()
                .name("DefaultMaterial")
                .build();
    }

    /**
     * このマテリアルをベースにした新しいBuilderを作成
     */
    public Builder toBuilder() {
        return builder()
                .name(name)
                .baseColorTexture(baseColorTexture)
                .metallicRoughnessTexture(metallicRoughnessTexture)
                .normalTexture(normalTexture)
                .occlusionTexture(occlusionTexture)
                .emissiveTexture(emissiveTexture)
                .baseColorFactor(baseColorFactor)
                .emissiveFactor(emissiveFactor)
                .metallicFactor(metallicFactor)
                .roughnessFactor(roughnessFactor)
                .normalScale(normalScale)
                .occlusionStrength(occlusionStrength)
                .alphaMode(alphaMode)
                .alphaCutoff(alphaCutoff)
                .doubleSided(doubleSided);
    }
}
