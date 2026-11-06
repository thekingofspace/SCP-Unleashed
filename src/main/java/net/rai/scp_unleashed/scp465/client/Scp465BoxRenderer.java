package net.rai.scp_unleashed.scp465.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.scp465.Scp465BoxBlock;
import net.rai.scp_unleashed.scp465.Scp465BoxBlockEntity;
import net.rai.scp_unleashed.scp465.Scp465Lid;

public class Scp465BoxRenderer implements BlockEntityRenderer<Scp465BoxBlockEntity> {
    public static final int ViewDistance = 96;

    public Scp465BoxRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(Scp465BoxBlockEntity Carton, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light, int Overlay) {
        BlockState State = Carton.getBlockState();
        if (!(State.getBlock() instanceof Scp465BoxBlock)) {
            return;
        }
        Pose.pushPose();
        Pose.mulPose(Scp465Lid.transform(State.getValue(Scp465BoxBlock.Facing), Carton.lidAngle(Partial)));
        Scp465BoxModels.lid(Pose, Buffers, Light, Overlay);
        Pose.popPose();
    }

    @Override
    public int getViewDistance() {
        return ViewDistance;
    }

    @Override
    public AABB getRenderBoundingBox(Scp465BoxBlockEntity Carton) {
        return new AABB(Carton.getBlockPos()).inflate(0.5, 0.0, 0.5).expandTowards(0.0, 0.8, 0.0);
    }
}
