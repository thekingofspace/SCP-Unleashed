package net.rai.scp_unleashed.scp5001.mekhane.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.shadercompat.client.VeilPost;

public final class MekhaneOverlay {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "mekhane_overlay");
    public static final ResourceLocation Bloom = VeilPost.id("grenade_flash");
    public static final int FlashColour = 0xFFF2D8;
    public static final int TintColour = 0xFFE4A6;
    public static final float BloomFloor = 0.25f;

    private MekhaneOverlay() {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        float Comfort = Client.options.screenEffectScale().get().floatValue();
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        float Tint = MekhaneClientState.tint(Partial);
        if (Tint > 0.01f) {
            float Pulse = 0.07f + 0.03f * Mth.sin((MekhaneClientState.ticks() + Partial) * 0.12f) + 0.05f * MekhaneClientState.alarm();
            Graphics.fill(0, 0, Width, Height, argb(TintColour, Tint * Pulse));
        }
        float Flash = MekhaneClientState.flash(Partial) * (0.4f + 0.6f * Comfort);
        if (Flash > 0.01f) {
            if (Flash > BloomFloor && Comfort > 0.0f) {
                Graphics.flush();
                float Sigma = 4.0f * Flash * Client.getMainRenderTarget().height / 1080.0f;
                VeilPost.run(Bloom, Values -> Values.set("Sigma", Sigma).set("Strength", Flash));
            }
            Graphics.fill(0, 0, Width, Height, argb(FlashColour, Math.min(0.85f, Flash)));
        }
    }

    private static int argb(int Colour, float Alpha) {
        int A = Mth.clamp(Math.round(Alpha * 255.0f), 0, 255);
        return A << 24 | Colour & 0xFFFFFF;
    }
}
