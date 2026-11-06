package net.rai.scp_unleashed.labware.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.labware.LabViralStorageBlock;
import net.rai.scp_unleashed.labware.LabViralStorageBlockEntity;

public class LabViralStorageRenderer implements BlockEntityRenderer<LabViralStorageBlockEntity> {
    public static final float HingeY = 15.0f / 16.0f;
    public static final float HingeZ = 13.0f / 16.0f;
    public static final double LidReach = 0.6;

    public LabViralStorageRenderer(BlockEntityRendererProvider.Context Context) {
    }

    private static float yaw(Direction Front) {
        return switch (Front) {
            case EAST -> 90.0f;
            case SOUTH -> 180.0f;
            case WEST -> 270.0f;
            default -> 0.0f;
        };
    }

    @Override
    public void render(LabViralStorageBlockEntity Locker, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Locker.getLevel();
        BlockState State = Locker.getBlockState();
        if (World == null || !(State.getBlock() instanceof LabViralStorageBlock)) {
            return;
        }
        float Angle = Locker.getLidAngle(PartialTick);
        boolean Frosted = State.getValue(LabViralStorageBlock.Chilled);
        ModelManager Models = Minecraft.getInstance().getModelManager();
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(-yaw(State.getValue(LabViralStorageBlock.Facing))));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        Pose.translate(0.0f, HingeY, HingeZ);
        Pose.mulPose(Axis.XP.rotationDegrees(Angle));
        Pose.translate(0.0f, -HingeY, -HingeZ);
        LabwareQuads.render(Pose.last(), Buffers.getBuffer(RenderType.solid()), Models.getModel(LabwareModels.storageLid(Frosted)), World, PackedLight, PackedOverlay);
        Pose.popPose();
    }

    @Override
    public boolean shouldRenderOffScreen(LabViralStorageBlockEntity Locker) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(LabViralStorageBlockEntity Locker) {
        return new AABB(Locker.getBlockPos()).expandTowards(0.0, LidReach, 0.0);
    }
}
