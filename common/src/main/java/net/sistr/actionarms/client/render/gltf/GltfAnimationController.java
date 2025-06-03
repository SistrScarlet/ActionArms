package net.sistr.actionarms.client.render.gltf;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class GltfAnimationController {
    private final ProcessedGltfModel model;

    // アニメーション状態
    private ProcessedAnimation currentAnimation;
    private String currentAnimationName;
    private float animationSpeed = 1.0f;
    private boolean looping = true;
    private float localTime = 0.0f;

    public GltfAnimationController(ProcessedGltfModel model) {
        this.model = model;
    }

    public void update(RenderingContext context) {
        float deltaTime = (Float) context.getCustomProperty("delta");
        if (deltaTime > 0) {
            localTime += deltaTime * animationSpeed;
        }

        // アニメーション時間の正規化
        if (currentAnimation != null) {
            localTime = currentAnimation.normalizeTime(localTime);
        }

        context.setAnimationTime(localTime);
        updateBoneMatrices(context);
        updateMorphWeights(context);
    }

    private void updateBoneMatrices(RenderingContext context) {
        for (ProcessedSkin skin : model.getSkins()) {
            Matrix4f[] boneMatrices = computeAnimatedBoneMatrices(skin, localTime);
            context.setBoneMatrices(boneMatrices);
            break; // 最初のスキンのみ処理（複数スキン対応は後で実装）
        }
    }

    private Matrix4f[] computeAnimatedBoneMatrices(ProcessedSkin skin, float time) {
        // アニメーションが指定されている場合はアニメーション済み変換を使用
        if (currentAnimation != null) {
            updateBoneAnimations(skin, time);
        }

        return skin.computeAllBoneMatrices(currentAnimation != null);
    }

    private void updateBoneAnimations(ProcessedSkin skin, float time) {
        if (currentAnimation == null) return;

        for (ProcessedBone bone : skin.getBones()) {
            String boneName = bone.getName();

            // 各変換プロパティの値を取得
            Object translation = currentAnimation.getValueAt(boneName, "translation", time);
            Object rotation = currentAnimation.getValueAt(boneName, "rotation", time);
            Object scale = currentAnimation.getValueAt(boneName, "scale", time);

            // ボーンに適用
            if (translation instanceof Vector3f v) {
                bone.setAnimatedTranslation(v.x, v.y, v.z);
            }

            if (rotation instanceof Quaternionf q) {
                bone.setAnimatedRotation(q.x, q.y, q.z, q.w);
            }

            if (scale instanceof Vector3f v) {
                bone.setAnimatedScale(v.x, v.y, v.z);
            }
        }
    }

    private void updateMorphWeights(RenderingContext context) {
        int morphTargetCount = model.getMaxMorphTargetCount();
        float[] morphWeights = new float[morphTargetCount];

        if (currentAnimation != null) {
            // アニメーションからモーフウェイトを取得
            for (int i = 0; i < morphTargetCount; i++) {
                String weightKey = "weights"; // モーフウェイトのキー
                Object weightValue = currentAnimation.getValueAt("", weightKey, localTime);

                if (weightValue instanceof Float f) {
                    morphWeights[i] = f;
                } else if (weightValue instanceof float[] weights && i < weights.length) {
                    morphWeights[i] = weights[i];
                }
            }
        } else {
            // デフォルトではすべて0（ベースメッシュの状態）
            // テスト用の正弦波アニメーション（実際のアニメーションがない場合）
            for (int i = 0; i < morphTargetCount; i++) {
                morphWeights[i] = (float) Math.sin(localTime + i) * 0.5f + 0.5f;
            }
        }

        context.setMorphWeights(morphWeights);
    }

    // アニメーション制御用メソッド
    public void playAnimation(String animationName) {
        ProcessedAnimation animation = model.getAnimation(animationName);
        if (animation != null) {
            this.currentAnimation = animation;
            this.currentAnimationName = animationName;
            this.localTime = 0.0f;
        } else {
            // アニメーションが見つからない場合はログを出力
            System.err.println("Animation not found: " + animationName);
            listAvailableAnimations();
        }
    }

    public void playAnimation(int animationIndex) {
        ProcessedAnimation animation = model.getAnimation(animationIndex);
        if (animation != null) {
            this.currentAnimation = animation;
            this.currentAnimationName = animation.getName();
            this.localTime = 0.0f;
        } else {
            System.err.println("Animation index out of bounds: " + animationIndex);
            listAvailableAnimations();
        }
    }

    public void stopAnimation() {
        this.currentAnimation = null;
        this.currentAnimationName = null;
        this.localTime = 0.0f;
    }

    public void pauseAnimation() {
        // アニメーションを一時停止（時間を進めない）
        this.animationSpeed = 0.0f;
    }

    public void resumeAnimation() {
        this.animationSpeed = 1.0f;
    }

    public void setAnimationSpeed(float speed) {
        this.animationSpeed = speed;
    }

    public void setLooping(boolean looping) {
        this.looping = looping;
    }

    public void setAnimationTime(float time) {
        this.localTime = time;
    }

    // 状態取得メソッド
    public boolean isPlaying() {
        return currentAnimation != null && animationSpeed > 0;
    }

    public String getCurrentAnimationName() {
        return currentAnimationName;
    }

    public ProcessedAnimation getCurrentAnimation() {
        return currentAnimation;
    }

    public float getCurrentTime() {
        return localTime;
    }

    public float getAnimationProgress() {
        if (currentAnimation == null || currentAnimation.getDuration() <= 0) {
            return 0.0f;
        }
        return localTime / currentAnimation.getDuration();
    }

    public void listAvailableAnimations() {
        System.out.println("Available animations:");
        for (int i = 0; i < model.getAnimationCount(); i++) {
            ProcessedAnimation anim = model.getAnimation(i);
            System.out.println("  [" + i + "] " + anim.getName() + " (duration: " + anim.getDuration() + "s)");
        }
    }

    // デバッグ用メソッド
    public void printCurrentAnimationInfo() {
        if (currentAnimation != null) {
            System.out.println("Current Animation Info:");
            currentAnimation.printInfo();
            System.out.println("Current Time: " + localTime + "s");
            System.out.println("Progress: " + (getAnimationProgress() * 100) + "%");
        } else {
            System.out.println("No animation currently playing");
        }
    }
}
