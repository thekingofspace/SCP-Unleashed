package net.rai.scp_unleashed.chunkloader;

import net.minecraft.ChatFormatting;
import net.minecraft.util.StringRepresentable;

public enum ChunkLoaderStatus implements StringRepresentable {
    Off("off", ChatFormatting.GRAY, 0xFF606060, 0.55f, 0.58f, 0.6f),
    Unpowered("unpowered", ChatFormatting.RED, 0xFFB01818, 0.95f, 0.25f, 0.2f),
    Starting("starting", ChatFormatting.AQUA, 0xFF206A8A, 0.35f, 0.8f, 1.0f),
    Active("active", ChatFormatting.GREEN, 0xFF207A20, 0.3f, 1.0f, 0.55f),
    LowPower("low_power", ChatFormatting.GOLD, 0xFFA06000, 1.0f, 0.68f, 0.15f);

    private static final ChunkLoaderStatus[] Values = values();

    private final String Name;
    private final ChatFormatting Colour;
    private final int PanelColour;
    private final float Red;
    private final float Green;
    private final float Blue;

    ChunkLoaderStatus(String Name, ChatFormatting Colour, int PanelColour, float Red, float Green, float Blue) {
        this.Name = Name;
        this.Colour = Colour;
        this.PanelColour = PanelColour;
        this.Red = Red;
        this.Green = Green;
        this.Blue = Blue;
    }

    public static ChunkLoaderStatus byIndex(int Index) {
        return Index >= 0 && Index < Values.length ? Values[Index] : Off;
    }

    public static ChunkLoaderStatus byName(String Wanted) {
        for (ChunkLoaderStatus Status : Values) {
            if (Status.Name.equals(Wanted)) {
                return Status;
            }
        }
        return Off;
    }

    public boolean isHolding() {
        return this == Starting || this == Active || this == LowPower;
    }

    public ChatFormatting getColour() {
        return Colour;
    }

    public int getPanelColour() {
        return PanelColour;
    }

    public float getRed() {
        return Red;
    }

    public float getGreen() {
        return Green;
    }

    public float getBlue() {
        return Blue;
    }

    public String getTranslationKey() {
        return "gui.scp_unleashed.chunk_loader.status." + Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
