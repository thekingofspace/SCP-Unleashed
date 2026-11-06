package net.rai.scp_unleashed.scp066.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp066.Scp066Entity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp066Renderer extends GeoEntityRenderer<Scp066Entity> {
    public static final float Shadow = 0.46f;

    public Scp066Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp066GeoModel());
        shadowRadius = Shadow;
    }

    @Override
    protected float getDeathMaxRotation(Scp066Entity Heap) {
        return 0.0f;
    }
}
