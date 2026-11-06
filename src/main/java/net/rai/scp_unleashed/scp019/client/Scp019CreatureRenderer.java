package net.rai.scp_unleashed.scp019.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp019.Scp019Creature;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp019CreatureRenderer<T extends Scp019Creature> extends GeoEntityRenderer<T> {
    public Scp019CreatureRenderer(EntityRendererProvider.Context Context, Scp019GeoModel<T> Geometry, float Shadow) {
        super(Context, Geometry);
        shadowRadius = Shadow;
    }

    @Override
    protected float getShadowRadius(T Creature) {
        byte Current = Creature.action();
        if (Current == Scp019Creature.ActionEmerge || Current == Scp019Creature.ActionEnter) {
            return 0.0f;
        }
        if (Current == Scp019Creature.ActionForm) {
            return shadowRadius * (float) Math.min(1.0, Creature.actionElapsed() / Creature.formTicks());
        }
        return shadowRadius;
    }

    @Override
    public void render(T Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setGaitPartial(PartialTick);
        Creature.applyLockedYaw();
        super.render(Creature, Creature.locksYaw() ? Creature.getYRot() : Yaw, PartialTick, Pose, Buffers, Light);
    }
}
