package net.sistr.actionarms.client.render.gltf.converter;

import de.javagl.jgltf.model.AccessorFloatData;
import de.javagl.jgltf.model.AccessorModel;
import de.javagl.jgltf.model.NodeModel;
import de.javagl.jgltf.model.SkinModel;
import net.sistr.actionarms.client.render.gltf.data.ProcessedBone;
import net.sistr.actionarms.client.render.gltf.data.ProcessedSkin;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GltfSkinExtractor {

    public ProcessedSkin extractSkin(SkinModel skin) {
        var builder = ProcessedSkin.builder();

        List<NodeModel> jointNodes = skin.getJoints();
        if (jointNodes == null || jointNodes.isEmpty()) {
            throw new RuntimeException("Skin has no joints");
        }

        String skinName = getSkinName(skin);
        builder.name(skinName);

        // ボーンの作成
        var boneBuilders = createProcessedBones(jointNodes);

        // 逆バインド行列の設定
        setInverseBindMatrices(boneBuilders, skin);

        // 階層構造の構築
        var bones = buildBoneHierarchy(boneBuilders, jointNodes);
        builder.addBones(List.of(bones));

        return builder.build();
    }

    private String getSkinName(SkinModel skin) {
        // SkinModelから名前を取得（実装によっては取得方法が異なる可能性があります）
        if (skin.getName() != null && !skin.getName().trim().isEmpty()) {
            return skin.getName();
        }
        // 名前が取得できない場合はデフォルト名を使用
        return "Skin_" + System.identityHashCode(skin);
    }

    private ProcessedBone.Builder[] createProcessedBones(List<NodeModel> jointNodes) {
        var builders = new ProcessedBone.Builder[jointNodes.size()];

        for (int i = 0; i < jointNodes.size(); i++) {
            NodeModel jointNode = jointNodes.get(i);
            String boneName = getNodeName(jointNode, i);

            ProcessedBone.Builder builder = new ProcessedBone.Builder(i, boneName);

            // 基本変換情報の設定
            setTransformData(builder, jointNode);

            builders[i] = builder;
        }

        return builders;
    }

    private String getNodeName(NodeModel node, int fallbackIndex) {
        // NodeModelから名前を取得
        String name = node.getName();
        return name != null && !name.trim().isEmpty() ?
                name.trim() : "Joint_" + fallbackIndex;
    }

    private void setTransformData(ProcessedBone.Builder builder, NodeModel jointNode) {
        // 基本変換の設定
        float[] translation = jointNode.getTranslation();
        if (translation != null) {
            builder.translation(translation[0], translation[1], translation[2]);
        }

        float[] rotation = jointNode.getRotation();
        if (rotation != null) {
            builder.rotation(rotation[0], rotation[1], rotation[2], rotation[3]);
        }

        float[] scale = jointNode.getScale();
        if (scale != null) {
            builder.scale(scale[0], scale[1], scale[2]);
        }

        // 直接行列が指定されている場合の処理
        float[] matrix = jointNode.getMatrix();
        if (matrix != null) {
            // 行列からTRSを分解して設定
            decomposeTRSFromMatrix(builder, matrix);
        }
    }

    private void decomposeTRSFromMatrix(ProcessedBone.Builder builder, float[] matrixArray) {
        Matrix4f matrix = new Matrix4f().set(matrixArray);

        // 行列からTRSを分解
        Vector3f translation = new Vector3f();
        Quaternionf rotation = new Quaternionf();
        Vector3f scale = new Vector3f();

        matrix.getTranslation(translation);
        matrix.getUnnormalizedRotation(rotation);
        matrix.getScale(scale);

        builder.translation(translation.x, translation.y, translation.z)
                .rotation(rotation.x, rotation.y, rotation.z, rotation.w)
                .scale(scale.x, scale.y, scale.z);
    }

    private void setInverseBindMatrices(ProcessedBone.Builder[] builders, SkinModel skin) {
        AccessorModel accessor = skin.getInverseBindMatrices();

        if (accessor == null) {
            // 逆バインド行列が指定されていない場合は単位行列を使用
            for (ProcessedBone.Builder builder : builders) {
                builder.inverseBindMatrix(new Matrix4f().identity());
            }
            return;
        }

        // アクセサから逆バインド行列データを取得
        AccessorFloatData accessorData = (AccessorFloatData) accessor.getAccessorData();
        int numMatrices = accessor.getCount();

        if (numMatrices != builders.length) {
            throw new RuntimeException(
                    String.format("Inverse bind matrix count (%d) doesn't match joint count (%d)",
                            numMatrices, builders.length)
            );
        }

        for (int i = 0; i < numMatrices; i++) {
            float[] matrixData = new float[16];
            for (int j = 0; j < 16; j++) {
                matrixData[j] = accessorData.get(i, j);
            }

            Matrix4f inverseBindMatrix = new Matrix4f().set(matrixData);
            builders[i].inverseBindMatrix(inverseBindMatrix);
        }
    }

    private ProcessedBone[] buildBoneHierarchy(ProcessedBone.Builder[] builders, List<NodeModel> jointNodes) {
        // NodeModelのインデックスマッピングを作成
        Map<NodeModel, Integer> nodeToIndex = new HashMap<>();
        for (int i = 0; i < jointNodes.size(); i++) {
            nodeToIndex.put(jointNodes.get(i), i);
        }

        // 各ボーンの親子関係を設定
        for (int i = 0; i < jointNodes.size(); i++) {
            NodeModel jointNode = jointNodes.get(i);

            // 子ノードを探して追加
            List<NodeModel> children = jointNode.getChildren();
            if (children != null) {
                for (NodeModel childNode : children) {
                    Integer childIndex = nodeToIndex.get(childNode);
                    if (childIndex != null) {
                        builders[i].addChild(builders[childIndex]);
                    }
                }
            }
        }

        ProcessedBone[] bones = new ProcessedBone[builders.length];

        // ルートボーンを見つけてビルド
        for (int i = 0; i < jointNodes.size(); i++) {
            NodeModel jointNode = jointNodes.get(i);

            // 親ノードを探す
            var parentNode = findParentNode(jointNode, jointNodes);
            if (parentNode == null) {
                // ルートボーンの場合
                builders[i].build(null, bones);
            }
        }

        return bones;
    }

    @Nullable
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
}