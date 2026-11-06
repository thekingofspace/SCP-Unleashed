package net.rai.scp_unleashed.dclass.theodore.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.dclass.theodore.TheodoreAmalgamation;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AmalgamationRenderer extends GeoEntityRenderer<TheodoreAmalgamation> {
    public static final float Shadow = 1.2f;

    public AmalgamationRenderer(EntityRendererProvider.Context Context) {
        super(Context, new AmalgamationGeoModel());
        shadowRadius = Shadow;
    }

    @Override
    protected float getDeathMaxRotation(TheodoreAmalgamation Owner) {
        return 0.0f;
    }
}
