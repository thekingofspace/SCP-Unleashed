package net.rai.scp_unleashed.scp002.client;

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
import net.minecraft.client.resources.model.ModelManager;
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
import net.rai.scp_unleashed.scp002.BulkheadDoorBlock;
import net.rai.scp_unleashed.scp002.BulkheadDoorBlockEntity;
import net.rai.scp_unleashed.scp002.BulkheadMotion;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;

public class BulkheadDoorRenderer implements BlockEntityRenderer<BulkheadDoorBlockEntity> {
    public static final ModelResourceLocation LeafModel = standalone("door_leaf");
    public static final ModelResourceLocation WheelModel = standalone("door_wheel");
    public static final ModelResourceLocation DogsModel = standalone("door_dogs");
    public static final ModelResourceLocation ArmModel = standalone("door_arm");
    public static final List<ModelResourceLocation> AllModels = List.of(LeafModel, WheelModel, DogsModel, ArmModel);
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private final RandomSource Random = RandomSource.create();

    public BulkheadDoorRenderer(BlockEntityRendererProvider.Context Context) {
    }

    private static ModelResourceLocation standalone(String Name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/scp002/" + Name));
    }

    private static float facingDegrees(Direction Front) {
        return switch (Front) {
            case EAST -> 90.0f;
            case SOUTH -> 180.0f;
            case WEST -> 270.0f;
            default -> 0.0f;
        };
    }

    private static int brightest(Level World, BlockPos First, BlockPos Second) {
        int Lower = LevelRenderer.getLightColor(World, First);
        int Upper = LevelRenderer.getLightColor(World, Second);
        return Math.max(Lower & 0xFFFF, Upper & 0xFFFF) | Math.max(Lower >>> 16, Upper >>> 16) << 16;
    }

    private static int blend(int Outside, int Inside, float Amount) {
        if (Amount <= 0.0f) {
            return Outside;
        }
        if (Amount >= 1.0f) {
            return Inside;
        }
        int BlockPart = Math.round(Mth.lerp(Amount, Outside & 0xFFFF, Inside & 0xFFFF));
        int SkyPart = Math.round(Mth.lerp(Amount, Outside >>> 16, Inside >>> 16));
        return BlockPart | SkyPart << 16;
    }

    private static float shade(Level World, Vector3f Normal, boolean Shaded) {
        if (!Shaded) {
            return World.getShade(Direction.UP, false);
        }
        float Up = World.getShade(Direction.UP, true);
        float Down = World.getShade(Direction.DOWN, true);
        float Vertical = Down + (Up - Down) * (Normal.y() + 1.0f) * 0.5f;
        float Across = World.getShade(Direction.EAST, true);
        float Along = World.getShade(Direction.NORTH, true);
        return Math.min(1.0f, Normal.x() * Normal.x() * Across + Normal.y() * Normal.y() * Vertical + Normal.z() * Normal.z() * Along);
    }

    @Override
    public void render(BulkheadDoorBlockEntity Door, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Door.getLevel();
        BlockState State = Door.getBlockState();
        if (World == null || !(State.getBlock() instanceof BulkheadDoorBlock)) {
            return;
        }
        Direction Front = State.getValue(BulkheadDoorBlock.Facing);
        BlockPos Pos = Door.getBlockPos();
        BlockPos Ahead = Pos.relative(Front);
        BlockPos Behind = Pos.relative(Front.getOpposite());
        int OutsideLight = brightest(World, Ahead, Ahead.above());
        int InsideLight = brightest(World, Behind, Behind.above());
        BulkheadMotion.Pose Motion = Door.getPose(PartialTick);
        float Facing = facingDegrees(Front);
        float Swing = Motion.swingDegrees();
        float PivotX = BulkheadMotion.PivotX / 16.0f;
        float PivotZ = BulkheadMotion.PivotZ / 16.0f;
        float WheelX = BulkheadMotion.WheelX / 16.0f;
        float WheelY = BulkheadMotion.WheelY / 16.0f;
        ModelManager Models = Minecraft.getInstance().getModelManager();
        VertexConsumer Consumer = Buffers.getBuffer(RenderType.solid());
        Quaternionf Swung = new Quaternionf().rotateY(Swing * Mth.DEG_TO_RAD);
        Quaternionf Placed = new Quaternionf().rotateY(-Facing * Mth.DEG_TO_RAD).mul(Swung);
        Quaternionf WheelTurn = new Quaternionf().rotateZ(-Motion.wheelDegrees() * Mth.DEG_TO_RAD);
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(-Facing));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        Pose.translate(PivotX, 0.0f, PivotZ);
        Pose.mulPose(Axis.YP.rotationDegrees(Swing));
        Pose.translate(-PivotX, 0.0f, -PivotZ);
        draw(Pose.last(), Consumer, Models.getModel(ArmModel), World, Placed, Swung, OutsideLight, InsideLight, PackedOverlay);
        Pose.translate(0.0f, 0.0f, -Motion.unseatPixels() / 16.0f);
        draw(Pose.last(), Consumer, Models.getModel(LeafModel), World, Placed, Swung, OutsideLight, InsideLight, PackedOverlay);
        Pose.pushPose();
        Pose.translate(-Motion.dogPixels() / 16.0f, 0.0f, 0.0f);
        draw(Pose.last(), Consumer, Models.getModel(DogsModel), World, Placed, Swung, OutsideLight, InsideLight, PackedOverlay);
        Pose.popPose();
        Pose.pushPose();
        Pose.translate(WheelX, WheelY, 0.0f);
        Pose.mulPose(WheelTurn);
        Pose.translate(-WheelX, -WheelY, 0.0f);
        draw(Pose.last(), Consumer, Models.getModel(WheelModel), World, new Quaternionf(Placed).mul(WheelTurn), new Quaternionf(Swung).mul(WheelTurn), OutsideLight, InsideLight,
                PackedOverlay);
        Pose.popPose();
        Pose.popPose();
    }

    private void draw(PoseStack.Pose Last, VertexConsumer Consumer, BakedModel Model, Level World, Quaternionf Placed, Quaternionf Swung, int OutsideLight, int InsideLight,
                      int Overlay) {
        for (Direction Side : QuadSides) {
            Random.setSeed(42L);
            for (BakedQuad Quad : Model.getQuads(null, Side, Random, ModelData.EMPTY, null)) {
                Direction Facing = Quad.getDirection();
                Vector3f Normal = Placed.transform(new Vector3f(Facing.getStepX(), Facing.getStepY(), Facing.getStepZ()));
                Vector3f Inward = Swung.transform(new Vector3f(Facing.getStepX(), Facing.getStepY(), Facing.getStepZ()));
                float Shade = shade(World, Normal, Quad.isShade());
                int Light = blend(OutsideLight, InsideLight, Mth.clamp(Inward.z(), 0.0f, 1.0f));
                Consumer.putBulkData(Last, Quad, Shade, Shade, Shade, 1.0f, Light, Overlay);
            }
        }
    }

    @Override
    public boolean shouldRenderOffScreen(BulkheadDoorBlockEntity Door) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public AABB getRenderBoundingBox(BulkheadDoorBlockEntity Door) {
        return Door.getRenderBounds();
    }
}
