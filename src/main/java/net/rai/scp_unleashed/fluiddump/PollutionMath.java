package net.rai.scp_unleashed.fluiddump;

public final class PollutionMath {
    public static final int Step = 20;
    public static final double MinRadius = 2.0;
    public static final double RadiusPerRootBucket = 2.5;
    public static final double MaxRadius = 40.0;
    public static final double MaxLoad = 400.0;
    public static final double DensityScale = 48.0;
    public static final double GrowPerTick = 0.01;
    public static final double ShrinkPerTick = 0.004;
    public static final long GraceTicks = 1200L;
    public static final double HalfLifeTicks = 6000.0;
    public static final double ForgetLoad = 0.1;
    public static final double CoreShare = 0.3;
    public static final double SlackBase = 3.0;
    public static final double SlackPerRadius = 0.5;
    public static final double DamageThreshold = 0.45;
    public static final float DamageLow = 1.0f;
    public static final float DamageHigh = 2.0f;
    public static final double CoverDensity = 0.1;
    public static final double LightDensity = 0.05;
    public static final double MurkyDensity = 0.25;
    public static final double MergeRange = 6.0;
    public static final double TintFull = 0.8;
    public static final double SiltPoint = 0.45;
    public static final int Silt = 0x6B7550;
    public static final int Murk = 0x6A4F2E;
    public static final float FogFarShare = 0.55f;

    private PollutionMath() {
    }

    public static double targetRadius(double Load) {
        if (Load <= 0.0) {
            return 0.0;
        }
        return Math.min(MaxRadius, MinRadius + RadiusPerRootBucket * Math.sqrt(Load));
    }

    public static double density(double Load) {
        return Load <= 0.0 ? 0.0 : 1.0 - Math.exp(-Load / DensityScale);
    }

    public static double feed(double Load, double Buckets) {
        return Math.min(MaxLoad, Load + Math.max(0.0, Buckets));
    }

    public static double stepRadius(double Radius, double Target, long Ticks) {
        if (Radius < Target) {
            return Math.min(Target, Radius + GrowPerTick * Ticks);
        }
        return Math.max(Target, Radius - ShrinkPerTick * Ticks);
    }

    public static double decay(double Load, long SinceFed, long Ticks) {
        long Decaying = Math.min(Ticks, SinceFed - GraceTicks);
        if (Decaying <= 0L) {
            return Load;
        }
        return Load * Math.pow(0.5, Decaying / HalfLifeTicks);
    }

    public static boolean forgotten(double Load) {
        return Load < ForgetLoad;
    }

    public static double slack(double Radius) {
        return SlackBase + SlackPerRadius * Radius;
    }

    public static double verticalReach(double Radius) {
        return slack(Radius) + Radius;
    }

    public static double falloff(double Share) {
        if (Share <= CoreShare) {
            return 1.0;
        }
        if (Share >= 1.0) {
            return 0.0;
        }
        double T = (Share - CoreShare) / (1.0 - CoreShare);
        return 1.0 - T * T * (3.0 - 2.0 * T);
    }

    public static double local(double Dx, double Dy, double Dz, double Radius, double Density) {
        if (Radius <= 0.0 || Density <= 0.0) {
            return 0.0;
        }
        double Excess = Math.max(0.0, Math.abs(Dy) - slack(Radius));
        double Squared = Dx * Dx + Dz * Dz + Excess * Excess;
        if (Squared >= Radius * Radius) {
            return 0.0;
        }
        return Density * falloff(Math.sqrt(Squared) / Radius);
    }

    public static double combine(double Current, double Extra) {
        return 1.0 - (1.0 - Current) * (1.0 - Extra);
    }

    public static float damage(double Density) {
        if (Density < DamageThreshold) {
            return 0.0f;
        }
        double T = Math.min(1.0, (Density - DamageThreshold) / (1.0 - DamageThreshold));
        return (float) (DamageLow + (DamageHigh - DamageLow) * T);
    }

    public static double tintShare(double Density) {
        return Math.max(0.0, Math.min(1.0, Density / TintFull));
    }

    public static int tint(int Water, double Density) {
        double Share = tintShare(Density);
        if (Share <= 0.0) {
            return Water;
        }
        int Colour = Share <= SiltPoint ? mix(Water, Silt, Share / SiltPoint) : mix(Silt, Murk, (Share - SiltPoint) / (1.0 - SiltPoint));
        return Water & 0xFF000000 | Colour & 0xFFFFFF;
    }

    public static int mix(int From, int To, double Share) {
        double T = Math.max(0.0, Math.min(1.0, Share));
        int Red = (int) Math.round(((From >> 16) & 255) * (1.0 - T) + ((To >> 16) & 255) * T);
        int Green = (int) Math.round(((From >> 8) & 255) * (1.0 - T) + ((To >> 8) & 255) * T);
        int Blue = (int) Math.round((From & 255) * (1.0 - T) + (To & 255) * T);
        return Red << 16 | Green << 8 | Blue;
    }

    public static String level(double Density) {
        if (Density >= DamageThreshold) {
            return "toxic";
        }
        if (Density >= MurkyDensity) {
            return "murky";
        }
        if (Density >= LightDensity) {
            return "light";
        }
        return "clear";
    }
}
