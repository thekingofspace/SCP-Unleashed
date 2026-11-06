package net.rai.scp_unleashed.particleemitter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.particleemitter.ParticleEmitterBlockEntity;

public class ParticleEmitterRenderer implements BlockEntityRenderer<ParticleEmitterBlockEntity> {
    public static final int ViewDistance = 96;

    public ParticleEmitterRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(ParticleEmitterBlockEntity Emitter, float Partial, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        switch (Emitter.getMode()) {
            case ColdFloor -> ColdFloorRenderer.render(Emitter, Partial, Pose, Buffers);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(ParticleEmitterBlockEntity Emitter) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return ViewDistance;
    }

    @Override
    public AABB getRenderBoundingBox(ParticleEmitterBlockEntity Emitter) {
        ColdFloorEffect Fog = ColdFloorEffect.of(Emitter);
        AABB Bounds = Fog == null ? null : Fog.bounds();
        return Bounds != null ? Bounds : new AABB(Emitter.getBlockPos());
    }
}
