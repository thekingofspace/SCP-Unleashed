package net.rai.scp_unleashed.forcefield.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.forcefield.ForcefieldEmitterBlockEntity;
import net.rai.scp_unleashed.forcefield.ForcefieldPhase;
import net.rai.scp_unleashed.forcefield.ForcefieldRegistry;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

public class ForcefieldRenderer implements BlockEntityRenderer<ForcefieldEmitterBlockEntity> {
    public static final ResourceLocation FieldTexture = ForcefieldRegistry.location("textures/entity/forcefield/field.png");
    public static final ResourceLocation FxTexture = ForcefieldRegistry.location("textures/entity/forcefield/fx.png");
    public static final int FieldFrames = 16;
    public static final int FieldFrameTicks = 3;
    public static final int ScanFrames = 8;
    public static final int ScanFrameTicks = 2;
    public static final float FxWidth = 64.0f;
    public static final float FxHeight = 256.0f;
    public static final float Pixel = 1.0f / 16.0f;
    public static final float LowIntegrity = 0.35f;
    public static final int SweepTicks = 40;
    public static final int SweepTravel = 20;
    public static final float[] Cyan = {0.45f, 0.95f, 1.0f};

    public ForcefieldRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(ForcefieldEmitterBlockEntity Emitter, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        if (!shown(Emitter) || ShaderCompat.renderingShadowPass()) {
            return;
        }
        ForcefieldDrawPlan.Pass Chosen = ForcefieldFieldPass.currentPass();
        if (ForcefieldDrawPlan.deferred(Chosen)) {
            ForcefieldFieldPass.queue(Emitter, PartialTick, Chosen);
            return;
        }
        draw(Emitter, PartialTick, Pose.last(), Buffers);
    }

    public static boolean shown(ForcefieldEmitterBlockEntity Emitter) {
        return Emitter.getLevel() != null && !Emitter.isRemoved() && Emitter.getPhase() != ForcefieldPhase.Offline && Emitter.getHeight() > 0;
    }

    private static float[] rows(ForcefieldEmitterBlockEntity Emitter, float PartialTick, long Now) {
        int Height = Emitter.getHeight();
        float Clock = (Now % 1200000L) + PartialTick;
        float Ticks = Emitter.getPhaseTicks() + PartialTick;
        float Fraction = Emitter.getClientFraction();
        long Slot = Mth.floor(Clock / 2.0f);
        int Base = Emitter.getBlockPos().getY() + 1;
        float[] Rows = new float[Height];
        for (int Cell = 0; Cell < Height; Cell++) {
            Rows[Cell] = rowAlpha(Emitter.getPhase(), Ticks, Fraction, Slot, Base + Cell);
        }
        return Rows;
    }

    public static void stamp(ForcefieldEmitterBlockEntity Emitter, float PartialTick, PoseStack.Pose Last, MultiBufferSource Buffers) {
        Level World = Emitter.getLevel();
        if (World == null || !shown(Emitter)) {
            return;
        }
        long Now = World.getGameTime();
        float[] Rows = rows(Emitter, PartialTick, Now);
        boolean AlongX = Emitter.axis() == Direction.Axis.X;
        int FieldFrame = (int) ((Now / FieldFrameTicks) % FieldFrames);
        float FieldV0 = (float) FieldFrame / FieldFrames;
        float FieldV1 = (float) (FieldFrame + 1) / FieldFrames;
        VertexConsumer Depth = Buffers.getBuffer(ForcefieldRenderTypes.stamp(FieldTexture));
        for (int Cell = 0; Cell < Rows.length; Cell++) {
            if (Rows[Cell] > 0.01f) {
                quad(Depth, Last, AlongX, 0.0f, 1.0f + Cell, 1.0f, 2.0f + Cell, 0.0f, FieldV0, 1.0f, FieldV1, 1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
    }

    public static void draw(ForcefieldEmitterBlockEntity Emitter, float PartialTick, PoseStack.Pose Last, MultiBufferSource Buffers) {
        Level World = Emitter.getLevel();
        if (World == null || !shown(Emitter)) {
            return;
        }
        ForcefieldPhase Phase = Emitter.getPhase();
        int Height = Emitter.getHeight();
        long Now = World.getGameTime();
        float Ticks = Emitter.getPhaseTicks() + PartialTick;
        boolean AlongX = Emitter.axis() == Direction.Axis.X;
        int Before = neighbour(World, Emitter, AlongX, -1);
        int After = neighbour(World, Emitter, AlongX, 1);
        float[] Rows = rows(Emitter, PartialTick, Now);
        int FieldFrame = (int) ((Now / FieldFrameTicks) % FieldFrames);
        float FieldV0 = (float) FieldFrame / FieldFrames;
        float FieldV1 = (float) (FieldFrame + 1) / FieldFrames;
        VertexConsumer Sheet = Buffers.getBuffer(ForcefieldRenderTypes.sheet(FieldTexture));
        for (int Cell = 0; Cell < Height; Cell++) {
            if (Rows[Cell] > 0.01f) {
                quad(Sheet, Last, AlongX, 0.0f, 1.0f + Cell, 1.0f, 2.0f + Cell, 0.0f, FieldV0, 1.0f, FieldV1, 1.0f, 1.0f, 1.0f, Rows[Cell]);
            }
        }
        boolean Additive = ForcefieldRenderTypes.additive();
        VertexConsumer Glow = Buffers.getBuffer(ForcefieldRenderTypes.glow(FxTexture));
        int ScanFrame = (int) ((Now / ScanFrameTicks) % ScanFrames);
        float ScanV0 = ScanFrame * 16.0f / FxHeight;
        float ScanV1 = (ScanFrame + 1) * 16.0f / FxHeight;
        for (int Cell = 0; Cell < Height; Cell++) {
            float Strength = Rows[Cell] * 0.8f;
            if (Strength > 0.01f) {
                glowQuad(Glow, Last, AlongX, 0.0f, 1.0f + Cell, 1.0f, 2.0f + Cell, 0.0f, ScanV0, 16.0f / FxWidth, ScanV1, Strength, Additive);
            }
        }
        float EdgeU0 = 16.0f / FxWidth;
        float EdgeU1 = 18.0f / FxWidth;
        for (int Cell = 0; Cell < Height; Cell++) {
            float Strength = Math.min(1.0f, Rows[Cell] * 1.2f);
            if (Strength <= 0.01f) {
                continue;
            }
            if (Cell >= Before) {
                glowQuad(Glow, Last, AlongX, 0.0f, 1.0f + Cell, 2.0f * Pixel, 2.0f + Cell, EdgeU0, ScanV0, EdgeU1, ScanV1, Strength, Additive);
            }
            if (Cell >= After) {
                glowQuad(Glow, Last, AlongX, 1.0f - 2.0f * Pixel, 1.0f + Cell, 1.0f, 2.0f + Cell, EdgeU1, ScanV0, EdgeU0, ScanV1, Strength, Additive);
            }
        }
        if (Phase == ForcefieldPhase.Booting) {
            float Cycle = Ticks % SweepTicks;
            if (Cycle < SweepTravel) {
                float Row = Mth.floor((1.0f + Height * Cycle / SweepTravel) * 16.0f) / 16.0f;
                float Strength = 0.4f + 0.6f * Mth.clamp(Ticks / ForcefieldEmitterBlockEntity.BootTicks, 0.0f, 1.0f);
                glowQuad(Glow, Last, AlongX, 0.0f, Row, 1.0f, Row + Pixel, 16.0f / FxWidth, 16.0f / FxHeight, 32.0f / FxWidth, 17.0f / FxHeight, Strength, Additive);
            }
        }
        if (Phase == ForcefieldPhase.Online) {
            ripples(Glow, Last, Emitter, AlongX, Height, Now, Additive);
        }
    }

    private static int neighbour(Level World, ForcefieldEmitterBlockEntity Emitter, boolean AlongX, int Step) {
        BlockPos Beside = Emitter.getBlockPos().relative(AlongX ? Direction.Axis.X : Direction.Axis.Z, Step);
        if (World.getBlockEntity(Beside) instanceof ForcefieldEmitterBlockEntity Other && Other.axis() == Emitter.axis()
                && Other.getPhase() != ForcefieldPhase.Offline && Other.getPhase() == Emitter.getPhase()) {
            return Other.getHeight();
        }
        return 0;
    }

    private static float rowAlpha(ForcefieldPhase Phase, float Ticks, float Fraction, long Slot, int Row) {
        switch (Phase) {
            case Online -> {
                if (Fraction < LowIntegrity && noise(Slot, Row) < (LowIntegrity - Fraction) * 0.9f) {
                    return 0.3f;
                }
                return 1.0f;
            }
            case Booting -> {
                float Progress = Mth.clamp(Ticks / ForcefieldEmitterBlockEntity.BootTicks, 0.0f, 1.0f);
                if (Progress > 0.92f) {
                    return 0.6f + 0.4f * (Progress - 0.92f) / 0.08f;
                }
                float Chance = 0.12f + 0.7f * Progress * Progress;
                if (noise(Slot, 7) > Chance || noise(Slot, Row) > 0.35f + 0.65f * Progress) {
                    return 0.0f;
                }
                return 0.2f + 0.45f * Progress;
            }
            case Collapsing -> {
                float Progress = Mth.clamp(Ticks / ForcefieldEmitterBlockEntity.CollapseTicks, 0.0f, 1.0f);
                if (Ticks < 2.0f) {
                    return 1.0f;
                }
                if (noise(Slot, Row + 31) < Progress * 0.9f) {
                    return 0.0f;
                }
                return 1.0f - 0.7f * Progress;
            }
            default -> {
                return 0.0f;
            }
        }
    }

    private static float noise(long Slot, int Salt) {
        long Mixed = Slot * 0x9E3779B97F4A7C15L + Salt * 0xC2B2AE3D27D4EB4FL;
        Mixed ^= Mixed >>> 33;
        Mixed *= 0xFF51AFD7ED558CCDL;
        Mixed ^= Mixed >>> 33;
        return (Mixed >>> 40) / (float) (1L << 24);
    }

    private static void ripples(VertexConsumer Glow, PoseStack.Pose Last, ForcefieldEmitterBlockEntity Emitter, boolean AlongX, int Height, long Now, boolean Additive) {
        BlockPos Pos = Emitter.getBlockPos();
        double Plane = AlongX ? Pos.getZ() + 0.5 : Pos.getX() + 0.5;
        double Start = AlongX ? Pos.getX() : Pos.getZ();
        double Bottom = Pos.getY() + 1.0;
        double Top = Bottom + Height;
        for (ForcefieldRipples.Ripple Wave : ForcefieldRipples.active(Now)) {
            if (Wave.AlongX() != AlongX) {
                continue;
            }
            double Across = AlongX ? Wave.Point().z : Wave.Point().x;
            if (Math.abs(Across - Plane) > 0.25) {
                continue;
            }
            double Along = AlongX ? Wave.Point().x : Wave.Point().z;
            double Low = Math.max(Along - 1.0, Start);
            double High = Math.min(Along + 1.0, Start + 1.0);
            double Floor = Math.max(Wave.Point().y - 1.0, Bottom);
            double Ceiling = Math.min(Wave.Point().y + 1.0, Top);
            if (Low >= High || Floor >= Ceiling) {
                continue;
            }
            int Frame = (int) Mth.clamp((Now - Wave.Start()) / ForcefieldRipples.FrameTicks, 0, ForcefieldRipples.Frames - 1);
            float FrameTop = Frame * 32.0f;
            float U0 = (float) (32.0 + (Low - (Along - 1.0)) * 16.0) / FxWidth;
            float U1 = (float) (32.0 + (High - (Along - 1.0)) * 16.0) / FxWidth;
            float VTop = (float) (FrameTop + (Wave.Point().y + 1.0 - Ceiling) * 16.0) / FxHeight;
            float VBottom = (float) (FrameTop + (Wave.Point().y + 1.0 - Floor) * 16.0) / FxHeight;
            float Strength = Mth.clamp(0.45f + 0.75f * Wave.Strength(), 0.0f, 1.0f);
            glowQuad(Glow, Last, AlongX, (float) (Low - Start), (float) (Floor - Pos.getY()), (float) (High - Start), (float) (Ceiling - Pos.getY()), U0, VTop, U1, VBottom, Strength, Additive);
        }
    }

    private static void glowQuad(VertexConsumer Consumer, PoseStack.Pose Last, boolean AlongX, float S0, float Y0, float S1, float Y1,
                                 float U0, float V0, float U1, float V1, float Strength, boolean Additive) {
        if (Additive) {
            quad(Consumer, Last, AlongX, S0, Y0, S1, Y1, U0, V0, U1, V1, Cyan[0] * Strength, Cyan[1] * Strength, Cyan[2] * Strength, 1.0f);
        } else {
            quad(Consumer, Last, AlongX, S0, Y0, S1, Y1, U0, V0, U1, V1, Cyan[0], Cyan[1], Cyan[2], Strength);
        }
    }

    private static void quad(VertexConsumer Consumer, PoseStack.Pose Last, boolean AlongX, float S0, float Y0, float S1, float Y1,
                             float U0, float V0, float U1, float V1, float Red, float Green, float Blue, float Alpha) {
        vertex(Consumer, Last, AlongX, S0, Y0, U0, V1, Red, Green, Blue, Alpha);
        vertex(Consumer, Last, AlongX, S1, Y0, U1, V1, Red, Green, Blue, Alpha);
        vertex(Consumer, Last, AlongX, S1, Y1, U1, V0, Red, Green, Blue, Alpha);
        vertex(Consumer, Last, AlongX, S0, Y1, U0, V0, Red, Green, Blue, Alpha);
    }

    private static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, boolean AlongX, float S, float Y, float U, float V,
                               float Red, float Green, float Blue, float Alpha) {
        float X = AlongX ? S : 0.5f;
        float Z = AlongX ? 0.5f : S;
        Consumer.addVertex(Last, X, Y, Z)
                .setColor(Red, Green, Blue, Alpha)
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(Last, AlongX ? 0.0f : 1.0f, 0.0f, AlongX ? 1.0f : 0.0f);
    }

    @Override
    public boolean shouldRenderOffScreen(ForcefieldEmitterBlockEntity Emitter) {
        return false;
    }

    @Override
    public AABB getRenderBoundingBox(ForcefieldEmitterBlockEntity Emitter) {
        return Emitter.renderBox();
    }
}
