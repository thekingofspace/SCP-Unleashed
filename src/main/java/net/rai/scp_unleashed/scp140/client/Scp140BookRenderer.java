package net.rai.scp_unleashed.scp140.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.scp140.Scp140BookBlock;
import net.rai.scp_unleashed.scp140.Scp140BookBlockEntity;

public class Scp140BookRenderer implements BlockEntityRenderer<Scp140BookBlockEntity> {
    public Scp140BookRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(Scp140BookBlockEntity Book, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Book.getLevel();
        BlockState State = Book.getBlockState();
        if (World == null || !(State.getBlock() instanceof Scp140BookBlock)) {
            return;
        }
        Direction Facing = State.getValue(Scp140BookBlock.FACING);
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(-Facing.toYRot()));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        Scp140BookModels.render(Pose, Buffers, World, Book.getMotion().open(PartialTick), Book.getMotion().leaf(PartialTick), Book.isInked(), PackedLight, PackedOverlay);
        Pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(Scp140BookBlockEntity Book) {
        return Book.getRenderBounds();
    }
}
