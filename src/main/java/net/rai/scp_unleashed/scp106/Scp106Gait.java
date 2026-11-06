package net.rai.scp_unleashed.scp106;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

public final class Scp106Gait {
    public static final double Stride = 1.6;
    public static final double Response = 0.4;
    public static final double StartSpeed = 0.015;
    public static final int CalmTicks = 5;
    public static final double Hearing = 24.0;
    public static final double FootSpread = 0.22;
    private double Phase;
    private double PhaseO;
    private double Speed;
    private boolean Striding;
    private int Calm;
    private float Partial;

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

    public void tick(Scp106OldMan Owner) {
        PhaseO = Phase;
        double Dx = Owner.getX() - Owner.xo;
        double Dz = Owner.getZ() - Owner.zo;
        double Moved = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Moved > 2.0) {
            Moved = Speed;
        }
        Speed += (Moved - Speed) * Response;
        boolean Free = Owner.isAlive() && Owner.action() == Scp106OldMan.ActionNone && !Owner.isInvisible();
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
            footfall(Owner, Math.floor(Phase * 2.0) % 2.0 == 0.0);
        }
        if (Phase > 64.0) {
            Phase -= 64.0;
            PhaseO -= 64.0;
        }
    }

    private void footfall(Scp106OldMan Owner, boolean Left) {
        Level World = Owner.level();
        RandomSource Random = Owner.getRandom();
        float Yaw = Owner.yBodyRot * Mth.DEG_TO_RAD;
        double Side = Left ? FootSpread : -FootSpread;
        double X = Owner.getX() + Mth.cos(Yaw) * Side;
        double Z = Owner.getZ() + Mth.sin(Yaw) * Side;
        if (Owner.onGround()) {
            World.addParticle(Scp106Registry.Puddle.get(), X, Owner.getY() + 0.02, Z, 0.0, 0.0, 0.0);
        }
        if (Owner.isSilent() || !World.hasNearbyAlivePlayer(Owner.getX(), Owner.getY(), Owner.getZ(), Hearing)) {
            return;
        }
        float Pitch = 0.9f * (0.94f + Random.nextFloat() * 0.1f);
        World.playLocalSound(X, Owner.getY(), Z, Scp106Registry.Step.get(), Owner.getSoundSource(), 0.75f, Pitch, false);
        World.playLocalSound(X, Owner.getY(), Z, Scp106Registry.StepWet.get(), Owner.getSoundSource(), 0.55f, Pitch * (0.96f + Random.nextFloat() * 0.08f), false);
    }
}
