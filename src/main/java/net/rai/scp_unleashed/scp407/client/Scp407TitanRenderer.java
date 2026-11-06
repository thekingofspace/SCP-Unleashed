package net.rai.scp_unleashed.scp407.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp407.Scp407RigData;
import net.rai.scp_unleashed.scp407.Scp407Titan;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.util.Color;

public class Scp407TitanRenderer extends GeoEntityRenderer<Scp407Titan> {
    public static final float Shadow = 0.75f;

    public Scp407TitanRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp407TitanModel());
        shadowRadius = Shadow;
        addRenderLayer(new Scp407GlowLayer<>(this));
    }

    @Override
    protected float getDeathMaxRotation(Scp407Titan Creature) {
        return 0.0f;
    }

    @Override
    protected float getShadowRadius(Scp407Titan Creature) {
        if (Creature.isEmerging()) {
            return Shadow * (float) Math.min(1.0, Creature.actionElapsed() / Scp407RigData.TitanEmergeTicks);
        }
        return Shadow;
    }

    @Override
    public Color getRenderColor(Scp407Titan Creature, float PartialTick, int PackedLight) {
        return Scp407Tint.tint(Creature.isDeadOrDying() ? Math.min(1.0f, (Creature.deathTime + PartialTick) / 30.0f) : Creature.witherShare());
    }

    @Override
    public void render(Scp407Titan Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setRenderPartial(PartialTick);
        Creature.applyLockedYaw();
        super.render(Creature, Creature.locksYaw() ? Creature.getYRot() : Yaw, PartialTick, Pose, Buffers, Light);
    }
}
