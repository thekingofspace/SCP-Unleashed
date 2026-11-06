package net.rai.scp_unleashed.scp178.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.logging.LogUtils;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp178.Scp178GlassesItem;
import net.rai.scp_unleashed.scp178.Scp178Watcher;
import net.rai.scp_unleashed.shadercompat.client.VeilPost;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.slf4j.Logger;

public final class Scp178Vision {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp178_anaglyph");
    public static final ResourceLocation Pipeline = VeilPost.id("scp178_anaglyph");
    public static final float FadeStep = 1.0f / 20.0f;
    public static final float ShownFloor = 0.003f;
    public static final float ShiftShare = 0.0045f;
    public static final float MinShift = 2.0f;
    public static final float SwayAmount = 0.22f;
    public static final float FallbackGreen = 0.06f;
    public static final int EdgeSteps = 5;
    public static final float EdgeShare = 0.2f;
    public static final float EdgeAlpha = 0.2f;
    public static final int RedTint = 0xE03A2E;
    public static final int BlueTint = 0x2E62EA;
    public static final int IdleTicks = 600;
    public static final int AlphaSteps = 16;

    private static final Logger Log = LogUtils.getLogger();
    private static float Fade;
    private static float FadeOld;
    private static int Clock;
    private static int Idle;
    private static TextureTarget Copy;
    private static boolean CopyBroken;

    private Scp178Vision() {
    }

    public static void tick(Minecraft Client) {
        FadeOld = Fade;
        boolean Wearing = Client.player != null && Scp178GlassesItem.isWorn(Client.player);
        Fade = Wearing ? Math.min(1.0f, Fade + FadeStep) : Math.max(0.0f, Fade - FadeStep);
        if (!Client.isPaused()) {
            Clock++;
        }
        Idle = Fade > 0.0f || FadeOld > 0.0f ? 0 : Idle + 1;
        if (Idle > IdleTicks && Copy != null) {
            discard();
        }
    }

    public static float fade(float Partial) {
        float Raw = Mth.clamp(Mth.lerp(Partial, FadeOld, Fade), 0.0f, 1.0f);
        return Raw * Raw * (3.0f - 2.0f * Raw);
    }

    public static boolean sees(Scp178Watcher Watcher) {
        return Minecraft.getInstance().player != null && (Fade > 0.0f || FadeOld > 0.0f);
    }

    public static float alpha(Scp178Watcher Watcher, float Partial) {
        if (!sees(Watcher)) {
            return 0.0f;
        }
        float Raw = fade(Partial) * Watcher.presence(Partial) * (1.0f - Watcher.leavingShare(Partial)) * (1.0f - Watcher.deathShare(Partial));
        return Math.round(Mth.clamp(Raw, 0.0f, 1.0f) * AlphaSteps) / (float) AlphaSteps;
    }

    public static float shift(int Width, float Scale) {
        return Math.max(MinShift, Math.round(Width * ShiftShare)) * Scale;
    }

    public static float sway(float Seconds) {
        return 1.0f + SwayAmount * Mth.sin(Seconds * 0.9f) * Mth.sin(Seconds * 0.37f + 1.3f);
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.level == null || Client.getCameraEntity() != Client.player) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        float Shown = fade(Partial);
        if (Shown < ShownFloor) {
            return;
        }
        float Scale = Client.options.screenEffectScale().get().floatValue();
        float Seconds = (Clock + Partial) / 20.0f;
        Graphics.flush();
        float Pixels = shift(Client.getMainRenderTarget().width, Scale);
        if (!VeilPost.run(Pipeline, Values -> Values.set("Strength", Shown).set("Shift", Pixels).set("Seconds", Seconds % 3600.0f))) {
            fallback(Client, Graphics, Shown, Scale, Seconds);
        }
    }

    private static void fallback(Minecraft Client, GuiGraphics Graphics, float Shown, float Scale, float Seconds) {
        RenderTarget Main = Client.getMainRenderTarget();
        float Pixels = shift(Main.width, Scale) * Shown * sway(Seconds);
        boolean DepthTest = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (Pixels >= 0.5f && copy(Main)) {
            float Across = Math.round(Pixels) / (float) Main.width;
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.setShaderTexture(0, Copy.getColorTextureId());
            RenderSystem.colorMask(true, false, false, false);
            quad(Graphics, Across, Shown);
            RenderSystem.colorMask(false, true, true, false);
            quad(Graphics, -Across, Shown);
            RenderSystem.colorMask(true, true, true, true);
        }
        RenderSystem.colorMask(false, true, false, false);
        dim(Graphics, FallbackGreen * Shown);
        RenderSystem.colorMask(true, true, true, true);
        edges(Graphics, Shown);
        Graphics.flush();
        RenderSystem.disableBlend();
        if (DepthTest) {
            RenderSystem.enableDepthTest();
        }
    }

    private static void quad(GuiGraphics Graphics, float Across, float Alpha) {
        Matrix4f Pose = Graphics.pose().last().pose();
        float Width = Graphics.guiWidth();
        float Height = Graphics.guiHeight();
        BufferBuilder Builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        Builder.addVertex(Pose, 0.0f, 0.0f, 0.0f).setUv(Across, 1.0f).setColor(1.0f, 1.0f, 1.0f, Alpha);
        Builder.addVertex(Pose, 0.0f, Height, 0.0f).setUv(Across, 0.0f).setColor(1.0f, 1.0f, 1.0f, Alpha);
        Builder.addVertex(Pose, Width, Height, 0.0f).setUv(1.0f + Across, 0.0f).setColor(1.0f, 1.0f, 1.0f, Alpha);
        Builder.addVertex(Pose, Width, 0.0f, 0.0f).setUv(1.0f + Across, 1.0f).setColor(1.0f, 1.0f, 1.0f, Alpha);
        BufferUploader.drawWithShader(Builder.buildOrThrow());
    }

    private static void dim(GuiGraphics Graphics, float Alpha) {
        Matrix4f Pose = Graphics.pose().last().pose();
        float Width = Graphics.guiWidth();
        float Height = Graphics.guiHeight();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder Builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        Builder.addVertex(Pose, 0.0f, 0.0f, 0.0f).setColor(0.0f, 0.0f, 0.0f, Alpha);
        Builder.addVertex(Pose, 0.0f, Height, 0.0f).setColor(0.0f, 0.0f, 0.0f, Alpha);
        Builder.addVertex(Pose, Width, Height, 0.0f).setColor(0.0f, 0.0f, 0.0f, Alpha);
        Builder.addVertex(Pose, Width, 0.0f, 0.0f).setColor(0.0f, 0.0f, 0.0f, Alpha);
        BufferUploader.drawWithShader(Builder.buildOrThrow());
    }

    private static void edges(GuiGraphics Graphics, float Shown) {
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        int Band = Math.max(2, Math.round(Width * EdgeShare / EdgeSteps));
        for (int Step = 0; Step < EdgeSteps; Step++) {
            float Alpha = EdgeAlpha * Shown * (EdgeSteps - Step) / EdgeSteps;
            Graphics.fill(Step * Band, 0, (Step + 1) * Band, Height, argb(Alpha, RedTint));
            Graphics.fill(Width - (Step + 1) * Band, 0, Width - Step * Band, Height, argb(Alpha, BlueTint));
        }
    }

    private static boolean copy(RenderTarget Main) {
        if (CopyBroken) {
            return false;
        }
        try {
            if (Copy == null) {
                Copy = new TextureTarget(Main.width, Main.height, false, Minecraft.ON_OSX);
            } else if (Copy.width != Main.width || Copy.height != Main.height) {
                Copy.resize(Main.width, Main.height, Minecraft.ON_OSX);
            }
            GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, Main.frameBufferId);
            GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, Copy.frameBufferId);
            GlStateManager._glBlitFrameBuffer(0, 0, Main.width, Main.height, 0, 0, Copy.width, Copy.height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
            Main.bindWrite(false);
            return true;
        } catch (RuntimeException Problem) {
            Log.warn("SCP-178 could not copy the screen for its fallback overlay; only the tint will show", Problem);
            CopyBroken = true;
            Main.bindWrite(false);
            return false;
        }
    }

    private static int argb(float Alpha, int Colour) {
        int A = Mth.clamp(Math.round(Alpha * 255.0f), 0, 255);
        return A << 24 | Colour & 0xFFFFFF;
    }

    public static void discard() {
        if (Copy != null) {
            Copy.destroyBuffers();
            Copy = null;
        }
    }

    public static void reset() {
        discard();
        CopyBroken = false;
        Fade = 0.0f;
        FadeOld = 0.0f;
        Idle = 0;
    }
}
