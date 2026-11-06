package net.rai.scp_unleashed.door.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
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
import net.rai.scp_unleashed.door.DoorGeometry;
import net.rai.scp_unleashed.door.DoorKind;
import net.rai.scp_unleashed.door.PanelDoorBlock;
import net.rai.scp_unleashed.door.PanelDoorBlockEntity;

import java.util.ArrayList;
import java.util.List;

public class PanelDoorRenderer implements BlockEntityRenderer<PanelDoorBlockEntity> {
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    public static final int LampFlashTicks = 5;
    private final RandomSource Random = RandomSource.create();

    public PanelDoorRenderer(BlockEntityRendererProvider.Context Context) {
    }

    public static ModelResourceLocation tileModel(DoorKind Kind, int Column, int Row, int LeafIndex) {
        boolean Shared = DoorGeometry.tileLeaves(Kind, Column, Row).length > 1;
        return cellModel(Kind, "leaf", Column, Row, Shared ? "_" + LeafIndex : "");
    }

    public static ModelResourceLocation glassModel(DoorKind Kind, int Column, int Row) {
        return cellModel(Kind, "glass", Column, Row, "");
    }

    public static ModelResourceLocation lampModel(DoorKind Kind, int Column, int Row) {
        return cellModel(Kind, "lamp", Column, Row, "");
    }

    private static ModelResourceLocation cellModel(DoorKind Kind, String Layer, int Column, int Row, String Suffix) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId,
                "block/door/" + Kind.getSerializedName() + "/" + Layer + "_" + Column + "_" + Row + Suffix));
    }

    public static List<ModelResourceLocation> allTileModels() {
        List<ModelResourceLocation> Models = new ArrayList<>();
        for (DoorKind Kind : DoorKind.values()) {
            for (int Part = 0; Part < Kind.getCellCount(); Part++) {
                int Column = Kind.columnOf(Part);
                int Row = Kind.rowOf(Part);
                int[] Indices = DoorGeometry.tileLeaves(Kind, Column, Row);
                for (int LeafIndex : Indices) {
                    Models.add(tileModel(Kind, Column, Row, LeafIndex));
                }
                if (Kind.isGlazed() && Indices.length == 1) {
                    Models.add(glassModel(Kind, Column, Row));
                }
            }
            for (int[] Cell : DoorGeometry.lampCells(Kind)) {
                Models.add(lampModel(Kind, Cell[0], Cell[1]));
            }
        }
        return Models;
    }

    @Override
    public void render(PanelDoorBlockEntity Door, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Door.getLevel();
        BlockState State = Door.getBlockState();
        if (World == null || !(State.getBlock() instanceof PanelDoorBlock Frame)) {
            return;
        }
        DoorKind Kind = Frame.getKind();
        Direction Front = State.getValue(PanelDoorBlock.Facing);
        int Turns = DoorGeometry.quarterTurns(Front);
        float Eased = DoorGeometry.ease(Door.getProgress(PartialTick));
        BlockPos Origin = Door.getBlockPos();
        ModelManager Models = Minecraft.getInstance().getModelManager();
        VertexConsumer Consumer = Buffers.getBuffer(RenderType.solid());
        VertexConsumer Glazing = Kind.isGlazed() ? Buffers.getBuffer(Sheets.translucentCullBlockSheet()) : null;
        List<DoorGeometry.Leaf> Leaves = DoorGeometry.leaves(Kind);
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(-90.0f * Turns));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        for (int Part = 0; Part < Kind.getCellCount(); Part++) {
            int Column = Kind.columnOf(Part);
            int Row = Kind.rowOf(Part);
            int[] Indices = DoorGeometry.tileLeaves(Kind, Column, Row);
            for (int LeafIndex : Indices) {
                DoorGeometry.Leaf Moving = Leaves.get(LeafIndex);
                double ShiftU = Moving.TravelU() * Eased;
                double ShiftV = Moving.TravelV() * Eased;
                if (isBuried(World, Origin, Front, Column, Row, ShiftU, ShiftV)) {
                    continue;
                }
                int LightColumn = Mth.clamp(Column + (int) Math.round(ShiftU / 16.0), 0, Kind.getWidth() - 1);
                int LightRow = Mth.clamp(Row + (int) Math.round(ShiftV / 16.0), 0, Kind.getHeight() - 1);
                int Light = LevelRenderer.getLightColor(World, PanelDoorBlock.cellPos(Origin, Front, LightColumn, LightRow));
                BakedModel Model = Models.getModel(tileModel(Kind, Column, Row, LeafIndex));
                Pose.pushPose();
                Pose.translate(-Column - ShiftU / 16.0, Row + ShiftV / 16.0, 0.0);
                renderTile(Pose.last(), Consumer, Model, World, Turns, Light, PackedOverlay, true);
                if (Glazing != null && Indices.length == 1) {
                    renderTile(Pose.last(), Glazing, Models.getModel(glassModel(Kind, Column, Row)), World, Turns, Light, PackedOverlay, false);
                }
                Pose.popPose();
            }
        }
        if (Kind.hasWarningLamps() && Door.isTravelling()) {
            boolean LeftLit = (World.getGameTime() / LampFlashTicks) % 2 == 0;
            for (int[] Cell : DoorGeometry.lampCells(Kind)) {
                if ((Cell[0] < Kind.getWidth() / 2) != LeftLit) {
                    continue;
                }
                Pose.pushPose();
                Pose.translate(-Cell[0], Cell[1], 0.0);
                renderTile(Pose.last(), Consumer, Models.getModel(lampModel(Kind, Cell[0], Cell[1])), World, Turns, LightTexture.FULL_BRIGHT, PackedOverlay, false);
                Pose.popPose();
            }
        }
        Pose.popPose();
    }

    private static boolean isBuried(Level World, BlockPos Origin, Direction Front, int Column, int Row, double ShiftU, double ShiftV) {
        if (Math.abs(ShiftU % 16.0) > 1.0E-4 || Math.abs(ShiftV % 16.0) > 1.0E-4) {
            return false;
        }
        BlockPos Cell = PanelDoorBlock.cellPos(Origin, Front, Column + (int) Math.round(ShiftU / 16.0), Row + (int) Math.round(ShiftV / 16.0));
        BlockState Occupant = World.getBlockState(Cell);
        return !(Occupant.getBlock() instanceof PanelDoorBlock) && Occupant.isSolidRender(World, Cell);
    }

    private void renderTile(PoseStack.Pose Last, VertexConsumer Consumer, BakedModel Model, Level World, int Turns, int Light, int Overlay, boolean Shaded) {
        for (Direction Side : QuadSides) {
            Random.setSeed(42L);
            for (BakedQuad Quad : Model.getQuads(null, Side, Random, ModelData.EMPTY, null)) {
                float Shade = Shaded ? World.getShade(turned(Quad.getDirection(), Turns), Quad.isShade()) : 1.0f;
                Consumer.putBulkData(Last, Quad, Shade, Shade, Shade, 1.0f, Light, Overlay);
            }
        }
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
    public boolean shouldRenderOffScreen(PanelDoorBlockEntity Door) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(PanelDoorBlockEntity Door) {
        return Door.getRenderBounds();
    }
}
