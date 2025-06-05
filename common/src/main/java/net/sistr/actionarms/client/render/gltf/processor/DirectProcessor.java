package net.sistr.actionarms.client.render.gltf.processor;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.sistr.actionarms.client.render.gltf.data.*;
import net.sistr.actionarms.client.render.gltf.renderer.RenderingContext;
import net.sistr.actionarms.client.render.gltf.util.GltfMemoryPool;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * 中間オブジェクトを作らない直接描画を行うプロセッサ
 * メモリプールを活用して効率的な描画を実現
 */
public class DirectProcessor {

    /**
     * メッシュを直接描画（中間オブジェクトなし）
     * アニメーション計算→ボーン行列計算→頂点計算→描画を一気に実行
     */
    public void renderMeshDirect(ProcessedMesh mesh, RenderingContext context,
                                 ProcessedGltfModel model, MatrixStack matrixStack,
                                 VertexConsumer vertexConsumer) {

        if (!mesh.hasPositions()) {
            return;
        }

        // 作業用配列をメモリプールから借用
        ProcessedSkin skin = mesh.getSkin();
        int boneCount = skin != null ? skin.getBones().size() : 0;

        Matrix4f[] boneMatrices = null;
        float[] morphWeights = null;

        if (boneCount > 0) {
            boneMatrices = GltfMemoryPool.borrowMatrixArray(boneCount);
        }

        try {
            // ボーン行列を直接計算（中間オブジェクトなし）
            if (boneMatrices != null && context.animations().length > 0) {
                computeBoneMatricesDirect(context, skin, model, boneMatrices);
            }

            // TODO: モーフターゲット重み計算（現在は空配列）
            if (mesh.hasMorphTargets()) {
                morphWeights = new float[mesh.getMorphTargetCount()];
            }

            // 頂点を計算→即座に描画（頂点データをメモリに残さない）
            renderVerticesDirect(mesh, boneMatrices, morphWeights,
                    matrixStack, vertexConsumer, context);

        } finally {
            // メモリプールに返却
            if (boneMatrices != null) {
                GltfMemoryPool.returnMatrixArray(boneMatrices);
            }
        }
    }

    /**
     * ボーン行列を直接計算（ComputedTRSData、ComputedBoneMatricesDataを作らない）
     */
    private void computeBoneMatricesDirect(RenderingContext context, ProcessedSkin skin,
                                           ProcessedGltfModel model, Matrix4f[] boneMatrices) {

        var bones = skin.getBones();
        int stride = 3 + 4 + 3; // translation + rotation + scale

        // アニメーションデータを一時配列に計算（メモリプール使用）
        float[] animationData = GltfMemoryPool.borrowFloatArray(bones.size() * stride);
        Matrix4f[] localMatrices = GltfMemoryPool.borrowMatrixArray(bones.size());

        try {
            // アニメーションデータ計算（ComputedTRSDataを作らない）
            computeAnimationDataDirect(context, skin, model, animationData);

            // ローカル変換行列を計算
            for (int i = 0; i < bones.size(); i++) {
                int offset = i * stride;

                // Translation
                float tx = animationData[offset];
                float ty = animationData[offset + 1];
                float tz = animationData[offset + 2];

                // Rotation (quaternion)
                float rx = animationData[offset + 3];
                float ry = animationData[offset + 4];
                float rz = animationData[offset + 5];
                float rw = animationData[offset + 6];

                // Scale
                float sx = animationData[offset + 7];
                float sy = animationData[offset + 8];
                float sz = animationData[offset + 9];

                // ローカル変換行列を計算
                localMatrices[i].identity()
                        .translate(tx, ty, tz)
                        .rotate(new Quaternionf(rx, ry, rz, rw))
                        .scale(sx, sy, sz);
            }

            // ワールド変換行列を計算（階層構造を考慮）
            computeWorldMatrices(skin, localMatrices, boneMatrices);

        } finally {
            GltfMemoryPool.returnFloatArray(animationData);
            GltfMemoryPool.returnMatrixArray(localMatrices);
        }
    }

    /**
     * アニメーションデータを直接計算（ComputedTRSDataを作らない）
     */
    private void computeAnimationDataDirect(RenderingContext context, ProcessedSkin skin,
                                            ProcessedGltfModel model, float[] animationData) {
        var bones = skin.getBones();
        int stride = 3 + 4 + 3;

        // ボーンごとのデフォルト値を設定
        for (int i = 0; i < bones.size(); i++) {
            var bone = bones.get(i);
            int offset = i * stride;

            {
                // デフォルトのTRS値を設定
                Vector3f translation = bone.getTranslation();
                Quaternionf rotation = bone.getRotation();
                Vector3f scale = bone.getScale();

                animationData[offset] = translation.x;
                animationData[offset + 1] = translation.y;
                animationData[offset + 2] = translation.z;
                animationData[offset + 3] = rotation.x;
                animationData[offset + 4] = rotation.y;
                animationData[offset + 5] = rotation.z;
                animationData[offset + 6] = rotation.w;
                animationData[offset + 7] = scale.x;
                animationData[offset + 8] = scale.y;
                animationData[offset + 9] = scale.z;
            }

            // アニメーション状態を収集
            List<Vector3f> translations = new ArrayList<>();
            List<Float> translationsWeight = new ArrayList<>();
            List<Quaternionf> rotations = new ArrayList<>();
            List<Float> rotationsWeight = new ArrayList<>();
            List<Vector3f> scales = new ArrayList<>();
            List<Float> scalesWeight = new ArrayList<>();

            // 各アニメーション状態を処理
            for (RenderingContext.AnimationState animationState : context.animations()) {
                model.getAnimation(animationState.name())
                        .ifPresent(animation -> {
                            float time = animation.normalizeTime(animationState.seconds() * animationState.speed());
                            var translation = animation.getValueAt(bone.name(), "translation", time);
                            var rotation = animation.getValueAt(bone.name(), "rotation", time);
                            var scale = animation.getValueAt(bone.name(), "scale", time);
                            if (translation instanceof Vector3f v) {
                                translations.add(v);
                                translationsWeight.add(animationState.weight());
                            }
                            if (rotation instanceof Quaternionf q) {
                                rotations.add(q);
                                rotationsWeight.add(animationState.weight());
                            }
                            if (scale instanceof Vector3f v) {
                                scales.add(v);
                                scalesWeight.add(animationState.weight());
                            }
                        });
            }

            // 重み付き平均を計算して最終値を設定
            if (!translations.isEmpty()) {
                var result = weightedAverageV(translations, translationsWeight);
                animationData[offset] = result.x;
                animationData[offset + 1] = result.y;
                animationData[offset + 2] = result.z;
            }

            if (!rotations.isEmpty()) {
                var result = weightedAverageQ(rotations, rotationsWeight);
                animationData[offset + 3] = result.x;
                animationData[offset + 4] = result.y;
                animationData[offset + 5] = result.z;
                animationData[offset + 6] = result.w;
            }

            if (!scales.isEmpty()) {
                var result = weightedAverageV(scales, scalesWeight);
                animationData[offset + 7] = result.x;
                animationData[offset + 8] = result.y;
                animationData[offset + 9] = result.z;
            }
        }
    }

    /**
     * ワールド変換行列を計算（階層構造を考慮）
     */
    private void computeWorldMatrices(ProcessedSkin skin, Matrix4f[] localMatrices, Matrix4f[] worldMatrices) {
        // ルートボーンから開始
        for (ProcessedBone rootBone : skin.getRootBones()) {
            // ルートはワールド変換行列 = ローカル変換行列
            worldMatrices[rootBone.index()].set(localMatrices[rootBone.index()]);

            var stack = new ArrayDeque<ProcessedBone>();
            for (ProcessedBone child : rootBone.getChildren()) {
                stack.push(child);
            }

            while (!stack.isEmpty()) {
                var current = stack.pop();
                var parent = current.parent();
                var currentLocal = localMatrices[current.index()];
                var parentWorld = worldMatrices[parent.index()];

                // ワールド行列 = 親のワールド行列 * ローカル行列 * 逆バインド行列
                worldMatrices[current.index()].set(parentWorld)
                        .mul(currentLocal)
                        .mul(current.getInverseBindMatrix());

                for (ProcessedBone child : current.getChildren()) {
                    stack.push(child);
                }
            }
        }
    }

    /**
     * 頂点を直接描画（ComputedVertexDataを作らない）
     */
    private void renderVerticesDirect(ProcessedMesh mesh, Matrix4f[] boneMatrices,
                                      float[] morphWeights, MatrixStack matrixStack,
                                      VertexConsumer vertexConsumer, RenderingContext context) {

        int[] indices = mesh.getIndices();
        if (indices != null) {
            // インデックスバッファを使用
            for (int i = 0; i < indices.length; i += 3) {
                for (int j = 0; j < 3; j++) {
                    int vertexIndex = indices[i + j];
                    renderVertexDirect(mesh, vertexIndex, boneMatrices, morphWeights,
                            matrixStack, vertexConsumer, context);
                }
            }
        } else {
            // 直接頂点を描画
            int vertexCount = mesh.getVertexCount();
            for (int i = 0; i < vertexCount; i += 3) {
                for (int j = 0; j < 3; j++) {
                    if (i + j < vertexCount) {
                        renderVertexDirect(mesh, i + j, boneMatrices, morphWeights,
                                matrixStack, vertexConsumer, context);
                    }
                }
            }
        }
    }

    /**
     * 単一頂点を直接描画（頂点データをメモリに保存しない）
     */
    private void renderVertexDirect(ProcessedMesh mesh, int vertexIndex, Matrix4f[] boneMatrices,
                                    float[] morphWeights, MatrixStack matrixStack,
                                    VertexConsumer vertexConsumer, RenderingContext context) {

        // 元データから直接読み取り（コピーなし）
        float[] basePositions = mesh.getPositions().getFloatDataReadOnly();
        float[] baseNormals = mesh.getNormals() != null ? mesh.getNormals().getFloatDataReadOnly() : null;
        float[] uvs = mesh.getUvsRaw();

        int posIdx = vertexIndex * 3;
        int uvIdx = vertexIndex * 2;

        // 範囲チェック
        if (posIdx + 2 >= basePositions.length || uvIdx + 1 >= uvs.length) {
            return;
        }

        // ベース座標を取得
        float x = basePositions[posIdx];
        float y = basePositions[posIdx + 1];
        float z = basePositions[posIdx + 2];

        float nx = 0.0f, ny = 1.0f, nz = 0.0f;
        if (baseNormals != null && posIdx + 2 < baseNormals.length) {
            nx = baseNormals[posIdx];
            ny = baseNormals[posIdx + 1];
            nz = baseNormals[posIdx + 2];
        }

        // モーフィング適用（インプレース）
        if (morphWeights != null && morphWeights.length > 0 && mesh.hasMorphTargets()) {
            Vector3f morphResult = applyMorphingToVertex(mesh, vertexIndex, morphWeights, x, y, z);
            x = morphResult.x;
            y = morphResult.y;
            z = morphResult.z;
        }

        // スキニング適用（インプレース）
        if (mesh.hasSkinning() && boneMatrices != null) {
            Vector3f skinResult = applySkinningToVertex(mesh, vertexIndex, boneMatrices, x, y, z);
            Vector3f normalResult = applySkinningToNormal(mesh, vertexIndex, boneMatrices, nx, ny, nz);
            x = skinResult.x;
            y = skinResult.y;
            z = skinResult.z;
            nx = normalResult.x;
            ny = normalResult.y;
            nz = normalResult.z;
        }

        // 法線の正規化
        float normalLength = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (normalLength > 0.001f) {
            nx /= normalLength;
            ny /= normalLength;
            nz /= normalLength;
        }

        // 即座に描画（メモリに保存しない）
        vertexConsumer.vertex(matrixStack.peek().getPositionMatrix(), x, y, z)
                .color(255, 255, 255, 255)
                .texture(uvs[uvIdx], uvs[uvIdx + 1])
                .overlay(context.overlay())
                .light(context.light())
                .normal(matrixStack.peek().getNormalMatrix(), nx, ny, nz)
                .next();
    }

    /**
     * 単一頂点へのモーフィング適用
     */
    private Vector3f applyMorphingToVertex(ProcessedMesh mesh, int vertexIndex, float[] morphWeights,
                                           float x, float y, float z) {
        Vector3f result = new Vector3f(x, y, z);
        List<MorphTarget> morphTargets = mesh.getMorphTargets();

        for (int targetIndex = 0; targetIndex < morphTargets.size() && targetIndex < morphWeights.length; targetIndex++) {
            float weight = morphWeights[targetIndex];
            if (Math.abs(weight) < 0.001f) continue;

            MorphTarget target = morphTargets.get(targetIndex);
            if (target.hasPositionDeltas()) {
                float[] deltas = target.getPositionDeltasRaw();
                int deltaIdx = vertexIndex * 3;
                if (deltaIdx + 2 < deltas.length) {
                    result.x += deltas[deltaIdx] * weight;
                    result.y += deltas[deltaIdx + 1] * weight;
                    result.z += deltas[deltaIdx + 2] * weight;
                }
            }
        }

        return result;
    }

    /**
     * 単一頂点へのスキニング適用（汎用版）
     * @param isPosition trueの場合は位置（w=1.0）、falseの場合は法線（w=0.0）として処理
     */
    private Vector3f applySkinning(ProcessedMesh mesh, int vertexIndex, Matrix4f[] boneMatrices,
                                   float x, float y, float z, boolean isPosition) {
        AccessorData boneIndicesData = mesh.getBoneIndices();
        AccessorData boneWeightsData = mesh.getBoneWeights();

        if (boneIndicesData == null || boneWeightsData == null) {
            return new Vector3f(x, y, z);
        }

        int[] boneIndices = boneIndicesData.getIntDataReadOnly();
        float[] boneWeights = boneWeightsData.getFloatDataReadOnly();
        int weightIndex = vertexIndex * 4;

        if (weightIndex + 3 >= boneIndices.length || weightIndex + 3 >= boneWeights.length) {
            return new Vector3f(x, y, z);
        }

        Vector4f tempVec = new Vector4f();
        Vector4f resultVec = new Vector4f();
        resultVec.set(0, 0, 0, 0);

        float w = isPosition ? 1.0f : 0.0f;

        for (int boneSlot = 0; boneSlot < 4; boneSlot++) {
            int boneIndex = boneIndices[weightIndex + boneSlot];
            float weight = boneWeights[weightIndex + boneSlot];

            if (weight <= 0.001f || boneIndex < 0 || boneIndex >= boneMatrices.length) {
                continue;
            }

            Matrix4f boneMatrix = boneMatrices[boneIndex];
            tempVec.set(x, y, z, w);
            boneMatrix.transform(tempVec);
            resultVec.add(tempVec.x * weight, tempVec.y * weight, tempVec.z * weight, 0);
        }

        return new Vector3f(resultVec.x, resultVec.y, resultVec.z);
    }

    /**
     * 単一頂点へのスキニング適用（位置）
     */
    private Vector3f applySkinningToVertex(ProcessedMesh mesh, int vertexIndex, Matrix4f[] boneMatrices,
                                           float x, float y, float z) {
        return applySkinning(mesh, vertexIndex, boneMatrices, x, y, z, true);
    }

    /**
     * 単一頂点へのスキニング適用（法線）
     */
    private Vector3f applySkinningToNormal(ProcessedMesh mesh, int vertexIndex, Matrix4f[] boneMatrices,
                                           float nx, float ny, float nz) {
        return applySkinning(mesh, vertexIndex, boneMatrices, nx, ny, nz, false);
    }

    public static Quaternionf weightedAverageQ(List<Quaternionf> quaternions,
                                               List<Float> weights) {
        if (quaternions.isEmpty()) {
            throw new IllegalArgumentException("空のリストです");
        }

        if (quaternions.size() != weights.size()) {
            throw new IllegalArgumentException("クォータニオンと重みの数が一致しません");
        }

        // 基準となる最初のクォータニオン
        Quaternionf reference = new Quaternionf(quaternions.get(0));

        float w = 0, x = 0, y = 0, z = 0;
        float totalWeight = 0;

        for (int i = 0; i < quaternions.size(); i++) {
            Quaternionf q = quaternions.get(i);
            float weight = weights.get(i);

            // 内積を計算（JOMLのdot()メソッドを使用）
            float dot = reference.dot(q);

            // 内積が負の場合、クォータニオンを反転
            if (dot < 0) {
                w -= q.w * weight;
                x -= q.x * weight;
                y -= q.y * weight;
                z -= q.z * weight;
            } else {
                w += q.w * weight;
                x += q.x * weight;
                y += q.y * weight;
                z += q.z * weight;
            }

            totalWeight += weight;
        }

        // 重みで正規化
        w /= totalWeight;
        x /= totalWeight;
        y /= totalWeight;
        z /= totalWeight;

        // 結果のクォータニオンを作成して正規化
        Quaternionf result = new Quaternionf(x, y, z, w);
        return result.normalize();
    }

    /**
     * Vector3fの重み付き平均を計算
     *
     * @param vectors ベクトルのリスト
     * @param weights 各ベクトルに対応する重みのリスト
     * @return 重み付き平均ベクトル
     */
    public static Vector3f weightedAverageV(List<Vector3f> vectors,
                                            List<Float> weights) {
        if (vectors.isEmpty()) {
            throw new IllegalArgumentException("空のリストです");
        }

        if (vectors.size() != weights.size()) {
            throw new IllegalArgumentException("ベクトルと重みの数が一致しません");
        }

        float x = 0, y = 0, z = 0;
        float totalWeight = 0;

        for (int i = 0; i < vectors.size(); i++) {
            Vector3f v = vectors.get(i);
            float weight = weights.get(i);

            x += v.x * weight;
            y += v.y * weight;
            z += v.z * weight;

            totalWeight += weight;
        }

        // 重みで正規化
        if (totalWeight != 0) {
            x /= totalWeight;
            y /= totalWeight;
            z /= totalWeight;
        }

        return new Vector3f(x, y, z);
    }
}