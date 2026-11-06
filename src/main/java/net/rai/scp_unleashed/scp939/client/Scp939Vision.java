package net.rai.scp_unleashed.scp939.client;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Window;
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
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp939.Scp939Registry;
import net.rai.scp_unleashed.scp939.compressor.Amnestics;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import net.rai.scp_unleashed.shadercompat.client.VeilPost;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public final class Scp939Vision {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp939_vision");
    public static final ResourceLocation Pipeline = VeilPost.id("scp939_vision");
    public static final float EchoBase = 0.38f;
    public static final float EchoPerLevel = 0.12f;
    public static final float EchoExposure = 0.2f;
    public static final float EchoMax = 0.86f;
    public static final int EchoFade = 40;
    public static final float FallbackEcho = 0.8f;
    public static final int IdleTicks = 200;
    public static final int TexelTarget = 180;
    public static final int BandRows = 3;
    public static final int SmearColour = 0xB4262A;
    public static final int BleedColour = 0x2AA6B0;
    public static final int FlickerColour = 0x0A0608;
    private static boolean Broken;
    private static int ChainWidth;
    private static int ChainHeight;
    private static boolean Primed;
    private static TextureTarget Previous;
    private static boolean PreviousFresh;
    private static int Idle;
    private static float Echo;
    private static float EchoOld;
    private static float Wobble;

    private Scp939Vision() {
    }

    public static float echoTarget(LocalPlayer User) {
        MobEffectInstance Active = User.getEffect(Scp939Registry.Amnestic);
        if (Active == null) {
            return 0.0f;
        }
        int Level = Mth.clamp(Active.getAmplifier(), 0, Amnestics.MaxLevel);
        float Strength = Math.min(EchoMax, EchoBase + EchoPerLevel * Level + EchoExposure * Scp939ClientState.exposure());
        float Fade = Active.isInfiniteDuration() ? 1.0f : Mth.clamp(Active.getDuration() / (float) EchoFade, 0.0f, 1.0f);
        return Strength * Fade;
    }

    public static float wobbleTarget(LocalPlayer User) {
        MobEffectInstance Active = User.getEffect(Scp939Registry.Amnestic);
        if (Active == null || Active.getAmplifier() < 2) {
            return 0.0f;
        }
        return (Active.getAmplifier() - 1) * 0.5f * Mth.clamp(Active.getDuration() / (float) EchoFade, 0.0f, 1.0f);
    }

    public static void tick(Minecraft Client) {
        EchoOld = Echo;
        LocalPlayer User = Client.player;
        if (User == null) {
            reset();
            return;
        }
        if (Client.isPaused()) {
            return;
        }
        float Scale = Client.options.screenEffectScale().get().floatValue();
        float Wanted = echoTarget(User) * Scale;
        Echo += (Wanted - Echo) * 0.2f;
        if (Wanted <= 0.0f && Echo < 0.004f) {
            Echo = 0.0f;
        }
        Wobble += (wobbleTarget(User) * Scale - Wobble) * 0.1f;
        boolean Quiet = Echo <= 0.0f && Scp939ClientState.glitch(1.0f) <= 0.0f;
        Idle = Quiet ? Idle + 1 : 0;
        if (Idle > IdleTicks) {
            discardChain();
            discardPrevious();
        }
        if (Echo <= 0.0f) {
            Primed = false;
            PreviousFresh = false;
        }
    }

    public static float echo(float Partial) {
        return Mth.lerp(Partial, EchoOld, Echo);
    }

    public static float wobble() {
        return Wobble;
    }

    private static double seconds(Minecraft Client, float Partial) {
        return Client.level == null ? 0.0 : (Client.level.getGameTime() % 24000L + Partial) / 20.0;
    }

    public static void renderLevel(float Partial, float Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || ShaderCompat.skipPostEffects() || Client.getCameraEntity() != Client.player) {
            return;
        }
        float Glitch = Scp939ClientState.glitch(Partial);
        float Shown = echo(Partial);
        if (Glitch < 0.003f && Shown < 0.003f) {
            return;
        }
        RenderTarget Main = Client.getMainRenderTarget();
        if (Main.width != ChainWidth || Main.height != ChainHeight) {
            ChainWidth = Main.width;
            ChainHeight = Main.height;
            Primed = false;
        }
        float Prime = Primed && Shown > 0.003f ? 0.0f : 1.0f;
        float Seconds = (float) seconds(Client, Partial);
        Broken = !VeilPost.run(Pipeline, Values -> Values.set("Glitch", Mth.clamp(Glitch, 0.0f, 1.0f))
                .set("Echo", Mth.clamp(Shown, 0.0f, EchoMax))
                .set("Wobble", Mth.clamp(Wobble, 0.0f, 1.5f))
                .set("Seconds", Seconds)
                .set("Prime", Prime));
        Primed = !Broken && Shown > 0.003f;
    }

    public static void renderOverlay(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.getCameraEntity() != Client.player || !ShaderCompat.skipPostEffects() && !Broken) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        float Shown = echo(Partial);
        float Glitch = Scp939ClientState.glitch(Partial);
        Graphics.flush();
        if (Shown > 0.003f) {
            echoOverlay(Client, Graphics, Shown);
        } else {
            PreviousFresh = false;
        }
        if (Glitch > 0.003f) {
            glitchOverlay(Client, Graphics, Glitch, seconds(Client, Partial));
        }
    }

    private static void echoOverlay(Minecraft Client, GuiGraphics Graphics, float Shown) {
        RenderTarget Main = Client.getMainRenderTarget();
        if (Previous == null) {
            Previous = new TextureTarget(Main.width, Main.height, false, Minecraft.ON_OSX);
            PreviousFresh = false;
        } else if (Previous.width != Main.width || Previous.height != Main.height) {
            Previous.resize(Main.width, Main.height, Minecraft.ON_OSX);
            PreviousFresh = false;
        }
        if (PreviousFresh) {
            Window Display = Client.getWindow();
            float Width = Display.getGuiScaledWidth();
            float Height = Display.getGuiScaledHeight();
            Matrix4f Matrix = Graphics.pose().last().pose();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableDepthTest();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0, Previous.getColorTextureId());
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, Mth.clamp(Shown * FallbackEcho, 0.0f, 0.9f));
            BufferBuilder Builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            Builder.addVertex(Matrix, 0.0f, Height, 0.0f).setUv(0.0f, 0.0f);
            Builder.addVertex(Matrix, Width, Height, 0.0f).setUv(1.0f, 0.0f);
            Builder.addVertex(Matrix, Width, 0.0f, 0.0f).setUv(1.0f, 1.0f);
            Builder.addVertex(Matrix, 0.0f, 0.0f, 0.0f).setUv(0.0f, 1.0f);
            BufferUploader.drawWithShader(Builder.buildOrThrow());
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.enableDepthTest();
            RenderSystem.disableBlend();
        }
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, Main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, Previous.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, Main.width, Main.height, 0, 0, Previous.width, Previous.height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        Main.bindWrite(true);
        PreviousFresh = true;
    }

    private static int hash(int Value) {
        int Mixed = Value * 0x45D9F3B;
        Mixed = (Mixed ^ (Mixed >>> 16)) * 0x45D9F3B;
        return Mixed ^ (Mixed >>> 16);
    }

    private static float roll(int Value) {
        return (hash(Value) & 0xFFFF) / 65535.0f;
    }

    private static int argb(float Alpha, int Colour) {
        int A = Mth.clamp(Math.round(Alpha * 255.0f), 0, 255);
        return A << 24 | Colour & 0xFFFFFF;
    }

    private static void glitchOverlay(Minecraft Client, GuiGraphics Graphics, float Glitch, double Seconds) {
        Window Display = Client.getWindow();
        int Width = Display.getWidth();
        int Height = Display.getHeight();
        int Texel = Math.max(2, Math.round(Math.min(Width, Height) / (float) TexelTarget));
        int Band = Texel * BandRows;
        int Tick = (int) Math.floor(Seconds * 12.0);
        float Scale = (float) Display.getGuiScale();
        PoseStack Pose = Graphics.pose();
        Pose.pushPose();
        Pose.scale(1.0f / Scale, 1.0f / Scale, 1.0f);
        boolean Burst = roll(Tick * 31 + 7) > 1.0f - 0.18f * Glitch;
        for (int Row = 0; Row * Band < Height; Row++) {
            float Chance = roll(Row * 977 + Tick * 131);
            if (Chance < 1.0f - (0.22f + (Burst ? 0.25f : 0.0f)) * Glitch) {
                continue;
            }
            int Y = Row * Band;
            int Shift = Math.round((roll(Row * 53 + Tick * 17) - 0.5f) * Width * 0.06f * Glitch) / Texel * Texel;
            int Start = Math.round(roll(Row * 11 + Tick * 5) * Width * 0.5f) / Texel * Texel;
            int Length = Math.round((0.2f + 0.6f * roll(Row * 19 + Tick)) * Width) / Texel * Texel;
            Graphics.fill(Start + Shift, Y, Start + Shift + Length, Y + Texel, argb(0.22f * Glitch, SmearColour));
            Graphics.fill(Start - Shift, Y + Texel, Start - Shift + Length, Y + Texel * 2, argb(0.14f * Glitch, BleedColour));
        }
        if (roll(Tick * 7 + 3) > 0.82f) {
            Graphics.fill(0, 0, Width, Height, argb(0.1f * Glitch, FlickerColour));
        }
        Pose.popPose();
        Graphics.flush();
    }

    public static boolean broken() {
        return Broken;
    }

    public static void discardChain() {
        ChainWidth = 0;
        ChainHeight = 0;
        Primed = false;
    }

    public static void discardPrevious() {
        if (Previous != null) {
            Previous.destroyBuffers();
            Previous = null;
        }
        PreviousFresh = false;
    }

    public static void reset() {
        Echo = 0.0f;
        EchoOld = 0.0f;
        Wobble = 0.0f;
        Idle = 0;
        discardChain();
        discardPrevious();
        Broken = false;
    }
}
