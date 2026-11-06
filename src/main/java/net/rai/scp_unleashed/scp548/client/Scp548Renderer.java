package net.rai.scp_unleashed.scp548.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.scp548.Scp548Spider;

public class Scp548Renderer extends SharedGeoRenderer<Scp548Spider> {
    public static final float Scale = 0.3f;
    public static final float ShadowRadius = 0.22f;

    public Scp548Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp548GeoModel());
        withScale(Scale);
        shadowRadius = ShadowRadius;
    }

    @Override
    public void render(Scp548Spider Spider, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Spider.setRenderPartial(PartialTick);
        super.render(Spider, Yaw, PartialTick, Pose, Buffers, Light);
    }
}
