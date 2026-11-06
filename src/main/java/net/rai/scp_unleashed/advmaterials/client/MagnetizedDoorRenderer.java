package net.rai.scp_unleashed.advmaterials.client;

import com.mojang.blaze3d.vertex.PoseStack;
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
import net.neoforged.neoforge.registries.DeferredBlock;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.advmaterials.AdvancedMaterialsRegistry;
import net.rai.scp_unleashed.advmaterials.MagnetizedDoorBlock;
import net.rai.scp_unleashed.advmaterials.MagnetizedDoorBlockEntity;
import net.rai.scp_unleashed.door.DoorGeometry;
import net.rai.scp_unleashed.door.DoorKind;

import java.util.ArrayList;
import java.util.List;

public class MagnetizedDoorRenderer implements BlockEntityRenderer<MagnetizedDoorBlockEntity> {
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private final RandomSource Random = RandomSource.create();

    public MagnetizedDoorRenderer(BlockEntityRendererProvider.Context Context) {
    }

    public static ModelResourceLocation tileModel(String Name, int Column, int Row) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId,
                "block/advmaterials/" + Name + "/leaf_" + Column + "_" + Row));
    }

    public static List<ModelResourceLocation> allTileModels() {
        List<ModelResourceLocation> Models = new ArrayList<>();
        for (DeferredBlock<? extends MagnetizedDoorBlock> Entry : AdvancedMaterialsRegistry.MagnetizedDoors) {
            MagnetizedDoorBlock Door = Entry.get();
            DoorKind Kind = Door.getLayout();
            for (int Part = 0; Part < Kind.getCellCount(); Part++) {
                if (DoorGeometry.leafAt(Kind, Kind.columnOf(Part), Kind.rowOf(Part)) >= 0) {
                    Models.add(tileModel(Door.getIdPath(), Kind.columnOf(Part), Kind.rowOf(Part)));
                }
            }
        }
        return Models;
    }

    @Override
    public void render(MagnetizedDoorBlockEntity Door, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Door.getLevel();
        BlockState State = Door.getBlockState();
        if (World == null || !(State.getBlock() instanceof MagnetizedDoorBlock Frame)) {
            return;
        }
        DoorKind Kind = Frame.getLayout();
        String Name = Frame.getIdPath();
        Direction Front = State.getValue(MagnetizedDoorBlock.Facing);
        int Turns = DoorGeometry.quarterTurns(Front);
        float Eased = DoorGeometry.ease(Door.getProgress(PartialTick));
        BlockPos Origin = Door.getBlockPos();
        ModelManager Models = Minecraft.getInstance().getModelManager();
        List<DoorGeometry.Leaf> Leaves = DoorGeometry.leaves(Kind);
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(-90.0f * Turns));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        for (int Part = 0; Part < Kind.getCellCount(); Part++) {
            int Column = Kind.columnOf(Part);
            int Row = Kind.rowOf(Part);
            int LeafIndex = DoorGeometry.leafAt(Kind, Column, Row);
            if (LeafIndex < 0) {
                continue;
            }
            DoorGeometry.Leaf Moving = Leaves.get(LeafIndex);
            double ShiftU = Moving.TravelU() * Eased;
            double ShiftV = Moving.TravelV() * Eased;
            int LightColumn = Mth.clamp(Column + (int) Math.round(ShiftU / 16.0), 0, Kind.getWidth() - 1);
            int LightRow = Mth.clamp(Row + (int) Math.round(ShiftV / 16.0), 0, Kind.getHeight() - 1);
            int Light = LevelRenderer.getLightColor(World, MagnetizedDoorBlock.cellPos(Origin, Front, LightColumn, LightRow));
            BakedModel Model = Models.getModel(tileModel(Name, Column, Row));
            Pose.pushPose();
            Pose.translate(-Column - ShiftU / 16.0, Row + ShiftV / 16.0, 0.0);
            PoseStack.Pose Last = Pose.last();
            for (Direction Side : QuadSides) {
                Random.setSeed(42L);
                for (BakedQuad Quad : Model.getQuads(null, Side, Random, ModelData.EMPTY, null)) {
                    float Shade = World.getShade(turned(Quad.getDirection(), Turns), Quad.isShade());
                    Buffers.getBuffer(RenderType.solid()).putBulkData(Last, Quad, Shade, Shade, Shade, 1.0f, Light, PackedOverlay);
                }
            }
            Pose.popPose();
        }
        Pose.popPose();
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
    public boolean shouldRenderOffScreen(MagnetizedDoorBlockEntity Door) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(MagnetizedDoorBlockEntity Door) {
        return Door.getRenderBounds();
    }
}
