package net.rai.scp_unleashed.scp001.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp001.Scp001Prototype;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp001Renderer extends GeoEntityRenderer<Scp001Prototype> {
    public static final float ShadowSize = 0.75f;

    public Scp001Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp001GeoModel());
        addRenderLayer(new Scp001GlowLayer(this));
        shadowRadius = ShadowSize;
    }

    @Override
    protected float getDeathMaxRotation(Scp001Prototype Creature) {
        return 0.0f;
    }
}
