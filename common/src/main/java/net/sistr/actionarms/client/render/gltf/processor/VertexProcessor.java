package net.sistr.actionarms.client.render.gltf.processor;

import net.sistr.actionarms.client.render.gltf.data.MorphTarget;
import net.sistr.actionarms.client.render.gltf.renderer.RenderingContext;
import net.sistr.actionarms.client.render.gltf.data.ComputedBoneMatricesData;
import net.sistr.actionarms.client.render.gltf.data.ComputedVertexData;
import net.sistr.actionarms.client.render.gltf.data.ProcessedMesh;
import net.sistr.actionarms.client.render.gltf.data.AccessorData;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.util.List;

/**
 * 頂点処理クラス
 */
public class VertexProcessor {

    /**
     * メッシュの最終頂点データを計算
     * モーフターゲットとスキニングを適用して最終的な頂点位置・法線を生成
     */
    public ComputedVertexData computeVertices(ProcessedMesh mesh, RenderingContext context, ComputedBoneMatricesData boneMatrices, float[] morphWeights) {
        if (!mesh.hasPositions()) {
            throw new IllegalArgumentException("Mesh must have POSITION attribute: " + mesh.getName());
        }

        AccessorData positionData = mesh.getPositions();
        AccessorData normalData = mesh.getNormals();

        int vertexCount = positionData.getElementCount();

        // 最終結果用の配列
        float[] finalPositions = new float[vertexCount * 3];
        float[] finalNormals = normalData != null ? new float[vertexCount * 3] : null;

        // ベースデータをコピー
        copyBaseData(positionData, normalData, finalPositions, finalNormals);

        // モーフターゲットの適用
        if (morphWeights != null && morphWeights.length > 0 && mesh.hasMorphTargets()) {
            applyMorphTargets(mesh, morphWeights, finalPositions, finalNormals);
        }

        // スキニングの適用
        if (mesh.hasSkinning() && boneMatrices != null) {
            applySkinning(mesh, boneMatrices.getMatrices(), finalPositions, finalNormals);
        }

        // 法線の正規化
        if (finalNormals != null) {
            normalizeNormals(finalNormals);
        }

        return new ComputedVertexData(finalPositions, finalNormals, mesh.getUvsRaw());
    }

    /**
     * ベースデータのコピー（効率的な配列コピー）
     */
    private void copyBaseData(AccessorData positionData, AccessorData normalData,
                              float[] finalPositions, float[] finalNormals) {

        // 位置データのコピー
        float[] basePositions = positionData.getFloatDataReadOnly();
        System.arraycopy(basePositions, 0, finalPositions, 0, Math.min(basePositions.length, finalPositions.length));

        // 法線データのコピー
        if (normalData != null && finalNormals != null) {
            float[] baseNormals = normalData.getFloatDataReadOnly();
            System.arraycopy(baseNormals, 0, finalNormals, 0, Math.min(baseNormals.length, finalNormals.length));
        }
    }

    /**
     * モーフターゲットの適用（AccessorDataベース）
     */
    private void applyMorphTargets(ProcessedMesh mesh, float[] morphWeights,
                                   float[] positions, float[] normals) {
        List<MorphTarget> morphTargets = mesh.getMorphTargets();

        for (int targetIndex = 0; targetIndex < morphTargets.size() && targetIndex < morphWeights.length; targetIndex++) {
            float weight = morphWeights[targetIndex];
            if (Math.abs(weight) < 0.001f) continue; // 重みが小さい場合はスキップ

            MorphTarget target = morphTargets.get(targetIndex);

            // 位置の差分を適用
            if (target.hasPositionDeltas()) {
                float[] deltas = target.getPositionDeltasRaw();
                for (int i = 0; i < positions.length && i < deltas.length; i++) {
                    positions[i] += deltas[i] * weight;
                }
            }

            // 法線の差分を適用
            if (target.hasNormalDeltas() && normals != null) {
                float[] normalDeltas = target.getNormalDeltasRaw();
                for (int i = 0; i < normals.length && i < normalDeltas.length; i++) {
                    normals[i] += normalDeltas[i] * weight;
                }
            }
        }
    }

    /**
     * スキニングの適用（AccessorDataベース）
     */
    private void applySkinning(ProcessedMesh mesh, Matrix4f[] boneMatrices,
                               float[] positions, float[] normals) {

        AccessorData boneIndicesData = mesh.getBoneIndices();
        AccessorData boneWeightsData = mesh.getBoneWeights();

        if (boneIndicesData == null || boneWeightsData == null) return;

        int[] boneIndices = boneIndicesData.getIntDataReadOnly();
        float[] boneWeights = boneWeightsData.getFloatDataReadOnly();

        Vector4f tempPos = new Vector4f();
        Vector4f tempNormal = new Vector4f();
        Vector4f resultPos = new Vector4f();
        Vector4f resultNormal = new Vector4f();

        int vertexCount = positions.length / 3;

        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            int baseIndex = vertexIndex * 3;
            int weightIndex = vertexIndex * 4;

            // 範囲チェック
            if (weightIndex + 3 >= boneIndices.length || weightIndex + 3 >= boneWeights.length) {
                continue;
            }

            // 元の頂点位置と法線
            float origX = positions[baseIndex];
            float origY = positions[baseIndex + 1];
            float origZ = positions[baseIndex + 2];

            float origNx = normals != null ? normals[baseIndex] : 0;
            float origNy = normals != null ? normals[baseIndex + 1] : 1;
            float origNz = normals != null ? normals[baseIndex + 2] : 0;

            resultPos.set(0, 0, 0, 0);
            resultNormal.set(0, 0, 0, 0);

            // 4つのボーンの影響を合成
            for (int boneSlot = 0; boneSlot < 4; boneSlot++) {
                int boneIndex = boneIndices[weightIndex + boneSlot];
                float weight = boneWeights[weightIndex + boneSlot];

                if (weight <= 0.001f || boneIndex < 0 || boneIndex >= boneMatrices.length) {
                    continue;
                }

                Matrix4f boneMatrix = boneMatrices[boneIndex];

                // 位置変換
                tempPos.set(origX, origY, origZ, 1.0f);
                boneMatrix.transform(tempPos);
                resultPos.add(tempPos.x * weight, tempPos.y * weight, tempPos.z * weight, 0);

                // 法線変換（法線があるときのみ）
                if (normals != null) {
                    tempNormal.set(origNx, origNy, origNz, 0.0f);
                    boneMatrix.transform(tempNormal);
                    resultNormal.add(tempNormal.x * weight, tempNormal.y * weight, tempNormal.z * weight, 0);
                }
            }

            // 結果を配列に戻す
            positions[baseIndex] = resultPos.x;
            positions[baseIndex + 1] = resultPos.y;
            positions[baseIndex + 2] = resultPos.z;

            if (normals != null) {
                normals[baseIndex] = resultNormal.x;
                normals[baseIndex + 1] = resultNormal.y;
                normals[baseIndex + 2] = resultNormal.z;
            }
        }
    }

    /**
     * 法線の正規化
     */
    private void normalizeNormals(float[] normals) {
        for (int i = 0; i < normals.length; i += 3) {
            float x = normals[i];
            float y = normals[i + 1];
            float z = normals[i + 2];

            float length = (float) Math.sqrt(x * x + y * y + z * z);
            if (length > 0.001f) {
                normals[i] = x / length;
                normals[i + 1] = y / length;
                normals[i + 2] = z / length;
            }
        }
    }

}
