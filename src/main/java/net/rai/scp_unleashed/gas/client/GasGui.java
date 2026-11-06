package net.rai.scp_unleashed.gas.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gas.Gas;

public final class GasGui {
    public static final ResourceLocation FillTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/gas/gas_fill.png");
    public static final ResourceLocation IconTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/gas/gas_icon.png");
    public static final int Tile = 16;
    public static final int IconSheetWidth = 32;
    public static final int IconSize = 16;

    private GasGui() {
    }

    private static void tint(GuiGraphics Graphics, int Colour, float Alpha) {
        Graphics.setColor((Colour >> 16 & 255) / 255.0f, (Colour >> 8 & 255) / 255.0f, (Colour & 255) / 255.0f, Alpha);
    }

    public static void drawGas(GuiGraphics Graphics, Gas Kind, int X, int Y, int Width, int Height) {
        if (Width <= 0 || Height <= 0) {
            return;
        }
        RenderSystem.enableBlend();
        tint(Graphics, Kind.getColour(), 1.0f);
        int Bottom = Y + Height;
        for (int Drawn = 0; Drawn < Height; Drawn += Tile) {
            int Rows = Math.min(Tile, Height - Drawn);
            int Top = Bottom - Drawn - Rows;
            for (int Across = 0; Across < Width; Across += Tile) {
                int Columns = Math.min(Tile, Width - Across);
                Graphics.blit(FillTexture, X + Across, Top, 0, Tile - Rows, Columns, Rows, Tile, Tile);
            }
        }
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    public static void drawIcon(GuiGraphics Graphics, Gas Kind, int X, int Y) {
        RenderSystem.enableBlend();
        tint(Graphics, Kind.getColour(), 1.0f);
        Graphics.blit(IconTexture, X, Y, 0, 0, IconSize, IconSize, IconSheetWidth, IconSize);
        Graphics.setColor(1.0f, 1.0f, 1.0f, 1.0f);
        Graphics.blit(IconTexture, X, Y, IconSize, 0, IconSize, IconSize, IconSheetWidth, IconSize);
        RenderSystem.disableBlend();
    }
}
