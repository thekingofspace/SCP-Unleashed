package net.rai.scp_unleashed.scp902.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp902.Scp902Registry;
import net.rai.scp_unleashed.shadercompat.client.VeilPost;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;
import org.slf4j.Logger;

import java.nio.ByteBuffer;

public final class Scp902Overlay {
    public static final ResourceLocation GradeLayer = Scp902Registry.location("scp902_grade");
    public static final ResourceLocation CinemaLayer = Scp902Registry.location("scp902_cinema");
    public static final ResourceLocation Pipeline = VeilPost.id("scp902_grade");
    public static final ResourceLocation Vignette = Scp902Registry.location("textures/gui/scp902/vignette.png");
    public static final ResourceLocation Lens = Scp902Registry.location("textures/gui/scp902/lens.png");
    public static final String TextKey = "message.scp_unleashed.scp902.not_yet_time";
    public static final int VignetteWidth = 64;
    public static final int VignetteHeight = 36;
    public static final int LensSize = 32;
    public static final int StreakWidth = 8;
    public static final float Drain = 0.58f;
    public static final float Sepia = 0.5f;
    public static final float Warmth = 0.55f;
    public static final float FallbackSepia = 0.16f;
    public static final int FallbackColour = 0x6E5A3C;
    public static final float VignetteAlpha = 0.85f;
    public static final float LensAlpha = 0.75f;
    public static final float BarShare = 0.11f;
    public static final int WhiteColour = 0xFFF4E6;
    public static final int TextColour = 0xF2E4C8;
    public static final float TextScale = 2.0f;
    public static final float[][] Streaks = {
            {0.08f, 0.31f, 0.0f, 0.0f}, {0.21f, 0.22f, 0.37f, 1.0f}, {0.34f, 0.36f, 0.71f, 2.0f}, {0.52f, 0.27f, 0.13f, 3.0f},
            {0.66f, 0.33f, 0.52f, 1.0f}, {0.79f, 0.25f, 0.86f, 0.0f}, {0.92f, 0.29f, 0.29f, 2.0f}};
    public static final int CheckFrames = 40;
    public static final int DarkFrames = 3;
    public static final float LitFloor = 0.04f;
    public static final float DarkCeiling = 0.008f;
    public static final float[][] Probes = {{0.5f, 0.5f}, {0.3f, 0.55f}, {0.7f, 0.55f}, {0.5f, 0.7f}, {0.5f, 0.35f}};
    private static final Logger Log = LogUtils.getLogger();
    private static int Checks;
    private static int Dark;
    private static boolean Broken;

    private Scp902Overlay() {
    }

    public static boolean hidesHud() {
        return Scp902ClientScene.cinematic();
    }

    public static void grade(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || !Scp902ClientScene.inScene()) {
            return;
        }
        Graphics.flush();
        if (!Broken && post(Client)) {
            return;
        }
        Graphics.fill(0, 0, Graphics.guiWidth(), Graphics.guiHeight(), argb(FallbackSepia, FallbackColour));
        Graphics.flush();
    }

    private static boolean post(Minecraft Client) {
        RenderTarget Main = Client.getMainRenderTarget();
        Camera View = Client.gameRenderer.getMainCamera();
        float Fov = Client.options.fov().get().floatValue();
        float Aspect = Main.width / (float) Math.max(1, Main.height);
        float Across = Mth.wrapDegrees(Scp902Sky.SunYaw - View.getYRot());
        float Rise = -Scp902Sky.SunElevation - View.getXRot();
        float HalfV = Fov * 0.5f * Mth.DEG_TO_RAD;
        float HalfH = (float) Math.atan(Math.tan(HalfV) * Aspect);
        float SunX = 0.5f + (float) Math.tan(Mth.clamp(Across, -80.0f, 80.0f) * Mth.DEG_TO_RAD) / (2.0f * (float) Math.tan(HalfH));
        float SunY = 0.5f - (float) Math.tan(Mth.clamp(Rise, -80.0f, 80.0f) * Mth.DEG_TO_RAD) / (2.0f * (float) Math.tan(HalfV));
        float Shown = Mth.clamp(1.0f - (Math.abs(Across) - 60.0f) / 50.0f, 0.0f, 1.0f);
        boolean Checking = Checks < CheckFrames;
        float Before = Checking ? probe(Main) : 0.0f;
        boolean Done = VeilPost.run(Pipeline, Values -> Values.set("Drain", Drain).set("Sepia", Sepia).set("Warmth", Warmth * Shown)
                .set("SunX", SunX).set("SunY", 1.0f - SunY));
        if (!Done) {
            return false;
        }
        if (Checking) {
            Checks++;
            float After = probe(Main);
            if (Before > LitFloor && After < DarkCeiling) {
                Dark++;
                if (Dark >= DarkFrames) {
                    Log.warn("SCP-902 grading turned a lit frame black, switching to the plain overlay");
                    Broken = true;
                    discard();
                }
            } else {
                Dark = 0;
            }
        }
        return true;
    }

    private static float probe(RenderTarget Main) {
        Main.bindWrite(false);
        float Total = 0.0f;
        try (MemoryStack Stack = MemoryStack.stackPush()) {
            ByteBuffer Pixel = Stack.malloc(4);
            for (float[] Spot : Probes) {
                int X = Math.max(0, Math.min(Main.width - 1, Math.round(Main.width * Spot[0])));
                int Y = Math.max(0, Math.min(Main.height - 1, Math.round(Main.height * Spot[1])));
                GL11.glReadPixels(X, Y, 1, 1, GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, Pixel);
                Total += ((Pixel.get(0) & 255) + (Pixel.get(1) & 255) + (Pixel.get(2) & 255)) / 765.0f;
            }
        }
        return Total / Probes.length;
    }

    public static void cinema(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Scp902ClientScene.mode() == Scp902ClientScene.ModeNone) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        if (Scp902ClientScene.inScene()) {
            float Ticks = Scp902ClientScene.sceneTicks(Partial);
            texture(Graphics, Vignette, 0, 0, Width, Height, 0, 0, VignetteWidth, VignetteHeight, VignetteWidth, VignetteHeight, VignetteAlpha);
            lens(Graphics, Width, Height, Ticks);
        }
        float Bars = Scp902ClientScene.bars(Partial);
        if (Bars > 0.0f) {
            int Bar = Math.round(Height * BarShare * Bars);
            Graphics.fill(0, 0, Width, Bar, 0xFF000000);
            Graphics.fill(0, Height - Bar, Width, Height, 0xFF000000);
        }
        float Text = Scp902ClientScene.text(Partial);
        if (Text > 0.01f) {
            Font Writer = Client.font;
            Component Line = Component.translatable(TextKey);
            Graphics.pose().pushPose();
            Graphics.pose().translate(Width / 2.0f, Height / 2.0f, 0.0f);
            Graphics.pose().scale(TextScale, TextScale, 1.0f);
            int Alpha = Mth.clamp(Math.round(Text * 255.0f), 5, 255);
            Graphics.drawString(Writer, Line, -Writer.width(Line) / 2, -Writer.lineHeight / 2, Alpha << 24 | TextColour, true);
            Graphics.pose().popPose();
        }
        float White = Scp902ClientScene.white(Partial);
        if (White > 0.0f) {
            Graphics.fill(0, 0, Width, Height, argb(White, WhiteColour));
        }
        Graphics.flush();
    }

    private static void lens(GuiGraphics Graphics, int Width, int Height, float Ticks) {
        int Scale = 2;
        int SpriteHeight = LensSize * Scale;
        int SpriteWidth = StreakWidth * Scale;
        for (float[] Streak : Streaks) {
            float Travel = (Ticks * Streak[1] + Streak[2] * (Height + SpriteHeight)) % (Height + SpriteHeight);
            int X = Math.round(Streak[0] * Width) - SpriteWidth / 2;
            int Y = Math.round(Travel) - SpriteHeight;
            Y -= Y % Scale;
            int Sprite = (int) Streak[3];
            texture(Graphics, Lens, X, Y, SpriteWidth, SpriteHeight, Sprite * StreakWidth, 0, StreakWidth, LensSize, LensSize, LensSize, LensAlpha);
        }
    }

    private static void texture(GuiGraphics Graphics, ResourceLocation Texture, int X, int Y, int Width, int Height, int U, int V, int RegionWidth, int RegionHeight,
                                int TextureWidth, int TextureHeight, float Alpha) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.setColor(1.0f, 1.0f, 1.0f, Mth.clamp(Alpha, 0.0f, 1.0f));
        Graphics.blit(Texture, X, Y, Width, Height, U, V, RegionWidth, RegionHeight, TextureWidth, TextureHeight);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static int argb(float Alpha, int Colour) {
        int A = Mth.clamp(Math.round(Alpha * 255.0f), 0, 255);
        return A << 24 | Colour & 0xFFFFFF;
    }

    public static void tick() {
        if (Scp902ClientScene.mode() == Scp902ClientScene.ModeNone) {
            discard();
        }
    }

    public static void discard() {
        Checks = 0;
        Dark = 0;
    }

    public static void reset() {
        discard();
        Broken = false;
    }
}
