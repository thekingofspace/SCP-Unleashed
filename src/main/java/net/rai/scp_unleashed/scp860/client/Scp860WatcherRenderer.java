package net.rai.scp_unleashed.scp860.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.client.geo.SharedGeoRenderer;
import net.rai.scp_unleashed.scp860.Scp860Watcher;

public class Scp860WatcherRenderer extends SharedGeoRenderer<Scp860Watcher> {
    public static final float Scale = 1.6f;
    public static final float ShadowRadius = 0.6f;

    public Scp860WatcherRenderer(EntityRendererProvider.Context Context) {
        super(Context, new Scp860WatcherModel());
        withScale(Scale);
        shadowRadius = ShadowRadius;
        addRenderLayer(new Scp860GlowLayer(this));
    }

    @Override
    public void render(Scp860Watcher Owner, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Owner.setRenderPartial(PartialTick);
        super.render(Owner, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    protected float getDeathMaxRotation(Scp860Watcher Owner) {
        return 0.0f;
    }
}
