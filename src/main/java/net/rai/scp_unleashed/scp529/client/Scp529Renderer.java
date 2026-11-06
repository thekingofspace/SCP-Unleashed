package net.rai.scp_unleashed.scp529.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.scp529.Scp529Cat;

public class Scp529Renderer extends SharedGeoRenderer<Scp529Cat> {
    public static final float Scale = 0.8f;
    public static final float Shadow = 0.3f;

    public Scp529Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp529GeoModel());
        withScale(Scale);
        shadowRadius = Shadow;
    }

    @Override
    public void render(Scp529Cat Josie, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Josie.setRenderPartial(PartialTick);
        super.render(Josie, Yaw, PartialTick, Pose, Buffers, Light);
    }
}
