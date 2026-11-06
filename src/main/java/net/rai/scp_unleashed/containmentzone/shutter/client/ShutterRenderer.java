package net.rai.scp_unleashed.containmentzone.shutter.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.containmentzone.CellShutterBlock;
import net.rai.scp_unleashed.containmentzone.shutter.ShutterBlockEntity;
import net.rai.scp_unleashed.containmentzone.shutter.ShutterShapes;
import net.rai.scp_unleashed.props.PropShapes;

public class ShutterRenderer implements BlockEntityRenderer<ShutterBlockEntity> {
    public static final ResourceLocation SlatsTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/containment_room/lockdown_shutter/slats");
    public static final ResourceLocation BarTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "block/containment_room/lockdown_shutter/bar");
    public static final float CurtainFront = 7.0f;
    public static final float CurtainBack = 9.0f;
    public static final float LipFront = 6.0f;
    public static final float LipBack = 10.0f;
    public static final float LipInset = 2.0f;

    public ShutterRenderer(BlockEntityRendererProvider.Context Context) {
    }

    private record Paint(PoseStack.Pose Last, VertexConsumer Consumer, TextureAtlasSprite Sprite, Level World, int Turns, int Light, int Overlay) {
    }

    @Override
    public void render(ShutterBlockEntity Shutter, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Shutter.getLevel();
        BlockState State = Shutter.getBlockState();
        if (World == null || !(State.getBlock() instanceof CellShutterBlock) || !Shutter.showsCurtain()) {
            return;
        }
        ShutterShapes.Column Place = Shutter.place();
        float Progress = Shutter.getProgress(PartialTick);
        float Bar = ShutterShapes.barBottom(Place.Height(), Progress);
        float Ceiling = ShutterShapes.curtainTop(Place.Height());
        float Floor = 16.0f * Place.Row();
        float Roof = Floor + 16.0f;
        if (!(Bar < Ceiling) || Roof <= Bar || Floor >= Ceiling) {
            return;
        }
        boolean OpenLeft = !State.getValue(CellShutterBlock.Left);
        boolean OpenRight = !State.getValue(CellShutterBlock.Right);
        int Turns = PropShapes.quarterTurns(State.getValue(CellShutterBlock.FACING));
        VertexConsumer Consumer = Buffers.getBuffer(RenderType.cutout());
        TextureAtlasSprite Slats = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(SlatsTexture);
        TextureAtlasSprite BarSprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(BarTexture);
        Pose.pushPose();
        Pose.translate(0.5f, 0.0f, 0.5f);
        Pose.mulPose(Axis.YP.rotationDegrees(-90.0f * Turns));
        Pose.translate(-0.5f, 0.0f, -0.5f);
        Paint SlatPaint = new Paint(Pose.last(), Consumer, Slats, World, Turns, PackedLight, PackedOverlay);
        Paint BarPaint = new Paint(Pose.last(), Consumer, BarSprite, World, Turns, PackedLight, PackedOverlay);
        for (ShutterShapes.Band Slat : ShutterShapes.slats(Bar, Floor, Math.max(Floor, Bar + ShutterShapes.BarHeight), Math.min(Roof, Ceiling))) {
            vertical(SlatPaint, 0.0f, 16.0f, Slat.Low(), Slat.High(), CurtainFront, CurtainBack, Slat.VTop(), Slat.VBottom(), OpenLeft, OpenRight);
        }
        float BarLow = Math.max(Floor, Bar);
        float BarHigh = Math.min(Roof, Math.min(Ceiling, Bar + ShutterShapes.BarHeight));
        if (BarHigh > BarLow) {
            float Low = BarLow - Floor;
            float High = BarHigh - Floor;
            float VLow = 16.0f - (BarLow - Bar);
            float VHigh = 16.0f - (BarHigh - Bar);
            vertical(BarPaint, 0.0f, 16.0f, Low, High, CurtainFront, CurtainBack, VHigh, VLow, OpenLeft, OpenRight);
            lip(BarPaint, Low, High, LipFront, CurtainFront, VHigh, VLow, Direction.NORTH);
            lip(BarPaint, Low, High, CurtainBack, LipBack, VHigh, VLow, Direction.SOUTH);
            if (BarLow == Bar) {
                horizontal(BarPaint, 0.0f, 16.0f, Low, CurtainFront, CurtainBack, Direction.DOWN);
                horizontal(BarPaint, LipInset, 16.0f - LipInset, Low, LipFront, CurtainFront, Direction.DOWN);
                horizontal(BarPaint, LipInset, 16.0f - LipInset, Low, CurtainBack, LipBack, Direction.DOWN);
            }
            if (BarHigh == Bar + ShutterShapes.BarHeight) {
                horizontal(BarPaint, LipInset, 16.0f - LipInset, High, LipFront, CurtainFront, Direction.UP);
                horizontal(BarPaint, LipInset, 16.0f - LipInset, High, CurtainBack, LipBack, Direction.UP);
            }
        }
        Pose.popPose();
    }

    private static void vertical(Paint Brush, float X0, float X1, float Y0, float Y1, float Z0, float Z1, float VTop, float VBottom, boolean EdgeLeft, boolean EdgeRight) {
        quad(Brush, Direction.NORTH, X1, Y1, Z0, X1, Y0, Z0, X0, Y0, Z0, X0, Y1, Z0, 16.0f - X1, VTop, 16.0f - X0, VBottom);
        quad(Brush, Direction.SOUTH, X0, Y1, Z1, X0, Y0, Z1, X1, Y0, Z1, X1, Y1, Z1, X0, VTop, X1, VBottom);
        if (EdgeRight) {
            quad(Brush, Direction.WEST, X0, Y1, Z0, X0, Y0, Z0, X0, Y0, Z1, X0, Y1, Z1, Z0, VTop, Z1, VBottom);
        }
        if (EdgeLeft) {
            quad(Brush, Direction.EAST, X1, Y1, Z1, X1, Y0, Z1, X1, Y0, Z0, X1, Y1, Z0, 16.0f - Z1, VTop, 16.0f - Z0, VBottom);
        }
    }

    private static void lip(Paint Brush, float Y0, float Y1, float Z0, float Z1, float VTop, float VBottom, Direction Face) {
        float X0 = LipInset;
        float X1 = 16.0f - LipInset;
        if (Face == Direction.NORTH) {
            quad(Brush, Direction.NORTH, X1, Y1, Z0, X1, Y0, Z0, X0, Y0, Z0, X0, Y1, Z0, 16.0f - X1, VTop, 16.0f - X0, VBottom);
        } else {
            quad(Brush, Direction.SOUTH, X0, Y1, Z1, X0, Y0, Z1, X1, Y0, Z1, X1, Y1, Z1, X0, VTop, X1, VBottom);
        }
        quad(Brush, Direction.WEST, X0, Y1, Z0, X0, Y0, Z0, X0, Y0, Z1, X0, Y1, Z1, Z0, VTop, Z1, VBottom);
        quad(Brush, Direction.EAST, X1, Y1, Z1, X1, Y0, Z1, X1, Y0, Z0, X1, Y1, Z0, 16.0f - Z1, VTop, 16.0f - Z0, VBottom);
    }

    private static void horizontal(Paint Brush, float X0, float X1, float Y, float Z0, float Z1, Direction Face) {
        if (Face == Direction.DOWN) {
            quad(Brush, Direction.DOWN, X0, Y, Z1, X0, Y, Z0, X1, Y, Z0, X1, Y, Z1, X0, 16.0f - Z1, X1, 16.0f - Z0);
        } else {
            quad(Brush, Direction.UP, X0, Y, Z0, X0, Y, Z1, X1, Y, Z1, X1, Y, Z0, X0, Z0, X1, Z1);
        }
    }

    private static void quad(Paint Brush, Direction Face, float Ax, float Ay, float Az, float Bx, float By, float Bz, float Cx, float Cy, float Cz,
                             float Dx, float Dy, float Dz, float U0, float V0, float U1, float V1) {
        float Shade = Brush.World().getShade(turned(Face, Brush.Turns()), true);
        vertex(Brush, Face, Shade, Ax, Ay, Az, U0, V0);
        vertex(Brush, Face, Shade, Bx, By, Bz, U0, V1);
        vertex(Brush, Face, Shade, Cx, Cy, Cz, U1, V1);
        vertex(Brush, Face, Shade, Dx, Dy, Dz, U1, V0);
    }

    private static void vertex(Paint Brush, Direction Face, float Shade, float X, float Y, float Z, float U, float V) {
        TextureAtlasSprite Sprite = Brush.Sprite();
        Brush.Consumer().addVertex(Brush.Last(), X / 16.0f, Y / 16.0f, Z / 16.0f).setColor(Shade, Shade, Shade, 1.0f)
                .setUv(Sprite.getU(U / 16.0f), Sprite.getV(V / 16.0f)).setOverlay(Brush.Overlay()).setLight(Brush.Light())
                .setNormal(Brush.Last(), Face.getStepX(), Face.getStepY(), Face.getStepZ());
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
    public int getViewDistance() {
        return 96;
    }
}
