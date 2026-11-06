package net.rai.scp_unleashed.scp019.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp019.Scp019PotBlock;
import net.rai.scp_unleashed.scp019.Scp019PotBlockEntity;

public class Scp019PotRenderer implements BlockEntityRenderer<Scp019PotBlockEntity> {
    public static final ModelResourceLocation LidModel = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/scp019/pot_lid"));
    public static final float LiftPixels = 2.5f;
    public static final float LiftShare = 0.35f;
    public static final float SlidePixels = 9.0f;
    public static final float TiltDegrees = 76.0f;
    public static final float PivotX = 3.0f;
    public static final float PivotY = 13.0f;
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private final RandomSource Random = RandomSource.create();

    public Scp019PotRenderer(BlockEntityRendererProvider.Context Context) {
    }

    public static float smooth(float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, 1.0f);
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }

    @Override
    public void render(Scp019PotBlockEntity Pot, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Pot.getLevel();
        BlockState State = Pot.getBlockState();
        if (World == null || !(State.getBlock() instanceof Scp019PotBlock)) {
            return;
        }
        float Progress = Pot.lid(PartialTick);
        float Rise = smooth(Progress / LiftShare);
        float Swing = smooth((Progress - LiftShare) / (1.0f - LiftShare));
        float Up = LiftPixels * Rise * (1.0f - Swing);
        Direction Front = State.getValue(Scp019PotBlock.Facing);
        int Turns = quarterTurns(Front);
        BlockPos Upper = Pot.getBlockPos().above();
        int Light = LevelRenderer.getLightColor(World, Upper);
        BakedModel Model = Minecraft.getInstance().getModelManager().getModel(LidModel);
        VertexConsumer Consumer = Buffers.getBuffer(RenderType.solid());
        Pose.pushPose();
        Pose.translate(0.0f, 1.0f, 0.0f);
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(180.0f - Front.toYRot()));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        Pose.translate(0.0f, Up / 16.0f, 0.0f);
        Pose.translate(PivotX / 16.0f, PivotY / 16.0f, 0.0f);
        Pose.mulPose(Axis.ZP.rotationDegrees(TiltDegrees * Swing));
        Pose.translate(-PivotX / 16.0f, -PivotY / 16.0f, 0.0f);
        Pose.translate(-SlidePixels * Swing / 16.0f, 0.0f, 0.0f);
        for (Direction Side : QuadSides) {
            Random.setSeed(42L);
            for (BakedQuad Quad : Model.getQuads(null, Side, Random, ModelData.EMPTY, null)) {
                float Shade = World.getShade(turned(Quad.getDirection(), Turns), Quad.isShade());
                Consumer.putBulkData(Pose.last(), Quad, Shade, Shade, Shade, 1.0f, Light, PackedOverlay);
            }
        }
        Pose.popPose();
    }

    private static int quarterTurns(Direction Front) {
        return switch (Front) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
    }

    private static Direction turned(Direction Side, int Turns) {
        Direction Result = Side;
        if (Side.getAxis().isHorizontal()) {
            for (int Step = 0; Step < Turns; Step++) {
                Result = Result.getClockWise();
            }
        }
        return Result;
    }

    @Override
    public AABB getRenderBoundingBox(Scp019PotBlockEntity Pot) {
        BlockPos Pos = Pot.getBlockPos();
        return new AABB(Pos.getX() - 1.0, Pos.getY(), Pos.getZ() - 1.0, Pos.getX() + 2.0, Pos.getY() + 2.5, Pos.getZ() + 2.0);
    }
}
