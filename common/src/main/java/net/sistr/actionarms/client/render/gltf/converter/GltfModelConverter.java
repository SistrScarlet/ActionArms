package net.sistr.actionarms.client.render.gltf.converter;

import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.NodeModel;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.client.render.gltf.data.ProcessedAnimation;
import net.sistr.actionarms.client.render.gltf.data.ProcessedGltfModel;
import net.sistr.actionarms.client.render.gltf.data.ProcessedMesh;
import net.sistr.actionarms.client.render.gltf.data.ProcessedSkin;

import java.util.ArrayList;
import java.util.List;

public class GltfModelConverter {
    private final GltfVertexExtractor vertexExtractor;
    private final GltfSkinExtractor skinExtractor;
    private final GltfAnimationExtractor animationExtractor;

    public GltfModelConverter() {
        this.vertexExtractor = new GltfVertexExtractor();
        this.skinExtractor = new GltfSkinExtractor();
        this.animationExtractor = new GltfAnimationExtractor();
    }

    public List<ProcessedGltfModel> convertModel(String name, GltfModel gltfModel) {
        List<ProcessedGltfModel> models = new ArrayList<>();
        for (int i = 0; i < gltfModel.getSceneModels().size(); i++) {
            models.add(convertModel(name + "#" + i, gltfModel, i));
        }
        return models;
    }

    public ProcessedGltfModel convertModel(String name, GltfModel gltfModel, int sceneIndex) {
        List<ProcessedSkin> processedSkins = new ArrayList<>();
        List<ProcessedMesh> processedMeshes = new ArrayList<>();
        List<ProcessedAnimation> processedAnimations = new ArrayList<>();

        // アニメーションの抽出
        try {
            processedAnimations = animationExtractor.extractAnimations(gltfModel);
            ActionArms.LOGGER.info("Extracted {} animations from model {}", processedAnimations.size(), name);
        } catch (Exception e) {
            ActionArms.LOGGER.error("Failed to extract animations: {}", e.getMessage(), e);
        }

        var sceneModel = gltfModel.getSceneModels().get(sceneIndex);
        for (NodeModel rootNode : sceneModel.getNodeModels()) {
            var skinModel = rootNode.getSkinModel();
            if (skinModel == null) continue;
            try {
                ProcessedSkin processedSkin = skinExtractor.extractSkin(skinModel);
                processedSkins.add(processedSkin);

                for (MeshModel meshModel : rootNode.getMeshModels()) {
                    var meshes = vertexExtractor.extractMeshes(meshModel, processedSkin);
                    processedMeshes.addAll(meshes);
                }
            } catch (Exception e) {
                ActionArms.LOGGER.error("Failed to process model: {}", e.getMessage(), e);
            }
        }

        return new ProcessedGltfModel(name, processedMeshes, processedSkins, processedAnimations, gltfModel);
    }
}
