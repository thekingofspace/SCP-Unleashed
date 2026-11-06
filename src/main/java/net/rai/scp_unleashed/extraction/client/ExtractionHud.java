package net.rai.scp_unleashed.extraction.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;

import java.util.Locale;

public final class ExtractionHud {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/extraction/extraction_hud.png");
    public static final int TextureWidth = 128;
    public static final int TextureHeight = 32;
    public static final int FrameWidth = 85;
    public static final int FrameHeight = 9;
    public static final int Segments = 16;
    public static final int SegmentWidth = 4;
    public static final int SegmentHeight = 5;
    public static final int SegmentStride = 5;
    public static final int SegmentLeft = 3;
    public static final int SegmentTop = 2;
    public static final int FullU = 0;
    public static final int FullV = 10;
    public static final int EmptyU = 5;
    public static final int EmptyV = 10;
    public static final int GlyphU = 10;
    public static final int GlyphV = 10;
    public static final int GlyphSize = 7;
    public static final int BarOffset = 15;
    public static final int TextOffset = 27;
    public static final int LabelGap = 6;
    private static final int LostColour = 0xD8604E;
    private static final int StatusColour = 0xE6EAEE;

    private ExtractionHud() {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.options.hideGui || Game.level == null || Game.player == null) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        ExtractionTracker.HudView View = ExtractionTracker.hudView(Partial);
        if (View == null || View.Alpha() < 0.03F) {
            return;
        }
        float Alpha = Mth.clamp(View.Alpha(), 0.0F, 1.0F);
        FragmentRarity Grade = View.Grade();
        int Colour = View.Colour();
        float Red = (Colour >> 16 & 255) / 255.0F;
        float Green = (Colour >> 8 & 255) / 255.0F;
        float Blue = (Colour & 255) / 255.0F;
        boolean Active = View.State() == ExtractionTracker.HudState.Active;
        boolean Complete = View.State() == ExtractionTracker.HudState.Complete;
        boolean Lost = View.State() == ExtractionTracker.HudState.Lost;
        float Progress = Mth.clamp(View.Progress(), 0.0F, 1.0F);
        RandomSource Noise = RandomSource.create(Game.level.getGameTime() * 341873128712L + Grade.getTier() * 132897987541L);
        float Strain = Active ? Mth.clamp((Progress - 0.8F) / 0.2F, 0.0F, 1.0F) : 0.0F;
        int Amplitude = Strain > 0.66F ? 2 : Strain > 0.0F ? 1 : 0;
        int ShakeX = Amplitude == 0 ? 0 : Noise.nextInt(Amplitude * 2 + 1) - Amplitude;
        int ShakeY = Amplitude == 0 ? 0 : Noise.nextInt(3) - 1;
        int Left = Graphics.guiWidth() / 2 - FrameWidth / 2 + ShakeX;
        int Top = Graphics.guiHeight() / 2 + BarOffset + ShakeY;
        float Glitch = Active ? 0.06F + Progress * (0.12F + 0.05F * Grade.getTier()) : 0.0F;
        float Flash = Complete ? flash(View.Timer(), Partial) : 0.0F;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (Strain > 0.0F) {
            float Split = 0.3F * Strain * Alpha;
            Graphics.setColor(1.0F, 0.25F, 0.25F, Split);
            Graphics.blit(Texture, Left - 1, Top, 0, 0, FrameWidth, FrameHeight, TextureWidth, TextureHeight);
            Graphics.setColor(0.25F, 1.0F, 1.0F, Split);
            Graphics.blit(Texture, Left + 1, Top, 0, 0, FrameWidth, FrameHeight, TextureWidth, TextureHeight);
        }
        Graphics.setColor(lift(Red, Flash), lift(Green, Flash), lift(Blue, Flash), Alpha);
        Graphics.blit(Texture, Left, Top, 0, 0, FrameWidth, FrameHeight, TextureWidth, TextureHeight);
        float Filled = Progress * Segments;
        for (int Index = 0; Index < Segments; Index++) {
            int X = Left + SegmentLeft + Index * SegmentStride;
            int Y = Top + SegmentTop;
            if (Active && Noise.nextFloat() < Glitch * 0.3F) {
                Y += Noise.nextBoolean() ? 1 : -1;
            }
            float Amount = Mth.clamp(Filled - Index, 0.0F, 1.0F);
            Graphics.setColor(Red * 0.5F, Green * 0.5F, Blue * 0.5F, Alpha * 0.8F);
            Graphics.blit(Texture, X, Y, EmptyU, EmptyV, SegmentWidth, SegmentHeight, TextureWidth, TextureHeight);
            if (Amount <= 0.0F) {
                continue;
            }
            int Width = Amount >= 1.0F ? SegmentWidth : Math.max(1, Math.round(Amount * SegmentWidth));
            float Boost = Amount < 1.0F ? 0.55F + 0.45F * Noise.nextFloat() : Active && Noise.nextFloat() < Glitch * 0.25F ? 1.35F : 1.0F;
            Graphics.setColor(Math.min(1.0F, lift(Red, Flash) * Boost), Math.min(1.0F, lift(Green, Flash) * Boost), Math.min(1.0F, lift(Blue, Flash) * Boost), Alpha);
            Graphics.blit(Texture, X, Y, FullU, FullV, Width, SegmentHeight, TextureWidth, TextureHeight);
        }
        Graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        if (Active && Noise.nextFloat() < Glitch) {
            int Tears = 1 + Noise.nextInt(2);
            for (int Tear = 0; Tear < Tears; Tear++) {
                int Width = 3 + Noise.nextInt(10);
                int X = Left + 2 + Noise.nextInt(FrameWidth - Width - 3);
                int Y = Top + 1 + Noise.nextInt(FrameHeight - 2);
                Graphics.fill(X, Y, X + Width, Y + 1, argb(Alpha * 0.75F, lift(Red, 0.55F), lift(Green, 0.55F), lift(Blue, 0.55F)));
            }
        }
        int TextTop = Graphics.guiHeight() / 2 + TextOffset + ShakeY;
        int TextAlpha = Math.max(4, Math.round(Alpha * 255.0F));
        if (Active) {
            int GlyphShift = Noise.nextFloat() < Glitch * 0.5F ? (Noise.nextBoolean() ? 1 : -1) : 0;
            String Name = Component.translatable("hud.scp_unleashed.extraction.rarity." + View.Label()).getString().toUpperCase(Locale.ROOT);
            String Percent = Math.round(Progress * 100.0F) + "%";
            int PercentWidth = Game.font.width(Percent);
            int RowWidth = Math.max(FrameWidth, GlyphSize + 3 + Game.font.width(Name) + LabelGap + PercentWidth);
            int RowLeft = Left - (RowWidth - FrameWidth) / 2;
            Graphics.setColor(Red, Green, Blue, Alpha);
            Graphics.blit(Texture, RowLeft + GlyphShift, TextTop, GlyphU, GlyphV, GlyphSize, GlyphSize, TextureWidth, TextureHeight);
            Graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            Graphics.drawString(Game.font, Name, RowLeft + GlyphSize + 3, TextTop, TextAlpha << 24 | Colour & 0xFFFFFF, true);
            Graphics.drawString(Game.font, Percent, RowLeft + RowWidth - PercentWidth, TextTop, TextAlpha << 24 | StatusColour, true);
        } else {
            String Status = Component.translatable(Complete ? "hud.scp_unleashed.extraction.complete" : "hud.scp_unleashed.extraction.lost").getString();
            float Glow = 0.35F + 0.65F * Flash;
            int Tint = Lost ? LostColour : argb(1.0F, lift(Red, Glow), lift(Green, Glow), lift(Blue, Glow)) & 0xFFFFFF;
            int Start = Graphics.guiWidth() / 2 - (Game.font.width(Status) + GlyphSize + 3) / 2;
            Graphics.setColor((Tint >> 16 & 255) / 255.0F, (Tint >> 8 & 255) / 255.0F, (Tint & 255) / 255.0F, Alpha);
            Graphics.blit(Texture, Start, TextTop, GlyphU, GlyphV, GlyphSize, GlyphSize, TextureWidth, TextureHeight);
            Graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            Graphics.drawString(Game.font, Status, Start + GlyphSize + 3, TextTop, TextAlpha << 24 | Tint, true);
        }
        RenderSystem.disableBlend();
    }

    private static float flash(int Timer, float Partial) {
        float Elapsed = ExtractionTracker.CompleteTicks - Timer + Partial;
        if (Elapsed >= 10.0F) {
            return 0.0F;
        }
        return (0.5F + 0.5F * Mth.cos(Elapsed * Mth.PI / 2.5F)) * (1.0F - Elapsed / 10.0F);
    }

    private static float lift(float Channel, float Amount) {
        return Channel + (1.0F - Channel) * Mth.clamp(Amount, 0.0F, 1.0F);
    }

    private static int argb(float Alpha, float Red, float Green, float Blue) {
        return Math.round(Mth.clamp(Alpha, 0.0F, 1.0F) * 255.0F) << 24 | Math.round(Red * 255.0F) << 16 | Math.round(Green * 255.0F) << 8 | Math.round(Blue * 255.0F);
    }
}
