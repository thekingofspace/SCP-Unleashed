package net.rai.scp_unleashed.scp354;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.motion.PaceMeter;
import net.rai.scp_unleashed.motion.Settle;

import java.util.function.Supplier;

public final class Scp354Gait {
    public static final double Response = 0.35;
    public static final double StartShare = 0.3;
    public static final double StopShare = 0.15;
    public static final int StartTicks = 2;
    public static final int StopTicks = 3;
    public static final double Wrap = 64.0;
    public static final double LateShare = 0.5;
    public static final double SwitchWindow = 0.12;
    public static final double MaxPhaseStep = 0.125;
    public static final int SwitchTicks = 6;
    public static final int WalkMode = 0;
    public static final int RunMode = 1;
    public static final int StalkMode = 2;

    public record Profile(double CycleTicks, double Reference, double MinRate, double MaxRate, double[] Contacts, float[] Weights) {
    }

    public record Shape(Profile Walk, Profile Run, Profile Stalk, double RunAbove, double WalkBelow, int HoldTicks, Supplier<SoundEvent> Step, Supplier<SoundEvent> Layer,
                        float StepVolume, float LayerVolume, float Pitch, double Hearing) {
    }

    private final Shape Form;
    private final PaceMeter Meter = new PaceMeter();
    private final Settle<Integer> Shift = new Settle<>();
    private double Phase;
    private double PhaseO;
    private double Speed;
    private double Rate;
    private boolean Striding;
    private boolean Running;
    private boolean Stalking;
    private int Eager;
    private int Calm;
    private int Hold;
    private int Pending;
    private float Partial;

    public Scp354Gait(Shape Form) {
        this.Form = Form;
    }

    public boolean striding() {
        return Striding;
    }

    public boolean running() {
        return Running;
    }

    public boolean stalking() {
        return Stalking;
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
        if (Stalking && Form.Stalk() != null) {
            return Form.Stalk();
        }
        return Running ? Form.Run() : Form.Walk();
    }

    public void tick(Scp354Beast Owner) {
        PhaseO = Phase;
        double Dx = Owner.getX() - Owner.xo;
        double Dz = Owner.getZ() - Owner.zo;
        Speed += (Meter.push(Math.sqrt(Dx * Dx + Dz * Dz)) - Speed) * Response;
        boolean Grounded = Owner.isAlive() && Owner.onGround() && !Owner.isInWater() && !Owner.isPassenger();
        boolean Free = Grounded && !Owner.locksMovement();
        boolean WantStalk = Owner.isStalking() && Form.Stalk() != null;
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
            Eager = Free && Speed > lowest() * StartShare ? Eager + 1 : 0;
            if (Eager >= StartTicks) {
                Striding = true;
                Stalking = WantStalk;
                Running = !Stalking && Speed > Form.RunAbove();
                Eager = 0;
                Calm = 0;
                Hold = Form.HoldTicks();
                Phase = 0.0;
                PhaseO = 0.0;
            }
            return;
        }
        Calm = !Free || Speed < lowest() * StopShare ? Calm + 1 : 0;
        if (Calm >= StopTicks) {
            Striding = false;
            Running = false;
            Stalking = false;
            Calm = 0;
            Shift.clear();
            Pending = 0;
            Phase = 0.0;
            PhaseO = 0.0;
            Rate = 0.0;
            return;
        }
        boolean WantRun = Running;
        if (WantStalk) {
            WantRun = false;
        } else if (Running && Speed < Form.WalkBelow()) {
            WantRun = false;
        } else if (!Running && Speed > Form.RunAbove()) {
            WantRun = true;
        }
        int Mode = mode(Stalking, Running);
        int Wanted = mode(WantStalk, WantRun);
        if (Shift.ready(Mode, Wanted, Mode == StalkMode || Wanted == StalkMode ? 1 : SwitchTicks) && nearCycleStart()) {
            Stalking = WantStalk;
            Running = WantRun;
            Phase = 0.0;
            PhaseO = 0.0;
            Hold = Form.HoldTicks();
            Shift.clear();
        }
        Active = current();
        if (Hold > 0) {
            Hold--;
            return;
        }
        Rate = Mth.clamp(Speed / Active.Reference(), Active.MinRate(), Math.min(Active.MaxRate(), MaxPhaseStep * Active.CycleTicks()));
        Phase += Rate / Active.CycleTicks();
        for (int Index = 0; Index < Active.Contacts().length; Index++) {
            contact(Owner, Active, Index, Free);
        }
        if (Phase >= Wrap) {
            Phase -= Wrap;
            PhaseO -= Wrap;
        }
    }

    private static int mode(boolean Stalk, boolean Run) {
        return Stalk ? StalkMode : Run ? RunMode : WalkMode;
    }

    private boolean nearCycleStart() {
        double Share = Phase - Math.floor(Phase);
        return Share < SwitchWindow || Share > 1.0 - SwitchWindow;
    }

    private double lowest() {
        double Low = Form.Walk().Reference();
        if (Form.Stalk() != null) {
            Low = Math.min(Low, Form.Stalk().Reference());
        }
        return Low;
    }

    private void contact(Scp354Beast Owner, Profile Active, int Index, boolean Free) {
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

    private void footfall(Scp354Beast Owner, Profile Active, int Index) {
        RandomSource Random = Owner.getRandom();
        float Weight = (float) Mth.clamp(1.15 - 0.13 * Rate, 0.7, 1.0) * Active.Weights()[Index] * (Running ? 1.15f : Stalking ? 0.55f : 1.0f);
        float Pitch = Form.Pitch() * (Index % 2 == 0 ? 1.0f : 0.96f) * (0.94f + Random.nextFloat() * 0.1f) * (Running ? 1.06f : 1.0f);
        if (!play(Owner, Form.Step(), Form.StepVolume() * Weight * (0.88f + Random.nextFloat() * 0.12f), Pitch)) {
            return;
        }
        play(Owner, Form.Layer(), Form.LayerVolume() * Weight * (0.85f + Random.nextFloat() * 0.15f), Pitch * (0.97f + Random.nextFloat() * 0.06f));
    }

    private boolean play(Scp354Beast Owner, Supplier<SoundEvent> Sound, float Volume, float Pitch) {
        Level World = Owner.level();
        if (Owner.isSilent() || !World.hasNearbyAlivePlayer(Owner.getX(), Owner.getY(), Owner.getZ(), Form.Hearing())) {
            return false;
        }
        World.playLocalSound(Owner.getX(), Owner.getY(), Owner.getZ(), Sound.get(), Owner.getSoundSource(), Volume, Pitch, false);
        return true;
    }
}
