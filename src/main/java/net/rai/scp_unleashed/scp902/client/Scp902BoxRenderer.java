package net.rai.scp_unleashed.scp902.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.scp902.Scp902BoxBlock;
import net.rai.scp_unleashed.scp902.Scp902BoxBlockEntity;

public class Scp902BoxRenderer implements BlockEntityRenderer<Scp902BoxBlockEntity> {
    public Scp902BoxRenderer(BlockEntityRendererProvider.Context Context) {
    }

    public static float turn(Direction Facing) {
        return 180.0f - Facing.toYRot();
    }

    @Override
    public void render(Scp902BoxBlockEntity Tin, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light, int Overlay) {
        BlockState State = Tin.getBlockState();
        if (!State.hasProperty(Scp902BoxBlock.Facing)) {
            return;
        }
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(turn(State.getValue(Scp902BoxBlock.Facing))));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        Scp902Models.draw(Pose, Buffers, Light, Overlay, Tin.lidAngle(Partial));
        Pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(Scp902BoxBlockEntity Tin) {
        return new AABB(Tin.getBlockPos()).inflate(0.5, 0.0, 0.5).expandTowards(0.0, 0.5, 0.0);
    }
}
