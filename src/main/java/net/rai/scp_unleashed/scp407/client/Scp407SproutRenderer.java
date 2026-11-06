package net.rai.scp_unleashed.scp407.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp407.Scp407Sprout;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class Scp407SproutRenderer extends GeoEntityRenderer<Scp407Sprout> {
    public static final float Shadow = 0.28f;

    public Scp407SproutRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp407SproutModel());
        shadowRadius = Shadow;
        addRenderLayer(new Scp407GlowLayer<>(this));
    }

    @Override
    protected float getDeathMaxRotation(Scp407Sprout Creature) {
        return 0.0f;
    }

    @Override
    public Color getRenderColor(Scp407Sprout Creature, float PartialTick, int PackedLight) {
        return Scp407Tint.tint(Creature.isDeadOrDying() ? 1.0f : Creature.witherShare(PartialTick));
    }

    @Override
    public void render(Scp407Sprout Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setRenderPartial(PartialTick);
        super.render(Creature, Yaw, PartialTick, Pose, Buffers, Light);
    }
}
