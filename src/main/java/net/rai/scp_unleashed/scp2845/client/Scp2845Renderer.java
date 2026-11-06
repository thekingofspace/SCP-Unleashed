package net.rai.scp_unleashed.scp2845.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.scp2845.Scp2845;

public class Scp2845Renderer extends SharedGeoRenderer<Scp2845> {
    public static final float Scale = 2.2f;
    public static final float ShadowRadius = 0.9f;

    public Scp2845Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp2845Model());
        withScale(Scale);
        shadowRadius = ShadowRadius;
        addRenderLayer(new Scp2845GlowLayer<>(this));
    }

    @Override
    public void render(Scp2845 Owner, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Owner.setRenderPartial(PartialTick);
        super.render(Owner, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    protected float getDeathMaxRotation(Scp2845 Owner) {
        return 0.0f;
    }
}
