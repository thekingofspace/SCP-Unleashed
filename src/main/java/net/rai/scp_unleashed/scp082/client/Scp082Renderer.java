package net.rai.scp_unleashed.scp082.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp082.Scp082Fernand;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp082Renderer extends GeoEntityRenderer<Scp082Fernand> {
    public static final float ShadowSize = 0.95f;

    public Scp082Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp082GeoModel());
        shadowRadius = ShadowSize;
    }

    @Override
    public void render(Scp082Fernand Fernand, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Fernand.setGaitPartial(PartialTick);
        super.render(Fernand, Yaw, PartialTick, Pose, Buffers, Light);
    }
}
