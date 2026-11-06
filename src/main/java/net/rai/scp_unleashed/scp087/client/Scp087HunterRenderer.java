package net.rai.scp_unleashed.scp087.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp087.Scp087Hunter;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp087HunterRenderer extends GeoEntityRenderer<Scp087Hunter> {
    public static final float Shadow = 0.5f;

    public Scp087HunterRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp087HunterModel());
        addRenderLayer(new Scp087GlowLayer(this));
        shadowRadius = Shadow;
    }

    @Override
    public void render(Scp087Hunter Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setRenderPartial(PartialTick);
        super.render(Creature, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    protected float getDeathMaxRotation(Scp087Hunter Creature) {
        return 0.0f;
    }
}
