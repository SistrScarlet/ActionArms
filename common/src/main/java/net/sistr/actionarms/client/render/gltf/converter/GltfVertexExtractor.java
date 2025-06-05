package net.sistr.actionarms.client.render.gltf.converter;

import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.MeshPrimitiveModel;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.client.render.gltf.data.*;
import net.sistr.actionarms.client.render.gltf.util.DrawingMode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * glTFメッシュからProcessedMeshへの変換を行うクラス
 * AccessorDataCacheを使用してアクセサデータの重複を排除し、メモリ効率を向上
 */
public class GltfVertexExtractor {
    private final AccessorDataCache accessorCache;

    public GltfVertexExtractor() {
        this.accessorCache = new AccessorDataCache();
    }

    public List<ProcessedMesh> extractMeshes(MeshModel meshModel, ProcessedSkin associatedSkin) {
        List<MeshPrimitiveModel> primitives = meshModel.getMeshPrimitiveModels();
        List<ProcessedMesh> processedMeshes = new ArrayList<>();

        String baseMeshName = getMeshName(meshModel);

        for (int i = 0; i < primitives.size(); i++) {
            MeshPrimitiveModel primitive = primitives.get(i);

            try {
                // プリミティブごとに個別のProcessedMeshを作成
                String meshName = primitives.size() > 1 ?
                        baseMeshName + "_Primitive_" + i : baseMeshName;

                ProcessedMesh processedMesh = extractSinglePrimitive(
                        primitive, meshName, associatedSkin, i);

                processedMeshes.add(processedMesh);

            } catch (Exception e) {
                ActionArms.LOGGER.error("Failed to process primitive {} of mesh {}: {}", i, baseMeshName, e.getMessage());
            }
        }

        ActionArms.LOGGER.debug("Cache stats after processing mesh {}: {}", baseMeshName, accessorCache.getStats());
        return processedMeshes;
    }

    private String getMeshName(MeshModel meshModel) {
        // MeshModelから名前を取得
        String name = meshModel.getName();
        return name != null && !name.trim().isEmpty() ?
                name.trim() : "Mesh_" + System.identityHashCode(meshModel);
    }

    private ProcessedMesh extractSinglePrimitive(MeshPrimitiveModel primitive,
                                                 String meshName,
                                                 ProcessedSkin associatedSkin,
                                                 int primitiveIndex) {

        // AccessorDataを使用してアクセサレベルでの頂点データ管理
        Map<String, AccessorData> attributeData = extractAttributeData(primitive, meshName, primitiveIndex);

        // インデックスデータの抽出
        AccessorData indexData = extractIndexData(primitive, meshName, primitiveIndex);

        // モーフターゲットの抽出
        List<MorphTarget> morphTargets = extractMorphTargets(primitive, primitiveIndex);

        // マテリアルインデックスの取得
        int materialIndex = getMaterialIndex(primitive);

        // 描画モードの取得
        DrawingMode drawingMode = getDrawingMode(primitive);

        // ProcessedMeshの作成（新しい設計では頂点データは直接持たない）
        return new ProcessedMesh(
                meshName, attributeData, indexData, morphTargets,
                associatedSkin, materialIndex, drawingMode, primitiveIndex);
    }

    /**
     * プリミティブの全属性データをAccessorDataとして抽出
     */
    private Map<String, AccessorData> extractAttributeData(MeshPrimitiveModel primitive, String meshName, int primitiveIndex) {
        Map<String, AccessorData> attributeData = new HashMap<>();
        Map<String, AccessorModel> attributes = primitive.getAttributes();

        for (Map.Entry<String, AccessorModel> entry : attributes.entrySet()) {
            String attributeName = entry.getKey();
            AccessorModel accessor = entry.getValue();

            if (accessor == null) continue;

            try {
                // 属性名からAccessorDataTypeを推定
                AccessorDataType dataType = AccessorDataType.fromAttributeName(attributeName);

                // キャッシュからAccessorDataを取得または作成
                String accessorId = String.format("%s_P%d_%s", meshName, primitiveIndex, attributeName);
                AccessorData data = accessorCache.getOrCreate(accessor, dataType, accessorId);

                attributeData.put(attributeName, data);

                ActionArms.LOGGER.debug("Extracted attribute {} for mesh {}: {}",
                        attributeName, meshName, data.getDebugInfo());
            } catch (Exception e) {
                ActionArms.LOGGER.error("Failed to extract attribute {} for mesh {}: {}",
                        attributeName, meshName, e.getMessage());
            }
        }

        // 必須属性のチェックと補完
        ensureRequiredAttributes(attributeData, primitive, meshName, primitiveIndex);

        return attributeData;
    }

    /**
     * 必須属性のチェックと不足している属性の補完
     */
    private void ensureRequiredAttributes(Map<String, AccessorData> attributeData,
                                          MeshPrimitiveModel primitive, String meshName, int primitiveIndex) {
        // POSITION属性は必須
        if (!attributeData.containsKey("POSITION")) {
            throw new RuntimeException("POSITION attribute is required for mesh: " + meshName);
        }

        AccessorData positionData = attributeData.get("POSITION");
        int vertexCount = positionData.getElementCount();

        // NORMAL属性がない場合はデフォルト法線を生成
        if (!attributeData.containsKey("NORMAL")) {
            ActionArms.LOGGER.debug("Generating default normals for mesh: {}", meshName);
            AccessorData defaultNormals = createDefaultNormals(vertexCount, meshName, primitiveIndex);
            attributeData.put("NORMAL", defaultNormals);
        }

        // TEXCOORD_0属性がない場合はデフォルトUVを生成
        if (!attributeData.containsKey("TEXCOORD_0")) {
            ActionArms.LOGGER.debug("Generating default UVs for mesh: {}", meshName);
            AccessorData defaultUVs = createDefaultUVs(vertexCount, meshName, primitiveIndex);
            attributeData.put("TEXCOORD_0", defaultUVs);
        }
    }

    /**
     * デフォルト法線データを作成
     */
    private AccessorData createDefaultNormals(int vertexCount, String meshName, int primitiveIndex) {
        float[] normals = new float[vertexCount * 3];
        for (int i = 0; i < normals.length; i += 3) {
            normals[i] = 0.0f;     // x
            normals[i + 1] = 1.0f; // y (上向き)
            normals[i + 2] = 0.0f; // z
        }

        // ダミーのシグネチャを作成（デフォルトデータ用）
        AccessorSignature signature = new AccessorSignature(
                0, 0, 5126, vertexCount, "VEC3", false); // FLOAT, VEC3, not normalized

        String id = String.format("%s_P%d_DEFAULT_NORMAL", meshName, primitiveIndex);
        return new AccessorData(id, AccessorDataType.NORMAL, vertexCount, normals, false, signature);
    }

    /**
     * デフォルトUVデータを作成
     */
    private AccessorData createDefaultUVs(int vertexCount, String meshName, int primitiveIndex) {
        float[] uvs = new float[vertexCount * 2]; // すべて(0,0)で初期化

        // ダミーのシグネチャを作成（デフォルトデータ用）
        AccessorSignature signature = new AccessorSignature(
                0, 0, 5126, vertexCount, "VEC2", false); // FLOAT, VEC2, not normalized

        String id = String.format("%s_P%d_DEFAULT_UV", meshName, primitiveIndex);
        return new AccessorData(id, AccessorDataType.UV_0, vertexCount, uvs, false, signature);
    }

    /**
     * インデックスデータの抽出
     */
    private AccessorData extractIndexData(MeshPrimitiveModel primitive, String meshName, int primitiveIndex) {
        AccessorModel indexAccessor = primitive.getIndices();

        if (indexAccessor == null) {
            // インデックスがない場合は連番インデックスを生成
            int vertexCount = getVertexCount(primitive);
            return createSequentialIndices(vertexCount, meshName, primitiveIndex);
        }

        try {
            String accessorId = String.format("%s_P%d_INDICES", meshName, primitiveIndex);
            return accessorCache.getOrCreate(indexAccessor, AccessorDataType.SCALAR_INT, accessorId);
        } catch (Exception e) {
            ActionArms.LOGGER.error("Failed to extract indices for mesh {}: {}", meshName, e.getMessage());
            // フォールバック：連番インデックスを生成
            int vertexCount = getVertexCount(primitive);
            return createSequentialIndices(vertexCount, meshName, primitiveIndex);
        }
    }

    /**
     * 連番インデックスを作成
     */
    private AccessorData createSequentialIndices(int vertexCount, String meshName, int primitiveIndex) {
        int[] indices = new int[vertexCount];
        for (int i = 0; i < vertexCount; i++) {
            indices[i] = i;
        }

        AccessorSignature signature = new AccessorSignature(
                0, 0, 5125, vertexCount, "SCALAR", false); // UNSIGNED_INT, SCALAR

        String id = String.format("%s_P%d_SEQ_INDICES", meshName, primitiveIndex);
        return new AccessorData(id, AccessorDataType.SCALAR_INT, vertexCount, indices, false, signature);
    }

    /**
     * モーフターゲットの抽出（AccessorDataCache使用版）
     */
    private List<MorphTarget> extractMorphTargets(MeshPrimitiveModel primitive, int primitiveIndex) {
        List<Map<String, AccessorModel>> targets = primitive.getTargets();
        List<MorphTarget> morphTargets = new ArrayList<>();

        for (int i = 0; i < targets.size(); i++) {
            Map<String, AccessorModel> target = targets.get(i);
            String targetName = "MorphTarget_P" + primitiveIndex + "_T" + i;

            try {
                // 位置の差分
                AccessorData positionDeltas = null;
                AccessorModel positionDelta = target.get("POSITION");
                if (positionDelta != null) {
                    String deltaId = targetName + "_POSITION";
                    positionDeltas = accessorCache.getOrCreate(positionDelta, AccessorDataType.MORPH_POSITION, deltaId);
                }

                // 法線の差分
                AccessorData normalDeltas = null;
                AccessorModel normalDelta = target.get("NORMAL");
                if (normalDelta != null) {
                    String deltaId = targetName + "_NORMAL";
                    normalDeltas = accessorCache.getOrCreate(normalDelta, AccessorDataType.MORPH_NORMAL, deltaId);
                }

                // MorphTargetの作成（新しいAccessorDataコンストラクタを使用）
                MorphTarget morphTarget = new MorphTarget(targetName, positionDeltas, normalDeltas);
                morphTargets.add(morphTarget);

                ActionArms.LOGGER.debug("Created morph target: {}", targetName);
            } catch (Exception e) {
                ActionArms.LOGGER.error("Failed to create morph target {}: {}", targetName, e.getMessage());
            }
        }

        return morphTargets;
    }

    private int getMaterialIndex(MeshPrimitiveModel primitive) {
        MaterialModel material = primitive.getMaterialModel();
        return material != null ? System.identityHashCode(material) : -1;
    }

    private DrawingMode getDrawingMode(MeshPrimitiveModel primitive) {
        int mode = primitive.getMode();
        DrawingMode drawingMode = DrawingMode.from(mode);
        return drawingMode != null ? drawingMode : DrawingMode.TRIANGLES; // デフォルト
    }

    private int getVertexCount(MeshPrimitiveModel primitive) {
        AccessorModel positionAccessor = primitive.getAttributes().get("POSITION");
        return positionAccessor != null ? positionAccessor.getCount() : 0;
    }

    /**
     * キャッシュ統計情報を取得
     */
    public AccessorDataCache.CacheStats getCacheStats() {
        return accessorCache.getStats();
    }

    /**
     * キャッシュをクリア
     */
    public void clearCache() {
        accessorCache.clear();
    }

    /**
     * キャッシュの内容をデバッグ出力
     */
    public void printCacheContents() {
        accessorCache.printCacheContents();
    }
}
