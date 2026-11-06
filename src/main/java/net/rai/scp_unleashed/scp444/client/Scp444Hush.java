package net.rai.scp_unleashed.scp444.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.integration.voicechat.VoiceGate;
import net.rai.scp_unleashed.scp444.Scp444Pages;

public final class Scp444Hush {
    public static final ResourceLocation LayerId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp444_hush");
    public static final ResourceLocation IconTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/scp444/hud.png");
    public static final int IconSize = 9;
    public static final int IconTextureWidth = 16;
    public static final int IconTextureHeight = 16;
    public static final int Glyphs = 3;
    public static final int GlyphGap = 7;
    public static final int HotbarHalf = 91;
    public static final int SideGap = 6;
    public static final int RowFromBottom = 15;
    public static final int Tint = 0xD6CEE4;
    public static final float PeakAlpha = 0.42f;
    public static final float IconAlpha = 0.7f;
    public static final long Cycle = 5200L;
    public static final long FadeIn = 1500L;

    private Scp444Hush() {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (!Scp444ClientState.isSilenced() || Client.player == null || Client.options.hideGui || Client.player.isSpectator()) {
            return;
        }
        long Now = System.currentTimeMillis();
        float Arrival = Mth.clamp((Now - Scp444ClientState.since()) / (float) FadeIn, 0.0f, 1.0f);
        int X = Graphics.guiWidth() / 2 + HotbarHalf + SideGap;
        int Y = Graphics.guiHeight() - RowFromBottom;
        RenderSystem.enableBlend();
        if (VoiceGate.isLoaded()) {
            Graphics.setColor(1.0f, 1.0f, 1.0f, IconAlpha * Arrival);
            Graphics.blit(IconTexture, X, Y - 1, 0, 0, IconSize, IconSize, IconTextureWidth, IconTextureHeight);
            Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            X += IconSize + 3;
        }
        for (int Index = 0; Index < Glyphs; Index++) {
            long Offset = Index * Cycle / Glyphs;
            long Phase = Now + Offset;
            long Round = Phase / Cycle;
            float Wave = (float) Math.sin(Math.PI * ((Phase % Cycle) / (double) Cycle));
            float Alpha = PeakAlpha * Wave * Wave * Arrival;
            int Alpha8 = Mth.clamp(Math.round(Alpha * 255.0f), 0, 255);
            if (Alpha8 < 6) {
                continue;
            }
            int Glyph = Math.floorMod((int) (Round * 31 + Index * 17), Scp444Pages.MainGlyphs);
            Graphics.drawString(Client.font, Scp444Script.sequence(Scp444Pages.glyph(Glyph), Scp444Pages.MainFont), X + Index * GlyphGap, Y, Alpha8 << 24 | Tint, false);
        }
        RenderSystem.disableBlend();
    }
}
