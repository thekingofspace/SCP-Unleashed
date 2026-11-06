package net.rai.scp_unleashed.steam.integration;

import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.rai.scp_unleashed.gas.integration.GasSheet;

public final class SteamJeiLayout {
    public static final int TankWidth = GasSheet.TankWidth;
    public static final int TankHeight = GasSheet.TankHeight;
    public static final int TankY = 5;
    public static final int TextColour = 0xFF404040;

    private SteamJeiLayout() {
    }

    public static IDrawableStatic tankInset(IGuiHelper Helper) {
        return GasSheet.part(Helper, GasSheet.TankInsetU, GasSheet.PartsV, TankWidth + 2, TankHeight + 2);
    }

    public static void centred(GuiGraphics Graphics, Component Text, int Width, int Y) {
        Font Writer = Minecraft.getInstance().font;
        Graphics.drawString(Writer, Text, (Width - Writer.width(Text)) / 2, Y, TextColour, false);
    }

    public static void left(GuiGraphics Graphics, Component Text, int X, int Y) {
        Graphics.drawString(Minecraft.getInstance().font, Text, X, Y, TextColour, false);
    }
}
