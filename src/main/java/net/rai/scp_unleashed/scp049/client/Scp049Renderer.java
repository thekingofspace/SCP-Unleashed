package net.rai.scp_unleashed.scp049.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp049.Scp049Doctor;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp049Renderer extends GeoEntityRenderer<Scp049Doctor> {
    public static final float ShadowSize = 0.45f;

    public Scp049Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp049GeoModel());
        shadowRadius = ShadowSize;
    }

    @Override
    public void render(Scp049Doctor Doctor, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Doctor.setGaitPartial(PartialTick);
        super.render(Doctor, Yaw, PartialTick, Pose, Buffers, Light);
    }
}
