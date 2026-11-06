package net.rai.scp_unleashed.scp131.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp131.Scp131Pod;
import org.jetbrains.annotations.Nullable;

public final class Scp131Face {
    public static final float IrisRangeX = 1.7f;
    public static final float IrisRangeY = 1.6f;
    public static final float GazeYawSpan = 45.0f;
    public static final float GazePitchSpan = 38.0f;
    public static final float BehindYaw = 110.0f;
    public static final float LidRest = 0.06f;
    public static final float LidCurious = 0.0f;
    public static final float LidHappy = 0.12f;
    public static final float LowerHappy = 0.42f;
    public static final float LidPlayful = 0.04f;
    public static final float LowerPlayful = 0.16f;
    public static final float LidShy = 0.34f;
    public static final float LowerShy = 0.2f;
    public static final float LidSad = 0.5f;
    public static final float LowerSad = 0.08f;
    public static final float LowerBlink = 0.14f;
    public static final float PupilRest = 1.0f;
    public static final float PupilCurious = 1.25f;
    public static final float PupilPlayful = 1.08f;
    public static final float PupilStartled = 0.55f;
    public static final float PupilShy = 0.92f;
    public static final float PupilSad = 1.18f;
    public static final float SocketCurious = 1.03f;
    public static final float SocketStartled = 1.07f;
    public static final float LidHeight = 8.0f;
    public static final float LashTravel = 6.95f;
    public static final float LashWide = 0.8f;
    public static final float LeanPerSpeed = 30.0f;
    public static final float LeanPerAccel = 70.0f;
    public static final float LeanLimit = 14.0f;
    public static final float BackLimit = 9.0f;
    public static final float RollGain = 2.0f;
    public static final float RollLimit = 12.0f;
    public static final float TiltCurious = 8.0f;
    public static final float LookUpLean = 7.0f;
    public static final float SadLean = 6.0f;
    public static final float SadSquash = 0.035f;
    public static final float SquashKick = 0.11f;
    public static final float BumpKick = 0.08f;
    public static final float SquashSpring = 0.32f;
    public static final float SquashDamping = 0.62f;
    public static final float SquashLimit = 0.14f;
    public static final float BrakeKick = 0.035f;
    public static final float BreathDepth = 0.012f;
    public static final float BreathTicks = 60.0f;
    public static final float HopHeight = 2.5f;
    public static final float HopTicks = 8.0f;
    public static final float BobTicks = 16.0f;
    public static final float BobDepth = 0.06f;
    public static final float TipBob = 2.0f;
    public static final float TipBobTicks = 50.0f;
    public static final float TipWag = 11.0f;
    public static final float TipWagTicks = 10.0f;
    public static final float TipTrailPerSpeed = 28.0f;
    public static final float TipTrailLimit = 14.0f;
    public static final float TipDroop = 12.0f;
    public static final float TipPerk = -5.0f;
    public static final float TipShy = 6.0f;
    public static final float TipBrake = 40.0f;
    public static final float TipSwingLimit = 8.0f;
    public static final float WheelRadius = Scp131Rig.WheelUnits / 32.0f;
    public static final float BlinkMin = 50.0f;
    public static final float BlinkSpread = 90.0f;
    public static final float BlinkClose = 1.5f;
    public static final float BlinkHold = 0.7f;
    public static final float BlinkOpen = 2.2f;
    public static final float DoubleBlink = 0.25f;
    public static final float DoubleGap = 2.5f;
    public static final float SadBlink = 1.6f;
    public static final float FixMin = 18.0f;
    public static final float FixSpread = 50.0f;
    public static final float GlanceMin = 50.0f;
    public static final float GlanceSpread = 80.0f;
    public static final float GlanceTicks = 9.0f;
    public static final float SaccadeTau = 0.9f;
    public static final float LidTau = 2.5f;
    public static final float PupilTau = 4.0f;
    public static final float BodyTau = 3.0f;
    public static final float SpeedTau = 2.0f;
    public static final float ZoomSpeed = 0.25f;
    public static final float WhirrSpeed = 0.17f;
    public static final double PeopleRange = 8.0;
    public static final float MaxStep = 3.0f;

    private final RandomSource Random;
    private final Scp131Pose Pose = new Scp131Pose();
    private float LastTime = Float.NaN;
    private Vec3 LastPos = Vec3.ZERO;
    private float LastYaw;
    private float Speed;
    private float Forward;
    private float Accel;
    private float YawRate;
    private float IrisX;
    private float IrisY;
    private float GoalX;
    private float GoalY;
    private float FixLeft;
    private boolean FixOnTarget;
    private float LookUp;
    private float GlanceClock = 60.0f;
    private float GlanceLeft;
    private float GlanceX;
    private float GlanceY;
    private float BlinkClock = 40.0f;
    private float BlinkAge = -1.0f;
    private boolean Again;
    private float LidTop = LidRest;
    private float LidBottom;
    private float Pupil = PupilRest;
    private float Socket = 1.0f;
    private float Pitch;
    private float Roll;
    private float Squash;
    private float SquashSpeed;
    private float HopAge = -1.0f;
    private float BobAge = -1.0f;
    private final float[] TipPitch = new float[Scp131Pose.Joints];
    private final float[] TipRoll = new float[Scp131Pose.Joints];
    private float Wheel;
    @Nullable
    private Scp131Whirr Whirr;

    private Scp131Face(Scp131Pod Pod) {
        this.Random = RandomSource.create(Pod.getUUID().getLeastSignificantBits());
        this.BlinkClock = BlinkMin * Random.nextFloat();
    }

    public static Scp131Face of(Scp131Pod Pod) {
        if (Pod.face() instanceof Scp131Face Existing) {
            return Existing;
        }
        Scp131Face Made = new Scp131Face(Pod);
        Pod.setFace(Made);
        return Made;
    }

    public static float lashOffset(float Lid) {
        float Wide = Math.max(0.0f, LidRest - Lid) / LidRest;
        return -LashTravel * Math.min(1.0f, Lid) + Wide * LashWide;
    }

    public static float approach(float Current, float Target, float Tau, float Step) {
        if (Tau <= 0.0f) {
            return Target;
        }
        return Current + (Target - Current) * (1.0f - (float) Math.exp(-Step / Tau));
    }

    public static float blinkCurve(float Age) {
        if (Age < 0.0f) {
            return 0.0f;
        }
        if (Age < BlinkClose) {
            return Age / BlinkClose;
        }
        if (Age < BlinkClose + BlinkHold) {
            return 1.0f;
        }
        float Opening = (Age - BlinkClose - BlinkHold) / BlinkOpen;
        return Opening >= 1.0f ? 0.0f : 1.0f - Opening;
    }

    public static float moodLid(byte Mood) {
        return switch (Mood) {
            case Scp131Pod.MoodHappy -> LidHappy;
            case Scp131Pod.MoodCurious, Scp131Pod.MoodStartled -> LidCurious;
            case Scp131Pod.MoodShy -> LidShy;
            case Scp131Pod.MoodSad -> LidSad;
            case Scp131Pod.MoodPlayful -> LidPlayful;
            default -> LidRest;
        };
    }

    public static float moodLower(byte Mood) {
        return switch (Mood) {
            case Scp131Pod.MoodHappy -> LowerHappy;
            case Scp131Pod.MoodShy -> LowerShy;
            case Scp131Pod.MoodSad -> LowerSad;
            case Scp131Pod.MoodPlayful -> LowerPlayful;
            default -> 0.0f;
        };
    }

    public static float moodPupil(byte Mood) {
        return switch (Mood) {
            case Scp131Pod.MoodCurious -> PupilCurious;
            case Scp131Pod.MoodStartled -> PupilStartled;
            case Scp131Pod.MoodShy -> PupilShy;
            case Scp131Pod.MoodSad -> PupilSad;
            case Scp131Pod.MoodPlayful -> PupilPlayful;
            default -> PupilRest;
        };
    }

    public static float moodSocket(byte Mood) {
        return Mood == Scp131Pod.MoodStartled ? SocketStartled : Mood == Scp131Pod.MoodCurious ? SocketCurious : 1.0f;
    }

    public static float tipBase(byte Mood) {
        return switch (Mood) {
            case Scp131Pod.MoodCurious -> TipPerk;
            case Scp131Pod.MoodShy -> TipShy;
            case Scp131Pod.MoodSad -> TipDroop;
            default -> 0.0f;
        };
    }

    public static float gazeX(float YawDifference) {
        return -Mth.clamp(YawDifference / GazeYawSpan, -1.0f, 1.0f) * IrisRangeX;
    }

    public static float gazeY(float PitchDegrees) {
        return Mth.clamp(PitchDegrees / GazePitchSpan, -1.0f, 1.0f) * IrisRangeY;
    }

    public void cue(byte Kind) {
        if (Kind == Scp131Pod.EventBump) {
            SquashSpeed += BumpKick;
        } else if (Kind == Scp131Pod.EventHop || Kind == Scp131Pod.EventCheer) {
            HopAge = 0.0f;
        } else if (Kind == Scp131Pod.EventBob) {
            BobAge = 0.0f;
        } else if (Kind == Scp131Pod.EventStartle) {
            HopAge = HopTicks * 0.35f;
            SquashSpeed -= BumpKick;
            BlinkAge = -1.0f;
        } else if (Kind == Scp131Pod.EventPetted) {
            BlinkAge = 0.0f;
        }
    }

    public Scp131Pose update(Scp131Pod Pod, float Partial) {
        float Now = Pod.tickCount + Partial;
        float Step = Now - LastTime;
        Vec3 Position = Pod.getPosition(Partial);
        float Yaw = Mth.rotLerp(Partial, Pod.yBodyRotO, Pod.yBodyRot);
        boolean Fresh = Float.isNaN(LastTime) || Step < 0.0f || Step > 40.0f;
        if (Fresh) {
            Step = 0.0f;
            LastPos = Position;
            LastYaw = Yaw;
            for (int Joint = 0; Joint < Scp131Pose.Joints; Joint++) {
                TipPitch[Joint] = tipBase(Pod.getMood());
            }
        }
        LastTime = Now;
        Step = Math.min(Step, MaxStep);
        motion(Pod, Position, Yaw, Step);
        eyes(Pod, Partial, Position, Yaw, Step);
        body(Pod, Now, Step);
        tip(Pod, Now, Step);
        whirr(Pod);
        return Pose;
    }

    private void motion(Scp131Pod Pod, Vec3 Position, float Yaw, float Step) {
        if (Step <= 0.0f) {
            return;
        }
        Vec3 Moved = Position.subtract(LastPos);
        float Flat = (float) Moved.horizontalDistance() / Step;
        Vec3 Facing = Vec3.directionFromRotation(0.0f, Yaw);
        float Along = (float) (Moved.x * Facing.x + Moved.z * Facing.z) / Step;
        float Before = Speed;
        Speed = approach(Speed, Flat, SpeedTau, Step);
        Forward = approach(Forward, Along, SpeedTau, Step);
        float Change = (Speed - Before) / Step;
        Accel = approach(Accel, Change, SpeedTau, Step);
        YawRate = approach(YawRate, Mth.wrapDegrees(Yaw - LastYaw) / Step, SpeedTau, Step);
        if (Change < -BrakeKick && Before > Scp131Pod.WanderModifier * 0.3) {
            SquashSpeed += SquashKick * Math.min(1.0f, -Change / BrakeKick * 0.5f);
        }
        LastPos = Position;
        LastYaw = Yaw;
        Wheel += Forward * Step / WheelRadius * Mth.RAD_TO_DEG;
        Wheel = Wheel % 360.0f;
    }

    private void eyes(Scp131Pod Pod, float Partial, Vec3 Position, float Yaw, float Step) {
        byte Mood = Pod.getMood();
        Entity Focus = Pod.getFocus();
        Vec3 Eye = Position.add(0.0, Scp131Pod.EyeHeight, 0.0);
        float TrackX = 0.0f;
        float TrackY = 0.0f;
        boolean Tracking = false;
        Entity Watched = Focus != null ? Focus : nearestPerson(Pod);
        if (Watched != null) {
            Vec3 Target = Watched.getPosition(Partial).add(0.0, Watched instanceof Scp131Pod ? Scp131Pod.EyeHeight : Watched.getEyeHeight(), 0.0);
            Vec3 Toward = Target.subtract(Eye);
            float TargetYaw = (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f;
            float Difference = Mth.wrapDegrees(TargetYaw - Yaw);
            float Elevation = (float) (Mth.atan2(Toward.y, Toward.horizontalDistance()) * Mth.RAD_TO_DEG);
            if (Math.abs(Difference) < BehindYaw) {
                TrackX = gazeX(Difference);
                TrackY = gazeY(Elevation);
                Tracking = Focus != null || FixOnTarget;
            }
        }
        FixLeft -= Step;
        GlanceClock -= Step;
        if (GlanceLeft > 0.0f) {
            GlanceLeft -= Step;
        }
        if (FixLeft <= 0.0f) {
            FixLeft = FixMin + Random.nextFloat() * FixSpread;
            float Chance = Random.nextFloat();
            FixOnTarget = Watched != null && Chance < 0.45f;
            if (FixOnTarget) {
                GoalX = TrackX;
                GoalY = TrackY;
            } else if (Chance < 0.6f) {
                GoalX = (Random.nextFloat() - 0.5f) * 0.4f;
                GoalY = (Random.nextFloat() - 0.5f) * 0.4f;
            } else {
                GoalX = (Random.nextFloat() * 2.0f - 1.0f) * IrisRangeX;
                GoalY = (Random.nextFloat() * 1.4f - 0.8f) * IrisRangeY;
            }
            if (Random.nextFloat() < 0.15f && BlinkAge < 0.0f) {
                BlinkAge = 0.0f;
            }
        }
        if (GlanceClock <= 0.0f) {
            GlanceClock = GlanceMin + Random.nextFloat() * GlanceSpread;
            GlanceLeft = GlanceTicks;
            GlanceX = (Random.nextFloat() * 2.0f - 1.0f) * IrisRangeX;
            GlanceY = (Random.nextFloat() - 0.4f) * IrisRangeY;
        }
        float WantX;
        float WantY;
        if (Mood == Scp131Pod.MoodSad) {
            boolean Peek = GlanceLeft > 0.0f && Tracking;
            WantX = Peek ? TrackX * 0.6f : GoalX * 0.35f;
            WantY = Peek ? Math.min(TrackY, 0.0f) : -IrisRangeY * 0.95f;
        } else if (Mood == Scp131Pod.MoodShy && Watched != null) {
            boolean Peek = GlanceLeft > 0.0f;
            WantX = Peek ? TrackX : -Math.signum(TrackX == 0.0f ? 1.0f : TrackX) * IrisRangeX * 0.9f;
            WantY = Peek ? TrackY : -IrisRangeY * 0.6f;
        } else if (Speed > ZoomSpeed && Focus == null) {
            WantX = GlanceLeft > 0.0f ? GlanceX * 0.5f : 0.0f;
            WantY = IrisRangeY * 0.2f;
        } else if (Tracking) {
            boolean Away = GlanceLeft > 0.0f && Mood != Scp131Pod.MoodStartled;
            WantX = Away ? Mth.clamp(TrackX + GlanceX * 0.6f, -IrisRangeX, IrisRangeX) : TrackX;
            WantY = Away ? Mth.clamp(TrackY + GlanceY * 0.5f, -IrisRangeY, IrisRangeY) : TrackY;
        } else {
            WantX = GoalX;
            WantY = GoalY;
        }
        IrisX = approach(IrisX, WantX, SaccadeTau, Step);
        IrisY = approach(IrisY, WantY, SaccadeTau, Step);
        BlinkClock -= Step;
        if (BlinkClock <= 0.0f && BlinkAge < 0.0f) {
            BlinkAge = 0.0f;
            Again = Random.nextFloat() < DoubleBlink;
            BlinkClock = (BlinkMin + Random.nextFloat() * BlinkSpread) * (Mood == Scp131Pod.MoodSad ? SadBlink : 1.0f);
        }
        float Blink = 0.0f;
        if (BlinkAge >= 0.0f) {
            BlinkAge += Step / (Mood == Scp131Pod.MoodSad ? SadBlink : 1.0f);
            Blink = blinkCurve(BlinkAge);
            if (BlinkAge > BlinkClose + BlinkHold + BlinkOpen) {
                BlinkAge = -1.0f;
                if (Again) {
                    Again = false;
                    BlinkClock = DoubleGap;
                }
            }
        }
        if (Mood == Scp131Pod.MoodStartled) {
            Blink = 0.0f;
        }
        LidTop = approach(LidTop, moodLid(Mood), LidTau, Step);
        LidBottom = approach(LidBottom, moodLower(Mood), LidTau, Step);
        Pupil = approach(Pupil, moodPupil(Mood), PupilTau, Step);
        Socket = approach(Socket, moodSocket(Mood), Mood == Scp131Pod.MoodStartled ? 0.8f : LidTau, Step);
        Pose.IrisX = IrisX;
        Pose.IrisY = IrisY;
        Pose.Pupil = Pupil;
        Pose.LidTop = Math.max(LidTop, Blink);
        Pose.LidBottom = Math.max(LidBottom, Blink * LowerBlink);
        Pose.Socket = Socket;
        boolean Close = Focus != null && Focus.distanceToSqr(Pod) < 16.0;
        LookUp = Tracking && TrackY > 0.0f && Close && Mood != Scp131Pod.MoodSad ? -LookUpLean * TrackY / IrisRangeY : 0.0f;
    }

    @Nullable
    private Entity nearestPerson(Scp131Pod Pod) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null) {
            return null;
        }
        Player Nearest = null;
        double Best = PeopleRange * PeopleRange;
        for (Player User : Client.level.players()) {
            double Distance = User.distanceToSqr(Pod);
            if (Distance < Best && !User.isSpectator()) {
                Best = Distance;
                Nearest = User;
            }
        }
        return Nearest;
    }

    private void body(Scp131Pod Pod, float Now, float Step) {
        byte Mood = Pod.getMood();
        boolean Sad = Mood == Scp131Pod.MoodSad;
        float Lean = Mth.clamp(Speed * LeanPerSpeed + Accel * LeanPerAccel, -BackLimit, LeanLimit);
        float PitchGoal = Lean + LookUp + (Sad ? SadLean : 0.0f);
        float Tilt = Mood == Scp131Pod.MoodCurious && Speed < 0.02f ? TiltCurious * Mth.sin(Now * 0.035f + Pod.getId()) : 0.0f;
        float RollGoal = Mth.clamp(YawRate * Speed * RollGain, -RollLimit, RollLimit) + Tilt;
        Pitch = approach(Pitch, PitchGoal, BodyTau, Step);
        Roll = approach(Roll, RollGoal, BodyTau, Step);
        float Rest = Sad ? SadSquash : 0.0f;
        float Remaining = Step;
        while (Remaining > 0.0f) {
            float Slice = Math.min(0.5f, Remaining);
            SquashSpeed += (-(Squash - Rest) * SquashSpring - SquashSpeed * SquashDamping) * Slice;
            Squash = Mth.clamp(Squash + SquashSpeed * Slice, -SquashLimit, SquashLimit);
            Remaining -= Slice;
        }
        float Lift = 0.0f;
        if (HopAge >= 0.0f) {
            HopAge += Step;
            if (HopAge >= HopTicks) {
                HopAge = -1.0f;
                SquashSpeed += SquashKick * 0.8f;
            } else {
                Lift = Mth.sin(Mth.PI * HopAge / HopTicks) * HopHeight;
            }
        }
        float Bob = 0.0f;
        if (BobAge >= 0.0f) {
            BobAge += Step;
            if (BobAge >= BobTicks) {
                BobAge = -1.0f;
            } else {
                Bob = Mth.sin(BobAge / BobTicks * Mth.TWO_PI * 2.0f) * BobDepth * (1.0f - BobAge / BobTicks);
            }
        }
        float Breath = Mth.sin(Now * Mth.TWO_PI / BreathTicks) * BreathDepth;
        Pose.BodyPitch = Pitch;
        Pose.BodyRoll = Roll;
        Pose.Squash = Mth.clamp(Squash + Bob + Breath, -SquashLimit, SquashLimit);
        Pose.Lift = Lift;
        Pose.Wheel = Wheel;
    }

    private void tip(Scp131Pod Pod, float Now, float Step) {
        byte Mood = Pod.getMood();
        boolean Waggy = Mood == Scp131Pod.MoodHappy || Mood == Scp131Pod.MoodPlayful;
        float Base = tipBase(Mood);
        float Trail = Math.min(TipTrailLimit, Speed * TipTrailPerSpeed);
        float Swing = Mth.clamp(Accel * TipBrake, -TipSwingLimit, TipSwingLimit);
        for (int Joint = 0; Joint < Scp131Pose.Joints; Joint++) {
            float Weight = 0.5f + 0.5f * Joint / (Scp131Pose.Joints - 1.0f);
            float Bob = TipBob * Mth.sin(Now * Mth.TWO_PI / TipBobTicks - Joint * 0.6f) * (Mood == Scp131Pod.MoodSad ? 0.4f : 1.0f);
            float PitchGoal = Base + (Trail + Swing) * Weight + Bob;
            float Wag = Waggy ? TipWag * Mth.sin(Now * Mth.TWO_PI / TipWagTicks - Joint * 0.8f) * Weight : 0.0f;
            float RollGoal = Wag - Roll * 0.5f * Weight;
            float Tau = 2.0f + Joint;
            TipPitch[Joint] = approach(TipPitch[Joint], PitchGoal, Tau, Step);
            TipRoll[Joint] = approach(TipRoll[Joint], RollGoal, Tau, Step);
            Pose.TipPitch[Joint] = TipPitch[Joint];
            Pose.TipRoll[Joint] = TipRoll[Joint];
        }
    }

    private void whirr(Scp131Pod Pod) {
        if (Speed < WhirrSpeed || Pod.isSilent() || !Pod.isAlive()) {
            return;
        }
        if (Whirr == null || Whirr.isStopped()) {
            Whirr = new Scp131Whirr(Pod);
            Minecraft.getInstance().getSoundManager().play(Whirr);
        }
    }
}
