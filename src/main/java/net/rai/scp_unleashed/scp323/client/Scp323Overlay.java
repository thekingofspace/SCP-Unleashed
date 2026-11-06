package net.rai.scp_unleashed.scp323.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp323.Scp323Registry;

public final class Scp323Overlay {
    public static final ResourceLocation LureLayerId = Scp323Registry.location("scp323_lure_overlay");
    public static final ResourceLocation WatchLayerId = Scp323Registry.location("scp323_watch_overlay");
    public static final ResourceLocation LureVignette = Scp323Registry.location("textures/gui/scp323/lure.png");
    public static final ResourceLocation Overgrowth = Scp323Registry.location("textures/gui/scp323/overgrowth.png");
    public static final float BaseShade = 0.2f;
    public static final float ShadeRange = 0.45f;
    public static final float PulseShade = 0.12f;
    public static final float WatchShade = 0.78f;
    public static final float WatchPulse = 0.12f;
    public static final float PulsePeriod = 90.0f;

    private Scp323Overlay() {
    }

    public static void renderLure(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.options.hideGui) {
            return;
        }
        float Amount = Scp323ClientLure.intensity();
        if (Amount < 0.01f) {
            return;
        }
        float Pulse = Scp323ClientLure.pulse(Delta.getGameTimeDeltaPartialTick(false));
        float Alpha = Math.min(1.0f, (BaseShade + ShadeRange * Amount) * Math.min(1.0f, Amount * 6.0f) + PulseShade * Pulse * Amount);
        draw(Graphics, LureVignette, Alpha);
    }

    public static void renderWatch(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        float Fade = Scp323ClientWatch.fade(Partial);
        if (Fade <= 0.0f) {
            return;
        }
        float Time = Scp323ClientWatch.age() + Partial;
        float Pulse = 0.5f + 0.5f * Mth.sin(Time / PulsePeriod * Mth.TWO_PI);
        draw(Graphics, Overgrowth, Fade * (WatchShade + WatchPulse * Pulse));
    }

    private static void draw(GuiGraphics Graphics, ResourceLocation Texture, float Alpha) {
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.setColor(1.0f, 1.0f, 1.0f, Math.min(1.0f, Alpha));
        Graphics.blit(Texture, 0, 0, -90, 0.0f, 0.0f, Width, Height, Width, Height);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
