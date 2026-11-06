package net.rai.scp_unleashed.scp031.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp031.Scp031Polyp;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp031PolypRenderer extends GeoEntityRenderer<Scp031Polyp> {
    public static final float[] Shadows = {0.15f, 0.22f, 0.32f, 0.42f};

    public Scp031PolypRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp031PolypModel());
        shadowRadius = Shadows[0];
    }

    @Override
    public void render(Scp031Polyp Polyp, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight) {
        shadowRadius = Shadows[Polyp.stage()];
        super.render(Polyp, Yaw, PartialTick, Pose, Buffers, PackedLight);
    }

    @Override
    protected float getDeathMaxRotation(Scp031Polyp Polyp) {
        return 0.0f;
    }
}
