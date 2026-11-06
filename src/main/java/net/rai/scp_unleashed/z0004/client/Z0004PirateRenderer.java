package net.rai.scp_unleashed.z0004.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.z0004.Z0004Pirate;

public class Z0004PirateRenderer extends SharedGeoRenderer<Z0004Pirate> {
    public static final float CrewScale = 1.0f;
    public static final float CaptainScale = 1.12f;
    public static final float ShadowRadius = 0.45f;

    public Z0004PirateRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Z0004PirateGeoModel());
        shadowRadius = ShadowRadius;
    }

    @Override
    public void render(Z0004Pirate Owner, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Owner.setRenderPartial(PartialTick);
        float Size = Owner.captain() ? CaptainScale : CrewScale;
        scaleWidth = Size;
        scaleHeight = Size;
        super.render(Owner, Yaw, PartialTick, Pose, Buffers, Light);
    }
}
