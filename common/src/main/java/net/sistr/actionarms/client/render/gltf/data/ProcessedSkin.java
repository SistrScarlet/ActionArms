package net.sistr.actionarms.client.render.gltf.data;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ProcessedSkin {
    private final String name;
    private final List<ProcessedBone> bones;
    private final List<ProcessedBone> rootBones;

    public ProcessedSkin(String name, List<ProcessedBone> bones, List<ProcessedBone> rootBones) {
        this.name = name != null ? name : "DefaultSkin";
        this.bones = new ArrayList<>(bones);
        this.rootBones = new ArrayList<>(rootBones);
    }

    public ProcessedSkin(String name, List<ProcessedBone> bones) {
        this(
                name != null ? name : "DefaultSkin",
                new ArrayList<>(bones),
                findRootBones(bones)
        );
    }

    public String getName() {
        return name;
    }

    public String name() {
        return name;
    }

    public List<ProcessedBone> getBones() {
        return new ArrayList<>(bones);
    }

    public List<ProcessedBone> bones() {
        return new ArrayList<>(bones);
    }

    public int getBoneCount() {
        return bones.size();
    }

    public List<ProcessedBone> getRootBones() {
        return new ArrayList<>(rootBones);
    }

    public List<ProcessedBone> rootBones() {
        return new ArrayList<>(rootBones);
    }

    private static List<ProcessedBone> findRootBones(List<ProcessedBone> bones) {
        var rootBones = new ArrayList<ProcessedBone>();
        for (ProcessedBone bone : bones) {
            if (bone.parent() == null) {
                rootBones.add(bone);
            }
        }
        return rootBones;
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
            current = current.parent();
        }

        return false;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ProcessedSkin that = (ProcessedSkin) obj;
        return Objects.equals(name, that.name) &&
               Objects.equals(bones, that.bones) &&
               Objects.equals(rootBones, that.rootBones);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, bones, rootBones);
    }

    @Override
    public @NotNull String toString() {
        return String.format("Skin[%s: %d bones, %d rootBones]",
                name, bones.size(), rootBones.size());
    }
}