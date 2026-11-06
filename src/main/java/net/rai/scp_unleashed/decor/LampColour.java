package net.rai.scp_unleashed.decor;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.material.MapColor;

public enum LampColour implements StringRepresentable {
    White("white", MapColor.SNOW),
    Red("red", MapColor.COLOR_RED),
    Orange("orange", MapColor.COLOR_ORANGE),
    Yellow("yellow", MapColor.COLOR_YELLOW),
    Green("green", MapColor.COLOR_GREEN),
    Cyan("cyan", MapColor.COLOR_CYAN),
    Blue("blue", MapColor.COLOR_BLUE),
    Purple("purple", MapColor.COLOR_PURPLE);

    private final String Name;
    private final MapColor GlowColour;

    LampColour(String Name, MapColor GlowColour) {
        this.Name = Name;
        this.GlowColour = GlowColour;
    }

    public MapColor getGlowColour() {
        return GlowColour;
    }

    public String blockName() {
        return "facility_lamp_" + Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
