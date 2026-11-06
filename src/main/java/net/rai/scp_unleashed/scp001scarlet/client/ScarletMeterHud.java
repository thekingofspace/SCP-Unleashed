package net.rai.scp_unleashed.scp001scarlet.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp001scarlet.ScarletClientConfig;
import net.rai.scp_unleashed.scp001scarlet.Scp001ScarletRegistry;
import net.rai.scp_unleashed.scp001scarlet.core.ScarletState;
import net.rai.scp_unleashed.scp2317.Scp2317World;

public final class ScarletMeterHud {
    public static final ResourceLocation Layer = Scp001ScarletRegistry.location("scp001_scarlet_meter");
    public static final ResourceLocation Sheet = Scp001ScarletRegistry.location("textures/gui/scp001_scarlet/meter.png");
    public static final int SheetWidth = 128;
    public static final int SheetHeight = 32;
    public static final int RowWidth = 81;
    public static final int RowHeight = 10;
    public static final int BarWidth = 70;
    public static final int BarHeight = 5;
    public static final int BarTop = 2;
    public static final int LinkWidth = 3;
    public static final int LinkHeight = 7;
    public static final int LinkTop = 1;
    public static final int CrownSize = 9;
    public static final int CrownLeft = 72;
    public static final int FullV = 5;
    public static final int BrightV = 10;
    public static final int IntactU = 72;
    public static final int SnappedU = 76;
    public static final int CrownEmptyU = 80;
    public static final int CrownFullU = 90;
    public static final int CrownGlowU = 100;
    public static final float PulseFrom = 0.7f;

    private ScarletMeterHud() {
    }

    public static float pulse(float Fill, float Time) {
        if (Fill < PulseFrom) {
            return 0.0f;
        }
        float Strength = Mth.clamp((Fill - PulseFrom) / (1.0f - PulseFrom), 0.0f, 1.0f);
        return Strength * (0.35f + 0.35f * Mth.sin(Time * 0.08f));
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.player == null || Client.options.hideGui || !ScarletClientConfig.showMeter() || !ScarletClient.meterVisible() || Client.player.isSpectator() || !Scp2317World.inside(Client.player)) {
            return;
        }
        float Partial = Delta.getGameTimeDeltaPartialTick(false);
        float Fill = Mth.clamp(ScarletClient.shown(Partial), 0.0f, 1.0f);
        int Left = Graphics.guiWidth() / 2 + 91 - RowWidth;
        int Top = Graphics.guiHeight() - Client.gui.rightHeight;
        float Glow = pulse(Fill, ScarletClient.clock() + Partial);
        RenderSystem.enableBlend();
        Graphics.blit(Sheet, Left, Top + BarTop, 0, 0, BarWidth, BarHeight, SheetWidth, SheetHeight);
        int Filled = Math.round(Fill * BarWidth);
        if (Filled > 0) {
            Graphics.blit(Sheet, Left, Top + BarTop, 0, FullV, Filled, BarHeight, SheetWidth, SheetHeight);
            if (Glow > 0.0f) {
                Graphics.setColor(1.0f, 1.0f, 1.0f, Glow);
                Graphics.blit(Sheet, Left, Top + BarTop, 0, BrightV, Filled, BarHeight, SheetWidth, SheetHeight);
                Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
        int Broken = ScarletClient.broken();
        for (int Index = 1; Index <= ScarletState.Chains; Index++) {
            int Centre = Math.round(Index / (float) ScarletState.Chains * BarWidth) - 1;
            int LinkLeft = Mth.clamp(Centre - 1, 0, BarWidth - LinkWidth);
            Graphics.blit(Sheet, Left + LinkLeft, Top + LinkTop, Index <= Broken ? SnappedU : IntactU, 0, LinkWidth, LinkHeight, SheetWidth, SheetHeight);
        }
        Graphics.blit(Sheet, Left + CrownLeft, Top, CrownEmptyU, 0, CrownSize, CrownSize, SheetWidth, SheetHeight);
        int Rise = Math.round(Fill * CrownSize);
        if (Rise > 0) {
            Graphics.blit(Sheet, Left + CrownLeft, Top + CrownSize - Rise, CrownFullU, CrownSize - Rise, CrownSize, Rise, SheetWidth, SheetHeight);
        }
        if (Glow > 0.0f) {
            Graphics.setColor(1.0f, 1.0f, 1.0f, Glow);
            Graphics.blit(Sheet, Left + CrownLeft, Top, CrownGlowU, 0, CrownSize, CrownSize, SheetWidth, SheetHeight);
            Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        }
        RenderSystem.disableBlend();
        Client.gui.rightHeight += RowHeight;
    }
}
