package net.rai.scp_unleashed.scp016;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public final class Scp016Gait {
    public static final double Response = 0.35;
    public static final double StartShare = 0.3;
    public static final double StopShare = 0.15;
    public static final int StartTicks = 2;
    public static final int StopTicks = 3;
    public static final double HearingRange = 32.0;
    public static final double Wrap = 64.0;
    public static final double LateShare = 0.5;

    public record Profile(double CycleTicks, double Reference, double MinRate, double MaxRate, int HoldTicks, double[] Contacts, float[] Weights,
                          Supplier<SoundEvent> Step, @Nullable Supplier<SoundEvent> Thud, @Nullable Supplier<SoundEvent> Creak, @Nullable Supplier<SoundEvent> Shuffle,
                          float StepVolume, float ThudVolume, float CreakVolume, float ShuffleVolume, float Pitch, float CreakChance) {
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
    private int CreakDelay;
    private float Partial;

    public Scp016Gait(Profile Shape) {
        this.Shape = Shape;
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

    public void tick(Scp016Spawn Owner) {
        PhaseO = Phase;
        double Dx = Owner.getX() - Owner.xo;
        double Dz = Owner.getZ() - Owner.zo;
        Speed += (Math.sqrt(Dx * Dx + Dz * Dz) - Speed) * Response;
        boolean Grounded = Owner.isAlive() && Owner.onGround() && !Owner.isInWater() && !Owner.isPassenger();
        boolean Free = Grounded && !Owner.locksMovement();
        double Reference = Shape.Reference() * Math.max(0.1, Owner.getScale());
        if (Pending != 0) {
            for (int Index = 0; Index < Shape.Contacts().length; Index++) {
                if ((Pending & 1 << Index) != 0 && Free && Striding) {
                    footfall(Owner, Index);
                }
            }
            Pending = 0;
        }
        if (CreakDelay > 0 && --CreakDelay == 0 && Free && Shape.Creak() != null) {
            play(Owner, Shape.Creak(), Shape.CreakVolume(), Shape.Pitch() * (0.9f + Owner.getRandom().nextFloat() * 0.15f));
        }
        if (!Striding) {
            Eager = Free && Speed > Reference * StartShare ? Eager + 1 : 0;
            if (Eager >= StartTicks) {
                Striding = true;
                Eager = 0;
                Calm = 0;
                Hold = Shape.HoldTicks();
                Phase = 0.0;
                PhaseO = 0.0;
                shuffle(Owner);
            }
            return;
        }
        Calm = !Free || Speed < Reference * StopShare ? Calm + 1 : 0;
        if (Calm >= StopTicks) {
            Striding = false;
            Calm = 0;
            Pending = 0;
            Phase = 0.0;
            PhaseO = 0.0;
            Rate = 0.0;
            if (Grounded) {
                shuffle(Owner);
            }
            return;
        }
        if (Hold > 0) {
            Hold--;
            return;
        }
        Rate = Mth.clamp(Speed / Reference, Shape.MinRate(), Shape.MaxRate());
        Phase += Rate / Shape.CycleTicks();
        for (int Index = 0; Index < Shape.Contacts().length; Index++) {
            contact(Owner, Index, Free);
        }
        if (Phase >= Wrap) {
            Phase -= Wrap;
            PhaseO -= Wrap;
        }
    }

    private void contact(Scp016Spawn Owner, int Index, boolean Free) {
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

    private void footfall(Scp016Spawn Owner, int Index) {
        RandomSource Random = Owner.getRandom();
        float Weight = (float) Mth.clamp(1.15 - 0.13 * Rate, 0.7, 1.0) * Shape.Weights()[Index];
        float Pitch = Shape.Pitch() * (Index % 2 == 0 ? 1.0f : 0.96f) * (1.2f - 0.2f * Shape.Weights()[Index]) * (0.94f + Random.nextFloat() * 0.1f);
        play(Owner, Shape.Step(), Shape.StepVolume() * Weight * (0.88f + Random.nextFloat() * 0.12f), Pitch);
        if (Shape.Thud() != null) {
            play(Owner, Shape.Thud(), Shape.ThudVolume() * Weight * (0.85f + Random.nextFloat() * 0.15f), Pitch * (0.97f + Random.nextFloat() * 0.06f));
        }
        if (Shape.Creak() != null && CreakDelay <= 0 && Random.nextFloat() < Shape.CreakChance()) {
            CreakDelay = 2 + Random.nextInt(3);
        }
    }

    private void shuffle(Scp016Spawn Owner) {
        if (Shape.Shuffle() == null) {
            return;
        }
        RandomSource Random = Owner.getRandom();
        play(Owner, Shape.Shuffle(), Shape.ShuffleVolume() * (0.85f + Random.nextFloat() * 0.15f), Shape.Pitch() * (0.95f + Random.nextFloat() * 0.1f));
    }

    private static void play(Scp016Spawn Owner, Supplier<SoundEvent> Sound, float Volume, float Pitch) {
        Level World = Owner.level();
        if (Owner.isSilent() || !World.hasNearbyAlivePlayer(Owner.getX(), Owner.getY(), Owner.getZ(), HearingRange)) {
            return;
        }
        World.playLocalSound(Owner.getX(), Owner.getY(), Owner.getZ(), Sound.get(), Owner.getSoundSource(), Volume, Pitch, false);
    }
}
