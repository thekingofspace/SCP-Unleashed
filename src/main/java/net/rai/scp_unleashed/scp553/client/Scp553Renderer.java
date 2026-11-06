package net.rai.scp_unleashed.scp553.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.scp553.Scp553Butterfly;

public class Scp553Renderer extends SharedGeoRenderer<Scp553Butterfly> {
    public static final float Scale = 0.5f;
    public static final float Shadow = 0.15f;
    public static final int Shimmer = 7;

    public Scp553Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp553GeoModel());
        withScale(Scale);
        shadowRadius = Shadow;
    }

    @Override
    public void render(Scp553Butterfly Butterfly, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Butterfly.setRenderPartial(PartialTick);
        super.render(Butterfly, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    protected int getBlockLightLevel(Scp553Butterfly Butterfly, BlockPos Pos) {
        return Math.max(Shimmer, super.getBlockLightLevel(Butterfly, Pos));
    }
}
