package net.rai.scp_unleashed.props.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.props.PropShapes;
import net.rai.scp_unleashed.props.WallClockBlock;
import net.rai.scp_unleashed.props.WallClockBlockEntity;

public class WallClockRenderer implements BlockEntityRenderer<WallClockBlockEntity> {
    public static final ModelResourceLocation HourHand = PropModels.standalone("block/props/wall_clock/hour_hand");
    public static final ModelResourceLocation MinuteHand = PropModels.standalone("block/props/wall_clock/minute_hand");

    private final RandomSource Random = RandomSource.create();

    public WallClockRenderer(BlockEntityRendererProvider.Context Context) {
    }

    public static float[] handAngles(Level World, float PartialTick) {
        if (!World.dimensionType().natural()) {
            float Spin = (World.getGameTime() % 72000L) + PartialTick;
            return new float[]{(Spin * 3.1f) % 360.0f, 360.0f - (Spin * 17.3f) % 360.0f};
        }
        long Day = Math.floorMod(World.getDayTime(), 24000L);
        float Hours = (Day / 1000.0f + 6.0f) % 12.0f;
        float Minutes = (Day % 1000L) / 1000.0f;
        return new float[]{Hours * 30.0f, Minutes * 360.0f};
    }

    @Override
    public void render(WallClockBlockEntity Clock, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Clock.getLevel();
        BlockState State = Clock.getBlockState();
        if (World == null || !(State.getBlock() instanceof WallClockBlock)) {
            return;
        }
        float[] Angles = handAngles(World, PartialTick);
        VertexConsumer Consumer = Buffers.getBuffer(RenderType.cutout());
        Pose.pushPose();
        Pose.translate(0.5f, 0.5f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(-90.0f * PropShapes.quarterTurns(State.getValue(WallClockBlock.Facing))));
        renderHand(Pose, Consumer, HourHand, Angles[0], World, PackedLight, PackedOverlay);
        renderHand(Pose, Consumer, MinuteHand, Angles[1], World, PackedLight, PackedOverlay);
        Pose.popPose();
    }

    private void renderHand(PoseStack Pose, VertexConsumer Consumer, ModelResourceLocation Hand, float Angle, Level World, int Light, int Overlay) {
        Pose.pushPose();
        Pose.mulPose(Axis.ZP.rotationDegrees(Angle));
        Pose.translate(-0.5f, -0.5f, -0.5f);
        PropModels.render(Pose.last(), Consumer, PropModels.get(Hand), World, Light, Overlay, Random);
        Pose.popPose();
    }
}
