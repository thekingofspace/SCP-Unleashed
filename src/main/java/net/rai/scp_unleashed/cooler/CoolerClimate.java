package net.rai.scp_unleashed.cooler;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public enum CoolerClimate {
    Cold("cold"),
    Mild("mild"),
    Hot("hot");

    public static final float ColdBelow = 0.35f;
    public static final float HotFrom = 0.95f;

    private final String Key;

    CoolerClimate(String Key) {
        this.Key = Key;
    }

    public String key() {
        return Key;
    }

    public static CoolerClimate at(Level World, BlockPos Pos) {
        if (World.dimensionType().ultraWarm()) {
            return Hot;
        }
        Biome Here = World.getBiome(Pos).value();
        if (Here.coldEnoughToSnow(Pos)) {
            return Cold;
        }
        float Temperature = Here.getBaseTemperature();
        if (Temperature < ColdBelow) {
            return Cold;
        }
        return Temperature >= HotFrom ? Hot : Mild;
    }
}
