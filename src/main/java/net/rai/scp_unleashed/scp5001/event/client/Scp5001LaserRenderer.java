package net.rai.scp_unleashed.scp5001.event.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp5001.core.Scp5001State;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001DeviceBlock;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001DeviceBlockEntity;
import net.rai.scp_unleashed.scp5001.event.block.Scp5001LaserEmitterBlock;
import net.rai.scp_unleashed.scp5001.event.entity.Scp5001Vessel;
import org.joml.Matrix4f;

public class Scp5001LaserRenderer implements BlockEntityRenderer<Scp5001DeviceBlockEntity> {
    public static final float LensDepth = 5.5f / 16.0f;
    public static final int ViewDistance = 160;

    public Scp5001LaserRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(Scp5001DeviceBlockEntity Laser, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light, int Overlay) {
        BlockState State = Laser.getBlockState();
        if (!State.hasProperty(Scp5001LaserEmitterBlock.Firing) || !State.getValue(Scp5001LaserEmitterBlock.Firing)) {
            return;
        }
        Scp5001Vessel Vessel = Scp5001Beams.vessel();
        if (Vessel == null) {
            return;
        }
        BlockPos Pos = Laser.getBlockPos();
        Direction Facing = State.getValue(Scp5001DeviceBlock.Facing);
        Vec3 Lens = Scp5001RodRenderer.socket(Pos, Facing, LensDepth);
        int Seed = Pos.hashCode();
        Vec3 Base = Scp5001Beams.vesselAt(Vessel, Partial);
        double Height = 4.0 + Math.floorMod(Seed, 7);
        Vec3 Axis = new Vec3(Base.x, Base.y + Height, Base.z);
        Vec3 Toward = Lens.subtract(Axis);
        Vec3 Flat = new Vec3(Toward.x, 0.0, Toward.z);
        Vec3 Hit = Flat.lengthSqr() < 1.0E-4 ? Axis : Axis.add(Flat.normalize().scale(Scp5001Vessel.HullRadius * 0.8));
        Vec3 Origin = Vec3.atLowerCornerOf(Pos);
        Vec3 From = Lens.subtract(Origin);
        Vec3 To = Hit.subtract(Origin);
        float Time = Scp5001ClientState.now() + Partial;
        boolean Blast = Scp5001ClientState.phase() == Scp5001State.Phase.Resetting;
        float Pulse = 0.85f + 0.15f * Mth.sin(Time * 0.9f + Seed);
        float Scale = Blast ? 2.2f : 1.0f;
        VertexConsumer Buffer = Buffers.getBuffer(RenderType.lightning());
        Matrix4f Matrix = Pose.last().pose();
        Scp5001Beams.ribbon(Matrix, Buffer, From, To, 0.62f * Scale * Pulse, 156, 140, 240, 40);
        Scp5001Beams.ribbon(Matrix, Buffer, From, To, 0.34f * Scale * Pulse, 143, 227, 255, 110);
        Scp5001Beams.ribbon(Matrix, Buffer, From, To, 0.15f * Scale, 239, 252, 255, 235);
        Scp5001Beams.star(Matrix, Buffer, From, 0.38f * Scale * Pulse, Time * 0.1f, 220, 248, 255, 200);
        Scp5001Beams.star(Matrix, Buffer, To, (0.7f + 0.25f * Mth.sin(Time * 1.7f + Seed)) * Scale, Time * 0.31f, 239, 252, 255, 220);
        Scp5001Beams.star(Matrix, Buffer, To, 1.1f * Scale, -Time * 0.17f, 156, 140, 240, 90);
    }

    @Override
    public boolean shouldRenderOffScreen(Scp5001DeviceBlockEntity Laser) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return ViewDistance;
    }

    @Override
    public AABB getRenderBoundingBox(Scp5001DeviceBlockEntity Laser) {
        return new AABB(Laser.getBlockPos()).inflate(48.0);
    }
}
