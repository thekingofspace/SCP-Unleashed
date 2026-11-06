package net.rai.scp_unleashed.scp427.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp427.Scp427Exposure;
import net.rai.scp_unleashed.scp427.Scp427Registry;

public final class Scp427Overlay {
    public static final ResourceLocation LayerId = Scp427Registry.location("scp427_overlay");
    public static final ResourceLocation GlowTexture = Scp427Registry.location("textures/gui/scp427/glow.png");
    public static final ResourceLocation FleshTexture = Scp427Registry.location("textures/gui/scp427/flesh.png");
    public static final float GlowShade = 0.22f;
    public static final float GlowPulse = 0.05f;
    public static final float WarnShade = 0.14f;
    public static final float WarnRange = 0.2f;
    public static final float BeatShade = 0.12f;
    public static final float TransformShade = 0.45f;
    public static final float TransformRange = 0.35f;

    private Scp427Overlay() {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.options.hideGui) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        float Clock = Scp427ClientState.clock(Partial);
        float Glow = Scp427ClientState.glow(Partial);
        if (Glow > 0.01f) {
            draw(Graphics, GlowTexture, Glow * (GlowShade + GlowPulse * Mth.sin(Clock * 0.09f)));
        }
        float Transform = Scp427ClientState.transform(Partial);
        if (Transform > 0.0f) {
            draw(Graphics, FleshTexture, TransformShade + TransformRange * Transform + 0.12f * Mth.sin(Clock * 1.3f));
            return;
        }
        float Warning = Scp427ClientState.warning(Partial);
        if (Warning > 0.01f) {
            float Beat = 0.0f;
            if (Scp427ClientState.stage() >= 2) {
                float Phase = (Clock % Scp427Exposure.HeartbeatInterval) / Scp427Exposure.HeartbeatInterval;
                Beat = Phase < 0.12f ? 1.0f - Phase / 0.12f : Phase > 0.25f && Phase < 0.33f ? 0.6f : 0.0f;
            }
            draw(Graphics, FleshTexture, Warning * (WarnShade + WarnRange * Warning) + BeatShade * Beat * Warning);
        }
    }

    private static void draw(GuiGraphics Graphics, ResourceLocation Texture, float Alpha) {
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Graphics.setColor(1.0f, 1.0f, 1.0f, Mth.clamp(Alpha, 0.0f, 1.0f));
        Graphics.blit(Texture, 0, 0, -90, 0.0f, 0.0f, Width, Height, Width, Height);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
