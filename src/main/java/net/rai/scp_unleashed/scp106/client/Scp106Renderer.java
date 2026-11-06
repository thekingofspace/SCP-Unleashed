package net.rai.scp_unleashed.scp106.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp106.Scp106OldMan;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp106Renderer extends GeoEntityRenderer<Scp106OldMan> {
    public static final float Shadow = 0.55f;
    public static final float ModelScale = 0.72f;

    public Scp106Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp106GeoModel());
        withScale(ModelScale);
        shadowRadius = Shadow;
    }

    @Override
    public void render(Scp106OldMan Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setRenderPartial(PartialTick);
        if (Creature.hidden()) {
            return;
        }
        super.render(Creature, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    protected float getShadowRadius(Scp106OldMan Creature) {
        byte Action = Creature.action();
        if (Action == Scp106OldMan.ActionHidden || Action == Scp106OldMan.ActionSink || Action == Scp106OldMan.ActionRise || Action == Scp106OldMan.ActionStepOut) {
            return 0.0f;
        }
        return super.getShadowRadius(Creature);
    }

    @Override
    protected float getDeathMaxRotation(Scp106OldMan Creature) {
        return 0.0f;
    }
}
