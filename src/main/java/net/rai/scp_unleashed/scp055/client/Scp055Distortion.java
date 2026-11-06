package net.rai.scp_unleashed.scp055.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class Scp055Distortion {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp055_distortion");
    public static final int TexelTarget = 360;
    public static final int[] BlockSteps = {1, 2, 2, 3, 4, 5};
    public static final int BandRows = 3;
    public static final float WobbleTexels = 3.0f;
    public static final float WobbleSpeed = 0.55f;
    public static final float BandPhase = 0.9f;
    public static final int TearEvery = 7;
    public static final int TearTicks = 2;
    public static final int TearShift = 6;
    public static final float ChromaNear = 0.25f;
    public static final float ChromaFarAt = 0.7f;
    public static final int ChromaFar = 2;
    public static final int GhostShift = 3;
    public static final int GhostPeriod = 6;
    public static final float GhostAlpha = 0.22f;
    public static final int[] GhostTints = {0xFF8C8C, 0x8CFFFF};
    public static final int RingWidth = 3;
    public static final float[] RingAlphas = {0.42f, 0.26f, 0.12f};
    public static final int VignetteColour = 0x140A1E;
    private static TextureTarget Small;

    private Scp055Distortion() {
    }

    public static int texel(int Width, int Height) {
        return Math.max(1, Math.round(Math.min(Width, Height) / (float) TexelTarget));
    }

    public static int blockSize(int Width, int Height, float Level) {
        int Index = Mth.clamp(Math.round(Level * (BlockSteps.length - 1)), 0, BlockSteps.length - 1);
        return texel(Width, Height) * BlockSteps[Index];
    }

    public static long mix(long Seed, long Value) {
        long Mixed = Seed + Value * 0x9E3779B97F4A7C15L;
        Mixed = (Mixed ^ (Mixed >>> 30)) * 0xBF58476D1CE4E5B9L;
        Mixed = (Mixed ^ (Mixed >>> 27)) * 0x94D049BB133111EBL;
        return Mixed ^ (Mixed >>> 31);
    }

    public static int bandOffset(long Seed, int Band, int Bands, int Tick, float Level) {
        float Wave = Mth.sin(Tick * WobbleSpeed + Band * BandPhase + (Seed & 63L));
        int Offset = Math.round(Wave * WobbleTexels * Level);
        int Window = Tick / TearEvery;
        if (Tick % TearEvery < TearTicks) {
            long Roll = mix(Seed, Window);
            if (Math.floorMod(Roll, (long) Bands) == Band) {
                Offset += Math.round(((Roll >>> 20 & 1L) == 0L ? TearShift : -TearShift) * Level);
            }
        }
        return Offset;
    }

    public static int chroma(float Level) {
        return Level >= ChromaFarAt ? ChromaFar : Level >= ChromaNear ? 1 : 0;
    }

    public static int ghostPhase(int Tick) {
        return (Tick / GhostPeriod) % 2;
    }

    public static int ghost(int Tick) {
        return ghostPhase(Tick) == 0 ? GhostShift : -GhostShift;
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null) {
            return;
        }
        float Level = Scp055ClientEpisode.distortion(Delta.getGameTimeDeltaPartialTick(false));
        if (Level <= 0.0f) {
            return;
        }
        RenderTarget Main = Client.getMainRenderTarget();
        int Width = Main.width;
        int Height = Main.height;
        if (Width <= 0 || Height <= 0) {
            return;
        }
        int Block = blockSize(Width, Height, Level);
        int Columns = Math.max(1, Width / Block);
        int Rows = Math.max(1, Height / Block);
        Graphics.flush();
        capture(Main, Columns, Rows);
        int Tick = Scp055ClientEpisode.age();
        long Seed = Scp055ClientEpisode.seed();
        float Scale = (float) Client.getWindow().getGuiScale();
        PoseStack Pose = Graphics.pose();
        Pose.pushPose();
        Pose.scale(1.0f / Scale, 1.0f / Scale, 1.0f);
        Matrix4f Matrix = Pose.last().pose();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, Small.getColorTextureId());
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        int Shift = chroma(Level);
        RenderSystem.colorMask(true, false, false, false);
        bands(Matrix, Columns, Rows, Width, Height, Tick, Seed, Level, Shift);
        RenderSystem.colorMask(false, true, false, false);
        bands(Matrix, Columns, Rows, Width, Height, Tick, Seed, Level, 0);
        RenderSystem.colorMask(false, false, true, false);
        bands(Matrix, Columns, Rows, Width, Height, Tick, Seed, Level, -Shift);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int Tint = GhostTints[ghostPhase(Tick)];
        RenderSystem.setShaderColor((Tint >> 16 & 255) / 255.0f, (Tint >> 8 & 255) / 255.0f, (Tint & 255) / 255.0f, GhostAlpha * Level);
        bands(Matrix, Columns, Rows, Width, Height, Tick, Seed, Level, ghost(Tick));
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        vignette(Graphics, Width, Height, Width / (float) Columns, Level);
        Pose.popPose();
        RenderSystem.disableBlend();
    }

    private static void capture(RenderTarget Main, int Columns, int Rows) {
        if (Small == null) {
            Small = new TextureTarget(Columns, Rows, false, Minecraft.ON_OSX);
        } else if (Small.width != Columns || Small.height != Rows) {
            Small.resize(Columns, Rows, Minecraft.ON_OSX);
        }
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, Main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, Small.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, Main.width, Main.height, 0, 0, Columns, Rows, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        Small.bindWrite(false);
        RenderSystem.colorMask(false, false, false, true);
        RenderSystem.clearColor(0.0f, 0.0f, 0.0f, 1.0f);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.colorMask(true, true, true, true);
        Main.bindWrite(true);
    }

    private static void bands(Matrix4f Matrix, int Columns, int Rows, int Width, int Height, int Tick, long Seed, float Level, int Shift) {
        BufferBuilder Builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        float CellHeight = Height / (float) Rows;
        int Bands = (Rows + BandRows - 1) / BandRows;
        for (int Band = 0; Band < Bands; Band++) {
            int Top = Band * BandRows;
            int Bottom = Math.min(Rows, Top + BandRows);
            int Offset = bandOffset(Seed, Band, Bands, Tick, Level) + Shift;
            float U0 = -Offset / (float) Columns;
            float U1 = U0 + 1.0f;
            float V0 = 1.0f - Top / (float) Rows;
            float V1 = 1.0f - Bottom / (float) Rows;
            float Y0 = Top * CellHeight;
            float Y1 = Bottom == Rows ? Height : Bottom * CellHeight;
            Builder.addVertex(Matrix, 0.0f, Y0, 0.0f).setUv(U0, V0);
            Builder.addVertex(Matrix, 0.0f, Y1, 0.0f).setUv(U0, V1);
            Builder.addVertex(Matrix, Width, Y1, 0.0f).setUv(U1, V1);
            Builder.addVertex(Matrix, Width, Y0, 0.0f).setUv(U1, V0);
        }
        BufferUploader.drawWithShader(Builder.buildOrThrow());
    }

    private static void vignette(GuiGraphics Graphics, int Width, int Height, float Cell, float Level) {
        int Ring = Math.max(1, Math.round(Cell * RingWidth));
        for (int Index = 0; Index < RingAlphas.length; Index++) {
            int Alpha = Math.max(1, Math.min(255, Math.round(RingAlphas[Index] * Level * 255.0f)));
            int Colour = Alpha << 24 | VignetteColour;
            int Inset = Index * Ring;
            int Outer = Inset + Ring;
            Graphics.fill(Inset, Inset, Width - Inset, Outer, Colour);
            Graphics.fill(Inset, Height - Outer, Width - Inset, Height - Inset, Colour);
            Graphics.fill(Inset, Outer, Outer, Height - Outer, Colour);
            Graphics.fill(Width - Outer, Outer, Width - Inset, Height - Outer, Colour);
        }
    }

    public static void release() {
        if (Small != null) {
            Small.destroyBuffers();
            Small = null;
        }
    }
}
