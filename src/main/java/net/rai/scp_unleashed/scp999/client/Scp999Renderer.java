package net.rai.scp_unleashed.scp999.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.scp999.Scp999Blob;

public class Scp999Renderer extends SharedGeoRenderer<Scp999Blob> {
    public static final float Scale = 1.0f;
    public static final float Shadow = 0.42f;

    public Scp999Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp999GeoModel());
        withScale(Scale);
        shadowRadius = Shadow;
        addRenderLayer(new Scp999GelLayer(this));
    }

    @Override
    public void render(Scp999Blob Blob, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Blob.setRenderPartial(PartialTick);
        super.render(Blob, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    protected float getDeathMaxRotation(Scp999Blob Blob) {
        return 0.0f;
    }
}
