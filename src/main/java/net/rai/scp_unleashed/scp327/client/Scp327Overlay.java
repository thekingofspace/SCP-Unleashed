package net.rai.scp_unleashed.scp327.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp327.song.Scp327Song;

public final class Scp327Overlay {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp327_lure_overlay");
    public static final ResourceLocation Tide = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/scp327/tide.png");
    public static final int TideWidth = 96;
    public static final int TideHeight = 54;
    public static final float BaseShade = 0.25f;
    public static final float ShadeRange = 0.55f;
    public static final float Swell = 0.12f;
    public static final float HeldShade = 0.35f;

    private Scp327Overlay() {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.options.hideGui) {
            return;
        }
        float Amount = Scp327ClientLure.intensity();
        float Held = Scp327ClientStruggle.isHeld() ? 1.0f : 0.0f;
        if (Amount < 0.01f && Held <= 0.0f) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        float Bar = (Scp327ClientLure.clock() + Partial) / Scp327Song.Bar;
        float Pulse = 0.5f + 0.5f * Mth.sin(Bar * Mth.TWO_PI);
        float Alpha = Math.min(1.0f, (BaseShade + ShadeRange * Amount) * Math.min(1.0f, Amount * 5.0f) + Swell * Pulse * Amount + HeldShade * Held);
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        int Bob = Math.round(Mth.sin(Bar * Mth.PI) * Amount * 2.0f);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.setColor(1.0f, 1.0f, 1.0f, Alpha);
        Graphics.blit(Tide, 0, Bob - 2, -90, 0.0f, 0.0f, Width, Height + 4, Width, Height + 4);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
