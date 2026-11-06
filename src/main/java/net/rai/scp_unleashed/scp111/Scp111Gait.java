package net.rai.scp_unleashed.scp111;

import net.minecraft.util.Mth;

public final class Scp111Gait {
    public static final double Response = 0.35;
    public static final double StartShare = 0.3;
    public static final double StopShare = 0.15;
    public static final int StartTicks = 2;
    public static final int StopTicks = 5;
    public static final double Wrap = 64.0;

    public record Shape(double CycleTicks, double Reference, double MinRate, double MaxRate) {
    }

    private final Shape Form;
    private double Phase;
    private double PhaseO;
    private double Speed;
    private double Rate;
    private boolean Striding;
    private int Eager;
    private int Calm;
    private float Partial;

    public Scp111Gait(Shape Form) {
        this.Form = Form;
    }

    public boolean striding() {
        return Striding;
    }

    public double rate() {
        return Rate;
    }

    public void setPartial(float Value) {
        Partial = Mth.clamp(Value, 0.0f, 1.0f);
    }

    public double cycle() {
        double Value = PhaseO + (Phase - PhaseO) * Partial;
        return Math.min(Value - Math.floor(Value), 0.9999);
    }

    public void tick(Scp111Snail Owner) {
        PhaseO = Phase;
        double Scale = Math.max(0.2, Owner.getAgeScale());
        double Dx = Owner.getX() - Owner.xo;
        double Dz = Owner.getZ() - Owner.zo;
        Speed += (Math.sqrt(Dx * Dx + Dz * Dz) / Scale - Speed) * Response;
        boolean Free = Owner.isAlive() && !Owner.isPassenger() && !Owner.locksMovement();
        if (!Striding) {
            Eager = Free && Speed > Form.Reference() * StartShare ? Eager + 1 : 0;
            if (Eager >= StartTicks) {
                Striding = true;
                Eager = 0;
                Calm = 0;
                Phase = 0.0;
                PhaseO = 0.0;
            }
            return;
        }
        Calm = !Free || Speed < Form.Reference() * StopShare ? Calm + 1 : 0;
        if (Calm >= StopTicks) {
            Striding = false;
            Calm = 0;
            Phase = 0.0;
            PhaseO = 0.0;
            Rate = 0.0;
            return;
        }
        Rate = Mth.clamp(Speed / Form.Reference(), Form.MinRate(), Form.MaxRate());
        Phase += Rate / Form.CycleTicks();
        if (Phase >= Wrap) {
            Phase -= Wrap;
            PhaseO -= Wrap;
        }
    }
}
