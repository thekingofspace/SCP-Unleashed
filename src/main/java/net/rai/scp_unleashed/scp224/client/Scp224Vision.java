package net.rai.scp_unleashed.scp224.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.shadercompat.client.VeilPost;

public final class Scp224Vision {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp224_age_vision");
    public static final ResourceLocation Pipeline = VeilPost.id("scp224_age");
    public static final float ShownFloor = 0.003f;
    public static final double Onset = 0.75;
    public static final int WashColour = 0x8E8B86;
    public static final float WashAlpha = 0.5f;
    public static final int ShadeColour = 0x2B2824;
    public static final float[] RingAlphas = {0.34f, 0.24f, 0.15f, 0.07f};
    public static final int RingTexels = 3;
    public static final int TexelTarget = 90;

    private Scp224Vision() {
    }

    public static float strength(float Shown) {
        return (float) Math.pow(Mth.clamp(Shown, 0.0f, 1.0f), Onset);
    }

    public static void tick(Minecraft Client) {
        Scp224ClientState.tick();
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.getCameraEntity() != Client.player) {
            return;
        }
        float Shown = strength(Scp224ClientState.shown(Delta.getGameTimeDeltaPartialTick(false)));
        if (Shown < ShownFloor) {
            return;
        }
        Graphics.flush();
        if (!VeilPost.run(Pipeline, Values -> Values.set("Age", Shown))) {
            fallback(Client, Graphics, Shown);
        }
    }

    private static void fallback(Minecraft Client, GuiGraphics Graphics, float Shown) {
        Window Display = Client.getWindow();
        int Width = Display.getWidth();
        int Height = Display.getHeight();
        float Scale = (float) Display.getGuiScale();
        int Texel = Math.max(2, Math.round(Math.min(Width, Height) / (float) TexelTarget));
        PoseStack Pose = Graphics.pose();
        Pose.pushPose();
        Pose.scale(1.0f / Scale, 1.0f / Scale, 1.0f);
        Graphics.fill(0, 0, Width, Height, argb(WashAlpha * Shown * (0.55f + 0.45f * Shown), WashColour));
        int Band = RingTexels * Texel;
        for (int Ring = 0; Ring < RingAlphas.length; Ring++) {
            int Colour = argb(RingAlphas[Ring] * Shown, ShadeColour);
            int Inset = Ring * Band;
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
        Scp224ClientState.reset();
    }
}
