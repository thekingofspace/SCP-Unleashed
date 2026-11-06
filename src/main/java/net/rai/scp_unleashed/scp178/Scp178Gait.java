package net.rai.scp_unleashed.scp178;

import net.minecraft.util.Mth;

public final class Scp178Gait {
    public static final double Stride = 2.4;
    public static final double Response = 0.4;
    public static final double StartSpeed = 0.02;
    public static final int CalmTicks = 4;
    private double Phase;
    private double PhaseO;
    private double Speed;
    private boolean Striding;
    private int Calm;
    private float Partial;
    private boolean Stepped;
    private boolean Placed;
    private double LastX;
    private double LastZ;

    public boolean striding() {
        return Striding;
    }

    public void setPartial(float Value) {
        Partial = Mth.clamp(Value, 0.0f, 1.0f);
    }

    public double cycle() {
        double Value = PhaseO + (Phase - PhaseO) * Partial;
        return Math.min(Value - Math.floor(Value), 0.9999);
    }

    public boolean takeStep() {
        boolean Was = Stepped;
        Stepped = false;
        return Was;
    }

    public void tick(Scp178Watcher Owner) {
        PhaseO = Phase;
        if (!Placed) {
            LastX = Owner.getX();
            LastZ = Owner.getZ();
            Placed = true;
        }
        double Dx = Owner.getX() - LastX;
        double Dz = Owner.getZ() - LastZ;
        LastX = Owner.getX();
        LastZ = Owner.getZ();
        double Moved = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Moved > 2.0) {
            Moved = Speed;
        }
        Speed += (Moved - Speed) * Response;
        boolean Free = Owner.isAlive() && Owner.action() == Scp178Watcher.ActionNone && !Owner.isLeaving();
        if (Free && Speed > StartSpeed) {
            if (!Striding) {
                Phase = 0.0;
                PhaseO = 0.0;
            }
            Striding = true;
            Calm = 0;
        } else if (++Calm > CalmTicks) {
            Striding = false;
        }
        if (!Striding) {
            return;
        }
        double Before = Phase;
        Phase += Moved / Stride;
        if (Math.floor(Before * 2.0) != Math.floor(Phase * 2.0)) {
            Stepped = true;
        }
        if (Phase > 64.0) {
            Phase -= 64.0;
            PhaseO -= 64.0;
        }
    }
}
