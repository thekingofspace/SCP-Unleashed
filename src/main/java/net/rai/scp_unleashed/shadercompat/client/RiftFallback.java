package net.rai.scp_unleashed.shadercompat.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.rift.client.RiftRenderTypes;

public final class RiftFallback {
    public static final int AtlasWidth = 512;
    public static final int AtlasHeight = 896;
    public static final int Frames = 32;
    public static final float Period = 4.0f;
    public static final int CoreColumns = 16;
    public static final int GlowColumns = 8;
    public static final int CoreWidth = 30;
    public static final int CoreHeight = 52;
    public static final int GlowWidth = 64;
    public static final int GlowHeight = 80;
    public static final int HaloTop = 0;
    public static final int BaseTop = 320;
    public static final int TintTop = 424;
    public static final int WhiteTop = 528;
    public static final int EdgeTop = 632;
    public static final int BoltTop = 736;
    public static final int SpecialTop = 816;
    public static final int FlashTile = 0;
    public static final int SlitLineTile = 1;
    public static final int SlitHaloTile = 2;
    public static final int BoltTiles = 8;
    public static final float BaseGain = 4.0f;
    public static final float FlashGain = 4.0f;
    public static final float ReferenceWidth = 0.405f;
    public static final float CoreExtentX = 0.75f;
    public static final float CoreExtentY = 1.3f;
    public static final float GlowExtentX = 1.6f;
    public static final float GlowExtentY = 2.0f;
    public static final float MaxHeat = 4.0f;
    public static final float LiftShare = 0.01f;
    public static final float MinLift = 0.002f;
    private static final float ReferenceFlow = 0.57f;

    private RiftFallback() {
    }

    public static void draw(PoseStack.Pose Last, MultiBufferSource Buffers, float CentreX, float CentreY, float CentreZ, float AcrossX, float AcrossZ, float Half,
                            int Colour, int Pull, int Implosion, int Variant, int Profile) {
        draw(Last, Buffers, CentreX, CentreY, CentreZ, AcrossX, AcrossZ, Half, Colour, Pull, Implosion, Variant, Profile, RenderSystem.getShaderGameTime() * 1200.0f);
    }

    public static void draw(PoseStack.Pose Last, MultiBufferSource Buffers, float CentreX, float CentreY, float CentreZ, float AcrossX, float AcrossZ, float Half,
                            int Colour, int Pull, int Implosion, int Variant, int Profile, float Seconds) {
        float Open = ((Colour >>> 24) & 255) / 255.0f;
        float Red = ((Colour >> 16) & 255) / 255.0f;
        float Green = ((Colour >> 8) & 255) / 255.0f;
        float Blue = (Colour & 255) / 255.0f;
        float Extraction = Pull / 1000.0f;
        float Collapse = Implosion / 1000.0f;
        int Grade = Math.floorMod(Profile, 16);
        float Violence = Mth.clamp((Grade - 1.0f) * 0.25f, 0.0f, 1.0f);
        float Salt = fract(Variant * 0.6180339f);
        float Time = Seconds + Salt * 113.0f;
        float Strain = Extraction * Extraction;
        float Sway = (float) (Math.sin(Time * 0.23) * 0.07 + Math.sin(Time * 0.071 + 1.7) * 0.05);
        float Angle = Sway + (noise(Time * 23.0f, Salt * 40.0f) - 0.5f) * 0.09f * Strain;
        float Breath = (float) (1.0 + 0.035 * Math.sin(Time * 1.31) + 0.02 * Math.sin(Time * 2.93 + 0.4));
        float Bulge = 1.0f + 0.12f * smoothstep(0.0f, 0.3f, Collapse);
        float Shrink = 1.0f - smoothstep(0.3f, 0.97f, Collapse);
        float Grow = Mth.lerp(Mth.sqrt(Open), 0.22f, 1.0f);
        float Height = Math.max(Breath * Bulge * Grow * Mth.lerp(Shrink * Shrink, 0.02f, 1.0f), 0.0005f);
        float Pinch = 1.0f - smoothstep(0.25f, 0.75f, Collapse);
        float Pulse = (float) (1.0 + 0.07 * Math.sin(Time * 1.73 + 0.8) + 0.05 * Strain * Math.sin(Time * 31.0));
        float Width = (0.33f + 0.025f * Grade) * Pulse * (float) Math.pow(Open, 0.65) * Pinch;
        float Visible = smoothstep(0.0f, 0.08f, Open);
        float Heat = Math.min(1.0f + 1.8f * Extraction + 2.5f * smoothstep(0.2f, 0.9f, Collapse), MaxHeat);
        float Flicker = 0.85f + 0.15f * noise(Time * 17.0f, Salt * 40.0f);
        float Speed = (1.0f + 2.5f * Extraction) * (0.42f + 0.3f * Violence) / ReferenceFlow;
        int FrameIndex = Math.floorMod((int) Math.floor(Time * Speed * Frames / Period), Frames);
        boolean FlipU = (Variant & 1) != 0;
        boolean FlipV = (Variant & 2) != 0;
        float Stretch = Width / ReferenceWidth;
        float Cosine = Mth.cos(Angle);
        float Sine = Mth.sin(Angle);
        Placement Hole = new Placement(Last, CentreX, CentreY, CentreZ, AcrossX, AcrossZ, Half, Height, Stretch, Cosine, Sine);
        VertexConsumer Solid = Buffers.getBuffer(RiftRenderTypes.FallbackCore);
        Hole.quad(Solid, CoreExtentX, CoreExtentY, true, tile(BaseTop, CoreColumns, CoreWidth, CoreHeight, FrameIndex, FlipU, FlipV),
                Red / BaseGain, Green / BaseGain, Blue / BaseGain);
        float Lift = Math.max(MinLift, Half * LiftShare);
        Placement Spot = Hole.lifted(Lift, Stretch);
        VertexConsumer Glow = Buffers.getBuffer(RiftRenderTypes.FallbackGlow);
        Spot.quad(Glow, GlowExtentX, GlowExtentY, true, tile(HaloTop, GlowColumns, GlowWidth, GlowHeight, FrameIndex, FlipU, FlipV),
                Red * Visible, Green * Visible, Blue * Visible);
        float Shine = Visible * Flicker;
        Spot.quad(Glow, CoreExtentX, CoreExtentY, true, tile(TintTop, CoreColumns, CoreWidth, CoreHeight, FrameIndex, FlipU, FlipV),
                Red * Shine, Green * Shine, Blue * Shine);
        Spot.quad(Glow, CoreExtentX, CoreExtentY, true, tile(WhiteTop, CoreColumns, CoreWidth, CoreHeight, FrameIndex, FlipU, FlipV), Shine, Shine, Shine);
        passes(Glow, Spot, CoreExtentX, CoreExtentY, true, tile(EdgeTop, CoreColumns, CoreWidth, CoreHeight, FrameIndex, FlipU, FlipV),
                hot(Red), hot(Green), hot(Blue), (Heat - 1.0f) * Shine, 3);
        float Slit = 1.0f - smoothstep(0.2f, 0.55f, Stretch);
        if (Slit > 0.001f) {
            Placement Thin = Hole.lifted(Lift, 1.0f);
            float Fade = Slit * Visible;
            Thin.quad(Glow, GlowExtentX, GlowExtentY, true, tile(SpecialTop, GlowColumns, GlowWidth, GlowHeight, SlitHaloTile, FlipU, FlipV),
                    Red * Fade, Green * Fade, Blue * Fade);
            passes(Glow, Thin, GlowExtentX, GlowExtentY, true, tile(SpecialTop, GlowColumns, GlowWidth, GlowHeight, SlitLineTile, FlipU, FlipV),
                    hot(Red), hot(Green), hot(Blue), Math.min(Heat, 2.0f) * Fade * Flicker, 2);
        }
        bolts(Glow, Spot, Time, Salt, Extraction, Violence, Heat * Visible, Red, Green, Blue);
        float Flash = smoothstep(0.7f, 1.0f, Collapse);
        if (Flash > 0.001f) {
            passes(Glow, Spot, GlowExtentX, GlowExtentY, false, tile(SpecialTop, GlowColumns, GlowWidth, GlowHeight, FlashTile, false, false),
                    hot(Red), hot(Green), hot(Blue), Flash * Flash * Visible * FlashGain, 4);
        }
    }

    private static void passes(VertexConsumer Glow, Placement Spot, float ExtentX, float ExtentY, boolean Shaped, Tile Region, float Red, float Green, float Blue,
                               float Amount, int Limit) {
        for (int Pass = 0; Pass < Limit && Pass < Amount; Pass++) {
            float Share = Math.min(1.0f, Amount - Pass);
            Spot.quad(Glow, ExtentX, ExtentY, Shaped, Region, Red * Share, Green * Share, Blue * Share);
        }
    }

    private static void bolts(VertexConsumer Glow, Placement Spot, float Time, float Salt, float Extraction, float Violence, float Strength, float Red, float Green, float Blue) {
        float Rate = 7.0f + 9.0f * Extraction + 3.0f * Violence;
        float Count = 1.6f + 2.0f * Violence + 3.0f * Extraction;
        for (int Bolt = 0; Bolt < 6 && Bolt + 0.5f <= Count; Bolt++) {
            float Clock = Time * Rate * (0.8f + 0.07f * Bolt) + Bolt * 0.618f;
            float Slot = (float) Math.floor(Clock);
            float RollX = hashX(Slot * 1.31f + Bolt * 17.7f, Salt * 50.0f + Bolt * 3.3f);
            float RollY = hashY(Slot * 1.31f + Bolt * 17.7f, Salt * 50.0f + Bolt * 3.3f);
            if (Clock - Slot > 0.45f + 0.4f * RollY || RollX < 0.3f - 0.25f * Extraction) {
                continue;
            }
            float Start = hash(Slot * 0.73f + Bolt * 3.1f, RollX * 41.0f);
            boolean Left = Start >= 0.5f;
            float Turn = Left ? 1.0f - Start : Start;
            int Index = Mth.clamp((int) (Turn * 2.0f * BoltTiles), 0, BoltTiles - 1);
            boolean Mirror = RollY > 0.5f;
            Tile Region = tile(BoltTop, GlowColumns, GlowWidth, GlowHeight, Mirror ? BoltTiles - 1 - Index : Index, Left, Mirror);
            passes(Glow, Spot, GlowExtentX, GlowExtentY, true, Region, spark(Red), spark(Green), spark(Blue), Strength * (0.7f + 0.6f * RollY) / 1.3f, 2);
        }
    }

    private static float hot(float Channel) {
        return Channel + (1.0f - Channel) * 0.72f;
    }

    private static float spark(float Channel) {
        return Channel + (1.0f - Channel) * 0.6f;
    }

    private static Tile tile(int Top, int Columns, int TileWidth, int TileHeight, int Index, boolean FlipU, boolean FlipV) {
        int Column = Index % Columns;
        int Row = Index / Columns;
        float Left = Column * TileWidth / (float) AtlasWidth;
        float Right = (Column + 1) * TileWidth / (float) AtlasWidth;
        float Upper = (Top + Row * TileHeight) / (float) AtlasHeight;
        float Lower = (Top + (Row + 1) * TileHeight) / (float) AtlasHeight;
        return new Tile(FlipU ? Right : Left, FlipV ? Lower : Upper, FlipU ? Left : Right, FlipV ? Upper : Lower);
    }

    private static float fract(float Value) {
        return Value - (float) Math.floor(Value);
    }

    private static float smoothstep(float Low, float High, float Value) {
        float Clamped = Mth.clamp((Value - Low) / (High - Low), 0.0f, 1.0f);
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }

    private static float hash(float CellX, float CellY) {
        float A = fract(CellX * 0.1031f);
        float B = fract(CellY * 0.1031f);
        float C = fract(CellX * 0.1031f);
        float Dot = A * (B + 33.33f) + B * (C + 33.33f) + C * (A + 33.33f);
        return fract((A + B + 2.0f * Dot) * (C + Dot));
    }

    private static float hashX(float CellX, float CellY) {
        float A = fract(CellX * 0.1031f);
        float B = fract(CellY * 0.103f);
        float C = fract(CellX * 0.0973f);
        float Dot = A * (B + 33.33f) + B * (C + 33.33f) + C * (A + 33.33f);
        return fract((A + B + 2.0f * Dot) * (C + Dot));
    }

    private static float hashY(float CellX, float CellY) {
        float A = fract(CellX * 0.1031f);
        float B = fract(CellY * 0.103f);
        float C = fract(CellX * 0.0973f);
        float Dot = A * (B + 33.33f) + B * (C + 33.33f) + C * (A + 33.33f);
        return fract((A + C + 2.0f * Dot) * (B + Dot));
    }

    private static float noise(float PointX, float PointY) {
        float CellX = (float) Math.floor(PointX);
        float CellY = (float) Math.floor(PointY);
        float PartX = PointX - CellX;
        float PartY = PointY - CellY;
        float BlendX = PartX * PartX * (3.0f - 2.0f * PartX);
        float BlendY = PartY * PartY * (3.0f - 2.0f * PartY);
        float Lower = Mth.lerp(BlendX, hash(CellX, CellY), hash(CellX + 1.0f, CellY));
        float Upper = Mth.lerp(BlendX, hash(CellX, CellY + 1.0f), hash(CellX + 1.0f, CellY + 1.0f));
        return Mth.lerp(BlendY, Lower, Upper);
    }

    private record Tile(float U0, float V0, float U1, float V1) {
    }

    private record Placement(PoseStack.Pose Last, float CentreX, float CentreY, float CentreZ, float AcrossX, float AcrossZ, float Half, float Height,
                             float Stretch, float Cosine, float Sine) {
        Placement lifted(float Lift, float Wide) {
            return new Placement(Last, CentreX - AcrossZ * Lift, CentreY, CentreZ + AcrossX * Lift, AcrossX, AcrossZ, Half, Height, Wide, Cosine, Sine);
        }

        void quad(VertexConsumer Consumer, float ExtentX, float ExtentY, boolean Shaped, Tile Region, float Red, float Green, float Blue) {
            float R = Mth.clamp(Red, 0.0f, 1.0f);
            float G = Mth.clamp(Green, 0.0f, 1.0f);
            float B = Mth.clamp(Blue, 0.0f, 1.0f);
            corner(Consumer, -ExtentX, -ExtentY, Shaped, Region.U0(), Region.V1(), R, G, B);
            corner(Consumer, ExtentX, -ExtentY, Shaped, Region.U1(), Region.V1(), R, G, B);
            corner(Consumer, ExtentX, ExtentY, Shaped, Region.U1(), Region.V0(), R, G, B);
            corner(Consumer, -ExtentX, ExtentY, Shaped, Region.U0(), Region.V0(), R, G, B);
        }

        private void corner(VertexConsumer Consumer, float PointX, float PointY, boolean Shaped, float U, float V, float Red, float Green, float Blue) {
            float LocalX = PointX;
            float LocalY = PointY;
            if (Shaped) {
                float Wide = PointX * Stretch;
                LocalX = Height * (Cosine * Wide + Sine * PointY);
                LocalY = Height * (Cosine * PointY - Sine * Wide);
            }
            Consumer.addVertex(Last, CentreX + AcrossX * LocalX * Half, CentreY + LocalY * Half, CentreZ + AcrossZ * LocalX * Half)
                    .setColor(Red, Green, Blue, 1.0f)
                    .setUv(U, V)
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(Last, 0.0f, 1.0f, 0.0f);
        }
    }
}
