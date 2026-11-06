package net.rai.scp_unleashed.daevite.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.daevite.entity.DaeviteOverseer;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class DaeviteOverseerRenderer extends GeoEntityRenderer<DaeviteOverseer> {
    public static final float ShadowSize = 0.35f;

    public DaeviteOverseerRenderer(EntityRendererProvider.Context Context) {
        super(Context, new DaeviteOverseerModel());
        addRenderLayer(new DaeviteGlowLayer<>(this));
        shadowRadius = ShadowSize;
        shadowStrength = 0.5f;
    }

    @Override
    public void render(DaeviteOverseer Owner, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Owner.setGaitPartial(PartialTick);
        Owner.setRenderPartial(PartialTick);
        super.render(Owner, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    protected float getDeathMaxRotation(DaeviteOverseer Owner) {
        return 0.0f;
    }
}
