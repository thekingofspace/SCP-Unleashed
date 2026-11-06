package net.rai.scp_unleashed.oil;

import net.minecraft.world.level.material.MapColor;

public enum OilGrade {
    Crude("oil", 0xFF141216, 0x1C1A20, 1.6f, 1300, 6000, 30, 2, 0.0045, true, MapColor.COLOR_BLACK),
    LightOil("light_oil", 0xFFC98A2A, 0x6E4614, 3.0f, 880, 2200, 15, 1, 0.009, false, MapColor.COLOR_ORANGE),
    HeavyOil("heavy_oil", 0xFF3E2414, 0x24140C, 2.0f, 1200, 5200, 25, 2, 0.006, true, MapColor.COLOR_BROWN),
    LightFuel("light_fuel", 0xFFE8D25A, 0x8A7A2A, 4.5f, 760, 900, 8, 1, 0.012, false, MapColor.COLOR_YELLOW),
    HeavyFuel("heavy_fuel", 0xFFB0382A, 0x5A1810, 3.0f, 920, 2600, 15, 1, 0.009, false, MapColor.COLOR_RED);

    private final String Name;
    private final int Colour;
    private final int FogColour;
    private final float FogEnd;
    private final int Density;
    private final int Viscosity;
    private final int TickRate;
    private final int Spread;
    private final double Motion;
    private final boolean Sticky;
    private final MapColor Map;

    OilGrade(String Name, int Colour, int FogColour, float FogEnd, int Density, int Viscosity, int TickRate, int Spread, double Motion, boolean Sticky, MapColor Map) {
        this.Name = Name;
        this.Colour = Colour;
        this.FogColour = FogColour;
        this.FogEnd = FogEnd;
        this.Density = Density;
        this.Viscosity = Viscosity;
        this.TickRate = TickRate;
        this.Spread = Spread;
        this.Motion = Motion;
        this.Sticky = Sticky;
        this.Map = Map;
    }

    public String getName() {
        return Name;
    }

    public int getColour() {
        return Colour;
    }

    public int getFogColour() {
        return FogColour;
    }

    public float getFogEnd() {
        return FogEnd;
    }

    public int getDensity() {
        return Density;
    }

    public int getViscosity() {
        return Viscosity;
    }

    public int getTickRate() {
        return TickRate;
    }

    public int getSpread() {
        return Spread;
    }

    public double getMotion() {
        return Motion;
    }

    public boolean isSticky() {
        return Sticky;
    }

    public MapColor getMapColour() {
        return Map;
    }

    public String getBucketName() {
        return Name + "_bucket";
    }
}
