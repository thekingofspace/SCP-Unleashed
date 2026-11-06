package net.rai.scp_unleashed.scp049;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

public final class Scp049Gait {
    public static final double Response = 0.35;
    public static final double StartShare = 0.3;
    public static final double StopShare = 0.15;
    public static final int StartTicks = 2;
    public static final int StopTicks = 3;
    public static final double HearingRange = 16.0;
    public static final double Wrap = 64.0;
    public static final double LateShare = 0.5;

    public record Profile(String Clip, double CycleTicks, double Reference, double MinRate, double MaxRate, int HoldTicks, double[] Contacts, float[] Weights,
                          Supplier<SoundEvent> Step, Supplier<SoundEvent> Layer, float StepVolume, float LayerVolume, float Pitch) {
    }

    private final Profile Shape;
    private double Phase;
    private double PhaseO;
    private double Speed;
    private double Rate;
    private boolean Striding;
    private int Eager;
    private int Calm;
    private int Hold;
    private int Pending;
    private float Partial;
    private int LastStep;

    public Scp049Gait(Profile Shape) {
        this.Shape = Shape;
    }

    public Profile shape() {
        return Shape;
    }

    public boolean striding() {
        return Striding;
    }

    public double rate() {
        return Rate;
    }

    public double phase() {
        return Phase - Math.floor(Phase);
    }

    public int ticksSinceStep(int Now) {
        return Now - LastStep;
    }

    public double ticksToStep() {
        if (!Striding || Rate <= 1.0e-4) {
            return Double.MAX_VALUE;
        }
        double Current = phase();
        double Nearest = 1.0;
        for (double Contact : Shape.Contacts()) {
            double Ahead = Contact - Current;
            Ahead -= Math.floor(Ahead);
            Nearest = Math.min(Nearest, Ahead);
        }
        return Nearest * Shape.CycleTicks() / Rate;
    }

    public void setPartial(float Value) {
        Partial = Mth.clamp(Value, 0.0f, 1.0f);
    }

    public double cycle() {
        double Value = PhaseO + (Phase - PhaseO) * Partial;
        return Math.min(Value - Math.floor(Value), 0.9999);
    }

    public void reset() {
        Phase = 0.0;
        PhaseO = 0.0;
        Speed = 0.0;
        Rate = 0.0;
        Striding = false;
        Eager = 0;
        Calm = 0;
        Hold = 0;
        Pending = 0;
    }

    public void tick(Scp049Doctor Owner) {
        PhaseO = Phase;
        double Dx = Owner.getX() - Owner.xo;
        double Dz = Owner.getZ() - Owner.zo;
        Speed += (Math.sqrt(Dx * Dx + Dz * Dz) - Speed) * Response;
        boolean Grounded = Owner.isAlive() && Owner.onGround() && !Owner.isInWater() && !Owner.isPassenger();
        boolean Free = Grounded && !Owner.locksMovement();
        if (Pending != 0) {
            for (int Index = 0; Index < Shape.Contacts().length; Index++) {
                if ((Pending & 1 << Index) != 0 && Free && Striding) {
                    footfall(Owner, Index);
                }
            }
            Pending = 0;
        }
        if (!Striding) {
            Eager = Free && Speed > Shape.Reference() * StartShare ? Eager + 1 : 0;
            if (Eager >= StartTicks) {
                Striding = true;
                Eager = 0;
                Calm = 0;
                Hold = Shape.HoldTicks();
                Phase = 0.0;
                PhaseO = 0.0;
            }
            return;
        }
        Calm = !Free || Speed < Shape.Reference() * StopShare ? Calm + 1 : 0;
        if (Calm >= StopTicks) {
            Striding = false;
            Calm = 0;
            Pending = 0;
            Phase = 0.0;
            PhaseO = 0.0;
            Rate = 0.0;
            return;
        }
        if (Hold > 0) {
            Hold--;
            return;
        }
        Rate = Mth.clamp(Speed / Shape.Reference(), Shape.MinRate(), Shape.MaxRate());
        Phase += Rate / Shape.CycleTicks();
        for (int Index = 0; Index < Shape.Contacts().length; Index++) {
            contact(Owner, Index, Free);
        }
        if (Phase >= Wrap) {
            Phase -= Wrap;
            PhaseO -= Wrap;
        }
    }

    private void contact(Scp049Doctor Owner, int Index, boolean Free) {
        double Contact = Shape.Contacts()[Index];
        double Before = Math.floor(PhaseO - Contact);
        double After = Math.floor(Phase - Contact);
        if (After <= Before) {
            return;
        }
        double Share = (Contact + After - PhaseO) / (Phase - PhaseO);
        if (Share >= LateShare) {
            Pending |= 1 << Index;
        } else if (Free) {
            footfall(Owner, Index);
        }
    }

    private void footfall(Scp049Doctor Owner, int Index) {
        RandomSource Random = Owner.getRandom();
        LastStep = Owner.tickCount;
        float Weight = (float) Mth.clamp(1.15 - 0.13 * Rate, 0.7, 1.0) * Shape.Weights()[Index];
        float Pitch = Shape.Pitch() * (Index % 2 == 0 ? 1.0f : 0.96f) * (0.94f + Random.nextFloat() * 0.1f);
        if (!play(Owner, Shape.Step(), Shape.StepVolume() * Weight * (0.88f + Random.nextFloat() * 0.12f), Pitch)) {
            return;
        }
        play(Owner, Shape.Layer(), Shape.LayerVolume() * Weight * (0.85f + Random.nextFloat() * 0.15f), Pitch * (0.97f + Random.nextFloat() * 0.06f));
    }

    private static boolean play(Scp049Doctor Owner, Supplier<SoundEvent> Sound, float Volume, float Pitch) {
        Level World = Owner.level();
        if (Owner.isSilent() || !World.hasNearbyAlivePlayer(Owner.getX(), Owner.getY(), Owner.getZ(), HearingRange)) {
            return false;
        }
        World.playLocalSound(Owner.getX(), Owner.getY(), Owner.getZ(), Sound.get(), Owner.getSoundSource(), Volume, Pitch, false);
        return true;
    }
}
