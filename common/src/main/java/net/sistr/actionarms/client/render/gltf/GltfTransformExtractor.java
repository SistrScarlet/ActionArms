package net.sistr.actionarms.client.render.gltf;

import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.NodeModel;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

public class GltfTransformExtractor {

    // ノードのローカル変換行列を取得
    public Matrix4f getNodeLocalTransform(NodeModel node) {
        Matrix4f matrix = new Matrix4f();

        // jglTFのNodeModelから変換情報を取得
        float[] nodeMatrix = node.getMatrix();
        if (nodeMatrix != null) {
            // 4x4行列が直接指定されている場合
            matrix.set(nodeMatrix);
        } else {
            // TRS（Translation, Rotation, Scale）から構築
            float[] translation = node.getTranslation();
            float[] rotation = node.getRotation(); // クォータニオン
            float[] scale = node.getScale();

            matrix.identity();

            // スケール適用
            if (scale != null) {
                matrix.scale(scale[0], scale[1], scale[2]);
            }

            // 回転適用（クォータニオン）
            if (rotation != null) {
                Quaternionf quat = new Quaternionf(rotation[0], rotation[1], rotation[2], rotation[3]);
                matrix.rotate(quat);
            }

            // 平行移動適用
            if (translation != null) {
                matrix.translate(translation[0], translation[1], translation[2]);
            }
        }

        return matrix;
    }

    // ワールド変換行列を計算（親ノードまで遡って累積）
    public Matrix4f getWorldTransform(NodeModel node, GltfModel gltfModel) {
        Matrix4f worldMatrix = new Matrix4f().identity();

        // ルートから現在ノードまでのパスを取得
        List<NodeModel> nodePath = getNodePath(node, gltfModel);

        // ルートから順番に変換を累積
        for (NodeModel pathNode : nodePath) {
            Matrix4f localTransform = getNodeLocalTransform(pathNode);
            worldMatrix.mul(localTransform);
        }

        return worldMatrix;
    }

    private List<NodeModel> getNodePath(NodeModel targetNode, GltfModel gltfModel) {
        List<NodeModel> path = new ArrayList<>();

        // 親を辿ってルートまでのパスを構築
        NodeModel current = targetNode;
        while (current != null) {
            path.add(0, current); // 先頭に挿入
            current = findParent(current, gltfModel);
        }

        return path;
    }

    private NodeModel findParent(NodeModel child, GltfModel gltfModel) {
        for (NodeModel node : gltfModel.getNodeModels()) {
            if (node.getChildren().contains(child)) {
                return node;
            }
        }
        return null;
    }
}
