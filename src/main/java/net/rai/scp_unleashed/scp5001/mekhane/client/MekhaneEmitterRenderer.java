package net.rai.scp_unleashed.scp5001.mekhane.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp5001.mekhane.block.Scp5001SkyEmitterBlock;
import net.rai.scp_unleashed.scp5001.mekhane.block.Scp5001SkyEmitterBlockEntity;

public class MekhaneEmitterRenderer implements BlockEntityRenderer<Scp5001SkyEmitterBlockEntity> {
    public MekhaneEmitterRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(Scp5001SkyEmitterBlockEntity Emitter, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light, int Overlay) {
        boolean Lit = Emitter.getBlockState().getValue(Scp5001SkyEmitterBlock.Lit);
        float Time = MekhaneClientState.ticks() + Partial;
        Vec3 Centre = new Vec3(0.5, 1.02, 0.5);
        float Charge = Lit ? 1.0f : 0.25f;
        MekhaneLight.disc(Buffers, Pose, MekhaneLight.Major, Centre, new Vec3(0.0, 1.0, 0.0), 0.62f, Time * (Lit ? 0.12f : 0.02f), 1.0f, 0.9f, 0.62f, 0.9f * Charge);
        MekhaneLight.disc(Buffers, Pose, MekhaneLight.Minor, Centre.add(0.0, 0.08, 0.0), new Vec3(0.0, 1.0, 0.0), 0.32f, -Time * (Lit ? 0.2f : 0.03f), 1.0f, 0.95f, 0.75f, Charge);
        if (Lit) {
            MekhaneLight.column(Buffers, Pose, new Vec3(0.5, 1.0, 0.5), 6.0f, 0.42f, Time * 0.05f, -Time * 0.1f, 0.9f);
        }
    }

    @Override
    public boolean shouldRenderOffScreen(Scp5001SkyEmitterBlockEntity Emitter) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public AABB getRenderBoundingBox(Scp5001SkyEmitterBlockEntity Emitter) {
        return new AABB(Emitter.getBlockPos()).inflate(2.0, 8.0, 2.0);
    }
}
