package net.rai.scp_unleashed.grenade;

public final class GrenadeTuning {
    public static final int StackSize = 16;
    public static final int FuseTicks = 60;
    public static final int CooldownTicks = 20;
    public static final int FullChargeTicks = 20;
    public static final float MinSpeed = 0.55F;
    public static final float MaxSpeed = 1.3F;
    public static final float ThrowSpread = 0.6F;
    public static final float DispenseSpeed = 0.9F;
    public static final float DispenseSpread = 4.0F;
    public static final double Gravity = 0.05;
    public static final double AirDrag = 0.99;
    public static final double WaterDrag = 0.8;
    public static final double WallBounce = 0.45;
    public static final double WallScrape = 0.85;
    public static final double FloorBounce = 0.35;
    public static final double BounceSkid = 0.8;
    public static final double CeilingBounce = 0.3;
    public static final double BounceFloor = 0.12;
    public static final double RollGrip = 1.35;
    public static final double MaxRollKeep = 0.92;
    public static final double SettleSpeed = 0.012;
    public static final double EntityBounce = 0.25;
    public static final int BumpCooldown = 6;
    public static final double GlassBreakSpeed = 0.5;
    public static final double ClinkSpeed = 0.12;
    public static final int ClinkGap = 3;
    public static final float SpinPerBlock = 7.0F;
    public static final float MaxSpinPerTick = 1.1F;

    public static final float FragPower = 3.0F;
    public static final double ShrapnelReach = 6.0;
    public static final float ShrapnelDamage = 6.0F;
    public static final int MaxShrapnelShatter = 12;
    public static final int DebrisDelay = 6;
    public static final int DebrisTicks = 12;
    public static final float BulletproofResistance = 1200.0F;

    public static final double FireRadius = 3.5;
    public static final float FireChanceNear = 0.9F;
    public static final float FireChanceFar = 0.35F;
    public static final int BurnTicks = 140;
    public static final float BurstIgniteSeconds = 6.0F;
    public static final float LingerIgniteSeconds = 4.0F;
    public static final int IgniteInterval = 10;
    public static final int BurnSoundInterval = 36;
    public static final float BurstDamage = 2.0F;

    public static final double FlashRadius = 12.0;
    public static final double FlashFullDistance = 2.0;
    public static final float FlashNearCurve = 0.75F;
    public static final double FacingFull = 0.6;
    public static final float AwayShare = 0.12F;
    public static final float WallRingShare = 0.5F;
    public static final float EarplugRingShare = 0.3F;
    public static final float FlashFloor = 0.02F;
    public static final int StunMinTicks = 60;
    public static final int StunMaxTicks = 120;
    public static final int StunAmplifier = 3;
    public static final int MaxSightLayers = 6;

    public static final float WhitePeakGain = 1.3F;
    public static final int WhiteHoldTicks = 50;
    public static final float WhiteHoldCurve = 1.5F;
    public static final int WhiteFadeMinTicks = 20;
    public static final int WhiteFadeMaxTicks = 120;
    public static final float WhiteFadeCurve = 1.3F;
    public static final float BlurGain = 1.3F;
    public static final int BlurRiseTicks = 4;
    public static final float BlurHandoff = 0.5F;
    public static final int BlurFadeMinTicks = 40;
    public static final int BlurFadeMaxTicks = 110;
    public static final float BlurRadius = 10.0F;
    public static final float BlurReferenceHeight = 1080.0F;
    public static final int RingMinTicks = 90;
    public static final int RingMaxTicks = 160;
    public static final float RingCurve = 1.3F;
    public static final float RingFloor = 0.01F;
    public static final int RingGrainTicks = 12;
    public static final int MuffleMinTicks = 40;
    public static final int MuffleMaxTicks = 120;
    public static final float MaxMuffle = 0.7F;

    public static final int ReleaseTicks = 180;
    public static final int PuffInterval = 10;
    public static final int GasPerPuff = 80;
    public static final int GasWanderSteps = 5;
    public static final double GasReach = 3.5;
    public static final int SpitInterval = 14;

    private GrenadeTuning() {
    }
}
