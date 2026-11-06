package net.rai.scp_unleashed.dclass;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public final class HumanoidGait {
    public static final double Response = 0.4;
    public static final double WalkCycle = 1.7;
    public static final double RunCycle = 2.6;
    public static final double StartSpeed = 0.022;
    public static final double StopSpeed = 0.01;
    public static final double RunSpeed = 0.16;
    public static final int StartTicks = 2;
    public static final int StopTicks = 3;
    public static final double HearingRange = 24.0;
    public static final double Wrap = 64.0;
    public static final float StepVolume = 0.15f;
    private static final double[] Contacts = {0.0, 0.5};

    private double Phase;
    private double PhaseO;
    private double Speed;
    private boolean Striding;
    private boolean Running;
    private int Eager;
    private int Calm;
    private float Partial;

    public boolean striding() {
        return Striding;
    }

    public boolean running() {
        return Running;
    }

    public double speed() {
        return Speed;
    }

    public void hold(double Cycle, boolean Stride, boolean Run) {
        Striding = Stride;
        Running = Run;
        Phase = Cycle;
        PhaseO = Cycle;
        Eager = 0;
        Calm = 0;
    }

    public void setPartial(float Value) {
        Partial = Mth.clamp(Value, 0.0f, 1.0f);
    }

    public double cycle() {
        double Value = PhaseO + (Phase - PhaseO) * Partial;
        return Math.min(Value - Math.floor(Value), 0.9999);
    }

    public void tick(DClassLike Owner, boolean Sounds) {
        PhaseO = Phase;
        double Dx = Owner.getX() - Owner.xo;
        double Dz = Owner.getZ() - Owner.zo;
        Speed += (Math.sqrt(Dx * Dx + Dz * Dz) - Speed) * Response;
        boolean Grounded = Owner.isAlive() && Owner.onGround() && !Owner.isInWater() && !Owner.isPassenger() && !Owner.isRestingPose();
        if (!Striding) {
            Eager = Grounded && Speed > StartSpeed ? Eager + 1 : 0;
            if (Eager >= StartTicks) {
                Striding = true;
                Eager = 0;
                Calm = 0;
                Phase = 0.25;
                PhaseO = 0.25;
            }
            return;
        }
        Calm = !Grounded || Speed < StopSpeed ? Calm + 1 : 0;
        if (Calm >= StopTicks) {
            Striding = false;
            Calm = 0;
            Phase = 0.0;
            PhaseO = 0.0;
            return;
        }
        Running = Owner.isSprinting() || Speed > RunSpeed;
        Phase += Speed / (Running ? RunCycle : WalkCycle);
        if (Sounds && Grounded) {
            for (double Contact : Contacts) {
                if (Math.floor(Phase - Contact) > Math.floor(PhaseO - Contact)) {
                    footfall(Owner);
                }
            }
        }
        if (Phase >= Wrap) {
            Phase -= Wrap;
            PhaseO -= Wrap;
        }
    }

    private void footfall(DClassLike Owner) {
        Level World = Owner.level();
        if (Owner.isSilent() || !World.hasNearbyAlivePlayer(Owner.getX(), Owner.getY(), Owner.getZ(), HearingRange)) {
            return;
        }
        BlockPos Below = Owner.getOnPos();
        BlockState State = World.getBlockState(Below);
        if (State.isAir()) {
            return;
        }
        SoundType Type = State.getSoundType(World, Below, Owner);
        float Weight = Running ? 1.25f : Owner.isCrouching() ? 0.45f : 1.0f;
        float Pitch = Type.getPitch() * (0.92f + Owner.getRandom().nextFloat() * 0.14f);
        World.playLocalSound(Owner.getX(), Owner.getY(), Owner.getZ(), Type.getStepSound(), Owner.getSoundSource(), Type.getVolume() * StepVolume * Weight, Pitch, false);
    }
}
