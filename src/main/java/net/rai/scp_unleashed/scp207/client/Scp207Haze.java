package net.rai.scp_unleashed.scp207.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.shadercompat.client.VeilPost;


public final class Scp207Haze {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp207_haze");
    public static final ResourceLocation Pipeline = VeilPost.id("scp207_toll");
    public static final ResourceLocation Veil = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/scp207/veil.png");
    public static final int VeilWidth = 64;
    public static final int VeilHeight = 36;
    public static final float ShownFloor = 0.004f;
    public static final float BlurGuiPixels = 1.0f;
    public static final float SigmaShare = 0.58f;
    public static final float GhostGuiPixels = 3.0f;
    public static final float GhostMixDepth = 0.42f;
    public static final float SwayRate = 0.045f;
    public static final float BobRate = 0.029f;
    public static final float DimStart = 0.85f;
    public static final float DimDepth = 0.18f;
    public static final float VeilDepth = 0.3f;
    public static final float FallbackGrey = 0.45f;
    public static final float FallbackVeil = 0.6f;
    public static final int GreyColour = 0x8E8E8E;
    public static final int VeilColour = 0x6A6A6A;

    private Scp207Haze() {
    }

    public static void tick(Minecraft Client) {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.getCameraEntity() != Client.player) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        float Toll = Scp207ClientState.toll(Partial);
        if (Toll <= ShownFloor) {
            return;
        }
        float Blur = Scp207ClientState.blur(Partial);
        float Grey = Scp207ClientState.grey(Partial);
        float Dim = DimDepth * Scp207ClientState.smooth((Toll - DimStart) / (1.0f - DimStart));
        float Distortion = Client.options.screenEffectScale().get().floatValue();
        boolean Fallback = !process(Client, Graphics, Delta, Blur * Distortion, Grey, Dim);
        if (Fallback) {
            Graphics.fill(0, 0, Graphics.guiWidth(), Graphics.guiHeight(), argb(Grey * FallbackGrey + Dim, GreyColour));
            Graphics.flush();
        }
        veil(Graphics, Blur * (Fallback ? FallbackVeil : VeilDepth), VeilColour);
    }

    private static int argb(float Alpha, int Colour) {
        int A = Mth.clamp(Math.round(Alpha * 255.0f), 0, 255);
        return A << 24 | Colour & 0xFFFFFF;
    }

    private static void veil(GuiGraphics Graphics, float Alpha, int Colour) {
        if (Alpha <= 0.0f) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.setColor(((Colour >> 16) & 255) / 255.0f, ((Colour >> 8) & 255) / 255.0f, (Colour & 255) / 255.0f, Mth.clamp(Alpha, 0.0f, 1.0f));
        Graphics.blit(Veil, 0, 0, Graphics.guiWidth(), Graphics.guiHeight(), 0.0f, 0.0f, VeilWidth, VeilHeight, VeilWidth, VeilHeight);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static boolean process(Minecraft Client, GuiGraphics Graphics, DeltaTracker Delta, float Blur, float Grey, float Dim) {
        if (!VeilPost.ready(Pipeline)) {
            return false;
        }
        Graphics.flush();
        float GuiScale = (float) Client.getWindow().getGuiScale();
        float Time = Scp207ClientState.clock() + Delta.getGameTimeDeltaPartialTick(false);
        float Ghost = Blur * GhostGuiPixels * GuiScale;
        float Sigma = SigmaShare * Math.round(Blur * BlurGuiPixels * GuiScale);
        return VeilPost.run(Pipeline, Values -> Values.set("Sigma", Sigma)
                .set("GhostX", Ghost * (0.75f + 0.25f * Mth.sin(Time * SwayRate)))
                .set("GhostY", Ghost * 0.3f * Mth.sin(Time * BobRate))
                .set("GhostMix", GhostMixDepth * Blur)
                .set("Saturation", 1.0f - Grey)
                .set("Dim", Dim));
    }

    public static void reset() {
    }
}
