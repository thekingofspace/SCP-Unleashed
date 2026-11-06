package net.rai.scp_unleashed.scp131.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp131.Scp131Pod;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp131Renderer extends GeoEntityRenderer<Scp131Pod> {
    public static final float Shadow = 0.26f;

    public Scp131Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp131GeoModel());
        withScale(Scp131GeoModel.ModelScale);
        shadowRadius = Shadow;
        addRenderLayer(new Scp131GlassLayer<>(this));
    }
}
