package net.sistr.actionarms.client.render.gltf;

import de.javagl.jgltf.model.*;
import net.sistr.actionarms.ActionArms;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GltfVertexExtractor {

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

        // 基本頂点データの抽出
        List<ProcessedVertex> vertices = extractVertices(primitive);

        // インデックスデータの抽出
        int[] indices = extractIndices(primitive);

        // モーフターゲットの抽出
        List<MorphTarget> morphTargets = extractMorphTargets(primitive, primitiveIndex);

        // マテリアルインデックスの取得
        int materialIndex = getMaterialIndex(primitive);

        // 描画モードの取得
        DrawingMode drawingMode = getDrawingMode(primitive);

        // ProcessedMeshの作成
        return new ProcessedMesh(
                meshName, vertices, indices, morphTargets,
                associatedSkin, materialIndex, drawingMode, primitiveIndex);
    }

    //todo ここで取得されるverticesは全primitive共通のため、同一頂点がprimitiveごとに生成されてしまう
    private List<ProcessedVertex> extractVertices(MeshPrimitiveModel primitive) {
        // 頂点数の取得
        int vertexCount = getVertexCount(primitive);
        if (vertexCount == 0) {
            return new ArrayList<>();
        }

        // 各属性の抽出
        float[] positions = extractPositions(primitive);
        float[] normals = extractNormals(primitive);
        float[] uvs = extractUVs(primitive);
        int[] boneIndices = extractBoneIndices(primitive);
        float[] boneWeights = extractBoneWeights(primitive);

        // ProcessedVertexリストの作成
        List<ProcessedVertex> vertices = new ArrayList<>();

        for (int i = 0; i < vertexCount; i++) {
            ProcessedVertex vertex = new ProcessedVertex();

            // 位置
            if (positions != null && i * 3 + 2 < positions.length) {
                vertex.setPosition(positions[i * 3], positions[i * 3 + 1], positions[i * 3 + 2]);
            }

            // 法線
            if (normals != null && i * 3 + 2 < normals.length) {
                vertex.setNormal(normals[i * 3], normals[i * 3 + 1], normals[i * 3 + 2]);
            }

            // UV
            if (uvs != null && i * 2 + 1 < uvs.length) {
                vertex.setUV(uvs[i * 2], uvs[i * 2 + 1]);
            }

            // スキニングデータ
            if (boneIndices != null && boneWeights != null) {
                for (int j = 0; j < 4; j++) {
                    int indexPos = i * 4 + j;
                    if (indexPos < boneIndices.length && indexPos < boneWeights.length) {
                        vertex.setBoneData(j, boneIndices[indexPos], boneWeights[indexPos]);
                    }
                }
                vertex.normalizeWeights();
            }

            vertices.add(vertex);
        }

        return vertices;
    }

    private int[] extractBoneIndices(MeshPrimitiveModel primitive) {
        AccessorModel jointsAccessor = primitive.getAttributes().get("JOINTS_0");
        return jointsAccessor != null ? extractIntArray(jointsAccessor, 4) : null;
    }

    private float[] extractBoneWeights(MeshPrimitiveModel primitive) {
        AccessorModel weightsAccessor = primitive.getAttributes().get("WEIGHTS_0");
        return weightsAccessor != null ? extractFloatArray(weightsAccessor, 4) : null;
    }

    private List<MorphTarget> extractMorphTargets(MeshPrimitiveModel primitive, int primitiveIndex) {
        List<Map<String, AccessorModel>> targets = primitive.getTargets();
        List<MorphTarget> morphTargets = new ArrayList<>();

        for (int i = 0; i < targets.size(); i++) {
            Map<String, AccessorModel> target = targets.get(i);
            String targetName = "MorphTarget_P" + primitiveIndex + "_T" + i;

            float[] positionDeltas = null;
            float[] normalDeltas = null;

            // 位置の差分
            AccessorModel positionDelta = target.get("POSITION");
            if (positionDelta != null) {
                positionDeltas = extractFloatArray(positionDelta, 3);
            }

            // 法線の差分
            AccessorModel normalDelta = target.get("NORMAL");
            if (normalDelta != null) {
                normalDeltas = extractFloatArray(normalDelta, 3);
            }

            MorphTarget morphTarget = new MorphTarget(targetName, positionDeltas, normalDeltas, null);
            morphTargets.add(morphTarget);
        }

        return morphTargets;
    }

    private int getMaterialIndex(MeshPrimitiveModel primitive) {
        // マテリアルインデックスの取得
        MaterialModel material = primitive.getMaterialModel();
        return material != null ? getMaterialIndex(material) : -1;
    }

    private int getMaterialIndex(MaterialModel material) {
        // MaterialModelから実際のインデックスを取得
        // 実装はglTFライブラリの詳細に依存
        return System.identityHashCode(material); // 暫定実装
    }

    private DrawingMode getDrawingMode(MeshPrimitiveModel primitive) {
        // プリミティブの描画モードを取得
        int mode = primitive.getMode();

        return DrawingMode.from(mode);
    }

    // 既存のメソッドは変更なし
    private float[] extractPositions(MeshPrimitiveModel primitive) {
        AccessorModel positionAccessor = primitive.getAttributes().get("POSITION");
        if (positionAccessor == null) {
            throw new RuntimeException("Position attribute not found");
        }
        return extractFloatArray(positionAccessor, 3);
    }

    private float[] extractNormals(MeshPrimitiveModel primitive) {
        AccessorModel normalAccessor = primitive.getAttributes().get("NORMAL");
        if (normalAccessor == null) {
            return generateDefaultNormals(extractPositions(primitive));
        }
        return extractFloatArray(normalAccessor, 3);
    }

    private float[] extractUVs(MeshPrimitiveModel primitive) {
        AccessorModel uvAccessor = primitive.getAttributes().get("TEXCOORD_0");
        if (uvAccessor == null) {
            int vertexCount = getVertexCount(primitive);
            return new float[vertexCount * 2]; // すべて(0,0)
        }
        return extractFloatArray(uvAccessor, 2);
    }

    private int[] extractIndices(MeshPrimitiveModel primitive) {
        AccessorModel indexAccessor = primitive.getIndices();
        if (indexAccessor == null) {
            int vertexCount = getVertexCount(primitive);
            int[] indices = new int[vertexCount];
            for (int i = 0; i < vertexCount; i++) {
                indices[i] = i;
            }
            return indices;
        }

        AccessorData accessorData = indexAccessor.getAccessorData();
        int[] indices = new int[indexAccessor.getCount()];

        if (accessorData instanceof AccessorIntData accessorIntData) {
            for (int i = 0; i < indices.length; i++) {
                indices[i] = accessorIntData.get(i, 0);
            }
        } else if (accessorData instanceof AccessorShortData accessorShortData) {
            for (int i = 0; i < indices.length; i++) {
                indices[i] = accessorShortData.getInt(i, 0);
            }
        }

        return indices;
    }

    // ユーティリティメソッドは変更なし
    private float[] extractFloatArray(AccessorModel accessor, int componentCount) {
        AccessorFloatData data = (AccessorFloatData) accessor.getAccessorData();
        int elementCount = accessor.getCount();
        float[] result = new float[elementCount * componentCount];

        for (int i = 0; i < elementCount; i++) {
            for (int j = 0; j < componentCount; j++) {
                result[i * componentCount + j] = data.get(i, j);
            }
        }

        return result;
    }

    private int[] extractIntArray(AccessorModel accessor, int componentCount) {
        AccessorData accessorData = accessor.getAccessorData();
        int elementCount = accessor.getCount();
        int[] result = new int[elementCount * componentCount];

        if (accessorData instanceof AccessorIntData accessorIntData) {
            for (int i = 0; i < elementCount; i++) {
                for (int j = 0; j < componentCount; j++) {
                    result[i * componentCount + j] = accessorIntData.get(i, j);
                }
            }
        } else if (accessorData instanceof AccessorShortData accessorShortData) {
            for (int i = 0; i < elementCount; i++) {
                for (int j = 0; j < componentCount; j++) {
                    result[i * componentCount + j] = accessorShortData.getInt(i, j);
                }
            }
        }

        return result;
    }

    private int getVertexCount(MeshPrimitiveModel primitive) {
        AccessorModel positionAccessor = primitive.getAttributes().get("POSITION");
        return positionAccessor != null ? positionAccessor.getCount() : 0;
    }

    private float[] generateDefaultNormals(float[] positions) {
        // 簡単な法線生成（上向き法線）
        float[] normals = new float[positions.length];
        for (int i = 0; i < normals.length; i += 3) {
            normals[i] = 0.0f;     // x
            normals[i + 1] = 1.0f; // y (上向き)
            normals[i + 2] = 0.0f; // z
        }
        return normals;
    }
}
