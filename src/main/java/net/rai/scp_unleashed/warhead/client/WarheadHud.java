package net.rai.scp_unleashed.warhead.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;

public final class WarheadHud {
    public static final ResourceLocation Vignette = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/warhead/alert_vignette.png");
    public static final ResourceLocation Plate = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/warhead/countdown_plate.png");
    public static final int VignetteWidth = 128;
    public static final int VignetteHeight = 72;
    public static final int PlateWidth = 120;
    public static final int PlateHeight = 24;
    public static final int PlateTextureWidth = 128;
    public static final int PlateTextureHeight = 32;
    public static final int TextLeft = 25;
    public static final int TitleColour = 0xFFE2DED2;
    public static final int TimeColour = 0xFFFF4834;
    public static final int BlinkColour = 0xFFFFECDC;

    private static int BossBottom;

    private WarheadHud() {
    }

    public static void resetBossBars() {
        BossBottom = 0;
    }

    public static void bossBar(int Top, int Increment) {
        BossBottom = Math.max(BossBottom, Top + Increment);
    }

    private static float pulse(float Time, boolean Fast) {
        float Period = Fast ? 8.0f : 20.0f;
        float Wave = Mth.sin((Time / Period) * Mth.PI);
        return Wave * Wave;
    }

    public static void renderAlert(GuiGraphics Graphics, DeltaTracker Delta) {
        float PartialTick = Delta.getGameTimeDeltaPartialTick(false);
        int Width = Graphics.guiWidth();
        int Height = Graphics.guiHeight();
        if (!WarheadEffects.enabled()) {
            return;
        }
        if (WarheadAlarm.isDanger()) {
            float Alpha = 0.22f + 0.68f * pulse(WarheadAlarm.clock() + PartialTick, WarheadAlarm.isLocked());
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            Graphics.setColor(1.0f, 1.0f, 1.0f, Alpha);
            Graphics.blit(Vignette, 0, 0, Width, Height, 0.0f, 0.0f, VignetteWidth, VignetteHeight, VignetteWidth, VignetteHeight);
            Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            RenderSystem.disableBlend();
        }
        float Glare = WarheadEffects.flash(PartialTick);
        if (Glare > 0.004f) {
            int Alpha = (int) (Math.min(0.95f, Glare) * 255.0f);
            Graphics.fill(0, 0, Width, Height, Alpha << 24 | 0xFFEBD2);
        }
    }

    public static String clockText(float TicksLeft) {
        int Seconds = Mth.ceil(TicksLeft / 20.0f);
        return String.format("%02d:%02d", Seconds / 60, Seconds % 60);
    }

    public static int[] plateArea(int GuiWidth) {
        if (!WarheadAlarm.isActive() || Minecraft.getInstance().options.hideGui) {
            return null;
        }
        int Left = (GuiWidth - PlateWidth) / 2;
        int Top = Math.max(4, BossBottom + 2);
        return new int[]{Left, Top, Left + PlateWidth, Top + PlateHeight};
    }

    public static void renderPlate(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        int[] Area = plateArea(Graphics.guiWidth());
        if (Area == null) {
            return;
        }
        int Left = Area[0];
        int Top = Area[1];
        RenderSystem.enableBlend();
        Graphics.blit(Plate, Left, Top, 0.0f, 0.0f, PlateWidth, PlateHeight, PlateTextureWidth, PlateTextureHeight);
        RenderSystem.disableBlend();
        Graphics.drawString(Client.font, Component.translatable("hud.scp_unleashed.warhead.title"), Left + TextLeft, Top + 4, TitleColour, true);
        boolean Blink = WarheadAlarm.isLocked() && (WarheadAlarm.clock() / 5L) % 2L == 0L;
        Component Time = Component.translatable("hud.scp_unleashed.warhead.tminus", clockText(WarheadAlarm.ticksLeft()));
        Graphics.drawString(Client.font, Time, Left + TextLeft, Top + 13, Blink ? BlinkColour : TimeColour, true);
    }
}
