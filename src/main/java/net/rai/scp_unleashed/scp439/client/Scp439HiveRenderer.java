package net.rai.scp_unleashed.scp439.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.scp439.Scp439Hive;
import net.rai.scp_unleashed.scp439.Scp439Look;

public class Scp439HiveRenderer extends SharedGeoRenderer<Scp439Hive> {
    public static final float PersonScale = 0.9375f;

    public Scp439HiveRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp439HiveModel());
        addRenderLayer(new Scp439VillagerLayer(this));
        addRenderLayer(new Scp439GearLayer(this));
        addRenderLayer(new Scp439GrowthLayer(this));
        shadowRadius = 0.85f;
    }

    @Override
    public void render(Scp439Hive Hive, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        float Scale = Hive.look().Kind() == Scp439Look.KindDClass ? 1.0f : PersonScale;
        withScale(Scale);
        super.render(Hive, Hive.getYRot(), PartialTick, Pose, Buffers, Light);
    }

    @Override
    public boolean shouldShowName(Scp439Hive Hive) {
        return false;
    }

    @Override
    public int getPackedOverlay(Scp439Hive Hive, float U, float PartialTick) {
        return Hive.deathTime > 0 ? OverlayTexture.NO_OVERLAY : super.getPackedOverlay(Hive, U, PartialTick);
    }

    @Override
    protected float getDeathMaxRotation(Scp439Hive Hive) {
        return 0.0f;
    }
}
