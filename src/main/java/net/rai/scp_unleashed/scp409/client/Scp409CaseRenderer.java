package net.rai.scp_unleashed.scp409.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.scp409.Scp409CaseBlock;
import net.rai.scp_unleashed.scp409.Scp409CaseBlockEntity;

public class Scp409CaseRenderer implements BlockEntityRenderer<Scp409CaseBlockEntity> {
    public static final float Pixel = 1.0f / 16.0f;
    public static final float Shown = 0.002f;

    public Scp409CaseRenderer(BlockEntityRendererProvider.Context Context) {
    }

    public static float turn(Direction Facing) {
        return 180.0f - Facing.toYRot();
    }

    @Override
    public void render(Scp409CaseBlockEntity Case, float Partial, PoseStack Pose, MultiBufferSource Buffers, int Light, int Overlay) {
        BlockState State = Case.getBlockState();
        if (!State.hasProperty(Scp409CaseBlock.Facing)) {
            return;
        }
        float Open = Case.openness(Partial);
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(turn(State.getValue(Scp409CaseBlock.Facing))));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        draw(Pose, Buffers, Light, Open);
        Pose.popPose();
    }

    public static void draw(PoseStack Pose, MultiBufferSource Buffers, int Light, float Open) {
        Scp409Crystals.solid(Pose, Buffers, Scp409Crystals.CaseBase, Light);
        if (Open > Shown) {
            Scp409Crystals.crystal(Pose, Buffers, Scp409Crystals.CaseShell, Scp409Crystals.CaseCore, Light, true);
        }
        Pose.pushPose();
        Pose.translate(0.0f, (float) Scp409CaseBlock.HingeY * Pixel, (float) Scp409CaseBlock.HingeZ * Pixel);
        Pose.mulPose(Axis.XP.rotationDegrees(Scp409CaseBlock.OpenAngle * Open));
        Pose.translate(0.0f, (float) -Scp409CaseBlock.HingeY * Pixel, (float) -Scp409CaseBlock.HingeZ * Pixel);
        Scp409Crystals.solid(Pose, Buffers, Scp409Crystals.CaseLid, Light);
        Pose.popPose();
    }
}
