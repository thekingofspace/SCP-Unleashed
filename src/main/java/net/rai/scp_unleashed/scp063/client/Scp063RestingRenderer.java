package net.rai.scp_unleashed.scp063.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.scp063.Scp063RestingBlock;
import net.rai.scp_unleashed.scp063.Scp063RestingBlockEntity;

public class Scp063RestingRenderer implements BlockEntityRenderer<Scp063RestingBlockEntity> {
    public static final float Scale = 0.42f;
    public static final float ModelBottom = 7.0f;
    public static final float ModelMiddle = 8.0f;
    public static final float ModelCentreX = 8.5f;
    public static final float Lift = 0.004f;
    public static final float Tilt = 24.0f;
    public static final float ScrubPeriod = 6.0f;
    public static final float ScrubReach = 0.035f;
    public static final float ScrubWobble = 3.0f;

    public Scp063RestingRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(Scp063RestingBlockEntity Rest, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int Light, int Overlay) {
        Level World = Rest.getLevel();
        if (World == null) {
            return;
        }
        float Yaw = 180.0f - Rest.getBlockState().getValue(Scp063RestingBlock.Facing).toYRot() + Tilt;
        float Slide = 0.0f;
        float Wobble = 0.0f;
        if (Rest.isActive()) {
            float Time = (World.getGameTime() % 72000L) + PartialTick;
            float Wave = Mth.sin(Time * Mth.TWO_PI / ScrubPeriod);
            Slide = Wave * ScrubReach;
            Wobble = Mth.cos(Time * Mth.TWO_PI / ScrubPeriod) * ScrubWobble;
        }
        Pose.pushPose();
        Pose.translate(0.5f, (ModelMiddle - ModelBottom) / 16.0f * Scale + Lift, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(Yaw));
        Pose.translate(Slide - (ModelCentreX - ModelMiddle) / 16.0f * Scale, 0.0f, 0.0f);
        Pose.mulPose(Axis.XP.rotationDegrees(Wobble));
        Pose.scale(Scale, Scale, Scale);
        Minecraft.getInstance().getItemRenderer().renderStatic(Rest.kept(), ItemDisplayContext.NONE, Light, Overlay, Pose, Buffers, World, (int) Rest.getBlockPos().asLong());
        Pose.popPose();
    }
}
