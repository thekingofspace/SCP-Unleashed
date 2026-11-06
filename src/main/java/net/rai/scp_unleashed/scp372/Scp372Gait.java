package net.rai.scp_unleashed.scp372;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

public final class Scp372Gait {
    public static final double Response = 0.35;
    public static final double StartShare = 0.3;
    public static final double StopShare = 0.15;
    public static final int StartTicks = 2;
    public static final int StopTicks = 3;
    public static final double Wrap = 64.0;
    public static final double LateShare = 0.5;

    public record Profile(double CycleTicks, double Reference, double MinRate, double MaxRate, double[] Contacts) {
    }

    public record Shape(Profile Walk, Profile Skitter, double SkitterAbove, double WalkBelow, Supplier<SoundEvent> Step, float StepVolume, float Pitch, double Hearing) {
    }

    private final Shape Form;
    private double Phase;
    private double PhaseO;
    private double Speed;
    private double Rate;
    private boolean Striding;
    private boolean Skittering;
    private int Eager;
    private int Calm;
    private int Pending;
    private float Partial;

    public Scp372Gait(Shape Form) {
        this.Form = Form;
    }

    public boolean striding() {
        return Striding;
    }

    public boolean skittering() {
        return Skittering;
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
        return Skittering ? Form.Skitter() : Form.Walk();
    }

    public void tick(Scp372Insect Owner) {
        PhaseO = Phase;
        double Dx = Owner.getX() - Owner.xo;
        double Dz = Owner.getZ() - Owner.zo;
        Speed += (Math.sqrt(Dx * Dx + Dz * Dz) - Speed) * Response;
        boolean Free = Owner.isAlive() && Owner.onGround() && !Owner.isInWater() && !Owner.isPassenger() && !Owner.locksMovement();
        Profile Active = current();
        if (Pending != 0) {
            for (int Index = 0; Index < Active.Contacts().length; Index++) {
                if ((Pending & 1 << Index) != 0 && Free && Striding) {
                    footfall(Owner, Index);
                }
            }
            Pending = 0;
        }
        if (!Striding) {
            Eager = Free && Speed > Form.Walk().Reference() * StartShare ? Eager + 1 : 0;
            if (Eager >= StartTicks) {
                Striding = true;
                Skittering = Speed > Form.SkitterAbove();
                Eager = 0;
                Calm = 0;
                Phase = 0.0;
                PhaseO = 0.0;
            }
            return;
        }
        Calm = !Free || Speed < Form.Walk().Reference() * StopShare ? Calm + 1 : 0;
        if (Calm >= StopTicks) {
            Striding = false;
            Skittering = false;
            Calm = 0;
            Pending = 0;
            Phase = 0.0;
            PhaseO = 0.0;
            Rate = 0.0;
            return;
        }
        if (Skittering && Speed < Form.WalkBelow()) {
            Skittering = false;
        } else if (!Skittering && Speed > Form.SkitterAbove()) {
            Skittering = true;
        }
        Active = current();
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

    private void contact(Scp372Insect Owner, Profile Active, int Index, boolean Free) {
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
            footfall(Owner, Index);
        }
    }

    private void footfall(Scp372Insect Owner, int Index) {
        Level World = Owner.level();
        if (Owner.isSilent() || !World.hasNearbyAlivePlayer(Owner.getX(), Owner.getY(), Owner.getZ(), Form.Hearing())) {
            return;
        }
        RandomSource Random = Owner.getRandom();
        float Pitch = Form.Pitch() * (Index % 2 == 0 ? 1.0f : 0.95f) * (0.92f + Random.nextFloat() * 0.16f) * (Skittering ? 1.12f : 1.0f);
        float Volume = Form.StepVolume() * (Skittering ? 1.3f : 1.0f) * (0.85f + Random.nextFloat() * 0.15f);
        World.playLocalSound(Owner.getX(), Owner.getY(), Owner.getZ(), Form.Step().get(), Owner.getSoundSource(), Volume, Pitch, false);
    }
}
