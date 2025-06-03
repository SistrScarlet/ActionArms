package net.sistr.actionarms.client.render.gltf;

import org.joml.Matrix4f;
import org.joml.Vector4f;
import java.util.List;

public class VertexProcessor {
    
    public ComputedVertexData computeVertices(ProcessedMesh mesh, RenderingContext context) {
        int vertexCount = mesh.getVertexCount();
        
        float[] finalPositions = new float[vertexCount * 3];
        float[] finalNormals = new float[vertexCount * 3];
        
        // ベース頂点データをコピー
        float[] basePositions = mesh.getPositions();
        float[] baseNormals = mesh.getNormals();
        System.arraycopy(basePositions, 0, finalPositions, 0, finalPositions.length);
        System.arraycopy(baseNormals, 0, finalNormals, 0, finalNormals.length);
        
        // モーフターゲットの適用
        if (context.getMorphWeights() != null && context.getMorphWeights().length > 0) {
            applyMorphTargets(mesh, context.getMorphWeights(), finalPositions, finalNormals);
        }
        
        // スキニングの適用
        if (mesh.hasSkinning() && context.getBoneMatrices() != null) {
            applySkinning(mesh, context.getBoneMatrices(), finalPositions, finalNormals);
        }
        
        return new ComputedVertexData(finalPositions, finalNormals);
    }
    
    private void applyMorphTargets(ProcessedMesh mesh, float[] morphWeights, 
                                  float[] positions, float[] normals) {
        List<MorphTarget> morphTargets = mesh.getMorphTargets();
        
        for (int targetIndex = 0; targetIndex < morphTargets.size() && targetIndex < morphWeights.length; targetIndex++) {
            float weight = morphWeights[targetIndex];
            if (Math.abs(weight) < 0.001f) continue; // 重みが小さい場合はスキップ
            
            MorphTarget target = morphTargets.get(targetIndex);
            
            // 位置の差分を適用
            if (target.hasPositionDeltas()) {
                float[] deltas = target.getPositionDeltas();
                for (int i = 0; i < positions.length && i < deltas.length; i++) {
                    positions[i] += deltas[i] * weight;
                }
            }
            
            // 法線の差分を適用
            if (target.hasNormalDeltas()) {
                float[] normalDeltas = target.getNormalDeltas();
                for (int i = 0; i < normals.length && i < normalDeltas.length; i++) {
                    normals[i] += normalDeltas[i] * weight;
                }
            }
        }
    }
    
    private void applySkinning(ProcessedMesh mesh, Matrix4f[] boneMatrices,
                              float[] positions, float[] normals) {
        int[] boneIndices = mesh.getBoneIndices();
        float[] boneWeights = mesh.getBoneWeights();
        
        if (boneIndices == null || boneWeights == null) return;
        
        Vector4f tempPos = new Vector4f();
        Vector4f tempNormal = new Vector4f();
        Vector4f resultPos = new Vector4f();
        Vector4f resultNormal = new Vector4f();
        
        int vertexCount = positions.length / 3;
        
        for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
            int baseIndex = vertexIndex * 3;
            int weightIndex = vertexIndex * 4;
            
            // 元の頂点位置と法線
            float origX = positions[baseIndex];
            float origY = positions[baseIndex + 1];
            float origZ = positions[baseIndex + 2];
            
            float origNx = normals[baseIndex];
            float origNy = normals[baseIndex + 1];
            float origNz = normals[baseIndex + 2];
            
            resultPos.set(0, 0, 0, 0);
            resultNormal.set(0, 0, 0, 0);
            
            // 4つのボーンの影響を合成
            for (int boneSlot = 0; boneSlot < 4; boneSlot++) {
                int boneIndex = boneIndices[weightIndex + boneSlot];
                float weight = boneWeights[weightIndex + boneSlot];
                
                if (weight <= 0 || boneIndex < 0 || boneIndex >= boneMatrices.length) {
                    continue;
                }
                
                Matrix4f boneMatrix = boneMatrices[boneIndex];
                
                // 位置変換
                tempPos.set(origX, origY, origZ, 1.0f);
                boneMatrix.transform(tempPos);
                resultPos.add(tempPos.x * weight, tempPos.y * weight, tempPos.z * weight, 0);
                
                // 法線変換（位置と同じ行列を使用、wは0）
                tempNormal.set(origNx, origNy, origNz, 0.0f);
                boneMatrix.transform(tempNormal);
                resultNormal.add(tempNormal.x * weight, tempNormal.y * weight, tempNormal.z * weight, 0);
            }
            
            // 結果を配列に戻す
            positions[baseIndex] = resultPos.x;
            positions[baseIndex + 1] = resultPos.y;
            positions[baseIndex + 2] = resultPos.z;
            
            normals[baseIndex] = resultNormal.x;
            normals[baseIndex + 1] = resultNormal.y;
            normals[baseIndex + 2] = resultNormal.z;
        }
        
        // 法線の正規化
        normalizeNormals(normals);
    }
    
    private void normalizeNormals(float[] normals) {
        for (int i = 0; i < normals.length; i += 3) {
            float x = normals[i];
            float y = normals[i + 1];
            float z = normals[i + 2];
            
            float length = (float) Math.sqrt(x * x + y * y + z * z);
            if (length > 0.001f) {
                normals[i] = x / length;
                normals[i + 1] = y / length;
                normals[i + 2] = z / length;
            }
        }
    }
}
