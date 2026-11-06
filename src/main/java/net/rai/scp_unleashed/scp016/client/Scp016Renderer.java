package net.rai.scp_unleashed.scp016.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.rai.scp_unleashed.scp016.Scp016Spawn;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class Scp016Renderer<T extends Scp016Spawn> extends GeoEntityRenderer<T> {
    public Scp016Renderer(EntityRendererProvider.Context Context, Scp016GeoModel<T> Geometry, float Shadow) {
        super(Context, Geometry);
        addRenderLayer(new Scp016GlowLayer<>(this));
        shadowRadius = Shadow;
    }

    @Override
    public void render(T Creature, float Yaw, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light) {
        Creature.setGaitPartial(PartialTick);
        super.render(Creature, Yaw, PartialTick, Pose, Buffers, Light);
    }

    @Override
    protected float getDeathMaxRotation(T Creature) {
        return Creature.hasDeathAnimation() ? 0.0f : super.getDeathMaxRotation(Creature);
    }

    @Override
    public boolean isShaking(T Creature) {
        return super.isShaking(Creature) || Creature.isCollapsing();
    }
}
