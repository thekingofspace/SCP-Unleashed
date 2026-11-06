package net.rai.scp_unleashed.scp024.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp024.Scp024Host;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class Scp024HostRenderer extends GeoEntityRenderer<Scp024Host> {
    public Scp024HostRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp024HostModel());
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
        shadowRadius = 0.45f;
    }
}
