package net.rai.scp_unleashed.scp372.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp372.Scp372Insect;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp372Renderer extends GeoEntityRenderer<Scp372Insect> {
    public static final float Shadow = 0.54f;
    public static final float RenderScale = 1.2f;
    public static final int MaterializeFlicker = 9;

    public Scp372Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp372GeoModel());
        withScale(RenderScale);
        shadowRadius = Shadow;
        addRenderLayer(new Scp372GlowLayer<>(this, Creature -> !Creature.isFlickering()));
    }

    public static boolean blinkedOut(Scp372Insect Creature) {
        int Frame = (int) Creature.actionElapsed();
        if (Creature.isFlickering()) {
            return Frame % 3 == 1 || Frame > Scp372Insect.FlickerTicks / 2 && Frame % 2 == 1;
        }
        return Creature.isMaterializing() && Frame < MaterializeFlicker && Frame % 3 == 2;
    }

    @Override
    protected float getShadowRadius(Scp372Insect Creature) {
        float Share = 1.0f;
        if (Creature.isMaterializing()) {
            Share = (float) Math.min(1.0, Creature.actionElapsed() / Scp372Insect.MaterializeTicks);
        } else if (Creature.isFlickering()) {
            Share = (float) Math.max(0.0, 1.0 - Creature.actionElapsed() / Scp372Insect.FlickerTicks);
        }
        return Shadow * Share;
    }

    @Override
    public void render(Scp372Insect Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setRenderPartial(PartialTick);
        if (blinkedOut(Creature)) {
            return;
        }
        super.render(Creature, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    protected float getDeathMaxRotation(Scp372Insect Creature) {
        return 0.0f;
    }
}
