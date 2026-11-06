package net.rai.scp_unleashed.scp682;

public enum Scp682Size {
    Full(1.0f, 0.0, 0.0, 0.0, 0.0, 40.0f, "full"),
    Reduced(0.5f, -0.5, -0.4, -0.15, -0.4, 8.0f, "reduced"),
    Shrunk(0.22f, -0.8, -0.7, -0.35, -0.8, -1.0f, "shrunk");

    private final float Scale;
    private final double HealthShift;
    private final double DamageShift;
    private final double SpeedShift;
    private final double SteadyShift;
    private final float BreakHardness;
    private final String Key;

    Scp682Size(float Scale, double HealthShift, double DamageShift, double SpeedShift, double SteadyShift, float BreakHardness, String Key) {
        this.Scale = Scale;
        this.HealthShift = HealthShift;
        this.DamageShift = DamageShift;
        this.SpeedShift = SpeedShift;
        this.SteadyShift = SteadyShift;
        this.BreakHardness = BreakHardness;
        this.Key = Key;
    }

    public float scale() {
        return Scale;
    }

    public double healthShift() {
        return HealthShift;
    }

    public double damageShift() {
        return DamageShift;
    }

    public double speedShift() {
        return SpeedShift;
    }

    public double steadyShift() {
        return SteadyShift;
    }

    public float breakHardness() {
        return BreakHardness;
    }

    public boolean breaksBlocks() {
        return BreakHardness > 0.0f;
    }

    public boolean holdsInStasis() {
        return this == Shrunk;
    }

    public String key() {
        return Key;
    }

    public Scp682Size smaller() {
        return this == Full ? Reduced : Shrunk;
    }

    public Scp682Size larger() {
        return this == Shrunk ? Reduced : Full;
    }

    public static Scp682Size byIndex(int Index) {
        Scp682Size[] All = values();
        return All[Math.max(0, Math.min(All.length - 1, Index))];
    }
}
