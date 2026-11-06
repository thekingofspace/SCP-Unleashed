package net.rai.scp_unleashed.scp428;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class Scp428Cords {
    public static final double HeadUp = 0.25;
    public static final double HeadBack = 0.25;
    public static final double Sink = 0.2;
    public static final double ShortCord = 2.5;
    public static final double ShortFloor = 0.35;
    public static final double Centripetal = 0.5;
    public static final double ClimbStart = 0.3;
    public static final double SteepStart = 0.94;
    public static final double HeadClear = 0.45;
    public static final double HookPull = 1.0;
    public static final double CrestFloor = 0.38;
    public static final double Direct = 0.8;
    public static final double RootRise = 0.62;
    public static final double RootOut = 0.55;
    public static final double ArchBase = 0.15;
    public static final double ArchPerBlock = 0.02;
    public static final double HookBack = 0.3;
    public static final double HookUp = 0.22;
    public static final double CrestHeight = 0.7;
    public static final double CrestLead = 0.2;
    public static final double CrestFade = 0.7;
    public static final double Settle = 0.3;
    public static final double SampleBias = 1.4;
    public static final double SagShare = 0.07;
    public static final double SwayShare = 0.025;
    public static final double WobbleShare = 0.012;
    public static final double SlackRange = 14.0;
    public static final double ReachShare = 0.72;
    public static final double ReachArch = 0.45;
    public static final double ReachArchPerBlock = 0.08;
    public static final double ReachRise = 0.86;
    public static final double SearchShare = 0.12;
    public static final double HoverBack = 0.35;
    public static final double HoverUp = 0.2;
    public static final double GripCurl = 0.18;
    public static final double GripArch = 0.3;
    public static final double GripSqueeze = 0.02;
    public static final double GripSide = 0.32;
    public static final double GripHigh = 0.95;
    public static final double GripLow = 0.45;
    public static final double GripFront = 0.05;
    public static final double GripWrap = 0.18;
    public static final double GripRootSide = 0.4;
    public static final double GripRootOut = 0.22;
    public static final double GripRootHigh = 0.5;
    public static final double GripRootLow = 0.3;
    public static final double GripPhase = 2.1;
    public static final double StandardHeight = 1.8;
    public static final double NeckHeight = 1.501;
    public static final double CrouchDrop = 0.3875;
    public static final double NeckShare = NeckHeight / StandardHeight;
    private static final Vec3 Up = new Vec3(0.0, 1.0, 0.0);

    private Scp428Cords() {
    }

    public static double smooth(double Value) {
        double Clamped = Mth.clamp(Value, 0.0, 1.0);
        return Clamped * Clamped * (3.0 - 2.0 * Clamped);
    }

    public static double phaseOf(int Id) {
        return (Id * 0.618034 % 1.0) * Math.PI * 2.0;
    }

    public static double sample(int Index, int Count) {
        return 1.0 - Math.pow(1.0 - (double) Index / Count, SampleBias);
    }

    public static Vec3[] frame(Vec3 From, Vec3 To) {
        Vec3 Span = To.subtract(From);
        Vec3 Forward = Span.lengthSqr() < 1.0e-8 ? new Vec3(1.0, 0.0, 0.0) : Span.normalize();
        Vec3 Side = Forward.cross(Up);
        if (Side.lengthSqr() < 1.0e-6) {
            Side = new Vec3(1.0, 0.0, 0.0);
        }
        return new Vec3[]{Forward, Side.normalize()};
    }

    public static Vec3 root(Vec3 Centre, double Width, double Height, Vec3 Toward) {
        Vec3 Flat = new Vec3(Toward.x - Centre.x, 0.0, Toward.z - Centre.z);
        if (Flat.lengthSqr() < 1.0e-6) {
            Flat = new Vec3(1.0, 0.0, 0.0);
        }
        Flat = Flat.normalize();
        return Centre.add(Flat.scale(Width * 0.5 * RootOut)).add(0.0, Height * RootRise, 0.0);
    }

    public static Vec3 reachRoot(Vec3 Centre, double Height) {
        return Centre.add(0.0, Height * ReachRise, 0.0);
    }

    public static Vec3 gripRoot(Vec3 Centre, Vec3 Victim, double Width, double Height, int Arm) {
        Vec3[] Sides = frame(Centre, new Vec3(Victim.x, Centre.y, Victim.z));
        double Offset = Arm == 0 ? GripSide : Arm == 1 ? -GripSide : 0.0;
        return Centre.add(Sides[1].scale(Offset * Width * GripRootSide)).add(Sides[0].scale(Width * GripRootOut)).add(0.0, Height * (Arm == 2 ? GripRootLow : GripRootHigh), 0.0);
    }

    public static Vec3 gripEnd(Vec3 Centre, Vec3 Victim, double Tall, int Arm) {
        Vec3[] Sides = frame(Centre, Victim);
        double Offset = Arm == 0 ? GripSide : Arm == 1 ? -GripSide : 0.0;
        double Lift = (Arm == 2 ? GripLow : GripHigh) * Tall / StandardHeight;
        return Victim.add(Sides[1].scale(Offset)).add(0.0, Lift, 0.0).add(Sides[0].scale(Arm == 2 ? GripWrap : GripFront));
    }

    public static Vec3 forward(double YawDegrees) {
        double Yaw = Math.toRadians(YawDegrees);
        return new Vec3(-Math.sin(Yaw), 0.0, Math.cos(Yaw));
    }

    public static Vec3 headPoint(Vec3 Neck, double YawDegrees, double PitchDegrees, double Scale) {
        Vec3 Forward = forward(YawDegrees);
        double Pitch = Math.toRadians(PitchDegrees);
        Vec3 Facing = Forward.scale(Math.cos(Pitch)).subtract(Up.scale(Math.sin(Pitch)));
        Vec3 Top = Up.scale(Math.cos(Pitch)).add(Forward.scale(Math.sin(Pitch)));
        return Neck.add(Top.scale(HeadUp * Scale)).subtract(Facing.scale(HeadBack * Scale));
    }

    public static double neckHeight(LivingEntity Body) {
        if (Body instanceof Player) {
            return Body.getScale() * (Body.isCrouching() ? NeckHeight - CrouchDrop : NeckHeight);
        }
        return Body.getBbHeight() * NeckShare;
    }

    public static boolean upright(LivingEntity Body) {
        Pose Current = Body.getPose();
        return Current == Pose.STANDING || Current == Pose.CROUCHING;
    }

    public static double grip(Vec3 From, Vec3 To) {
        return Mth.clamp(From.distanceTo(To) / ShortCord, ShortFloor, 1.0);
    }

    public static Vec3[] waypoints(Vec3 From, Vec3 To, Vec3 Back) {
        double Scale = grip(From, To);
        double Climb = (To.y - From.y) / Math.max(1.0e-4, To.distanceTo(From));
        double Lift = 1.0 - smooth((Climb - ClimbStart) / (1.0 - ClimbStart));
        double Under = smooth((Climb - SteepStart) / (1.0 - SteepStart));
        Vec3 Head = To.subtract(Back.scale(HeadBack));
        Vec3 Toward = new Vec3(From.x - Head.x, 0.0, From.z - Head.z);
        Toward = Toward.lengthSqr() < 1.0e-8 ? Back.reverse() : Toward.normalize();
        double Front = -Toward.dot(Back);
        double Over = smooth((Front + CrestFade) / CrestFade) * (1.0 - Under);
        Vec3 Hook = To.add(Back.scale(HookBack * Scale)).add(0.0, HookUp * Scale * Lift, 0.0);
        Vec3 Crest = Head.add(0.0, Math.max(CrestHeight * Scale, CrestFloor), 0.0).add(Toward.scale(CrestLead * Scale));
        boolean Close = From.distanceTo(To) < Direct;
        Vec3 Lead = Close ? From.add(Hook).scale(0.5) : Hook.add(From.subtract(Hook).scale(0.25));
        Vec3 Bend = clear(Lead.add(Crest.subtract(Lead).scale(Over)), Head, Math.min(HeadClear, From.distanceTo(Head)));
        Vec3 Entry = To.add(lean(Bend.subtract(To), Back).scale(HookBack * Scale));
        if (Close) {
            return new Vec3[]{From, Bend, Entry, To};
        }
        double Length = Bend.distanceTo(From);
        Vec3 Middle = From.add(Bend).scale(0.5).add(0.0, (ArchBase + ArchPerBlock * Length) * Scale, 0.0);
        return new Vec3[]{From, Middle, Bend, Entry, To};
    }

    private static Vec3 clear(Vec3 Point, Vec3 Centre, double Radius) {
        Vec3 Out = Point.subtract(Centre);
        if (Out.length() >= Radius) {
            return Point;
        }
        return Centre.add((Out.lengthSqr() < 1.0e-8 ? Up : Out.normalize()).scale(Radius));
    }

    private static Vec3 lean(Vec3 Toward, Vec3 Back) {
        Vec3 Along = Toward.lengthSqr() < 1.0e-8 ? Back : Toward.normalize();
        Vec3 Tilted = Along.add(Back.scale(HookPull));
        return Tilted.lengthSqr() < 1.0e-8 ? Back : Tilted.normalize();
    }

    public static Vec3 spline(Vec3[] Points, double Along) {
        int Count = Points.length - 1;
        double[] Chords = new double[Count];
        double Total = 0.0;
        for (int Index = 0; Index < Count; Index++) {
            Chords[Index] = Math.max(1.0e-4, Points[Index + 1].distanceTo(Points[Index]));
            Total += Chords[Index];
        }
        double Target = Mth.clamp(Along, 0.0, 1.0) * Total;
        int Index = 0;
        while (Index < Count - 1 && Target > Chords[Index]) {
            Target -= Chords[Index];
            Index++;
        }
        double Local = Math.min(1.0, Target / Chords[Index]);
        Vec3 Start = Points[Index];
        Vec3 End = Points[Index + 1];
        Vec3 Before = Index > 0 ? Points[Index - 1] : Start.scale(2.0).subtract(End);
        Vec3 After = Index + 2 <= Count ? Points[Index + 2] : End.scale(2.0).subtract(Start);
        double K0 = 0.0;
        double K1 = K0 + knot(Before, Start);
        double K2 = K1 + knot(Start, End);
        double K3 = K2 + knot(End, After);
        double Knot = K1 + (K2 - K1) * Local;
        Vec3 A1 = blend(Before, Start, K0, K1, Knot);
        Vec3 A2 = blend(Start, End, K1, K2, Knot);
        Vec3 A3 = blend(End, After, K2, K3, Knot);
        Vec3 B1 = blend(A1, A2, K0, K2, Knot);
        Vec3 B2 = blend(A2, A3, K1, K3, Knot);
        return blend(B1, B2, K1, K2, Knot);
    }

    private static double knot(Vec3 From, Vec3 To) {
        return Math.max(1.0e-4, Math.pow(From.distanceTo(To), Centripetal));
    }

    private static Vec3 blend(Vec3 First, Vec3 Second, double Low, double High, double At) {
        double Span = Math.max(1.0e-9, High - Low);
        return First.scale((High - At) / Span).add(Second.scale((At - Low) / Span));
    }

    public static Vec3 turn(Vec3 Carried, Vec3 From, Vec3 To) {
        Vec3 Axis = From.cross(To);
        double Sine = Axis.length();
        if (Sine < 1.0e-9) {
            return Carried;
        }
        Axis = Axis.scale(1.0 / Sine);
        double Angle = Math.atan2(Sine, From.dot(To));
        double Cosine = Math.cos(Angle);
        return Carried.scale(Cosine).add(Axis.cross(Carried).scale(Math.sin(Angle))).add(Axis.scale(Axis.dot(Carried) * (1.0 - Cosine)));
    }

    public static Vec3[][] frames(Vec3[] Points) {
        int Count = Points.length;
        Vec3[] Tangents = new Vec3[Count];
        Vec3 Last = new Vec3(1.0, 0.0, 0.0);
        for (int Index = 0; Index < Count; Index++) {
            Vec3 Along = Points[Math.min(Count - 1, Index + 1)].subtract(Points[Math.max(0, Index - 1)]);
            Last = Along.lengthSqr() < 1.0e-12 ? Last : Along.normalize();
            Tangents[Index] = Last;
        }
        Vec3[] Sides = new Vec3[Count];
        Vec3[] Lifts = new Vec3[Count];
        Vec3 Side = Tangents[0].cross(Up);
        if (Side.lengthSqr() < 1.0e-4) {
            Side = Tangents[0].cross(new Vec3(1.0, 0.0, 0.0));
        }
        Sides[0] = Side.normalize();
        Lifts[0] = Sides[0].cross(Tangents[0]).normalize();
        for (int Index = 1; Index < Count; Index++) {
            Vec3 Carried = turn(Sides[Index - 1], Tangents[Index - 1], Tangents[Index]);
            Vec3 Lift = Carried.cross(Tangents[Index]);
            if (Lift.lengthSqr() < 1.0e-12) {
                Lift = Lifts[Index - 1];
            }
            Lifts[Index] = Lift.normalize();
            Sides[Index] = Tangents[Index].cross(Lifts[Index]).normalize();
        }
        return new Vec3[][]{Sides, Lifts};
    }

    public static Vec3 tetherPoint(Vec3 From, Vec3 To, Vec3 Back, double Along, double Time, double Phase) {
        if (Along <= 0.0) {
            return From;
        }
        if (Along >= 1.0) {
            return To;
        }
        Vec3 Curve = spline(waypoints(From, To, Back), Along);
        double Length = To.distanceTo(From);
        Vec3 Side = frame(From, To)[1];
        double Bell = Math.sin(Math.PI * Along) * (1.0 - smooth((Along - (1.0 - Settle)) / Settle));
        double Slack = Mth.clamp(1.25 - Length / SlackRange, 0.25, 1.0);
        double Sag = SagShare * Length * Slack * Bell;
        double Sway = SwayShare * Length * Bell * Math.sin(Time * 0.07 + Phase + Along * 2.5);
        double Wobble = WobbleShare * Length * Bell * Math.sin(Time * 0.05 + Phase * 1.7 + Along * 3.0);
        return Curve.add(0.0, Wobble - Sag, 0.0).add(Side.scale(Sway));
    }

    public static Vec3 reachPoint(Vec3 From, Vec3 To, double Along, double Time, double Phase) {
        if (Along <= 0.0) {
            return From;
        }
        if (Along >= 1.0) {
            return To;
        }
        Vec3 Span = To.subtract(From);
        double Length = Span.length();
        Vec3 Side = frame(From, To)[1];
        double Bell = Math.sin(Math.PI * Along);
        double Arch = (ReachArch + ReachArchPerBlock * Length) * Bell;
        double Search = SearchShare * Along * Bell * Math.sin(Time * 0.6 + Phase);
        double Tremble = 0.03 * Bell * Math.sin(Time * 1.7 + Along * 9.0);
        return From.add(Span.scale(Along)).add(0.0, Arch + Tremble, 0.0).add(Side.scale(Search));
    }

    public static Vec3 reachEnd(Vec3 Head, Vec3 HeadForward, double Progress) {
        Vec3 Hover = Head.subtract(HeadForward.scale(HoverBack)).add(0.0, HoverUp, 0.0);
        double Contact = smooth((Progress - ReachShare) / (1.0 - ReachShare));
        return Hover.add(Head.subtract(Hover).scale(Contact));
    }

    public static double reachExtension(double Progress) {
        return smooth(Progress / ReachShare);
    }

    public static Vec3 gripPoint(Vec3 From, Vec3 To, double Along, double Time, double Phase) {
        if (Along <= 0.0) {
            return From;
        }
        if (Along >= 1.0) {
            return To;
        }
        Vec3 Span = To.subtract(From);
        Vec3 Side = frame(From, To)[1];
        double Bell = Math.sin(Math.PI * Along);
        double Curl = GripCurl * Along * Bell * Math.sin(Along * Math.PI * 2.0 + Phase);
        double Squeeze = GripSqueeze * Bell * Math.sin(Time * 0.9 + Phase);
        return From.add(Span.scale(Along)).add(0.0, GripArch * Bell + Squeeze, 0.0).add(Side.scale(Curl));
    }
}
