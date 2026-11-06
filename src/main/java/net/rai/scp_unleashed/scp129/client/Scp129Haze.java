package net.rai.scp_unleashed.scp129.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.shadercompat.client.VeilPost;


public final class Scp129Haze {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp129_haze");
    public static final ResourceLocation Pipeline = VeilPost.id("scp129_haze");
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/scp129/haze.png");
    public static final int TextureWidth = 64;
    public static final int TextureHeight = 36;
    public static final float MaxRadius = 7.0f;
    public static final float SigmaShare = 0.9f;
    public static final float ShownFloor = 0.003f;
    public static final float SharpHaze = 0.22f;
    public static final float FallbackHaze = 0.62f;
    public static final float FallbackWash = 0.3f;
    public static final int WashColour = 0xB7C7A4;
    public static final int HazeColour = 0xD4E2C2;
    public static final int SickColour = 0x6E8A55;
    public static final float[] SickAlpha = {0.0f, 0.0f, 0.0f, 0.28f, 0.44f};
    public static final float SickPulse = 0.06f;
    public static final int PulseSteps = 4;

    private Scp129Haze() {
    }

    public static void tick(Minecraft Client) {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.getCameraEntity() != Client.player) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        float Shown = Scp129ClientState.blur(Partial);
        if (Shown > ShownFloor) {
            boolean Fallback = !VeilPost.ready(Pipeline) || !blurWorld(Graphics, Shown * Client.options.screenEffectScale().get().floatValue());
            float Haze = Mth.clamp(Shown, 0.0f, 1.0f);
            if (Fallback) {
                Graphics.fill(0, 0, Graphics.guiWidth(), Graphics.guiHeight(), argb(Haze * FallbackWash, WashColour));
                Graphics.flush();
            }
            veil(Graphics, Haze * (Fallback ? FallbackHaze : SharpHaze), HazeColour);
        }
        int Stage = Scp129ClientState.stage();
        if (Stage > 0 && Stage < SickAlpha.length && SickAlpha[Stage] > 0.0f) {
            float Wave = 0.5f + 0.5f * Mth.sin((Scp129ClientState.clock() + Partial) * 0.05f);
            float Stepped = Math.round(Wave * PulseSteps) / (float) PulseSteps;
            veil(Graphics, SickAlpha[Stage] + SickPulse * Stepped, SickColour);
        }
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
        Graphics.blit(Texture, 0, 0, Graphics.guiWidth(), Graphics.guiHeight(), 0.0f, 0.0f, TextureWidth, TextureHeight, TextureWidth, TextureHeight);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static boolean blurWorld(GuiGraphics Graphics, float Shown) {
        if (Shown <= ShownFloor) {
            return true;
        }
        Graphics.flush();
        return VeilPost.run(Pipeline, Values -> Values.set("Sigma", SigmaShare * MaxRadius * Shown));
    }

    public static void reset() {
    }
}
