package net.rai.scp_unleashed.scp031.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp031.Scp031Mimic;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp031FleshRenderer extends GeoEntityRenderer<Scp031Mimic> {
    public static final float ShadowSize = 0.55f;

    public Scp031FleshRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp031FleshModel());
        shadowRadius = ShadowSize;
    }

    @Override
    public boolean shouldShowName(Scp031Mimic Mimic) {
        return Mimic.hasCustomName() && Mimic.isCustomNameVisible() && super.shouldShowName(Mimic);
    }
}
