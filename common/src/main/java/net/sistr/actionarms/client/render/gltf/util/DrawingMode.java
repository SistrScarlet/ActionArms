package net.sistr.actionarms.client.render.gltf.util;

import net.minecraft.client.render.VertexFormat;
import org.jetbrains.annotations.Nullable;

public enum DrawingMode {
    POINTS(0, null),
    LINES(1, VertexFormat.DrawMode.LINES),
    LINE_LOOP(2, null),
    LINE_STRIP(3, VertexFormat.DrawMode.LINE_STRIP),
    TRIANGLES(4, VertexFormat.DrawMode.TRIANGLES),
    TRIANGLE_STRIP(5, VertexFormat.DrawMode.TRIANGLE_STRIP),
    TRIANGLE_FAN(6, VertexFormat.DrawMode.TRIANGLE_FAN);

    private final int glValue;
    @Nullable
    private final VertexFormat.DrawMode drawMode;

    DrawingMode(int glValue, @Nullable VertexFormat.DrawMode drawMode) {
        this.glValue = glValue;
        this.drawMode = drawMode;
    }

    public int getGlValue() {
        return glValue;
    }

    @Nullable
    public VertexFormat.DrawMode getDrawMode() {
        return drawMode;
    }

    public boolean isDrawable() {
        return drawMode != null;
    }

    @Nullable
    public VertexFormat.DrawMode toMCMode() {
        return this.drawMode;
    }

    @Nullable
    public static DrawingMode from(int glValue) {
        for (DrawingMode mode : values()) {
            if (mode.glValue == glValue) {
                return mode;
            }
        }
        return null;
    }
}
