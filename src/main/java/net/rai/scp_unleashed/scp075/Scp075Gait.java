package net.rai.scp_unleashed.scp075;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

public final class Scp075Gait {
    public static final double Response = 0.35;
    public static final double StartShare = 0.3;
    public static final double StopShare = 0.15;
    public static final int StartTicks = 2;
    public static final int StopTicks = 4;
    public static final double Wrap = 64.0;
    public static final double LateShare = 0.5;

    public record Profile(double CycleTicks, double Reference, double MinRate, double MaxRate, double[] Contacts, float[] Weights) {
    }

    public record Shape(Profile Crawl, int HoldTicks, Supplier<SoundEvent> Step, Supplier<SoundEvent> Layer, float StepVolume, float LayerVolume, float Pitch, double Hearing) {
    }

    private final Shape Form;
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

    public Scp075Gait(Shape Form) {
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

    public void tick(Scp075Snail Owner) {
        PhaseO = Phase;
        double Scale = Math.max(0.2, Owner.getAgeScale());
        double Dx = Owner.getX() - Owner.xo;
        double Dz = Owner.getZ() - Owner.zo;
        Speed += (Math.sqrt(Dx * Dx + Dz * Dz) / Scale - Speed) * Response;
        boolean Free = Owner.isAlive() && Owner.onGround() && !Owner.isInWater() && !Owner.isPassenger() && !Owner.locksMovement();
        Profile Active = Form.Crawl();
        if (Pending != 0) {
            for (int Index = 0; Index < Active.Contacts().length; Index++) {
                if ((Pending & 1 << Index) != 0 && Free && Striding) {
                    footfall(Owner, Active, Index);
                }
            }
            Pending = 0;
        }
        if (!Striding) {
            Eager = Free && Speed > Active.Reference() * StartShare ? Eager + 1 : 0;
            if (Eager >= StartTicks) {
                Striding = true;
                Eager = 0;
                Calm = 0;
                Hold = Form.HoldTicks();
                Phase = 0.0;
                PhaseO = 0.0;
            }
            return;
        }
        Calm = !Free || Speed < Active.Reference() * StopShare ? Calm + 1 : 0;
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

    private void contact(Scp075Snail Owner, Profile Active, int Index, boolean Free) {
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

    private void footfall(Scp075Snail Owner, Profile Active, int Index) {
        RandomSource Random = Owner.getRandom();
        float Size = Owner.getAgeScale();
        float Weight = (float) Mth.clamp(1.15 - 0.12 * Rate, 0.7, 1.0) * Active.Weights()[Index] * (0.45f + 0.55f * Size);
        float Pitch = Form.Pitch() * (0.94f + Random.nextFloat() * 0.1f) * (1.0f + (1.0f - Size) * 0.6f);
        if (!play(Owner, Form.Step(), Form.StepVolume() * Weight * (0.88f + Random.nextFloat() * 0.12f), Pitch)) {
            return;
        }
        play(Owner, Form.Layer(), Form.LayerVolume() * Weight * (0.85f + Random.nextFloat() * 0.15f), Pitch * (0.97f + Random.nextFloat() * 0.06f));
    }

    private boolean play(Scp075Snail Owner, Supplier<SoundEvent> Sound, float Volume, float Pitch) {
        Level World = Owner.level();
        if (Owner.isSilent() || !World.hasNearbyAlivePlayer(Owner.getX(), Owner.getY(), Owner.getZ(), Form.Hearing())) {
            return false;
        }
        World.playLocalSound(Owner.getX(), Owner.getY(), Owner.getZ(), Sound.get(), Owner.getSoundSource(), Volume, Pitch, false);
        return true;
    }
}
