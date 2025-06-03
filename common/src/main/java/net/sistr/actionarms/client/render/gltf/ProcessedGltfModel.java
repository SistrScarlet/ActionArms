package net.sistr.actionarms.client.render.gltf;

import de.javagl.jgltf.model.GltfModel;
import java.util.ArrayList;
import java.util.List;

public class ProcessedGltfModel {
    private final String name;
    private final List<ProcessedMesh> meshes;
    private final List<ProcessedSkin> skins;
    private final List<ProcessedAnimation> animations;
    private final GltfModel originalGltfModel;
    private final int maxMorphTargetCount;
    
    public ProcessedGltfModel(String name, List<ProcessedMesh> meshes, List<ProcessedSkin> skins, 
                             List<ProcessedAnimation> animations, GltfModel originalGltfModel) {
        this.name = name != null ? name : "GltfModel";
        this.meshes = new ArrayList<>(meshes);
        this.skins = new ArrayList<>(skins);
        this.animations = new ArrayList<>(animations);
        this.originalGltfModel = originalGltfModel;
        this.maxMorphTargetCount = calculateMaxMorphTargetCount();
    }
    
    // 基本情報
    public String getName() { return name; }
    public GltfModel getOriginalGltfModel() { return originalGltfModel; }
    
    // メッシュ関連
    public List<ProcessedMesh> getMeshes() { return new ArrayList<>(meshes); }
    public void addMesh(ProcessedMesh mesh) { meshes.add(mesh); }
    public ProcessedMesh getMesh(int index) {
        return index >= 0 && index < meshes.size() ? meshes.get(index) : null;
    }
    public int getMeshCount() { return meshes.size(); }
    
    // スキン関連
    public List<ProcessedSkin> getSkins() { return new ArrayList<>(skins); }
    public void addSkin(ProcessedSkin skin) { skins.add(skin); }
    public ProcessedSkin getSkin(int index) {
        return index >= 0 && index < skins.size() ? skins.get(index) : null;
    }
    public int getSkinCount() { return skins.size(); }
    
    // アニメーション関連
    public List<ProcessedAnimation> getAnimations() { return new ArrayList<>(animations); }
    public void addAnimation(ProcessedAnimation animation) { animations.add(animation); }
    public ProcessedAnimation getAnimation(int index) {
        return index >= 0 && index < animations.size() ? animations.get(index) : null;
    }
    public ProcessedAnimation getAnimation(String name) {
        return animations.stream()
                .filter(anim -> name.equals(anim.getName()))
                .findFirst()
                .orElse(null);
    }
    public int getAnimationCount() { return animations.size(); }
    
    // モーフターゲット関連
    public int getMaxMorphTargetCount() { return maxMorphTargetCount; }
    
    private int calculateMaxMorphTargetCount() {
        int maxCount = 0;
        for (ProcessedMesh mesh : meshes) {
            maxCount = Math.max(maxCount, mesh.getMorphTargetCount());
        }
        return maxCount;
    }
    
    // 統計情報
    public int getTotalVertexCount() {
        return meshes.stream().mapToInt(ProcessedMesh::getVertexCount).sum();
    }
    
    public int getTotalTriangleCount() {
        return meshes.stream().mapToInt(ProcessedMesh::getTriangleCount).sum();
    }
    
    public int getTotalBoneCount() {
        return skins.stream().mapToInt(ProcessedSkin::getBoneCount).sum();
    }
    
    @Override
    public String toString() {
        return String.format("ProcessedGltfModel[%s: %d meshes, %d skins, %d animations, %d vertices, %d triangles]",
                name, getMeshCount(), getSkinCount(), getAnimationCount(), getTotalVertexCount(), getTotalTriangleCount());
    }
}
