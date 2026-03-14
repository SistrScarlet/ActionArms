package net.sistr.actionarms.client.render.gltf.renderer;

import org.joml.Quaternionf;

public class BoneTRS {
    private final float[] data;
    private final int offset;
    private float tx, ty, tz;
    private float rx, ry, rz, rw;
    private float sx, sy, sz;
    private boolean translationDirty;
    private boolean rotationDirty;
    private boolean scaleDirty;

    BoneTRS(float[] data, int offset) {
        this.data = data;
        this.offset = offset;
    }

    public void setTranslation(float x, float y, float z) {
        this.tx = x;
        this.ty = y;
        this.tz = z;
        this.translationDirty = true;
    }

    public void setRotation(Quaternionf q) {
        this.rx = q.x;
        this.ry = q.y;
        this.rz = q.z;
        this.rw = q.w;
        this.rotationDirty = true;
    }

    public void setScale(float x, float y, float z) {
        this.sx = x;
        this.sy = y;
        this.sz = z;
        this.scaleDirty = true;
    }

    void flush() {
        if (translationDirty) {
            data[offset] = tx;
            data[offset + 1] = ty;
            data[offset + 2] = tz;
        }
        if (rotationDirty) {
            data[offset + 3] = rx;
            data[offset + 4] = ry;
            data[offset + 5] = rz;
            data[offset + 6] = rw;
        }
        if (scaleDirty) {
            data[offset + 7] = sx;
            data[offset + 8] = sy;
            data[offset + 9] = sz;
        }
    }
}
