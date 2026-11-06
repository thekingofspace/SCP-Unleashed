package net.rai.scp_unleashed.reactor.core;

public final class ReactorTuning {
    public static final int MaxHeight = 8;
    public static final int MaxTowers = 64;
    public static final int MaxSegments = 384;
    public static final float Ambient = 20.0f;
    public static final float Boil = 100.0f;
    public static final float HotLevel = 900.0f;
    public static final float CriticalLevel = 1200.0f;
    public static final float ScaleTop = 1500.0f;
    public static final float SegmentCapacity = 50.0f;
    public static final float PassiveLoss = 0.003f;
    public static final float CoolantRate = 0.04f;
    public static final float CoolantCap = 32.0f;
    public static final float ChannelCap = 32.0f;
    public static final float ChannelYield = 1.0f;
    public static final float ChannelStart = 200.0f;
    public static final float ChannelPeak = 1000.0f;
    public static final float Conduction = 0.02f;
    public static final float GasPerHeat = 0.005f;
    public static final float WaterPerSteam = 1.0f;
    public static final int WaterPerSegment = 4000;
    public static final int SteamPerSegment = 1000;
    public static final int GasPerSegment = 250;
    public static final float RodSpeed = 1.0f;
    public static final float ScramSpeed = 8.0f;
    public static final float DamageBase = 1.0f;
    public static final float DamageSlope = 3.0f;
    public static final float DamageSpan = 300.0f;
    public static final float ResidualDamage = 0.2f;
    public static final float RepairRate = 0.25f;
    public static final float BreachLevel = 25.0f;
    public static final float FullIntegrity = 100.0f;
    public static final float HeatShock = 300.0f;
    public static final float FuelHeatScale = 40.0f;
    public static final int FuelBurnScale = 4;
    public static final float ReactingFloor = 0.01f;
    public static final float BoostPerSegment = 0.25f;
    public static final float MaxBoost = 1.0f;
    public static final float TicksPerSecond = 20.0f;

    private ReactorTuning() {
    }

    public static float channelEfficiency(float Temperature) {
        float Fraction = (Temperature - ChannelStart) / (ChannelPeak - ChannelStart);
        return Math.max(0.0f, Math.min(1.0f, Fraction));
    }

    public static float damagePerTick(float Temperature) {
        if (Temperature < CriticalLevel) {
            return 0.0f;
        }
        return (DamageBase + DamageSlope * (Temperature - CriticalLevel) / DamageSpan) / TicksPerSecond;
    }

    public static float boosterStrength(int Height) {
        return Math.min(MaxBoost, BoostPerSegment * Height);
    }

    public static float heatFraction(float Temperature) {
        return Math.max(0.0f, Math.min(1.0f, (Temperature - Ambient) / (ScaleTop - Ambient)));
    }
}
