package net.rai.scp_unleashed.rifttech;

import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.extraction.ExtractionTargeting;

public final class CollectorTuning {
    public static final int BaseRange = 16;
    public static final int RangePerUpgrade = 8;
    public static final int IdleDraw = 15;
    public static final int ApocalypticDraw = 2400;
    public static final int BufferSlots = 6;
    public static final int ScanTicks = 10;
    public static final int SightTicks = 5;
    public static final int WindowTicks = 10;
    public static final float PoweredFlow = 0.6f;
    public static final float WorkingFlow = 0.9f;
    public static final float YawSpeed = 9.0f;
    public static final float PitchSpeed = 6.0f;
    public static final float LockTolerance = 3.0f;
    public static final float MinPitch = -28.0f;
    public static final float MaxPitch = 75.0f;
    public static final float RestPitch = -18.0f;
    public static final double PivotHeight = 26.0 / 16.0;
    public static final double BarrelLength = 9.5 / 16.0;
    public static final double SightClearance = 0.8;
    private static final int[] Draws = {120, 200, 320, 520, 800};

    private CollectorTuning() {
    }

    public static int rangeFor(int RangeUpgrades) {
        return BaseRange + RangePerUpgrade * Math.max(0, RangeUpgrades);
    }

    public static int maxRange() {
        return rangeFor(net.rai.scp_unleashed.upgrade.UpgradeType.Range.getLimit());
    }

    public static int drawFor(FragmentRarity Grade, boolean Apocalyptic) {
        return Apocalyptic ? ApocalypticDraw : Draws[Grade.getTier() - 1];
    }

    public static float secondsFor(FragmentRarity Grade, boolean Apocalyptic) {
        float Seconds = ExtractionTargeting.secondsFor(Grade);
        return Apocalyptic ? Seconds / ApocalypticRiftEntity.Resistance : Seconds;
    }
}
