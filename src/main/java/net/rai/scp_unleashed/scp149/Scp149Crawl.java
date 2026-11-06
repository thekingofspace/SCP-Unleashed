package net.rai.scp_unleashed.scp149;

import net.minecraft.util.Mth;

public final class Scp149Crawl {
    public static final double CycleTicks = 6.0;
    public static final double Reference = 0.0229;
    public static final double MinRate = 0.5;
    public static final double MaxRate = 3.0;
    public static final double Response = 0.35;
    public static final double StartShare = 0.3;
    public static final double StopShare = 0.15;
    public static final int StartTicks = 2;
    public static final int StopTicks = 3;
    public static final double Wrap = 64.0;
    private double Phase;
    private double PhaseO;
    private double Speed;
    private boolean Crawling;
    private int Eager;
    private int Calm;
    private float Partial;

    public boolean crawling() {
        return Crawling;
    }

    public void setPartial(float Value) {
        Partial = Mth.clamp(Value, 0.0f, 1.0f);
    }

    public double cycle() {
        double Value = PhaseO + (Phase - PhaseO) * Partial;
        return Math.min(Value - Math.floor(Value), 0.9999);
    }

    public void tick(Scp149Larva Owner) {
        PhaseO = Phase;
        double Dx = Owner.getX() - Owner.xo;
        double Dz = Owner.getZ() - Owner.zo;
        Speed += (Math.sqrt(Dx * Dx + Dz * Dz) - Speed) * Response;
        boolean Free = Owner.isAlive() && Owner.onGround() && !Owner.isInWater() && !Owner.isPassenger();
        if (!Crawling) {
            Eager = Free && Speed > Reference * StartShare ? Eager + 1 : 0;
            if (Eager >= StartTicks) {
                Crawling = true;
                Eager = 0;
                Calm = 0;
                Phase = 0.0;
                PhaseO = 0.0;
            }
            return;
        }
        Calm = !Free || Speed < Reference * StopShare ? Calm + 1 : 0;
        if (Calm >= StopTicks) {
            Crawling = false;
            Calm = 0;
            Phase = 0.0;
            PhaseO = 0.0;
            return;
        }
        double Rate = Mth.clamp(Speed / Reference, MinRate, MaxRate);
        Phase += Rate / CycleTicks;
        if (Phase >= Wrap) {
            Phase -= Wrap;
            PhaseO -= Wrap;
        }
    }
}
