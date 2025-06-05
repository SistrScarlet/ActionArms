package net.sistr.actionarms.client.render.gltf.processor;

import net.sistr.actionarms.client.render.gltf.renderer.RenderingContext;
import net.sistr.actionarms.client.render.gltf.data.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class AnimationProcessor {

    public ComputedBoneMatricesData getBoneMatrices(RenderingContext context, ProcessedSkin skin, ProcessedGltfModel model) {
        var trs = computeAnimationData(context, skin, model);
        return computeBoneMatricesData(trs, skin);
    }

    public float[] getMorphWeights(RenderingContext context, ProcessedMesh mesh) {
        //todo モーフ実装
        return new float[mesh.getMorphTargetCount()];
    }

    //todo 再帰の方がスマート
    private ComputedBoneMatricesData computeBoneMatricesData(ComputedTRSData data, ProcessedSkin skin) {
        var localMatrices = new Matrix4f[data.getCount()];
        var worldMatrices = new Matrix4f[data.getCount()];

        // ローカル変換行列を計算する
        for (int i = 0; i < localMatrices.length; i++) {
            localMatrices[i] = new Matrix4f().identity()
                    .translate(data.getTranslation(i))
                    .rotate(data.getRotation(i))
                    .scale(data.getScale(i));
        }

        // ワールド変換行列を計算する
        for (ProcessedBone rootBone : skin.getRootBones()) {
            // ルートはワールド変換行列 = ローカル変換行列
            worldMatrices[rootBone.index()] = new Matrix4f(localMatrices[rootBone.index()]);

            var stack = new ArrayDeque<ProcessedBone>();
            for (ProcessedBone child : rootBone.getChildren()) {
                stack.push(child);
            }

            while (!stack.isEmpty()) {
                var current = stack.pop();
                var parent = current.parent();
                var currentLocal = localMatrices[current.index()];
                var parentWorld = worldMatrices[parent.index()];
                // これのワールド行列 = 親のワールド行列 * これのローカル行列 * これの逆バインド行列
                worldMatrices[current.index()] = new Matrix4f(parentWorld)
                        .mul(currentLocal)
                        .mul(current.getInverseBindMatrix());

                for (ProcessedBone child : current.getChildren()) {
                    stack.push(child);
                }
            }
        }

        return new ComputedBoneMatricesData(worldMatrices);
    }

    // todo 配列だと分かりづらい
    private ComputedTRSData computeAnimationData(RenderingContext context, ProcessedSkin skin, ProcessedGltfModel model) {
        var bones = skin.getBones();

        int stride = 3 + 4 + 3;
        var animationData = new float[bones.size() * stride];

        // ボーンごと、アニメーションごとの各状態を取得する
        for (int i = 0; i < bones.size(); i++) {
            var bone = bones.get(i);
            animationData[i * stride] = bone.getTranslation().x;
            animationData[i * stride + 1] = bone.getTranslation().y;
            animationData[i * stride + 2] = bone.getTranslation().z;
            animationData[i * stride + 3] = bone.getRotation().x;
            animationData[i * stride + 4] = bone.getRotation().y;
            animationData[i * stride + 5] = bone.getRotation().z;
            animationData[i * stride + 6] = bone.getRotation().w;
            animationData[i * stride + 7] = bone.getScale().x;
            animationData[i * stride + 8] = bone.getScale().y;
            animationData[i * stride + 9] = bone.getScale().z;

            List<Vector3f> translations = new ArrayList<>();
            List<Float> translationsWeight = new ArrayList<>();
            List<Quaternionf> rotations = new ArrayList<>();
            List<Float> rotationsWeight = new ArrayList<>();
            List<Vector3f> scales = new ArrayList<>();
            List<Float> scalesWeight = new ArrayList<>();


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

            // ボーンの最終結果をウェイトで合成し、ボーンにセットする
            if (!translations.isEmpty()) {
                var result = weightedAverageV(translations, translationsWeight);
                animationData[i * stride] = result.x;
                animationData[i * stride + 1] = result.y;
                animationData[i * stride + 2] = result.z;
            }

            if (!rotations.isEmpty()) {
                var result = weightedAverageQ(rotations, rotationsWeight);
                animationData[i * stride + 3] = result.x;
                animationData[i * stride + 4] = result.y;
                animationData[i * stride + 5] = result.z;
                animationData[i * stride + 6] = result.w;
            }

            if (!scales.isEmpty()) {
                var result = weightedAverageV(scales, scalesWeight);
                animationData[i * stride + 7] = result.x;
                animationData[i * stride + 8] = result.y;
                animationData[i * stride + 9] = result.z;
            }
        }

        return new ComputedTRSData(animationData);
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
