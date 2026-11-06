package net.rai.scp_unleashed.scp346.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp346.Scp346Pterosaur;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp346Renderer extends GeoEntityRenderer<Scp346Pterosaur> {
    public static final float Shadow = 0.25f;

    public Scp346Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp346GeoModel());
        shadowRadius = Shadow;
    }

    @Override
    protected float getShadowRadius(Scp346Pterosaur Bird) {
        return Bird.isFlying() ? Shadow * 0.6f : Shadow;
    }
}
