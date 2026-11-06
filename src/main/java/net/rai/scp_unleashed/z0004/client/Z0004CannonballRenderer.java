package net.rai.scp_unleashed.z0004.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.z0004.Z0004Cannonball;

public class Z0004CannonballRenderer extends SharedGeoRenderer<Z0004Cannonball> {
    public static final float ShadowRadius = 0.2f;

    public Z0004CannonballRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Z0004CannonballGeoModel());
        shadowRadius = ShadowRadius;
    }
}
