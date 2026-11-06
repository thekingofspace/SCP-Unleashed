package net.rai.scp_unleashed.door.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.door.DoorLeverBlock;
import net.rai.scp_unleashed.door.DoorLeverBlockEntity;
import net.rai.scp_unleashed.door.DoorLeverGeometry;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DoorLeverRenderer implements BlockEntityRenderer<DoorLeverBlockEntity> {
    public static final ModelResourceLocation HandleModel = ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/door/door_lever/handle"));
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private final RandomSource Random = RandomSource.create();

    public DoorLeverRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(DoorLeverBlockEntity Lever, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Lever.getLevel();
        BlockState State = Lever.getBlockState();
        if (World == null || !(State.getBlock() instanceof DoorLeverBlock)) {
            return;
        }
        int RotX = DoorLeverBlock.rotationX(State);
        int RotY = DoorLeverBlock.rotationY(State);
        float Angle = Lever.getAngle(PartialTick);
        float Pivot = (float) (DoorLeverGeometry.PivotY / 16.0);
        Quaternionf Turn = new Quaternionf().rotateY(-RotY * Mth.DEG_TO_RAD).rotateX((Angle - RotX) * Mth.DEG_TO_RAD);
        BakedModel Model = Minecraft.getInstance().getModelManager().getModel(HandleModel);
        VertexConsumer Consumer = Buffers.getBuffer(RenderType.solid());
        Pose.pushPose();
        Pose.translate(0.5f, 0.5f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(-RotY));
        Pose.mulPose(Axis.XP.rotationDegrees(-RotX));
        Pose.translate(0.0f, Pivot - 0.5f, 0.0f);
        Pose.mulPose(Axis.XP.rotationDegrees(Angle));
        Pose.translate(-0.5f, -Pivot, -0.5f);
        PoseStack.Pose Last = Pose.last();
        for (Direction Side : QuadSides) {
            Random.setSeed(42L);
            for (BakedQuad Quad : Model.getQuads(null, Side, Random, ModelData.EMPTY, null)) {
                Direction Facing = Quad.getDirection();
                Vector3f Normal = Turn.transform(new Vector3f(Facing.getStepX(), Facing.getStepY(), Facing.getStepZ()));
                float Shade = World.getShade(Direction.getNearest(Normal.x(), Normal.y(), Normal.z()), Quad.isShade());
                Consumer.putBulkData(Last, Quad, Shade, Shade, Shade, 1.0f, PackedLight, PackedOverlay);
            }
        }
        Pose.popPose();
    }
}
