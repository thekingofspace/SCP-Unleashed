package net.rai.scp_unleashed.scp058.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp058.Scp058Heart;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp058Renderer extends GeoEntityRenderer<Scp058Heart> {
    public static final float Shadow = 0.7f;

    public Scp058Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp058GeoModel());
        shadowRadius = Shadow;
    }

    @Override
    protected float getShadowRadius(Scp058Heart Creature) {
        if (Creature.isEmerging()) {
            return Shadow * (float) Math.min(1.0, Creature.actionElapsed() / Scp058Heart.EmergeTicks);
        }
        return Shadow;
    }

    @Override
    public void render(Scp058Heart Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setRenderPartial(PartialTick);
        Creature.applyLockedYaw();
        super.render(Creature, Creature.locksYaw() ? Creature.getYRot() : Yaw, PartialTick, Pose, Buffers, Light);
    }
}
