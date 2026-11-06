package net.rai.scp_unleashed.props.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.props.PropShapes;
import net.rai.scp_unleashed.props.SecurityCameraBlock;
import net.rai.scp_unleashed.props.SecurityCameraBlockEntity;

public class SecurityCameraRenderer implements BlockEntityRenderer<SecurityCameraBlockEntity> {
    public static final ModelResourceLocation HeadModel = PropModels.standalone("block/props/security_camera/head");
    public static final float SweepDegrees = 38.0f;
    public static final float TiltDegrees = 22.0f;
    public static final long PeriodTicks = 260L;
    public static final float HeadPivotY = 10.0f;
    public static final float HeadPivotZ = 8.0f;
    public static final float WallPivotY = 11.0f;
    public static final float WallPivotZ = 10.0f;
    public static final float HangingPivotY = 11.0f;
    public static final float HangingPivotZ = 8.0f;

    private final RandomSource Random = RandomSource.create();

    public SecurityCameraRenderer(BlockEntityRendererProvider.Context Context) {
    }

    public static float sweep(long GameTime, float PartialTick, float Phase) {
        float Cycle = ((GameTime % (PeriodTicks * 16L)) + PartialTick) / PeriodTicks + Phase;
        return SweepDegrees * Mth.clamp(1.35f * Mth.sin(Cycle * Mth.TWO_PI), -1.0f, 1.0f);
    }

    @Override
    public void render(SecurityCameraBlockEntity Camera, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Camera.getLevel();
        BlockState State = Camera.getBlockState();
        if (World == null || !(State.getBlock() instanceof SecurityCameraBlock)) {
            return;
        }
        boolean Hanging = State.getValue(SecurityCameraBlock.Hanging);
        Direction Facing = State.getValue(SecurityCameraBlock.Facing);
        float Yaw = sweep(World.getGameTime(), PartialTick, Camera.getPhase());
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(-90.0f * PropShapes.quarterTurns(Facing)));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        Pose.translate(0.5f, (Hanging ? HangingPivotY : WallPivotY) / 16.0f, (Hanging ? HangingPivotZ : WallPivotZ) / 16.0f);
        Pose.mulPose(Axis.YP.rotationDegrees(Yaw));
        Pose.mulPose(Axis.XP.rotationDegrees(-TiltDegrees));
        Pose.translate(-0.5f, -HeadPivotY / 16.0f, -HeadPivotZ / 16.0f);
        PropModels.render(Pose.last(), Buffers.getBuffer(RenderType.cutout()), PropModels.get(HeadModel), World, PackedLight, PackedOverlay, Random);
        Pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SecurityCameraBlockEntity Camera) {
        return new AABB(Camera.getBlockPos()).inflate(0.25);
    }
}
