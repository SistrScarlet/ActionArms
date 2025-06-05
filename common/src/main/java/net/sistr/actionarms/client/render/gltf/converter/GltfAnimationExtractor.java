package net.sistr.actionarms.client.render.gltf.converter;

import de.javagl.jgltf.model.*;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.client.render.gltf.data.ProcessedAnimation;
import net.sistr.actionarms.client.render.gltf.data.ProcessedChannel;
import net.sistr.actionarms.client.render.gltf.data.ProcessedKeyframe;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

import static de.javagl.jgltf.model.ElementType.*;

public class GltfAnimationExtractor {

    public List<ProcessedAnimation> extractAnimations(GltfModel gltfModel) {
        List<ProcessedAnimation> processedAnimations = new ArrayList<>();

        List<AnimationModel> animations = gltfModel.getAnimationModels();
        if (animations == null || animations.isEmpty()) {
            return processedAnimations;
        }

        for (int i = 0; i < animations.size(); i++) {
            AnimationModel animation = animations.get(i);

            try {
                ProcessedAnimation processedAnimation = extractSingleAnimation(animation, i);
                processedAnimations.add(processedAnimation);
            } catch (Exception e) {
                ActionArms.LOGGER.error("Failed to process animation {}: {}", i, e.getMessage(), e);
            }
        }

        return processedAnimations;
    }

    private ProcessedAnimation extractSingleAnimation(AnimationModel animation, int fallbackIndex) {
        String animationName = getAnimationName(animation, fallbackIndex);
        List<ProcessedChannel> channels = new ArrayList<>();

        List<AnimationModel.Channel> gltfChannels = animation.getChannels();
        if (gltfChannels != null) {
            for (AnimationModel.Channel gltfChannel : gltfChannels) {
                try {
                    ProcessedChannel processedChannel = extractChannel(gltfChannel);
                    if (processedChannel != null) {
                        channels.add(processedChannel);
                    }
                } catch (Exception e) {
                    ActionArms.LOGGER.error("Failed to process channel in animation {}: {}",
                            animationName, e.getMessage());
                }
            }
        }

        return new ProcessedAnimation(animationName, channels);
    }

    private String getAnimationName(AnimationModel animation, int fallbackIndex) {
        String name = animation.getName();
        return name != null && !name.trim().isEmpty() ?
                name.trim() : "Animation_" + fallbackIndex;
    }

    private ProcessedChannel extractChannel(AnimationModel.Channel gltfChannel) {
        // ターゲット情報の取得
        NodeModel targetNode = gltfChannel.getNodeModel();
        String targetPath = gltfChannel.getPath();
        String nodeName = getNodeName(targetNode);

        // サンプラーの取得
        AnimationModel.Sampler sampler = gltfChannel.getSampler();
        if (sampler == null) {
            ActionArms.LOGGER.warn("Channel has no sampler: {} -> {}", nodeName, targetPath);
            return null;
        }

        // 補間モードの取得
        ProcessedChannel.InterpolationMode interpolationMode = getInterpolationMode(sampler);

        // キーフレームの抽出
        List<ProcessedKeyframe> keyframes = extractKeyframes(sampler, targetPath);

        if (keyframes.isEmpty()) {
            ActionArms.LOGGER.warn("No keyframes extracted for channel: {} -> {}", nodeName, targetPath);
            return null;
        }

        return new ProcessedChannel(nodeName, targetPath, keyframes, interpolationMode);
    }

    private String getNodeName(NodeModel node) {
        if (node == null) return "UnknownNode";

        String name = node.getName();
        return name != null && !name.trim().isEmpty() ?
                name.trim() : "Node_" + System.identityHashCode(node);
    }

    private ProcessedChannel.InterpolationMode getInterpolationMode(AnimationModel.Sampler sampler) {
        var interpolation = sampler.getInterpolation();
        if (interpolation == null) {
            return ProcessedChannel.InterpolationMode.LINEAR; // デフォルト
        }

        switch (interpolation) {
            case STEP:
                return ProcessedChannel.InterpolationMode.STEP;
            case LINEAR:
                return ProcessedChannel.InterpolationMode.LINEAR;
            case CUBICSPLINE:
                return ProcessedChannel.InterpolationMode.CUBICSPLINE;
            default:
                ActionArms.LOGGER.warn("Unknown interpolation mode: {}. Using LINEAR.", interpolation);
                return ProcessedChannel.InterpolationMode.LINEAR;
        }
    }

    private List<ProcessedKeyframe> extractKeyframes(AnimationModel.Sampler sampler, String targetPath) {
        List<ProcessedKeyframe> keyframes = new ArrayList<>();

        AccessorModel inputAccessor = sampler.getInput();
        AccessorModel outputAccessor = sampler.getOutput();

        if (inputAccessor == null || outputAccessor == null) {
            return keyframes;
        }

        // 時間データの取得
        AccessorFloatData timeData = (AccessorFloatData) inputAccessor.getAccessorData();
        AccessorFloatData valueData = (AccessorFloatData) outputAccessor.getAccessorData();

        int keyframeCount = inputAccessor.getCount();
        var elementType = outputAccessor.getElementType();

        for (int i = 0; i < keyframeCount; i++) {
            float time = timeData.get(i, 0);
            Object value = extractValueFromAccessor(valueData, i, targetPath, elementType);

            if (value != null) {
                keyframes.add(new ProcessedKeyframe(time, value));
            }
        }

        return keyframes;
    }

    private Object extractValueFromAccessor(AccessorFloatData valueData, int index,
                                            String targetPath, ElementType elementType) {

        switch (targetPath) {
            case "translation":
            case "scale":
                // Vector3f
                if (VEC3.equals(elementType)) {
                    return new Vector3f(
                            valueData.get(index, 0),
                            valueData.get(index, 1),
                            valueData.get(index, 2)
                    );
                }
                break;

            case "rotation":
                // Quaternionf
                if (VEC4.equals(elementType)) {
                    return new Quaternionf(
                            valueData.get(index, 0),
                            valueData.get(index, 1),
                            valueData.get(index, 2),
                            valueData.get(index, 3)
                    );
                }
                break;

            case "weights":
                // float配列（モーフウェイト）
                if (SCALAR.equals(elementType)) {
                    return valueData.get(index, 0);
                } else {
                    // 複数のウェイト
                    int componentCount = elementType.getNumComponents();
                    float[] weights = new float[componentCount];
                    for (int i = 0; i < componentCount; i++) {
                        weights[i] = valueData.get(index, i);
                    }
                    return weights;
                }
        }

        ActionArms.LOGGER.warn("Unsupported animation target: {} with type {}", targetPath, elementType);
        return null;
    }

}
