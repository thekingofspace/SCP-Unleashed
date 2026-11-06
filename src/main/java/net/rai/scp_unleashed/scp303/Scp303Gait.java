package net.rai.scp_unleashed.scp303;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

public final class Scp303Gait {
    public static final double Response = 0.35;
    public static final double StartShare = 0.3;
    public static final double StopShare = 0.15;
    public static final int StartTicks = 2;
    public static final int StopTicks = 3;
    public static final double Wrap = 64.0;
    public static final double LateShare = 0.5;
    public static final double Leap = 1.5;

    public record Profile(double CycleTicks, double Reference, double MinRate, double MaxRate, double[] Contacts, float[] Weights) {
    }

    public record Shape(Profile Walk, Profile Run, double RunAbove, double WalkBelow, int HoldTicks, Supplier<SoundEvent> Step, Supplier<SoundEvent> Layer,
                        float StepVolume, float LayerVolume, float Pitch, double Hearing) {
    }

    private final Shape Form;
    private double Phase;
    private double PhaseO;
    private double Speed;
    private double Rate;
    private boolean Striding;
    private boolean Running;
    private int Eager;
    private int Calm;
    private int Hold;
    private int Pending;
    private float Partial;

    public Scp303Gait(Shape Form) {
        this.Form = Form;
    }

    public boolean striding() {
        return Striding;
    }

    public boolean running() {
        return Running;
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

    private Profile current() {
        return Running ? Form.Run() : Form.Walk();
    }

    public void tick(Scp303Doorman Owner) {
        PhaseO = Phase;
        double Dx = Owner.getX() - Owner.xo;
        double Dz = Owner.getZ() - Owner.zo;
        double Moved = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Moved > Leap) {
            Moved = 0.0;
            Speed = 0.0;
        }
        Speed += (Moved - Speed) * Response;
        boolean Grounded = Owner.isAlive() && Owner.onGround() && !Owner.isInWater() && !Owner.isPassenger();
        boolean Free = Grounded && !Owner.locksMovement();
        Profile Active = current();
        if (Pending != 0) {
            for (int Index = 0; Index < Active.Contacts().length; Index++) {
                if ((Pending & 1 << Index) != 0 && Free && Striding) {
                    footfall(Owner, Active, Index);
                }
            }
            Pending = 0;
        }
        if (!Striding) {
            Eager = Free && Speed > Form.Walk().Reference() * StartShare ? Eager + 1 : 0;
            if (Eager >= StartTicks) {
                Striding = true;
                Running = Speed > Form.RunAbove();
                Eager = 0;
                Calm = 0;
                Hold = Form.HoldTicks();
                Phase = 0.0;
                PhaseO = 0.0;
            }
            return;
        }
        Calm = !Free || Speed < Form.Walk().Reference() * StopShare ? Calm + 1 : 0;
        if (Calm >= StopTicks) {
            Striding = false;
            Running = false;
            Calm = 0;
            Pending = 0;
            Phase = 0.0;
            PhaseO = 0.0;
            Rate = 0.0;
            return;
        }
        if (Running && Speed < Form.WalkBelow()) {
            Running = false;
        } else if (!Running && Speed > Form.RunAbove()) {
            Running = true;
        }
        Active = current();
        if (Hold > 0) {
            Hold--;
            return;
        }
        Rate = Mth.clamp(Speed / Active.Reference(), Active.MinRate(), Active.MaxRate());
        Phase += Rate / Active.CycleTicks();
        for (int Index = 0; Index < Active.Contacts().length; Index++) {
            contact(Owner, Active, Index, Free);
        }
        if (Phase >= Wrap) {
            Phase -= Wrap;
            PhaseO -= Wrap;
        }
    }

    private void contact(Scp303Doorman Owner, Profile Active, int Index, boolean Free) {
        double Contact = Active.Contacts()[Index];
        double Before = Math.floor(PhaseO - Contact);
        double After = Math.floor(Phase - Contact);
        if (After <= Before) {
            return;
        }
        double Share = (Contact + After - PhaseO) / (Phase - PhaseO);
        if (Share >= LateShare) {
            Pending |= 1 << Index;
        } else if (Free) {
            footfall(Owner, Active, Index);
        }
    }

    private void footfall(Scp303Doorman Owner, Profile Active, int Index) {
        RandomSource Random = Owner.getRandom();
        float Weight = (float) Mth.clamp(1.15 - 0.13 * Rate, 0.7, 1.0) * Active.Weights()[Index] * (Running ? 1.15f : 1.0f);
        float Pitch = Form.Pitch() * (Index % 2 == 0 ? 1.0f : 0.96f) * (0.94f + Random.nextFloat() * 0.1f) * (Running ? 1.06f : 1.0f);
        if (!play(Owner, Form.Step(), Form.StepVolume() * Weight * (0.88f + Random.nextFloat() * 0.12f), Pitch)) {
            return;
        }
        play(Owner, Form.Layer(), Form.LayerVolume() * Weight * (0.85f + Random.nextFloat() * 0.15f), Pitch * (0.97f + Random.nextFloat() * 0.06f));
    }

    private boolean play(Scp303Doorman Owner, Supplier<SoundEvent> Sound, float Volume, float Pitch) {
        Level World = Owner.level();
        if (Owner.isSilent() || !World.hasNearbyAlivePlayer(Owner.getX(), Owner.getY(), Owner.getZ(), Form.Hearing())) {
            return false;
        }
        World.playLocalSound(Owner.getX(), Owner.getY(), Owner.getZ(), Sound.get(), Owner.getSoundSource(), Volume, Pitch, false);
        return true;
    }
}
