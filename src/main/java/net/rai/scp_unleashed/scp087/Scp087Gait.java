package net.rai.scp_unleashed.scp087;

import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

public final class Scp087Gait {
    public static final double Stride = 2.6;
    public static final double Response = 0.4;
    public static final double StartSpeed = 0.025;
    public static final double RunAbove = 0.2;
    public static final double WalkBelow = 0.15;
    public static final int CalmTicks = 4;
    public static final double Hearing = 24.0;
    private double Phase;
    private double PhaseO;
    private double Speed;
    private boolean Striding;
    private boolean Running;
    private int Calm;
    private float Partial;

    public boolean striding() {
        return Striding;
    }

    public boolean running() {
        return Running;
    }

    public void setPartial(float Value) {
        Partial = Mth.clamp(Value, 0.0f, 1.0f);
    }

    public double cycle() {
        double Value = PhaseO + (Phase - PhaseO) * Partial;
        return Math.min(Value - Math.floor(Value), 0.9999);
    }

    public void tick(Scp087Hunter Owner) {
        PhaseO = Phase;
        double Dx = Owner.getX() - Owner.xo;
        double Dz = Owner.getZ() - Owner.zo;
        double Moved = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Moved > 2.0) {
            Moved = Speed;
        }
        Speed += (Moved - Speed) * Response;
        boolean Free = Owner.isAlive() && Owner.onGround() && Owner.action() == Scp087Hunter.ActionNone;
        if (Free && Speed > StartSpeed) {
            if (!Striding) {
                Phase = 0.0;
                PhaseO = 0.0;
            }
            Striding = true;
            Calm = 0;
        } else if (++Calm > CalmTicks) {
            Striding = false;
            Running = false;
        }
        if (!Striding) {
            return;
        }
        if (Running && Speed < WalkBelow) {
            Running = false;
        } else if (!Running && Speed > RunAbove) {
            Running = true;
        }
        double Before = Phase;
        Phase += Moved / Stride;
        if (Math.floor(Before * 2.0) != Math.floor(Phase * 2.0)) {
            footfall(Owner);
        }
        if (Phase > 64.0) {
            Phase -= 64.0;
            PhaseO -= 64.0;
        }
    }

    private void footfall(Scp087Hunter Owner) {
        Level World = Owner.level();
        if (Owner.isSilent() || !World.hasNearbyAlivePlayer(Owner.getX(), Owner.getY(), Owner.getZ(), Hearing)) {
            return;
        }
        float Pitch = (Running ? 1.05f : 0.92f) * (0.94f + Owner.getRandom().nextFloat() * 0.1f);
        World.playLocalSound(Owner.getX(), Owner.getY(), Owner.getZ(), Scp087Registry.HunterStep.get(), Owner.getSoundSource(), Running ? 0.9f : 0.7f, Pitch, false);
    }
}
