package net.sistr.actionarms.client.render.gltf.converter;

import de.javagl.jgltf.model.GltfModel;
import de.javagl.jgltf.model.MaterialModel;
import de.javagl.jgltf.model.MeshModel;
import de.javagl.jgltf.model.NodeModel;
import net.sistr.actionarms.ActionArms;
import net.sistr.actionarms.client.render.gltf.data.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GltfModelConverter {
    private final GltfVertexExtractor vertexExtractor;
    private final GltfSkinExtractor skinExtractor;
    private final GltfAnimationExtractor animationExtractor;
    private final GltfMaterialExtractor materialExtractor;

    public GltfModelConverter() {
        this.vertexExtractor = new GltfVertexExtractor();
        this.skinExtractor = new GltfSkinExtractor();
        this.animationExtractor = new GltfAnimationExtractor();
        this.materialExtractor = new GltfMaterialExtractor();
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
        Map<MaterialModel, ProcessedMaterial> processedMaterials = new HashMap<>();

        // マテリアルの抽出
        try {
            processedMaterials = materialExtractor.extractMaterials(gltfModel);
            ActionArms.LOGGER.info("Extracted {} materials from model {}", processedMaterials.size(), name);
        } catch (RuntimeException e) {
            ActionArms.LOGGER.error("Failed to extract materials: {}", e.getMessage(), e);
        }

        // アニメーションの抽出
        try {
            processedAnimations = animationExtractor.extractAnimations(gltfModel);
            ActionArms.LOGGER.info("Extracted {} animations from model {}", processedAnimations.size(), name);
        } catch (RuntimeException e) {
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
                    var meshes = vertexExtractor.extractMeshes(processedMaterials, processedSkin, meshModel);
                    processedMeshes.addAll(meshes);
                }
                vertexExtractor.clearCache();
            } catch (RuntimeException e) {
                ActionArms.LOGGER.error("Failed to process model: {}", e.getMessage(), e);
            }
        }

        var builder = ProcessedGltfModel.builder();

        builder.name(name);
        builder.originalGltfModel(gltfModel);
        builder.addAnimations(processedAnimations);
        builder.addMaterials(new ArrayList<>(processedMaterials.values()));
        builder.addMeshes(processedMeshes);
        builder.addSkins(processedSkins);

        return builder.build();
    }
}
