package net.rai.scp_unleashed.scp076.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.dclass.client.HumanoidGearLayer;
import net.rai.scp_unleashed.scp076.Scp076Abel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp076AbelRenderer extends GeoEntityRenderer<Scp076Abel> {
    public static final float ShadowSize = 0.45f;

    public Scp076AbelRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp076AbelModel());
        addRenderLayer(new HumanoidGearLayer<>(this));
        addRenderLayer(new Scp076GlowLayer(this));
        shadowRadius = ShadowSize;
    }

    @Override
    public void render(Scp076Abel Owner, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Owner.setGaitPartial(PartialTick);
        Owner.setRenderPartial(PartialTick);
        super.render(Owner, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    protected float getDeathMaxRotation(Scp076Abel Owner) {
        return 0.0f;
    }
}
