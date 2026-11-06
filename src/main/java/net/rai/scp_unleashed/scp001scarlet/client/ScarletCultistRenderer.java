package net.rai.scp_unleashed.scp001scarlet.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp001scarlet.cult.ScarletCultist;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class ScarletCultistRenderer extends GeoEntityRenderer<ScarletCultist> {
    public ScarletCultistRenderer(EntityRendererProvider.Context Context) {
        super(Context, new ScarletCultistGeoModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0.45f;
    }
}
