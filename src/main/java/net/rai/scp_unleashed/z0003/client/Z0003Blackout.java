package net.rai.scp_unleashed.z0003.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.z0003.Z0003Petrify;
import net.rai.scp_unleashed.z0003.Z0003Registry;
import net.rai.scp_unleashed.z0003.Z0003Statue;

public final class Z0003Blackout {
    public static final ResourceLocation LayerId = Z0003Registry.location("z0003_blackout");
    public static final int Stages = 6;
    public static final int CrackWidth = 160;
    public static final int CrackHeight = 90;
    public static final int Black = 0xFF000000;
    public static final String HintKey = "gui.scp_unleashed.z0003.passing";
    public static final int HintGrey = 0x8C8C8C;
    public static final float HintLow = 60.0f;
    public static final float HintHigh = 130.0f;
    public static final float HintPulse = 0.05f;
    public static final float HintFadeTicks = 60.0f;
    public static final int HintBar = 80;

    private Z0003Blackout() {
    }

    public static ResourceLocation crackTexture(int Stage) {
        return Z0003Registry.location("textures/gui/z0003/cracks_" + Mth.clamp(Stage, 0, Stages - 1) + ".png");
    }

    public static float passingShare(Z0003Statue Statue, long Passes, float Now) {
        long Span = Passes - Statue.Since();
        return Span <= 0L ? 1.0f : Mth.clamp((Now - Statue.Since()) / Span, 0.0f, 1.0f);
    }

    private static void passing(GuiGraphics Graphics, Minecraft Game, Z0003Statue Statue, int Width, int Height, float Partial) {
        Long Passes = Game.player.getExistingDataOrNull(Z0003Registry.Passing.get());
        if (Passes == null || Passes <= Statue.Since()) {
            return;
        }
        float Now = Game.level.getGameTime() + Partial;
        float Share = passingShare(Statue, Passes, Now);
        float Breath = 0.5f + 0.5f * Mth.sin(Now * HintPulse);
        float Arrive = Mth.clamp((Now - Statue.Since()) / HintFadeTicks, 0.0f, 1.0f);
        int Alpha = Math.round((HintLow + (HintHigh - HintLow) * Breath) * Arrive);
        if (Alpha < 8) {
            return;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Component Line = Component.translatable(HintKey);
        int Top = Height - Height / 5;
        Graphics.drawString(Game.font, Line, (Width - Game.font.width(Line)) / 2, Top, Alpha << 24 | HintGrey, false);
        int Left = (Width - HintBar) / 2;
        int Rule = Top + Game.font.lineHeight + 3;
        Graphics.fill(Left, Rule, Left + HintBar, Rule + 1, (Alpha / 3) << 24 | HintGrey);
        Graphics.fill(Left, Rule, Left + Math.round(HintBar * Share), Rule + 1, Alpha << 24 | HintGrey);
        RenderSystem.disableBlend();
    }

    public static int stageFor(float Progress) {
        return Mth.clamp((int) (Progress * Stages), 0, Stages - 1);
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Game = Minecraft.getInstance();
        if (Game.player == null || Game.level == null) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        Z0003Statue Statue = Z0003StatueClient.local();
        if (Statue == null) {
            float Flash = Z0003StatueClient.flash(Partial);
            if (Flash > 0.0f) {
                int Alpha = Mth.clamp((int) (Flash * Flash * 255.0f), 0, 255);
                Graphics.fill(0, 0, Width, Height, Alpha << 24 | 0xFFFFFF);
            }
            return;
        }
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        Graphics.fill(0, 0, Width, Height, Black);
        if (Statue.cracking()) {
            float Progress = Statue.crackProgress(Game.level.getGameTime(), Partial, Z0003Petrify.CrackTicks);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            float Glow = 0.75f + 0.25f * Mth.sin((Game.level.getGameTime() + Partial) * 0.9f);
            Graphics.setColor(1.0f, 1.0f, 1.0f, Mth.clamp(0.35f + Progress * 0.65f, 0.0f, 1.0f) * Glow);
            Graphics.blit(crackTexture(stageFor(Progress)), 0, 0, Width, Height, 0.0f, 0.0f, CrackWidth, CrackHeight, CrackWidth, CrackHeight);
            Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            if (Progress > 0.85f) {
                int Alpha = Mth.clamp((int) ((Progress - 0.85f) / 0.15f * 200.0f), 0, 255);
                Graphics.fill(0, 0, Width, Height, Alpha << 24 | 0xFFFFFF);
            }
            RenderSystem.disableBlend();
        } else {
            passing(Graphics, Game, Statue, Width, Height, Partial);
        }
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }
}
