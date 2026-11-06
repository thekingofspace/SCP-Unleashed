package net.rai.scp_unleashed.gas.integration;

import mezz.jei.api.gui.drawable.IDrawableStatic;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;

public final class GasSheet {
    public static final ResourceLocation Sheet = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/gas/jei.png");
    public static final int SheetSize = 256;
    public static final int Width = 128;
    public static final int Height = 60;
    public static final int VaporizingV = 0;
    public static final int SourceV = 64;
    public static final int InputX = 5;
    public static final int OutputX = 107;
    public static final int TankY = 5;
    public static final int TankWidth = 16;
    public static final int TankHeight = 40;
    public static final int PictureX = 36;
    public static final int PictureY = 6;
    public static final int PictureWidth = 56;
    public static final int PictureHeight = 36;
    public static final int InArrowX = 24;
    public static final int OutArrowX = 94;
    public static final int FlowArrowY = 20;
    public static final int InfoY = 50;
    public static final int PartsV = 128;
    public static final int TankInsetU = 0;
    public static final int SlotU = 24;
    public static final int SlotSize = 18;
    public static final int SlotY = 16;
    public static final int TextColour = 0xFF404040;

    private GasSheet() {
    }

    public static IDrawableStatic part(IGuiHelper Helper, int U, int V, int PartWidth, int PartHeight) {
        return Helper.drawableBuilder(Sheet, U, V, PartWidth, PartHeight).setTextureSize(SheetSize, SheetSize).build();
    }
}
