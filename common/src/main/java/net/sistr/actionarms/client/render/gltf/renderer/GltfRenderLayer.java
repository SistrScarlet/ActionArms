package net.sistr.actionarms.client.render.gltf.renderer;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.sistr.actionarms.ActionArms;

import java.util.function.BiFunction;
import java.util.function.Function;

import static net.minecraft.client.render.RenderPhase.*;

public class GltfRenderLayer {

    private static final Function<Identifier, RenderLayer> ENTITY_CUTOUT_TRIANGLE = Util.memoize(
            texture -> {
                var params = RenderLayer.MultiPhaseParameters.builder()
                        .program(ENTITY_CUTOUT_PROGRAM)
                        .texture(new Texture(texture, false, false))
                        .transparency(NO_TRANSPARENCY)
                        .lightmap(ENABLE_LIGHTMAP)
                        .overlay(ENABLE_OVERLAY_COLOR)
                        .build(true);
                return RenderLayer.of(nameOf("entity_cutout_triangle"),
                        VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                        VertexFormat.DrawMode.TRIANGLES,
                        256,
                        true,
                        false,
                        params);
            });
    private static final Function<Identifier, RenderLayer> ENTITY_CUTOUT_NO_CULL_TRIANGLE = Util.memoize(
            texture -> {
                var params = RenderLayer.MultiPhaseParameters.builder()
                        .program(ENTITY_CUTOUT_PROGRAM)
                        .texture(new Texture(texture, false, false))
                        .transparency(NO_TRANSPARENCY)
                        .lightmap(ENABLE_LIGHTMAP)
                        .overlay(ENABLE_OVERLAY_COLOR)
                        .cull(DISABLE_CULLING)
                        .build(true);
                return RenderLayer.of(nameOf("entity_cutout_no_cull_triangle"),
                        VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                        VertexFormat.DrawMode.TRIANGLES,
                        256,
                        true,
                        false,
                        params);
            });
    private static final BiFunction<Identifier, Boolean, RenderLayer> ENTITY_TRANSLUCENT_TRIANGLE = Util.memoize(
            (texture, affectsOutline) -> {
                var params = RenderLayer.MultiPhaseParameters.builder()
                        .program(ENTITY_TRANSLUCENT_PROGRAM)
                        .texture(new Texture(texture, false, false))
                        .transparency(TRANSLUCENT_TRANSPARENCY)
                        .cull(DISABLE_CULLING)
                        .lightmap(ENABLE_LIGHTMAP)
                        .overlay(ENABLE_OVERLAY_COLOR)
                        .build(affectsOutline);
                return RenderLayer.of(nameOf("entity_translucent_triangle"),
                        VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                        VertexFormat.DrawMode.TRIANGLES,
                        256,
                        true,
                        true,
                        params);
            });

    public static RenderLayer getEntityCutoutTriangle(Identifier identifier) {
        return ENTITY_CUTOUT_TRIANGLE.apply(identifier);
    }

    public static RenderLayer getEntityCutoutNoCullTriangle(Identifier identifier) {
        return ENTITY_CUTOUT_NO_CULL_TRIANGLE.apply(identifier);
    }

    public static RenderLayer getEntityTranslucentTriangle(Identifier identifier, boolean affectsOutline) {
        return ENTITY_TRANSLUCENT_TRIANGLE.apply(identifier, affectsOutline);
    }

    private static String nameOf(String id) {
        return ActionArms.MOD_ID + ":" + id;
    }
}
