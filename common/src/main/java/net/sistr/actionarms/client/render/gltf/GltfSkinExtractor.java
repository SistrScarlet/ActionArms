package net.sistr.actionarms.client.render.gltf;

import de.javagl.jgltf.model.*;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GltfSkinExtractor {

    public ProcessedSkin extractSkin(SkinModel skin) {
        List<NodeModel> jointNodes = skin.getJoints();
        if (jointNodes == null || jointNodes.isEmpty()) {
            throw new RuntimeException("Skin has no joints");
        }

        String skinName = getSkinName(skin);

        // ボーンの作成
        List<ProcessedBone> bones = createProcessedBones(jointNodes);

        // 逆バインド行列の設定
        setInverseBindMatrices(bones, skin);

        // 階層構造の構築
        buildBoneHierarchy(bones, jointNodes);

        // ProcessedSkinの作成
        ProcessedSkin processedSkin = new ProcessedSkin(skinName, bones);

        // 階層の検証
        if (!processedSkin.validateHierarchy()) {
            System.err.println("Warning: Invalid bone hierarchy detected in skin: " + skinName);
        }

        return processedSkin;
    }

    private String getSkinName(SkinModel skin) {
        // SkinModelから名前を取得（実装によっては取得方法が異なる可能性があります）
        if (skin.getName() != null && !skin.getName().trim().isEmpty()) {
            return skin.getName();
        }
        // 名前が取得できない場合はデフォルト名を使用
        return "Skin_" + System.identityHashCode(skin);
    }

    private List<ProcessedBone> createProcessedBones(List<NodeModel> jointNodes) {
        List<ProcessedBone> bones = new ArrayList<>();

        for (int i = 0; i < jointNodes.size(); i++) {
            NodeModel jointNode = jointNodes.get(i);
            String boneName = getNodeName(jointNode, i);

            ProcessedBone bone = new ProcessedBone(i, boneName);

            // 基本変換情報の設定
            setTransformData(bone, jointNode);

            bones.add(bone);
        }

        return bones;
    }

    private String getNodeName(NodeModel node, int fallbackIndex) {
        // NodeModelから名前を取得
        String name = node.getName();
        return name != null && !name.trim().isEmpty() ?
                name.trim() : "Joint_" + fallbackIndex;
    }

    private void setTransformData(ProcessedBone bone, NodeModel jointNode) {
        // 基本変換の設定
        float[] translation = jointNode.getTranslation();
        if (translation != null) {
            bone.setTranslation(translation[0], translation[1], translation[2]);
            bone.setAnimatedTranslation(translation[0], translation[1], translation[2]);
        }

        float[] rotation = jointNode.getRotation();
        if (rotation != null) {
            bone.setRotation(rotation[0], rotation[1], rotation[2], rotation[3]);
            bone.setAnimatedRotation(rotation[0], rotation[1], rotation[2], rotation[3]);
        }

        float[] scale = jointNode.getScale();
        if (scale != null) {
            bone.setScale(scale[0], scale[1], scale[2]);
            bone.setAnimatedScale(scale[0], scale[1], scale[2]);
        }

        // 直接行列が指定されている場合の処理
        float[] matrix = jointNode.getMatrix();
        if (matrix != null) {
            // 行列からTRSを分解して設定
            decomposeTRSFromMatrix(bone, matrix);
        }
    }

    private void decomposeTRSFromMatrix(ProcessedBone bone, float[] matrixArray) {
        Matrix4f matrix = new Matrix4f().set(matrixArray);

        // 行列からTRSを分解
        Vector3f translation = new Vector3f();
        Quaternionf rotation = new Quaternionf();
        Vector3f scale = new Vector3f();

        matrix.getTranslation(translation);
        matrix.getUnnormalizedRotation(rotation);
        matrix.getScale(scale);

        bone.setTranslation(translation.x, translation.y, translation.z);
        bone.setRotation(rotation.x, rotation.y, rotation.z, rotation.w);
        bone.setScale(scale.x, scale.y, scale.z);

        // アニメーション用にも同じ値を設定
        bone.setAnimatedTranslation(translation.x, translation.y, translation.z);
        bone.setAnimatedRotation(rotation.x, rotation.y, rotation.z, rotation.w);
        bone.setAnimatedScale(scale.x, scale.y, scale.z);
    }

    private void setInverseBindMatrices(List<ProcessedBone> bones, SkinModel skin) {
        AccessorModel accessor = skin.getInverseBindMatrices();

        if (accessor == null) {
            // 逆バインド行列が指定されていない場合は単位行列を使用
            for (ProcessedBone bone : bones) {
                bone.setInverseBindMatrix(new Matrix4f().identity());
            }
            return;
        }

        // アクセサから逆バインド行列データを取得
        AccessorFloatData accessorData = (AccessorFloatData) accessor.getAccessorData();
        int numMatrices = accessor.getCount();

        if (numMatrices != bones.size()) {
            throw new RuntimeException(
                    String.format("Inverse bind matrix count (%d) doesn't match joint count (%d)",
                            numMatrices, bones.size())
            );
        }

        for (int i = 0; i < numMatrices; i++) {
            float[] matrixData = new float[16];
            for (int j = 0; j < 16; j++) {
                matrixData[j] = accessorData.get(i, j);
            }

            Matrix4f inverseBindMatrix = new Matrix4f().set(matrixData);
            bones.get(i).setInverseBindMatrix(inverseBindMatrix);
        }
    }

    private void buildBoneHierarchy(List<ProcessedBone> bones, List<NodeModel> jointNodes) {
        // NodeModelのインデックスマッピングを作成
        Map<NodeModel, Integer> nodeToIndex = new HashMap<>();
        for (int i = 0; i < jointNodes.size(); i++) {
            nodeToIndex.put(jointNodes.get(i), i);
        }

        // 各ボーンの親子関係を設定
        for (int i = 0; i < jointNodes.size(); i++) {
            NodeModel jointNode = jointNodes.get(i);
            ProcessedBone bone = bones.get(i);

            // 親ノードを探す
            NodeModel parentNode = findParentNode(jointNode, jointNodes);
            if (parentNode != null) {
                Integer parentIndex = nodeToIndex.get(parentNode);
                if (parentIndex != null) {
                    ProcessedBone parentBone = bones.get(parentIndex);
                    bone.setParent(parentBone);
                }
            }
        }
    }

    private NodeModel findParentNode(NodeModel childNode, List<NodeModel> allJointNodes) {
        // すべてのジョイントノードを調べて、このノードを子に持つものを探す
        for (NodeModel candidate : allJointNodes) {
            List<NodeModel> children = candidate.getChildren();
            if (children != null && children.contains(childNode)) {
                return candidate;
            }
        }
        return null;
    }

    public Matrix4f[] computeBoneMatricesFromProcessedSkin(ProcessedSkin processedSkin, boolean useAnimation) {
        return processedSkin.computeAllBoneMatrices(useAnimation);
    }
}