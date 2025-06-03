package net.sistr.actionarms.client.render.gltf;

import org.joml.Matrix4f;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ProcessedSkin {
    private final String name;
    private final List<ProcessedBone> bones;
    private final Map<String, ProcessedBone> bonesByName;
    private final ProcessedBone rootBone;
    private final Matrix4f[] currentBoneMatrices;
    
    public ProcessedSkin(String name, List<ProcessedBone> bones) {
        this.name = name != null ? name : "DefaultSkin";
        this.bones = new ArrayList<>(bones);
        this.bonesByName = new HashMap<>();
        this.currentBoneMatrices = new Matrix4f[bones.size()];
        
        // ボーン名でのマッピングを作成
        for (ProcessedBone bone : bones) {
            bonesByName.put(bone.getName(), bone);
            currentBoneMatrices[bone.getIndex()] = new Matrix4f();
        }
        
        // ルートボーンを見つける
        this.rootBone = findRootBone();
    }
    
    public String getName() { return name; }
    public List<ProcessedBone> getBones() { return new ArrayList<>(bones); }
    public int getBoneCount() { return bones.size(); }
    
    // ボーンの取得
    public ProcessedBone getBone(int index) {
        return index >= 0 && index < bones.size() ? bones.get(index) : null;
    }
    
    public ProcessedBone getBone(String name) {
        return bonesByName.get(name);
    }
    
    public ProcessedBone getRootBone() { return rootBone; }
    
    private ProcessedBone findRootBone() {
        for (ProcessedBone bone : bones) {
            if (bone.getParent() == null) {
                return bone;
            }
        }
        return bones.isEmpty() ? null : bones.get(0);
    }
    
    // ボーン行列の計算と取得
    public Matrix4f[] computeAllBoneMatrices(boolean useAnimation) {
        for (ProcessedBone bone : bones) {
            Matrix4f boneMatrix = bone.computeBoneMatrix(useAnimation);
            currentBoneMatrices[bone.getIndex()].set(boneMatrix);
        }
        
        return getCurrentBoneMatrices();
    }
    
    public Matrix4f[] getCurrentBoneMatrices() {
        Matrix4f[] result = new Matrix4f[currentBoneMatrices.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = new Matrix4f(currentBoneMatrices[i]);
        }
        return result;
    }
    
    // ボーン階層の検証
    public boolean validateHierarchy() {
        for (ProcessedBone bone : bones) {
            // 循環参照のチェック
            if (hasCircularReference(bone)) {
                return false;
            }
        }
        return true;
    }
    
    private boolean hasCircularReference(ProcessedBone bone) {
        Set<ProcessedBone> visited = new HashSet<>();
        ProcessedBone current = bone;
        
        while (current != null) {
            if (visited.contains(current)) {
                return true; // 循環参照発見
            }
            visited.add(current);
            current = current.getParent();
        }
        
        return false;
    }
    
    // デバッグ用の階層表示
    public void printHierarchy() {
        if (rootBone != null) {
            printBoneHierarchy(rootBone, 0);
        }
    }
    
    private void printBoneHierarchy(ProcessedBone bone, int depth) {
        String indent = "  ".repeat(depth);
        System.out.println(indent + bone.toString());
        
        for (ProcessedBone child : bone.getChildren()) {
            printBoneHierarchy(child, depth + 1);
        }
    }
    
    @Override
    public String toString() {
        return String.format("Skin[%s: %d bones, root=%s]", 
                name, bones.size(), rootBone != null ? rootBone.getName() : "none");
    }
}
