package net.rai.scp_unleashed.scp1730;

public final class Scp1730Aura {
    public static final double Range = 24.0;
    public static final double Falloff = 1.5;
    public static final double AnchoredShare = 0.12;
    public static final double CastBoost = 1.35;

    private Scp1730Aura() {
    }

    public static double base(double Distance) {
        if (Distance >= Range) {
            return 0.0;
        }
        return Math.pow(1.0 - Math.max(0.0, Distance) / Range, Falloff);
    }

    public static double strength(double Distance, boolean Anchored, boolean Casting) {
        double Value = base(Distance) * (Anchored ? AnchoredShare : 1.0) * (Casting && !Anchored ? CastBoost : 1.0);
        return Math.max(0.0, Math.min(1.0, Value));
    }

    public static double comfort(double Strength, double Scale) {
        return Strength * Math.max(0.0, Math.min(1.0, Scale));
    }
}
