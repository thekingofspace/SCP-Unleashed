package net.rai.scp_unleashed.scp149.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp149.Scp149Fly;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp149FlyRenderer extends GeoEntityRenderer<Scp149Fly> {
    public static final float Shadow = 0.25f;

    public Scp149FlyRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp149FlyGeoModel());
        shadowRadius = Shadow;
        addRenderLayer(new Scp149WingLayer<>(this));
    }

    @Override
    public void render(Scp149Fly Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setRenderPartial(PartialTick);
        super.render(Creature, Yaw, PartialTick, Pose, Buffers, Light);
    }
}
