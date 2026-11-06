package net.rai.scp_unleashed.scp001scarlet.king.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.scp001scarlet.king.ScarletThorn;

public class ScarletThornRenderer extends SharedGeoRenderer<ScarletThorn> {
    public static final float Scale = 1.0f;

    public ScarletThornRenderer(EntityRendererProvider.Context Context) {
        super(Context, new ScarletThornGeoModel());
        withScale(Scale);
        shadowRadius = 0.4f;
        addRenderLayer(new ScarletKingGlowLayer<>(this));
    }
}
