package net.sistr.actionarms.client.render.gltf.renderer;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.sistr.actionarms.ActionArms;

import java.util.function.Function;

import static net.minecraft.client.render.RenderPhase.*;

public class GltfRenderLayer {

    private static final Function<Identifier, RenderLayer> ENTITY_CUTOUT_TRIANGLE = Util.memoize(texture -> {
        RenderLayer.MultiPhaseParameters multiPhaseParameters
                = RenderLayer.MultiPhaseParameters.builder()
                .program(ENTITY_CUTOUT_PROGRAM)
                .texture(new RenderPhase.Texture(texture, false, false))
                .transparency(NO_TRANSPARENCY)
                .lightmap(ENABLE_LIGHTMAP)
                .overlay(ENABLE_OVERLAY_COLOR)
                .build(true);
        return RenderLayer.of(ActionArms.MOD_ID + ":entity_cutout_triangle",
                VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                VertexFormat.DrawMode.TRIANGLES,
                256,
                true,
                false,
                multiPhaseParameters);
    });

    public static RenderLayer getEntityCutoutTriangle(Identifier identifier) {
        return ENTITY_CUTOUT_TRIANGLE.apply(identifier);
    }
}
