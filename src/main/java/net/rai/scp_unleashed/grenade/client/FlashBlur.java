package net.rai.scp_unleashed.grenade.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.grenade.GrenadeTuning;
import net.rai.scp_unleashed.shadercompat.client.VeilPost;

public final class FlashBlur {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "grenade_flash_blur");
    public static final ResourceLocation Pipeline = VeilPost.id("grenade_flash");
    public static final float ShownFloor = 0.004F;
    public static final float SigmaShare = 0.5F;
    public static final int VeilColour = 0xF6F0E4;
    public static final float VeilAlpha = 0.2F;
    public static final float[] BandAlphas = {0.32F, 0.24F, 0.16F, 0.09F, 0.04F};
    public static final int BandTexels = 3;
    public static final int TexelTarget = 90;

    private FlashBlur() {
    }

    public static void tick() {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.getCameraEntity() != Client.player) {
            return;
        }
        float Strength = FlashOverlay.blur(Delta.getGameTimeDeltaPartialTick(false)) * Client.options.screenEffectScale().get().floatValue();
        if (Strength < ShownFloor) {
            return;
        }
        Graphics.flush();
        float Sigma = SigmaShare * GrenadeTuning.BlurRadius * Strength * Client.getMainRenderTarget().height / GrenadeTuning.BlurReferenceHeight;
        if (!VeilPost.run(Pipeline, Values -> Values.set("Sigma", Sigma).set("Strength", Strength))) {
            fallback(Client, Graphics, Strength);
        }
    }

    private static void fallback(Minecraft Client, GuiGraphics Graphics, float Strength) {
        Window Display = Client.getWindow();
        int Width = Display.getWidth();
        int Height = Display.getHeight();
        float Scale = (float) Display.getGuiScale();
        int Texel = Math.max(2, Math.round(Math.min(Width, Height) / (float) TexelTarget));
        int Band = BandTexels * Texel;
        PoseStack Pose = Graphics.pose();
        Pose.pushPose();
        Pose.scale(1.0F / Scale, 1.0F / Scale, 1.0F);
        Graphics.fill(0, 0, Width, Height, argb(VeilAlpha * Strength));
        for (int Step = 0; Step < BandAlphas.length; Step++) {
            int Colour = argb(BandAlphas[Step] * Strength);
            int Inset = Step * Band;
            Graphics.fill(Inset, Inset, Width - Inset, Inset + Band, Colour);
            Graphics.fill(Inset, Height - Inset - Band, Width - Inset, Height - Inset, Colour);
            Graphics.fill(Inset, Inset + Band, Inset + Band, Height - Inset - Band, Colour);
            Graphics.fill(Width - Inset - Band, Inset + Band, Width - Inset, Height - Inset - Band, Colour);
            int Corner = Inset + Band;
            Graphics.fill(Corner, Corner, Corner + Band, Corner + Band, Colour);
            Graphics.fill(Width - Corner - Band, Corner, Width - Corner, Corner + Band, Colour);
            Graphics.fill(Corner, Height - Corner - Band, Corner + Band, Height - Corner, Colour);
            Graphics.fill(Width - Corner - Band, Height - Corner - Band, Width - Corner, Height - Corner, Colour);
        }
        Pose.popPose();
        Graphics.flush();
    }

    private static int argb(float Alpha) {
        int A = Mth.clamp(Math.round(Alpha * 255.0F), 0, 255);
        return A << 24 | VeilColour & 0xFFFFFF;
    }

    public static void reset() {
    }
}
