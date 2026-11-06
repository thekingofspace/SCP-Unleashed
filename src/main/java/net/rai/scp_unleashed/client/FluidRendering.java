package net.rai.scp_unleashed.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.BlockAndTintGetter;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class FluidRendering {
    private static final float Epsilon = 1.0E-5f;

    private FluidRendering() {
    }

    public static void renderBox(PoseStack Pose, MultiBufferSource Buffers, FluidStack Liquid, @Nullable BlockAndTintGetter World, BlockPos Pos,
                                 float MinX, float MinY, float MinZ, float MaxX, float MaxY, float MaxZ, int Light, boolean Top, boolean Bottom) {
        if (Liquid.isEmpty() || MaxY - MinY < 1.0E-4f) {
            return;
        }
        IClientFluidTypeExtensions Extensions = IClientFluidTypeExtensions.of(Liquid.getFluid());
        TextureAtlasSprite Sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(Extensions.getStillTexture(Liquid));
        int Colour = World != null ? Extensions.getTintColor(Liquid.getFluid().defaultFluidState(), World, Pos) : Extensions.getTintColor(Liquid);
        if ((Colour >>> 24) == 0) {
            Colour |= 0xFF000000;
        }
        int Glow = Liquid.getFluid().getFluidType().getLightLevel(Liquid);
        int PackedLight = Glow > 0 ? LightTexture.pack(Math.max(Glow, LightTexture.block(Light)), LightTexture.sky(Light)) : Light;
        VertexConsumer Consumer = Buffers.getBuffer(Sheets.translucentCullBlockSheet());
        PoseStack.Pose Last = Pose.last();
        float[] Lows = {MinX, MinY, MinZ};
        float[] Highs = {MaxX, MaxY, MaxZ};
        for (Direction Side : Direction.values()) {
            if ((Side == Direction.UP && !Top) || (Side == Direction.DOWN && !Bottom)) {
                continue;
            }
            int First = Side.getAxis() == Direction.Axis.X ? 2 : 0;
            int Second = Side.getAxis() == Direction.Axis.Y ? 2 : 1;
            for (float[] SpanA : segments(Lows[First], Highs[First])) {
                for (float[] SpanB : segments(Lows[Second], Highs[Second])) {
                    float[] TileLow = Lows.clone();
                    float[] TileHigh = Highs.clone();
                    TileLow[First] = SpanA[0];
                    TileHigh[First] = SpanA[1];
                    TileLow[Second] = SpanB[0];
                    TileHigh[Second] = SpanB[1];
                    quad(Consumer, Last, Sprite, Colour, PackedLight, Side, TileLow, TileHigh, First, Second);
                }
            }
        }
    }

    private static List<float[]> segments(float Low, float High) {
        List<float[]> Parts = new ArrayList<>();
        float Start = Low;
        while (Start < High - Epsilon) {
            float End = Math.min(High, (float) Math.floor(Start + Epsilon) + 1.0f);
            Parts.add(new float[]{Start, End});
            Start = End;
        }
        return Parts;
    }

    private static void quad(VertexConsumer Consumer, PoseStack.Pose Last, TextureAtlasSprite Sprite, int Colour, int Light, Direction Side,
                             float[] Low, float[] High, int First, int Second) {
        float BaseA = (float) Math.floor(Low[First] + Epsilon);
        float BaseB = (float) Math.floor(Low[Second] + Epsilon);
        float SpanU = Sprite.getU1() - Sprite.getU0();
        float SpanV = Sprite.getV1() - Sprite.getV0();
        for (float[] Corner : corners(Side, Low, High)) {
            float U = Sprite.getU0() + SpanU * (Corner[First] - BaseA);
            float V = Sprite.getV0() + SpanV * (1.0f - (Corner[Second] - BaseB));
            Consumer.addVertex(Last, Corner[0], Corner[1], Corner[2])
                    .setColor(Colour)
                    .setUv(U, V)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(Light)
                    .setNormal(Last, Side.getStepX(), Side.getStepY(), Side.getStepZ());
        }
    }

    private static float[][] corners(Direction Side, float[] Low, float[] High) {
        float X0 = Low[0];
        float Y0 = Low[1];
        float Z0 = Low[2];
        float X1 = High[0];
        float Y1 = High[1];
        float Z1 = High[2];
        return switch (Side) {
            case DOWN -> new float[][]{{X0, Y0, Z1}, {X0, Y0, Z0}, {X1, Y0, Z0}, {X1, Y0, Z1}};
            case UP -> new float[][]{{X0, Y1, Z0}, {X0, Y1, Z1}, {X1, Y1, Z1}, {X1, Y1, Z0}};
            case NORTH -> new float[][]{{X1, Y1, Z0}, {X1, Y0, Z0}, {X0, Y0, Z0}, {X0, Y1, Z0}};
            case SOUTH -> new float[][]{{X0, Y1, Z1}, {X0, Y0, Z1}, {X1, Y0, Z1}, {X1, Y1, Z1}};
            case WEST -> new float[][]{{X0, Y1, Z0}, {X0, Y0, Z0}, {X0, Y0, Z1}, {X0, Y1, Z1}};
            case EAST -> new float[][]{{X1, Y1, Z1}, {X1, Y0, Z1}, {X1, Y0, Z0}, {X1, Y1, Z0}};
        };
    }
}
