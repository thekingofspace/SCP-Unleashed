package net.rai.scp_unleashed.scp143;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public final class Scp143Breeze {
    public static final double Calm = 0.004;
    public static final double Drift = 0.003;
    public static final double GustSpeed = 0.03;
    public static final double RainWind = 1.25;
    public static final double StormWind = 1.5;
    public static final double GustShed = 1.5;
    public static final double RainShed = 1.5;
    public static final double StormShed = 2.0;

    private Scp143Breeze() {
    }

    public static double gust(long Time) {
        double Wave = StrictMath.sin(Time / 173.0) * StrictMath.sin(Time / 61.0 + 1.3);
        return Math.max(0.0, Math.min(1.0, (Wave - 0.25) * 1.6));
    }

    public static boolean wet(Level World, BlockPos Pos) {
        return World.isRaining() && World.getBiome(Pos).value().hasPrecipitation();
    }

    public static double[] wind(Level World, BlockPos Pos, long Time) {
        double Turn = Time / 24000.0 * Math.PI * 1.2 + StrictMath.sin(Time / 3100.0) * 0.8;
        double Gust = gust(Time);
        double Speed = Calm + Drift * (0.5 + 0.5 * StrictMath.sin(Time / 911.0)) + GustSpeed * Gust;
        if (wet(World, Pos)) {
            Speed *= World.isThundering() ? StormWind : RainWind;
        }
        return new double[]{StrictMath.cos(Turn) * Speed, StrictMath.sin(Turn) * Speed, Gust};
    }

    public static double shedFactor(Level World, BlockPos Pos, long Time) {
        double Factor = 1.0 + GustShed * gust(Time);
        if (wet(World, Pos)) {
            Factor *= World.isThundering() ? StormShed : RainShed;
        }
        return Factor;
    }
}
