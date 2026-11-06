package net.rai.scp_unleashed.warhead.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.warhead.WarheadGeometry;
import net.rai.scp_unleashed.warhead.WarheadPanelBlock;
import net.rai.scp_unleashed.warhead.WarheadPanelBlockEntity;
import org.joml.Vector3f;

import java.util.List;
import java.util.Locale;

public class WarheadPanelRenderer implements BlockEntityRenderer<WarheadPanelBlockEntity> {
    public static final ModelResourceLocation LeverModel = part("panel_lever");
    public static final ModelResourceLocation CoverModel = part("panel_cover");
    public static final ModelResourceLocation ButtonModel = part("panel_button");
    public static final ModelResourceLocation AbortModel = part("panel_abort");
    public static final List<ModelResourceLocation> PartModels = List.of(LeverModel, CoverModel, ButtonModel, AbortModel);
    private static final ResourceLocation Glow = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
    private static final Direction[] QuadSides = {Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, null};
    private static final int[][] Segments = {{1, 6, 3, 7}, {3, 4, 4, 6}, {3, 1, 4, 3}, {1, 0, 3, 1}, {0, 1, 1, 3}, {0, 4, 1, 6}, {1, 3, 3, 4}};
    private static final int[] DigitMasks = {0b0111111, 0b0000110, 0b1011011, 0b1001111, 0b1100110, 0b1101101, 0b1111101, 0b0000111, 0b1111111, 0b1101111};
    private static final int DashMask = 0b1000000;
    private static final int[] SegmentOn = {255, 60, 40};
    private static final int[] SegmentOff = {58, 16, 12};
    private static final int[] GreenOn = {90, 255, 110};
    private static final int[] GreenOff = {24, 54, 30};
    private static final int[] AmberOn = {255, 176, 40};
    private static final int[] AmberOff = {60, 40, 14};
    private static final int[] RedOn = {255, 52, 36};
    private static final int[] RedOff = {62, 18, 14};
    private static final int[] LcdText = {130, 255, 150};
    private static final String GlyphOrder = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-.:!";
    private static final int[] Glyphs = {
            0b010101111101101, 0b110101110101110, 0b011100100100011, 0b110101101101110, 0b111100110100111, 0b111100110100100, 0b011100101101011,
            0b101101111101101, 0b111010010010111, 0b001001001101010, 0b101101110101101, 0b100100100100111, 0b101111111101101, 0b110101101101101,
            0b010101101101010, 0b110101110100100, 0b010101101110011, 0b110101110101101, 0b011100010001110, 0b111010010010010, 0b101101101101111,
            0b101101101101010, 0b101101101111101, 0b101101010101101, 0b101101010010010, 0b111001010100111,
            0b111101101101111, 0b010110010010111, 0b110001010100111, 0b110001010001110, 0b101101111001001, 0b111100110001110, 0b011100111101111,
            0b111001010010010, 0b111101111101111, 0b111101111001110,
            0b000000111000000, 0b000000000000010, 0b000010000010000, 0b010010010000010};
    private final RandomSource Random = RandomSource.create();

    public WarheadPanelRenderer(BlockEntityRendererProvider.Context Context) {
    }

    private static ModelResourceLocation part(String Name) {
        return ModelResourceLocation.standalone(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/warhead/" + Name));
    }

    @Override
    public void render(WarheadPanelBlockEntity Panel, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Panel.getLevel();
        BlockState State = Panel.getBlockState();
        if (World == null || !(State.getBlock() instanceof WarheadPanelBlock)) {
            return;
        }
        Direction Front = State.getValue(WarheadPanelBlock.Facing);
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(WarheadGeometry.yaw(Front)));
        Pose.translate(-0.5f - WarheadGeometry.OriginColumn, 0.0f, -0.5f);
        Pose.scale(1.0f / 16.0f, 1.0f / 16.0f, 1.0f / 16.0f);
        VertexConsumer Solid = Buffers.getBuffer(RenderType.solid());
        drawPart(World, Pose, Solid, LeverModel, WarheadGeometry.LeverPivot, Panel.leverAngle(PartialTick), 0.0f, 0.5f, PackedLight, PackedOverlay);
        drawPart(World, Pose, Solid, CoverModel, WarheadGeometry.CoverHinge, Panel.coverAngle(PartialTick), 0.0f, 0.25f, PackedLight, PackedOverlay);
        drawPart(World, Pose, Solid, ButtonModel, WarheadGeometry.ButtonOrigin, 0.0f, Panel.buttonDepth(PartialTick), 0.5f, PackedLight, PackedOverlay);
        drawPart(World, Pose, Solid, AbortModel, WarheadGeometry.AbortOrigin, 0.0f, Panel.abortDepth(PartialTick), 0.5f, PackedLight, PackedOverlay);
        VertexConsumer Lights = Buffers.getBuffer(RenderType.entityCutoutNoCull(Glow));
        PoseStack.Pose Last = Pose.last();
        long Now = World.getGameTime();
        float Clock = Now + PartialTick;
        int Left = Panel.ticksLeft(Now);
        boolean Counting = Left > 0 || Panel.isEmitting();
        digits(Lights, Last, readout(Panel, Counting, Left));
        lamps(Lights, Last, Panel, Counting, Clock);
        boolean Open = Panel.isUnlocked() || Counting;
        quad(Lights, Last, WarheadGeometry.LedU, WarheadGeometry.LedV, WarheadGeometry.LedU + 1.0f, WarheadGeometry.LedV + 1.0f, WarheadGeometry.LedDepth, Open ? GreenOn : RedOn);
        text(Lights, Last, lcdLine(Panel, Counting, Now), WarheadGeometry.LcdLeft, WarheadGeometry.LcdBase, WarheadGeometry.LcdHeight);
        Pose.popPose();
    }

    private void drawPart(Level World, PoseStack Pose, VertexConsumer Consumer, ModelResourceLocation Location, Vec3 Pivot, float Angle, float Push, float PivotY, int PackedLight, int PackedOverlay) {
        BakedModel Model = Minecraft.getInstance().getModelManager().getModel(Location);
        Pose.pushPose();
        Pose.translate(Pivot.x, Pivot.y, Pivot.z + Push);
        Pose.mulPose(Axis.XP.rotationDegrees(Angle));
        Pose.scale(16.0f, 16.0f, 16.0f);
        Pose.translate(-0.5f, -PivotY, -0.5f);
        PoseStack.Pose Last = Pose.last();
        for (Direction Side : QuadSides) {
            Random.setSeed(42L);
            for (BakedQuad Quad : Model.getQuads(null, Side, Random, ModelData.EMPTY, null)) {
                Direction Facing = Quad.getDirection();
                Vector3f Normal = Last.normal().transform(new Vector3f(Facing.getStepX(), Facing.getStepY(), Facing.getStepZ()));
                float Shade = World.getShade(Direction.getNearest(Normal.x(), Normal.y(), Normal.z()), Quad.isShade());
                Consumer.putBulkData(Last, Quad, Shade, Shade, Shade, 1.0f, PackedLight, PackedOverlay);
            }
        }
        Pose.popPose();
    }

    private static String readout(WarheadPanelBlockEntity Panel, boolean Counting, int Left) {
        if (Counting) {
            return String.format("%02d", Math.min(99, (Left + 19) / 20));
        }
        return Panel.isLeverThrown() ? "90" : "--";
    }

    private static boolean blink(float Clock, float Period) {
        return (Clock % Period) < Period * 0.5f;
    }

    private static void lamps(VertexConsumer Consumer, PoseStack.Pose Last, WarheadPanelBlockEntity Panel, boolean Counting, float Clock) {
        int[][] Colours = new int[4][];
        Colours[0] = Panel.getLinked() > 0 ? GreenOn : blink(Clock, 30.0f) ? RedOn : RedOff;
        Colours[1] = Panel.isUnlocked() || Counting ? GreenOn : GreenOff;
        Colours[2] = Panel.isLocked() ? RedOn : Panel.isLeverThrown() || Counting ? AmberOn : AmberOff;
        Colours[3] = Counting ? blink(Clock, Panel.isLocked() ? 6.0f : 20.0f) ? RedOn : RedOff : RedOff;
        for (int Index = 0; Index < 4; Index++) {
            float U = WarheadGeometry.LampLefts[Index];
            quad(Consumer, Last, U, WarheadGeometry.LampRow, U + 2.0f, WarheadGeometry.LampRow + 2.0f, WarheadGeometry.FaceDepth, Colours[Index]);
        }
    }

    private static void digits(VertexConsumer Consumer, PoseStack.Pose Last, String Text) {
        for (int Index = 0; Index < Math.min(2, Text.length()); Index++) {
            char Character = Text.charAt(Index);
            int Mask = Character == '-' ? DashMask : Character >= '0' && Character <= '9' ? DigitMasks[Character - '0'] : 0;
            float Origin = WarheadGeometry.DigitLefts[Index];
            for (int Segment = 0; Segment < Segments.length; Segment++) {
                int[] Box = Segments[Segment];
                quad(Consumer, Last, Origin + Box[0], WarheadGeometry.DigitBase + Box[1], Origin + Box[2], WarheadGeometry.DigitBase + Box[3], WarheadGeometry.FaceDepth,
                        (Mask & (1 << Segment)) != 0 ? SegmentOn : SegmentOff);
            }
        }
    }

    private static String lcdLine(WarheadPanelBlockEntity Panel, boolean Counting, long Now) {
        int Note = Panel.noteAt(Now);
        String Key;
        boolean Count = false;
        if (Note == WarheadPanelBlockEntity.NoteCancelled) {
            Key = "cancelled";
        } else if (Note == WarheadPanelBlockEntity.NoteDetonated) {
            Key = "detonated";
        } else if (Note == WarheadPanelBlockEntity.NoteFailed) {
            Key = "failed";
        } else if (Counting) {
            Key = Panel.isForeign() ? "remote" : Panel.isLocked() ? "locked" : "counting";
        } else if (Panel.getLinked() == 0) {
            Key = "no_link";
        } else {
            Count = true;
            Key = Panel.isLeverThrown() && Panel.isCoverOpen() ? "ready" : Panel.isLeverThrown() ? "armed" : Panel.isUnlocked() ? "key_ok" : "sealed";
        }
        String Line = Component.translatable("lcd.scp_unleashed.warhead." + Key).getString();
        if (Count) {
            Line = Line + " " + Component.translatable("lcd.scp_unleashed.warhead.linked", Panel.getLinked()).getString();
        }
        return Line.toUpperCase(Locale.ROOT);
    }

    private static void text(VertexConsumer Consumer, PoseStack.Pose Last, String Line, float Left, float Base, float Height) {
        float Cell = Height / 5.0f;
        float Cursor = Left;
        for (int Index = 0; Index < Line.length() && Cursor < 29.0f - Cell * 3.0f; Index++) {
            int Slot = GlyphOrder.indexOf(Line.charAt(Index));
            if (Slot >= 0) {
                int Bits = Glyphs[Slot];
                for (int Row = 0; Row < 5; Row++) {
                    for (int Column = 0; Column < 3; Column++) {
                        if ((Bits >> (14 - (Row * 3 + Column)) & 1) != 0) {
                            float U = Cursor + Column * Cell;
                            float V = Base + (4 - Row) * Cell;
                            quad(Consumer, Last, U, V, U + Cell, V + Cell, WarheadGeometry.FaceDepth, LcdText);
                        }
                    }
                }
            }
            Cursor += Cell * 4.0f;
        }
    }

    private static void quad(VertexConsumer Consumer, PoseStack.Pose Last, float U0, float V0, float U1, float V1, float Depth, int[] Colour) {
        float X0 = 32.0f - U1;
        float X1 = 32.0f - U0;
        vertex(Consumer, Last, X0, V0, Depth, 0.0f, 1.0f, Colour);
        vertex(Consumer, Last, X1, V0, Depth, 1.0f, 1.0f, Colour);
        vertex(Consumer, Last, X1, V1, Depth, 1.0f, 0.0f, Colour);
        vertex(Consumer, Last, X0, V1, Depth, 0.0f, 0.0f, Colour);
    }

    private static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, float X, float Y, float Z, float U, float V, int[] Colour) {
        Consumer.addVertex(Last, X, Y, Z)
                .setColor(Colour[0], Colour[1], Colour[2], 255)
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(Last, 0.0f, 0.0f, -1.0f);
    }

    @Override
    public boolean shouldRenderOffScreen(WarheadPanelBlockEntity Panel) {
        return true;
    }

    @Override
    public AABB getRenderBoundingBox(WarheadPanelBlockEntity Panel) {
        return Panel.getRenderBounds();
    }
}
