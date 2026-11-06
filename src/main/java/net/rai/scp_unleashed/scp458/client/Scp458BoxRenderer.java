package net.rai.scp_unleashed.scp458.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.scp458.Scp458BoxBlock;
import net.rai.scp_unleashed.scp458.Scp458BoxBlockEntity;
import net.rai.scp_unleashed.scp458.Scp458Contents;

public class Scp458BoxRenderer implements BlockEntityRenderer<Scp458BoxBlockEntity> {
    public Scp458BoxRenderer(BlockEntityRendererProvider.Context Context) {
    }

    public static float turn(Direction Facing) {
        return 180.0f - Facing.toYRot();
    }

    @Override
    public void render(Scp458BoxBlockEntity Carton, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light, int Overlay) {
        BlockState State = Carton.getBlockState();
        if (!State.hasProperty(Scp458BoxBlock.Facing)) {
            return;
        }
        Scp458Contents Held = Carton.contents();
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(turn(State.getValue(Scp458BoxBlock.Facing))));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        Scp458Models.draw(Pose, Buffers, Light, Overlay, Carton.lidAngle(Partial), Held.Pizza().orElse(null), Held.Taken());
        Pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(Scp458BoxBlockEntity Carton) {
        return new AABB(Carton.getBlockPos()).inflate(1.0, 0.0, 1.0).expandTowards(0.0, 1.0, 0.0);
    }
}
