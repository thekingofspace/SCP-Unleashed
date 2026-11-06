package net.rai.scp_unleashed.scp469.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.scp469.Scp469Angel;

public class Scp469Renderer extends SharedGeoRenderer<Scp469Angel> {
    public static final float SpreadShadow = 1.3f;
    public static final float FoldShadow = 0.55f;

    public Scp469Renderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp469GeoModel());
        shadowRadius = SpreadShadow;
    }

    @Override
    public void render(Scp469Angel Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setRenderPartial(PartialTick);
        shadowRadius = Creature.isDeadOrDying() ? 0.0f : Creature.isFolded() ? FoldShadow : SpreadShadow;
        super.render(Creature, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    public boolean shouldShowName(Scp469Angel Creature) {
        return Creature.hasCustomName() && super.shouldShowName(Creature);
    }

    @Override
    public int getPackedOverlay(Scp469Angel Creature, float U, float PartialTick) {
        return Creature.deathTime > 0 ? OverlayTexture.NO_OVERLAY : super.getPackedOverlay(Creature, U, PartialTick);
    }

    @Override
    protected float getDeathMaxRotation(Scp469Angel Creature) {
        return 0.0f;
    }
}
