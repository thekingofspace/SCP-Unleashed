package net.rai.scp_unleashed.solar;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

public final class SolarSun {
    public static final float MaxTilt = 65.0f;
    public static final float ParkTilt = 0.0f;
    public static final float HorizonDegrees = 90.0f;
    public static final float RainLoss = 0.5f;
    public static final float ThunderLoss = 0.3f;

    private SolarSun() {
    }

    public static boolean hasSun(Level World) {
        return World.dimensionType().hasSkyLight() && !World.dimensionType().hasCeiling();
    }

    public static float sunDegrees(Level World, float PartialTick) {
        return Mth.wrapDegrees(World.getSunAngle(PartialTick) * Mth.RAD_TO_DEG);
    }

    public static boolean isDay(Level World) {
        return hasSun(World) && Math.abs(sunDegrees(World, 1.0f)) < HorizonDegrees;
    }

    public static float trackingTilt(Level World, float PartialTick) {
        if (!hasSun(World)) {
            return ParkTilt;
        }
        float Degrees = sunDegrees(World, PartialTick);
        return Math.abs(Degrees) < HorizonDegrees ? Mth.clamp(Degrees, -MaxTilt, MaxTilt) : ParkTilt;
    }

    public static float sunStrength(Level World) {
        if (!hasSun(World)) {
            return 0.0f;
        }
        float Height = Mth.cos(World.getSunAngle(1.0f));
        return Height <= 0.0f ? 0.0f : (float) Math.sqrt(Height);
    }

    public static float weather(Level World, BlockPos Pos) {
        if (World.getBiome(Pos).value().getPrecipitationAt(Pos) == Biome.Precipitation.NONE) {
            return 1.0f;
        }
        return Mth.clamp(1.0f - RainLoss * World.getRainLevel(1.0f) - ThunderLoss * World.getThunderLevel(1.0f), 0.0f, 1.0f);
    }
}
