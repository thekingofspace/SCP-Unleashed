package net.rai.scp_unleashed.scp4666.christmas.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp4666.christmas.Scp4666Sighting;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class Scp4666SightingRenderer extends GeoEntityRenderer<Scp4666Sighting> {
    public static final float Shadow = 0.5f;

    public Scp4666SightingRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp4666SightingModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = Shadow;
    }
}
