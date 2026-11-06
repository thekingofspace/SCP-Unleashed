package net.rai.scp_unleashed.scp492.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.scp492.Scp492Figure;

public class Scp492Renderer extends SharedGeoRenderer<Scp492Figure> {
    public static final float Scale = 0.5f;
    public static final float Shadow = 0.22f;

    public Scp492Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp492GeoModel());
        withScale(Scale);
        shadowRadius = Shadow;
    }

    @Override
    public void render(Scp492Figure Figure, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Figure.setRenderPartial(PartialTick);
        super.render(Figure, Yaw, PartialTick, Pose, Buffers, Light);
    }
}
