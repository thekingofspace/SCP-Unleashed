package net.rai.scp_unleashed.scp098.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp098.Scp098Crab;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp098CrabRenderer extends GeoEntityRenderer<Scp098Crab> {
    public static final float Shadow = 0.5f;

    public Scp098CrabRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp098CrabGeoModel());
        shadowRadius = Shadow;
    }

    @Override
    protected float getShadowRadius(Scp098Crab Creature) {
        byte Action = Creature.action();
        if (Action == Scp098Crab.ActionBurrow) {
            return Shadow * (float) Math.max(0.0, 1.0 - Creature.actionElapsed() / Scp098Crab.BurrowTicks);
        }
        if (Action == Scp098Crab.ActionSurface) {
            return Shadow * (float) Math.min(1.0, Creature.actionElapsed() / Scp098Crab.SurfaceTicks);
        }
        if (Action == Scp098Crab.ActionEmerge) {
            return Shadow * (float) Math.min(1.0, 0.4 + Creature.actionElapsed() / Scp098Crab.EmergeTicks);
        }
        return Shadow;
    }

    @Override
    public void render(Scp098Crab Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setRenderPartial(PartialTick);
        super.render(Creature, Yaw, PartialTick, Pose, Buffers, Light);
    }
}
