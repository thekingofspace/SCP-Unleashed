package net.rai.scp_unleashed.daevite.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.daevite.entity.DaeviteKnight;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DaeviteKnightRenderer extends GeoEntityRenderer<DaeviteKnight> {
    public static final float Scale = 1.06f;
    public static final float ShadowSize = 0.5f;

    public DaeviteKnightRenderer(EntityRendererProvider.Context Context) {
        super(Context, new DaeviteKnightModel());
        addRenderLayer(new DaeviteGlowLayer<>(this));
        withScale(Scale);
        shadowRadius = ShadowSize;
    }

    @Override
    public void render(DaeviteKnight Owner, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Owner.setGaitPartial(PartialTick);
        Owner.setRenderPartial(PartialTick);
        super.render(Owner, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    protected float getDeathMaxRotation(DaeviteKnight Owner) {
        return 0.0f;
    }
}
