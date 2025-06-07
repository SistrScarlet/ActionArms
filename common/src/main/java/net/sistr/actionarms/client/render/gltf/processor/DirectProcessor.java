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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 描画を行うプロセッサ
 * メモリプールを活用して効率的な描画を実現
 */
public class DirectProcessor {

    /**
     * メッシュを直接描画
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
            // ボーン行列を直接計算
            if (boneMatrices != null && context.animations().length > 0) {
                computeBoneMatricesDirect(context, skin, model, boneMatrices);
            }

            // TODO: モーフターゲット重み計算（現在は空配列）
            if (mesh.hasMorphTargets()) {
                morphWeights = new float[mesh.getMorphTargetCount()];
            }

            // 頂点を計算→即座に描画（頂点データをメモリに残さない）

            int[] indices = mesh.getIndices();
            for (int vertexIndex : indices) {
                renderVertexDirect(mesh, vertexIndex, boneMatrices, morphWeights,
                        matrixStack, vertexConsumer, context);
            }

        } finally {
            // メモリプールに返却
            if (boneMatrices != null) {
                GltfMemoryPool.returnMatrixArray(boneMatrices);
            }
        }
    }

    /**
     * ボーン行列を直接計算
     */
    private void computeBoneMatricesDirect(RenderingContext context, ProcessedSkin skin,
                                           ProcessedGltfModel model, Matrix4f[] boneMatrices) {

        var bones = skin.getBones();
        int stride = 3 + 4 + 3; // translation + rotation + scale

        // アニメーションデータを一時配列に計算
        float[] animationData = GltfMemoryPool.borrowFloatArray(bones.size() * stride);
        Matrix4f[] localMatrices = GltfMemoryPool.borrowMatrixArray(bones.size());

        try {
            // アニメーションデータ計算
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
     * アニメーションデータを直接計算
     */
    private void computeAnimationDataDirect(RenderingContext context, ProcessedSkin skin,
                                            ProcessedGltfModel model, float[] animationData) {
        var animations = Arrays.stream(context.animations())
                .map(state -> model.getAnimation(state.name()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
        var bones = skin.getBones();
        for (int i = 0; i < bones.size(); i++) {
            var bone = bones.get(i);

            int stride = 3 + 4 + 3;
            int offset = i * stride;

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

            for (int j = 0; j < context.animations().length; j++) {
                var animation = animations.get(j);
                if (!animation.hasBone(bone.name())) {
                    continue;
                }
                var state = context.animations()[j];
                float time = animation.normalizeTime(state.seconds());
                animationOverwrite(bone, animation, time, offset, animationData);
            }
        }
    }

    // 単に上書きするのみ
    private void animationOverwrite(ProcessedBone bone, ProcessedAnimation animation,
                                    float time, int offset, float[] animationData) {
        var boneName = bone.name();
        var channels = animation.getChannels(boneName);
        var translation = channels[0] != null ? channels[0].getValueAt(time) : null;
        var rotation = channels[1] != null ? channels[1].getValueAt(time) : null;
        var scale = channels[2] != null ? channels[2].getValueAt(time) : null;

        if (translation instanceof Vector3f v) {
            animationData[offset] = v.x;
            animationData[offset + 1] = v.y;
            animationData[offset + 2] = v.z;
        }
        if (rotation instanceof Quaternionf q) {
            animationData[offset + 3] = q.x;
            animationData[offset + 4] = q.y;
            animationData[offset + 5] = q.z;
            animationData[offset + 6] = q.w;
        }
        if (scale instanceof Vector3f v) {
            animationData[offset + 7] = v.x;
            animationData[offset + 8] = v.y;
            animationData[offset + 9] = v.z;
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

        // 即座に描画
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
     * 単一頂点へのスキニング適用
     *
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

    private Vector3f applySkinningToVertex(ProcessedMesh mesh, int vertexIndex, Matrix4f[] boneMatrices,
                                           float x, float y, float z) {
        return applySkinning(mesh, vertexIndex, boneMatrices, x, y, z, true);
    }

    private Vector3f applySkinningToNormal(ProcessedMesh mesh, int vertexIndex, Matrix4f[] boneMatrices,
                                           float nx, float ny, float nz) {
        return applySkinning(mesh, vertexIndex, boneMatrices, nx, ny, nz, false);
    }
}