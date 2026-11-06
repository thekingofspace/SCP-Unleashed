package net.rai.scp_unleashed.scp049;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

import java.util.function.Supplier;

public final class Scp049Breath {
    public static final int IdleMin = 180;
    public static final int IdleMax = 260;
    public static final int MoveMin = 120;
    public static final int MoveMax = 160;
    public static final int WorkMin = 70;
    public static final int WorkMax = 90;
    public static final int IdleExhale = 34;
    public static final int MoveExhale = 30;
    public static final int WorkExhale = 24;
    public static final int ExhaleLimit = 30;
    public static final float IdleVolume = 0.45f;
    public static final float MoveVolume = 0.6f;
    public static final float WorkVolume = 1.0f;
    public static final float IdlePitch = 0.93f;
    public static final float MovePitch = 1.0f;
    public static final float WorkPitch = 1.07f;
    public static final float FadeStep = 0.035f;
    public static final float Silence = 0.02f;
    public static final float MaskShare = 0.55f;
    public static final int StepGuard = 3;
    public static final int Nudge = 4;
    public static final int QuietAhead = 14;
    public static final double HearingRange = 16.0;

    public enum Effort {
        Idle,
        Moving,
        Working
    }

    private int Next = 40;
    private int ExhaleIn = -1;
    private int ExhaleLate;
    private float Level;
    private float Pitch = 1.0f;
    private Effort Current = Effort.Idle;

    public float level() {
        return Level;
    }

    public Effort effort() {
        return Current;
    }

    public void tick(Scp049Doctor Owner, Effort Kind, Scp049Gait Gait, boolean Quiet) {
        RandomSource Random = Owner.getRandom();
        Current = Kind;
        float Target = Owner.isAlive() && !Owner.isSilent() ? volumeOf(Kind) : 0.0f;
        Level += Mth.clamp(Target - Level, -FadeStep, FadeStep);
        boolean Crowded = Gait.striding() && (Gait.ticksSinceStep(Owner.tickCount) < StepGuard || Gait.ticksToStep() < StepGuard);
        if (ExhaleIn > 0 && --ExhaleIn == 0) {
            if (Quiet || Crowded) {
                ExhaleLate += Nudge;
                ExhaleIn = ExhaleLate > ExhaleLimit ? -1 : Nudge;
            } else {
                float Out = Level * (0.9f + Random.nextFloat() * 0.1f);
                play(Owner, Scp049Registry.BreatheOut, Out, Pitch * (0.97f + Random.nextFloat() * 0.05f));
                play(Owner, Scp049Registry.BreatheMask, Out * MaskShare, Pitch * 0.86f);
                ExhaleIn = -1;
            }
        }
        if (--Next > 0 || ExhaleIn > 0) {
            return;
        }
        if (Quiet || Crowded) {
            Next = Nudge + Random.nextInt(Nudge);
            return;
        }
        Next = cycleOf(Kind, Random);
        Pitch = pitchOf(Kind) * (0.96f + Random.nextFloat() * 0.06f);
        float In = Level * (0.9f + Random.nextFloat() * 0.1f);
        play(Owner, Scp049Registry.BreatheIn, In, Pitch);
        play(Owner, Scp049Registry.BreatheMask, In * MaskShare, Pitch);
        ExhaleIn = exhaleOf(Kind) + Random.nextInt(5) - 2;
        ExhaleLate = 0;
    }

    public static int cycleOf(Effort Kind, RandomSource Random) {
        return switch (Kind) {
            case Idle -> IdleMin + Random.nextInt(IdleMax - IdleMin + 1);
            case Moving -> MoveMin + Random.nextInt(MoveMax - MoveMin + 1);
            case Working -> WorkMin + Random.nextInt(WorkMax - WorkMin + 1);
        };
    }

    public static int exhaleOf(Effort Kind) {
        return switch (Kind) {
            case Idle -> IdleExhale;
            case Moving -> MoveExhale;
            case Working -> WorkExhale;
        };
    }

    public static float volumeOf(Effort Kind) {
        return switch (Kind) {
            case Idle -> IdleVolume;
            case Moving -> MoveVolume;
            case Working -> WorkVolume;
        };
    }

    public static float pitchOf(Effort Kind) {
        return switch (Kind) {
            case Idle -> IdlePitch;
            case Moving -> MovePitch;
            case Working -> WorkPitch;
        };
    }

    private static void play(Scp049Doctor Owner, Supplier<SoundEvent> Sound, float Volume, float Pitch) {
        Level World = Owner.level();
        if (Volume < Silence || !World.hasNearbyAlivePlayer(Owner.getX(), Owner.getY(), Owner.getZ(), HearingRange)) {
            return;
        }
        World.playLocalSound(Owner.getX(), Owner.getEyeY(), Owner.getZ(), Sound.get(), Owner.getSoundSource(), Volume, Pitch, false);
    }
}
