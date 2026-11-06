package net.rai.scp_unleashed.scp016.client;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.scp016.Scp016Affliction;

public final class Scp016Hud {
    public static final ResourceLocation LayerId = Scp016GeoModel.location("scp016_affliction");
    public static final ResourceLocation Sheet = Scp016GeoModel.location("textures/gui/scp016/affliction.png");
    public static final int Icon = 9;
    public static final int Spacing = 8;
    public static final int SheetWidth = 27;
    public static final int SheetHeight = 9;
    public static final int Socket = 0;
    public static final int Lit = 9;
    public static final int Flare = 18;
    public static final int Row = 10;
    public static final int SlowPulse = 24;
    public static final int FastPulse = 6;

    private Scp016Hud() {
    }

    public static void render(GuiGraphics Graphics, DeltaTracker Delta) {
        Minecraft Client = Minecraft.getInstance();
        LocalPlayer User = Client.player;
        int Stage = Scp016ClientHooks.stage();
        if (User == null || Stage <= 0 || Client.options.hideGui || Client.gameMode == null || !Client.gameMode.canHurtPlayer()) {
            return;
        }
        int Left = Graphics.guiWidth() / 2 - 91;
        int Top = Graphics.guiHeight() - Client.gui.leftHeight;
        float Progress = Scp016ClientHooks.progress();
        int Period = Math.max(FastPulse, Math.round(Mth.lerp(Progress, SlowPulse, FastPulse)));
        boolean Bright = Math.floorMod(User.tickCount, Period) < Period / 2;
        for (int Index = 0; Index < Scp016Affliction.Stages; Index++) {
            int X = Left + Index * Spacing;
            int U = Socket;
            if (Index < Stage - 1) {
                U = Lit;
            } else if (Index == Stage - 1) {
                U = Bright ? Flare : Lit;
            }
            Graphics.blit(Sheet, X, Top, U, 0, Icon, Icon, SheetWidth, SheetHeight);
        }
        Client.gui.leftHeight += Row;
    }
}
