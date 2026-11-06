package net.rai.scp_unleashed.scp420j.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp420j.Scp420jRegistry;
import net.rai.scp_unleashed.shadercompat.client.VeilPost;

public final class Scp420jVivid {
    public static final ResourceLocation LayerId = Scp420jRegistry.location("scp420j_vivid");
    public static final ResourceLocation Pipeline = VeilPost.id("scp420j_vivid");
    public static final float ShownFloor = 0.003f;
    public static final float WashAlpha = 0.07f;
    public static final float[] BandAlphas = {0.26f, 0.18f, 0.11f, 0.05f};
    public static final int BandTexels = 3;
    public static final int TexelTarget = 90;
    public static final float[] Hues = {0.02f, 0.09f, 0.14f, 0.95f, 0.83f, 0.55f};
    public static final float HueSeconds = 3.2f;

    private Scp420jVivid() {
    }

    public static void tick() {
    }

    public static float seconds(float Partial) {
        return (Scp420jClientState.clock() + Partial) / 20.0f;
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.getCameraEntity() != Client.player) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        float Strength = Scp420jClientState.shown(Partial) * Client.options.screenEffectScale().get().floatValue();
        if (Strength < ShownFloor) {
            return;
        }
        Graphics.flush();
        float Seconds = seconds(Partial);
        if (!VeilPost.run(Pipeline, Values -> Values.set("Vivid", Strength).set("Seconds", Seconds % 3600.0f))) {
            fallback(Client, Graphics, Strength, Seconds);
        }
    }

    public static int tint(float Seconds, float Shift) {
        float Position = (Seconds / HueSeconds + Shift) % Hues.length;
        int Index = Mth.floor(Position);
        float Blend = Position - Index;
        float Eased = Blend * Blend * (3.0f - 2.0f * Blend);
        float From = Hues[Index % Hues.length];
        float To = Hues[(Index + 1) % Hues.length];
        float Gap = To - From;
        if (Gap > 0.5f) {
            Gap -= 1.0f;
        } else if (Gap < -0.5f) {
            Gap += 1.0f;
        }
        float Hue = From + Gap * Eased;
        return Mth.hsvToRgb(Hue - Mth.floor(Hue), 0.78f, 1.0f);
    }

    private static void fallback(Minecraft Client, GuiGraphics Graphics, float Strength, float Seconds) {
        Window Display = Client.getWindow();
        int Width = Display.getWidth();
        int Height = Display.getHeight();
        float Scale = (float) Display.getGuiScale();
        int Texel = Math.max(2, Math.round(Math.min(Width, Height) / (float) TexelTarget));
        int Band = BandTexels * Texel;
        PoseStack Pose = Graphics.pose();
        Pose.pushPose();
        Pose.scale(1.0f / Scale, 1.0f / Scale, 1.0f);
        Graphics.fill(0, 0, Width, Height, argb(WashAlpha * Strength, tint(Seconds, 0.0f)));
        int Edge = tint(Seconds, 1.5f);
        for (int Step = 0; Step < BandAlphas.length; Step++) {
            int Colour = argb(BandAlphas[Step] * Strength, Edge);
            int Inset = Step * Band;
            Graphics.fill(Inset, Inset, Width - Inset, Inset + Band, Colour);
            Graphics.fill(Inset, Height - Inset - Band, Width - Inset, Height - Inset, Colour);
            Graphics.fill(Inset, Inset + Band, Inset + Band, Height - Inset - Band, Colour);
            Graphics.fill(Width - Inset - Band, Inset + Band, Width - Inset, Height - Inset - Band, Colour);
        }
        Pose.popPose();
        Graphics.flush();
    }

    private static int argb(float Alpha, int Colour) {
        int A = Mth.clamp(Math.round(Alpha * 255.0f), 0, 255);
        return A << 24 | Colour & 0xFFFFFF;
    }

    public static void reset() {
    }
}
