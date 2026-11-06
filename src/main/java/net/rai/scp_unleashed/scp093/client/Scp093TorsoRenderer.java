package net.rai.scp_unleashed.scp093.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp093.Scp093Torso;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp093TorsoRenderer extends GeoEntityRenderer<Scp093Torso> {
    public static final float Shadow = 2.9f;

    public Scp093TorsoRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp093TorsoGeoModel());
        withScale(Scp093Torso.RenderScale);
        shadowRadius = Shadow;
        shadowStrength = 0.6f;
    }

    @Override
    protected float getDeathMaxRotation(Scp093Torso Creature) {
        return 0.0f;
    }

    @Override
    public void render(Scp093Torso Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setRenderPartial(PartialTick);
        super.render(Creature, Yaw, PartialTick, Pose, Buffers, Light);
    }
}
