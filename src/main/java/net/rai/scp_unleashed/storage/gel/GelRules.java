package net.rai.scp_unleashed.storage.gel;

public final class GelRules {
    public static final int MaxGel = 1200;
    public static final int SealLevel = 480;
    public static final int FormRate = 4;
    public static final int DecayRate = 1;
    public static final int HoldTicks = 600;
    public static final int PowerDraw = 8;
    public static final int PowerGrace = 10;
    public static final int TankCapacity = 2000;
    public static final int UpkeepWater = 5;
    public static final int UpkeepEvery = 20;
    public static final int FormWaterPerUnit = 10;
    public static final int SyncStep = 60;

    private GelRules() {
    }

    public static int percent(int Gel) {
        return Math.round(Gel * 100.0f / MaxGel);
    }
}
