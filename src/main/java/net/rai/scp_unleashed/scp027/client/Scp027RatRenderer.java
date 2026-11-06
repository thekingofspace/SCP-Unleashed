package net.rai.scp_unleashed.scp027.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp027.Scp027Rat;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp027RatRenderer extends GeoEntityRenderer<Scp027Rat> {
    public static final float Shadow = 0.25f;

    public Scp027RatRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp027RatGeoModel());
        shadowRadius = Shadow;
    }
}
