package net.rai.scp_unleashed.oil.integration;

import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public final class OilJeiSheet {
    public static final ResourceLocation Sheet = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/oil/jei.png");
    public static final int SheetSize = 256;
    public static final int Width = 132;
    public static final int Height = 62;
    public static final int DistillingV = 0;
    public static final int ProcessingV = 64;
    public static final int RefiningV = 128;
    public static final int FuelV = 192;
    public static final int TankY = 5;
    public static final int TankWidth = 16;
    public static final int TankHeight = 40;
    public static final int HalfHeight = 18;
    public static final int LowerY = 27;
    public static final int ArrowY = 20;
    public static final int InfoY = 51;
    public static final int TextColour = 0xFF404040;
    public static final int DistilInputX = 6;
    public static final int DistilArrowX = 26;
    public static final int ColumnX = 40;
    public static final int ColumnY = 4;
    public static final int ColumnWidth = 22;
    public static final int ColumnHeight = 42;
    public static final int DistilOutArrowX = 66;
    public static final int DistilTopArrowY = 9;
    public static final int DistilBottomArrowY = 31;
    public static final int DistilOutputX = 80;
    public static final int ProcessInputX = 26;
    public static final int ProcessArrowX = 50;
    public static final int ProcessPictureX = 62;
    public static final int ProcessPictureY = 12;
    public static final int ProcessOutputX = 90;
    public static final int RefineInputX = 12;
    public static final int RefineArrowX = 34;
    public static final int RefineSlotX = 50;
    public static final int RefineSlotY = 16;
    public static final int RefinePlusX = 72;
    public static final int RefineWaterX = 84;
    public static final int FuelInputX = 8;
    public static final int FuelArrowX = 28;
    public static final int FuelPictureX = 27;
    public static final int FuelPictureY = 26;
    public static final int FuelGasX = 108;
    public static final int FuelTextX = 48;

    private OilJeiSheet() {
    }

    public static IDrawableStatic part(IGuiHelper Helper, int V) {
        return Helper.drawableBuilder(Sheet, 0, V, Width, Height).setTextureSize(SheetSize, SheetSize).build();
    }

    public static void centred(GuiGraphics Graphics, Component Text, int Y) {
        Font Writer = Minecraft.getInstance().font;
        Graphics.drawString(Writer, Text, (Width - Writer.width(Text)) / 2, Y, TextColour, false);
    }
}
