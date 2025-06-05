package net.sistr.actionarms.client.render.gltf.data;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ComputedTRSData {
    public static final int SIZE = 3 + 4 + 3;
    private final float[] data;
    private final int count;

    public ComputedTRSData(float[] data) {
        this.data = data;
        this.count = data.length / SIZE;
    }

    public float[] getRawData() {
        return data;
    }

    public Vector3f getTranslation(int index) {
        return new Vector3f(
                data[index * SIZE],
                data[index * SIZE + 1],
                data[index * SIZE + 2]);
    }

    public Quaternionf getRotation(int index) {
        return new Quaternionf(
                data[index * SIZE + 3],
                data[index * SIZE + 4],
                data[index * SIZE + 5],
                data[index * SIZE + 6]);
    }

    public Vector3f getScale(int index) {
        return new Vector3f(
                data[index * SIZE + 7],
                data[index * SIZE + 8],
                data[index * SIZE + 9]);
    }

    public int getCount() {
        return count;
    }
}
