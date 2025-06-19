package net.sistr.actionarms.client.render.entity;

import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.util.Identifier;
import net.sistr.actionarms.entity.BulletEntity;

public class BulletEntityRenderer<T extends BulletEntity> extends EntityRenderer<T> {
    public BulletEntityRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public Identifier getTexture(T entity) {
        return new Identifier("textures/entity/arrow.png");
    }
}
