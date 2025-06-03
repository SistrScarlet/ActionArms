package net.sistr.actionarms.client.render.gltf;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ProcessedBone {
    private final int index;
    private final String name;
    private ProcessedBone parent;
    private final List<ProcessedBone> children;
    
    // 変換情報
    private final Vector3f translation;
    private final Quaternionf rotation;
    private final Vector3f scale;
    private final Matrix4f localTransform;
    private final Matrix4f worldTransform;
    private final Matrix4f inverseBindMatrix;
    
    // アニメーション用
    private final Vector3f animatedTranslation;
    private final Quaternionf animatedRotation;
    private final Vector3f animatedScale;
    
    public ProcessedBone(int index, String name) {
        this.index = index;
        this.name = name != null ? name : "Bone_" + index;
        this.children = new ArrayList<>();
        
        this.translation = new Vector3f();
        this.rotation = new Quaternionf();
        this.scale = new Vector3f(1.0f);
        this.localTransform = new Matrix4f();
        this.worldTransform = new Matrix4f();
        this.inverseBindMatrix = new Matrix4f();
        
        this.animatedTranslation = new Vector3f();
        this.animatedRotation = new Quaternionf();
        this.animatedScale = new Vector3f(1.0f);
    }
    
    // 基本情報
    public int getIndex() { return index; }
    public String getName() { return name; }
    
    // 階層構造
    public ProcessedBone getParent() { return parent; }
    public void setParent(ProcessedBone parent) { 
        this.parent = parent;
        if (parent != null) {
            parent.addChild(this);
        }
    }
    
    public List<ProcessedBone> getChildren() { return new ArrayList<>(children); }
    public void addChild(ProcessedBone child) {
        if (!children.contains(child)) {
            children.add(child);
        }
    }
    
    // 基本変換
    public void setTranslation(float x, float y, float z) {
        translation.set(x, y, z);
    }
    
    public void setRotation(float x, float y, float z, float w) {
        rotation.set(x, y, z, w);
    }
    
    public void setScale(float x, float y, float z) {
        scale.set(x, y, z);
    }
    
    public Vector3f getTranslation() { return new Vector3f(translation); }
    public Quaternionf getRotation() { return new Quaternionf(rotation); }
    public Vector3f getScale() { return new Vector3f(scale); }
    
    // アニメーション変換
    public void setAnimatedTranslation(float x, float y, float z) {
        animatedTranslation.set(x, y, z);
    }
    
    public void setAnimatedRotation(float x, float y, float z, float w) {
        animatedRotation.set(x, y, z, w);
    }
    
    public void setAnimatedScale(float x, float y, float z) {
        animatedScale.set(x, y, z);
    }
    
    public Vector3f getAnimatedTranslation() { return new Vector3f(animatedTranslation); }
    public Quaternionf getAnimatedRotation() { return new Quaternionf(animatedRotation); }
    public Vector3f getAnimatedScale() { return new Vector3f(animatedScale); }
    
    // 変換行列
    public Matrix4f getInverseBindMatrix() { return new Matrix4f(inverseBindMatrix); }
    public void setInverseBindMatrix(Matrix4f matrix) { 
        this.inverseBindMatrix.set(matrix); 
    }
    
    // ローカル変換行列の計算
    public Matrix4f computeLocalTransform(boolean useAnimation) {
        Vector3f t = useAnimation ? animatedTranslation : translation;
        Quaternionf r = useAnimation ? animatedRotation : rotation;
        Vector3f s = useAnimation ? animatedScale : scale;
        
        localTransform.identity()
                     .translate(t)
                     .rotate(r)
                     .scale(s);
        
        return new Matrix4f(localTransform);
    }
    
    // ワールド変換行列の計算
    public Matrix4f computeWorldTransform(boolean useAnimation) {
        Matrix4f local = computeLocalTransform(useAnimation);
        
        if (parent != null) {
            Matrix4f parentWorld = parent.computeWorldTransform(useAnimation);
            worldTransform.set(parentWorld).mul(local);
        } else {
            worldTransform.set(local);
        }
        
        return new Matrix4f(worldTransform);
    }
    
    // 最終的なボーン変換行列の計算
    public Matrix4f computeBoneMatrix(boolean useAnimation) {
        Matrix4f worldMatrix = computeWorldTransform(useAnimation);
        return new Matrix4f(worldMatrix).mul(inverseBindMatrix);
    }
    
    // ルートまでのパスを取得
    public List<ProcessedBone> getPathToRoot() {
        List<ProcessedBone> path = new ArrayList<>();
        ProcessedBone current = this;
        
        while (current != null) {
            path.add(0, current); // 先頭に挿入
            current = current.parent;
        }
        
        return path;
    }
    
    // 深度を取得
    public int getDepth() {
        int depth = 0;
        ProcessedBone current = parent;
        
        while (current != null) {
            depth++;
            current = current.parent;
        }
        
        return depth;
    }
    
    @Override
    public String toString() {
        return String.format("Bone[%d: %s, children=%d, depth=%d]", 
                index, name, children.size(), getDepth());
    }
}
