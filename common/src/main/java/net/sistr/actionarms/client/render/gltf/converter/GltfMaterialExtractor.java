package net.sistr.actionarms.client.render.gltf.converter;

import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.TextureModel;
import de.javagl.jgltf.model.v2.MaterialModelV2;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.client.render.gltf.data.ProcessedMaterial;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * glTFマテリアルからProcessedMaterialへの変換処理
 * jglTFのMaterialModelから必要な情報を抽出してProcessedMaterialを生成
 */
public class GltfMaterialExtractor {

    /**
     * glTFモデルからすべてのマテリアルを抽出
     *
     * @param gltfModel glTFモデル
     * @return 変換済みマテリアルのリスト
     */
    public Map<MaterialModel, ProcessedMaterial> extractMaterials(GltfModel gltfModel) {
        Map<MaterialModel, ProcessedMaterial> processedMaterials = new HashMap<>();

        try {
            List<MaterialModel> materialModels = gltfModel.getMaterialModels();

            for (int i = 0; i < materialModels.size(); i++) {
                MaterialModel materialModel = materialModels.get(i);

                try {
                    ProcessedMaterial processedMaterial = extractSingleMaterial(materialModel, i);
                    processedMaterials.put(materialModel, processedMaterial);

                    ActionArms.LOGGER.debug("Extracted material {}: {}", i, processedMaterial.getDebugInfo());
                } catch (RuntimeException e) {
                    ActionArms.LOGGER.error("Failed to extract material {}: {}", i, e.getMessage());

                    // フォールバック：デフォルトマテリアルを追加
                    ProcessedMaterial defaultMaterial = createFallbackMaterial(i);
                    processedMaterials.put(materialModel, defaultMaterial);
                }
            }

        } catch (RuntimeException e) {
            ActionArms.LOGGER.error("Failed to extract materials from model: {}", e.getMessage());
        }

        return processedMaterials;
    }

    /**
     * 単一のMaterialModelからProcessedMaterialを抽出
     *
     * @param materialModel jglTFのマテリアルモデル
     * @param materialIndex マテリアルインデックス
     * @return 変換済みマテリアル
     */
    private ProcessedMaterial extractSingleMaterial(MaterialModel materialModel, int materialIndex) {
        ProcessedMaterial.Builder builder = ProcessedMaterial.builder();

        // 基本情報の設定
        String materialName = materialModel.getName();
        if (materialName == null || materialName.trim().isEmpty()) {
            materialName = "Material_" + materialIndex;
        }
        builder.name(materialName);

        if (materialModel instanceof MaterialModelV2 material) {
            // - PBRメタリックラフネス情報の抽出

            // - 各種テクスチャの抽出（baseColor, metallicRoughness, normal, occlusion, emissive）

            var baseColorTexture = material.getBaseColorTexture();
            builder.baseColorTexture(extractTextureUri(baseColorTexture));

            var normalTexture = material.getNormalTexture();
            builder.normalTexture(extractTextureUri(normalTexture));

            var occlusionTexture = material.getOcclusionTexture();
            builder.occlusionTexture(extractTextureUri(occlusionTexture));

            var emissiveTexture = material.getEmissiveTexture();
            builder.emissiveTexture(extractTextureUri(emissiveTexture));

            //noinspection LocalVariableNamingConvention
            var metallicRoughnessTexture = material.getMetallicRoughnessTexture();
            builder.metallicRoughnessTexture(extractTextureUri(metallicRoughnessTexture));

            // - 係数値の抽出（baseColorFactor, metallicFactor, roughnessFactor等）
            builder.baseColorFactor(material.getBaseColorFactor());
            builder.metallicFactor(material.getMetallicFactor());
            builder.roughnessFactor(material.getRoughnessFactor());
            builder.emissiveFactor(material.getEmissiveFactor());
            builder.occlusionStrength(material.getOcclusionStrength());
            builder.normalScale(material.getNormalScale());

            // - アルファモード・アルファカットオフの抽出
            builder.alphaMode(material.getAlphaMode().name());
            builder.alphaCutoff(material.getAlphaCutoff());

            // - doubleSidedフラグの抽出
            builder.doubleSided(material.isDoubleSided());
        }

        return builder.build();
    }

    /**
     * フォールバック用のデフォルトマテリアルを作成
     *
     * @param materialIndex マテリアルインデックス
     * @return フォールバックマテリアル
     */
    private ProcessedMaterial createFallbackMaterial(int materialIndex) {
        return ProcessedMaterial.builder()
                .name("FallbackMaterial_" + materialIndex)
                .build();
    }

    /**
     * テクスチャのURIを抽出するヘルパーメソッド
     *
     * @param textureModel テクスチャモデル
     * @return テクスチャのURI文字列
     */
    private @Nullable String extractTextureUri(@Nullable TextureModel textureModel) {
        if (textureModel == null) return null;
        var imageModel = textureModel.getImageModel();
        return imageModel.getUri();
    }
}
