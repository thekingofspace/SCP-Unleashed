package net.rai.scp_unleashed.scp5001.xk.client;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp5001.xk.YaldabaothFlight;
import net.rai.scp_unleashed.scp5001.xk.network.YaldabaothPayload;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public final class YaldabaothClient {
    public static final double ModelScale = 4.0;
    public static final double Block = ModelScale / 16.0;
    public static final double HeadBack = 19.0;
    public static final double HeadScale = 1.3;
    public static final double RingLength = 12.0;
    public static final double Overlap = 0.78;
    public static final int Segments = 34;
    public static final double TrailLength = 120.0;
    public static final int BanishTicks = 30;
    public static final double BankGain = 36.0;
    public static final float BankLimit = 0.6f;
    public static final double Delay = 6.0;
    public static final double Band = 1.0;
    public static final double Gain = 0.03;
    public static final double Reach = 3.0;
    public static final double Grid = 0.5;
    public static final double Resync = 20.0;
    public static final double Teleport = 96.0;
    public static final int Keep = 420;
    private static boolean Present;
    private static boolean Banishing;
    private static int BanishAge;
    private static double RenderTime;
    private static double Rate = 1.0;
    private static double Latest;
    private static int Flinch;
    private static int Mode;
    private static Vec3 Portal = Vec3.ZERO;
    private static Vec3 BindDirection = new Vec3(1.0, 0.0, 0.0);
    private static float BindProgress;
    private static long Clock;
    private static float Sway = 1.0f;
    private static float SwayO = 1.0f;
    private static final ArrayList<Sample> History = new ArrayList<>();

    public record Placement(YaldabaothPart.Kind Kind, Vec3 Position, float Yaw, float Pitch, float Roll, float Scale, float Stretch, int Index) {
    }

    public record Sample(double Stamp, Vec3 Head, Vec3 Facing, float Jaw) {
    }

    public record Pose(Vec3 Head, Vec3 Facing, float Jaw) {
    }

    private YaldabaothClient() {
    }

    public static void receive(YaldabaothPayload Payload) {
        if (Payload.State() == YaldabaothPayload.Gone) {
            clear();
            return;
        }
        double Stamp = Payload.Stamp();
        Vec3 Facing = Payload.Facing().lengthSqr() > 1.0E-6 ? Payload.Facing().normalize() : new Vec3(1.0, 0.0, 0.0);
        Sample Fresh = new Sample(Stamp, Payload.Head(), Facing, Payload.Jaw());
        boolean Jumped = !History.isEmpty() && History.get(0).Head().distanceToSqr(Payload.Head()) > Teleport * Teleport;
        if (!Present || Jumped || Stamp < Latest - Resync) {
            seed(Fresh, Payload.Trail());
        } else if (Stamp > Latest) {
            History.add(0, Fresh);
            Latest = Stamp;
        } else {
            int Slot = 0;
            while (Slot < History.size() && History.get(Slot).Stamp() > Stamp) {
                Slot++;
            }
            if (Slot >= History.size() || History.get(Slot).Stamp() != Stamp) {
                History.add(Slot, Fresh);
            }
        }
        Present = true;
        Banishing = Payload.State() == YaldabaothPayload.Banishing;
        Flinch = Payload.Flinch();
        Mode = Payload.Mode();
        Portal = Payload.Portal();
        BindDirection = Payload.BindDirection().lengthSqr() > 1.0E-6 ? Payload.BindDirection().normalize() : BindDirection;
        BindProgress = Payload.BindProgress();
    }

    private static void seed(Sample Fresh, List<Vec3> Trail) {
        History.clear();
        History.add(Fresh);
        double Stamp = Fresh.Stamp();
        if (!Trail.isEmpty()) {
            for (int Index = 0; Index < Trail.size(); Index++) {
                History.add(new Sample(Stamp - (Index + 1) * YaldabaothFlight.TrailStep, Trail.get(Index), Fresh.Facing(), Fresh.Jaw()));
            }
        } else {
            for (int Index = 1; Index <= 80; Index++) {
                History.add(new Sample(Stamp - Index * 2.0, Fresh.Head().subtract(Fresh.Facing().scale(Index * 2.0)), Fresh.Facing(), Fresh.Jaw()));
            }
        }
        Latest = Stamp;
        RenderTime = Stamp - Delay;
        Rate = 1.0;
    }

    public static void clear() {
        Present = false;
        Banishing = false;
        BanishAge = 0;
        History.clear();
    }

    public static boolean present() {
        return Present;
    }

    public static long clock() {
        return Clock;
    }

    public static double renderTime() {
        return RenderTime;
    }

    private static Vec3 catmull(Vec3 P0, Vec3 P1, Vec3 P2, Vec3 P3, double T) {
        double T2 = T * T;
        double T3 = T2 * T;
        return P1.scale(2.0).add(P2.subtract(P0).scale(T)).add(P0.scale(2.0).subtract(P1.scale(5.0)).add(P2.scale(4.0)).subtract(P3).scale(T2))
                .add(P1.scale(3.0).subtract(P0).subtract(P2.scale(3.0)).add(P3).scale(T3)).scale(0.5);
    }

    private static int find(double Time, int Hint) {
        int Newer = Math.max(0, Math.min(Hint, History.size() - 1));
        while (Newer > 0 && History.get(Newer).Stamp() <= Time) {
            Newer--;
        }
        while (Newer + 1 < History.size() && History.get(Newer + 1).Stamp() > Time) {
            Newer++;
        }
        return Newer;
    }

    private static Pose pose(double Time, int Newer) {
        if (History.isEmpty()) {
            return new Pose(Vec3.ZERO, new Vec3(1.0, 0.0, 0.0), 0.0f);
        }
        Sample First = History.get(0);
        if (First.Stamp() <= Time) {
            if (History.size() > 1) {
                Sample Second = History.get(1);
                Vec3 Speed = First.Head().subtract(Second.Head()).scale(1.0 / Math.max(1.0E-6, First.Stamp() - Second.Stamp()));
                return new Pose(First.Head().add(Speed.scale(Math.min(Reach, Time - First.Stamp()))), First.Facing(), First.Jaw());
            }
            return new Pose(First.Head(), First.Facing(), First.Jaw());
        }
        if (Newer + 1 >= History.size()) {
            Sample Last = History.get(History.size() - 1);
            return new Pose(Last.Head(), Last.Facing(), Last.Jaw());
        }
        Sample Later = History.get(Newer);
        Sample Earlier = History.get(Newer + 1);
        Sample Before = Newer + 2 < History.size() ? History.get(Newer + 2) : Earlier;
        Sample After = Newer > 0 ? History.get(Newer - 1) : Later;
        double Span = Math.max(1.0E-6, Later.Stamp() - Earlier.Stamp());
        double Share = Mth.clamp((Time - Earlier.Stamp()) / Span, 0.0, 1.0);
        Vec3 Out = Later.Head().subtract(Before.Head()).scale(1.0 / Math.max(1.0E-6, Later.Stamp() - Before.Stamp()));
        Vec3 In = After.Head().subtract(Earlier.Head()).scale(1.0 / Math.max(1.0E-6, After.Stamp() - Earlier.Stamp()));
        double T2 = Share * Share;
        double T3 = T2 * Share;
        Vec3 Head = Earlier.Head().scale(2.0 * T3 - 3.0 * T2 + 1.0).add(Out.scale((T3 - 2.0 * T2 + Share) * Span)).add(Later.Head().scale(-2.0 * T3 + 3.0 * T2))
                .add(In.scale((T3 - T2) * Span));
        Vec3 Facing = Earlier.Facing().lerp(Later.Facing(), Share);
        Facing = Facing.lengthSqr() > 1.0E-6 ? Facing.normalize() : Later.Facing();
        return new Pose(Head, Facing, Mth.lerp((float) Share, Earlier.Jaw(), Later.Jaw()));
    }

    public static Pose pose(double Time) {
        return pose(Time, find(Time, 0));
    }

    public static double at(float Partial) {
        return RenderTime + Partial * Rate;
    }

    public static float jaw(float Partial) {
        return pose(at(Partial)).Jaw();
    }

    public static boolean shut() {
        return Flinch > 0;
    }

    public static boolean banishing() {
        return Banishing;
    }

    public static Vec3 head(float Partial) {
        return pose(at(Partial)).Head();
    }

    public static void tick() {
        Clock++;
        if (!Present) {
            return;
        }
        boolean Hovering = Mode == YaldabaothFlight.Mode.Arena.ordinal() || Mode == YaldabaothFlight.Mode.Attack.ordinal()
                || Mode == YaldabaothFlight.Mode.React.ordinal() || Mode == YaldabaothFlight.Mode.Gather.ordinal();
        SwayO = Sway;
        Sway += ((Hovering ? 2.2f : 1.0f) - Sway) * 0.04f;
        double Error = Latest - Delay - RenderTime;
        if (Math.abs(Error) > Resync) {
            RenderTime = Latest - Delay;
            Rate = 1.0;
        } else {
            double Push = Math.abs(Error) > Band ? (Error - Math.copySign(Band, Error)) * Gain : 0.0;
            Rate += (1.0 + Mth.clamp(Push, -0.25, 0.25) - Rate) * 0.1;
            RenderTime = Math.min(RenderTime + Rate, Latest + Reach);
        }
        while (History.size() > Keep) {
            History.remove(History.size() - 1);
        }
        if (Flinch > 0) {
            Flinch--;
        }
        if (Banishing && ++BanishAge > BanishTicks + 10) {
            clear();
        }
    }

    public static List<Vec3> spine(double Time) {
        List<Vec3> Points = new ArrayList<>(320);
        int Hint = find(Time, 0);
        Points.add(pose(Time, Hint).Head());
        if (History.isEmpty()) {
            return Points;
        }
        double Moment = Math.floor(Time / Grid) * Grid;
        if (Moment >= Time) {
            Moment -= Grid;
        }
        double Oldest = History.get(History.size() - 1).Stamp();
        double Walked = 0.0;
        while (Moment > Oldest && Walked < TrailLength + 24.0) {
            Hint = find(Moment, Hint);
            Vec3 Point = pose(Moment, Hint).Head();
            Walked += Point.distanceTo(Points.get(Points.size() - 1));
            Points.add(Point);
            Moment -= Grid;
        }
        return Points;
    }

    public static double segmentScale(int Index) {
        if (Index < 3) {
            return 0.86 + 0.07 * Index;
        }
        if (Index < 11) {
            return 1.0;
        }
        return 1.0 - 0.64 * (Index - 11) / (double) (Segments - 11);
    }

    public static YaldabaothPart.Kind segmentKind(int Index) {
        if (Index >= Segments) {
            return YaldabaothPart.Kind.Tail;
        }
        return switch (Index % 6) {
            case 1, 5 -> YaldabaothPart.Kind.RingEye;
            case 3 -> YaldabaothPart.Kind.RingMaw;
            case 4 -> YaldabaothPart.Kind.RingRib;
            default -> YaldabaothPart.Kind.Ring;
        };
    }

    public static float segmentRoll(int Index) {
        int Value = Index * 73856093 ^ 0x2F6B;
        Value ^= Value >>> 13;
        Value *= 0x5bd1e995;
        Value ^= Value >>> 15;
        return ((Value & 0xFFFF) / 65535.0f - 0.5f) * 0.36f;
    }

    private static Vec3[] sample(List<Vec3> Points, double Distance, Vec3 Facing) {
        double Walked = 0.0;
        for (int Index = 1; Index < Points.size(); Index++) {
            Vec3 A = Points.get(Index - 1);
            Vec3 B = Points.get(Index);
            double Step = A.distanceTo(B);
            if (Walked + Step >= Distance && Step > 1.0E-6) {
                double Blend = (Distance - Walked) / Step;
                return new Vec3[]{A.lerp(B, Blend), A.subtract(B)};
            }
            Walked += Step;
        }
        Vec3 Last = Points.get(Points.size() - 1);
        Vec3 Before = Points.size() > 1 ? Points.get(Points.size() - 2) : Last.add(Facing);
        Vec3 Tangent = Before.subtract(Last);
        double Over = Distance - Walked;
        Vec3 Direction = Tangent.lengthSqr() > 1.0E-6 ? Tangent.normalize() : Facing;
        return new Vec3[]{Last.subtract(Direction.scale(Over)), Tangent};
    }

    public static List<Placement> placements(float Partial) {
        List<Placement> Made = new ArrayList<>(Segments + 2);
        if (!Present || History.isEmpty()) {
            return Made;
        }
        double At = at(Partial);
        Pose Now = pose(At);
        List<Vec3> Points = spine(At);
        Vec3 Front = Now.Head();
        if (Points.size() < 2) {
            Points.add(Front.subtract(Now.Facing()));
        }
        float Time = Clock + Partial;
        float Sway = Mth.lerp(Partial, SwayO, YaldabaothClient.Sway);
        Vec3 Look = Now.Facing();
        float HeadYaw = (float) Math.atan2(Look.x, Look.z);
        float HeadPitch = (float) -Math.asin(Mth.clamp(Look.y, -1.0, 1.0));
        int HeadSlot = Made.size();
        Made.add(bound(new Placement(YaldabaothPart.Kind.Head, Front, HeadYaw, HeadPitch, 0.0f, (float) (ModelScale * HeadScale), 1.0f, -1)));
        double Cursor = HeadBack * Block;
        Vec3 Newer = null;
        float Bank = 0.0f;
        for (int Index = 0; Index <= Segments; Index++) {
            double Scale = segmentScale(Math.min(Index, Segments - 1));
            double Spacing = RingLength * Block * Scale * Overlap;
            Cursor += Spacing * 0.5;
            Vec3[] Found = sample(Points, Cursor, Look);
            Vec3 Tangent = Found[1].lengthSqr() > 1.0E-8 ? Found[1].normalize() : Look;
            Vec3 Side = Tangent.cross(new Vec3(0.0, 1.0, 0.0));
            Side = Side.lengthSqr() > 1.0E-6 ? Side.normalize() : new Vec3(1.0, 0.0, 0.0);
            double Amplitude = (0.15 + 1.1 * Index / (double) Segments) * Sway;
            Vec3 Wave = Side.scale(Math.sin(Time * 0.07 - Index * 0.42) * Amplitude).add(0.0, Math.sin(Time * 0.05 - Index * 0.3) * Amplitude * 0.4, 0.0);
            Vec3 Position = Found[0].add(Wave);
            float Yaw = (float) Math.atan2(Tangent.x, Tangent.z);
            float Pitch = (float) -Math.asin(Mth.clamp(Tangent.y, -1.0, 1.0));
            if (Newer != null) {
                double Turn = (Tangent.z * Newer.x - Tangent.x * Newer.z) / Math.max(0.5, Spacing);
                float Wanted = Mth.clamp((float) (-Turn * BankGain), -BankLimit, BankLimit);
                Bank = Index == 1 ? Wanted : Bank + (Wanted - Bank) * 0.5f;
                if (Index == 1) {
                    Placement Lead = Made.get(HeadSlot);
                    Made.set(HeadSlot, new Placement(Lead.Kind(), Lead.Position(), Lead.Yaw(), Lead.Pitch(), Bank * 0.6f, Lead.Scale(), Lead.Stretch(), Lead.Index()));
                }
            }
            Newer = Tangent;
            Made.add(bound(new Placement(segmentKind(Index), Position, Yaw, Pitch, segmentRoll(Index) + Bank, (float) (ModelScale * Scale), 1.0f, Index)));
            Cursor += Spacing * 0.5;
        }
        return Made;
    }

    private static Placement bound(Placement Part) {
        boolean Bound = Mode == YaldabaothFlight.Mode.Bind.ordinal() || Banishing;
        if (!Bound) {
            return Part;
        }
        double Depth = Part.Position().subtract(Portal).dot(BindDirection);
        float Scale = Part.Scale();
        float Stretch = 1.0f;
        if (Depth > -1.0) {
            return new Placement(Part.Kind(), Part.Position(), Part.Yaw(), Part.Pitch(), Part.Roll(), 0.0f, 1.0f, Part.Index());
        }
        if (Depth > -16.0) {
            float Near = (float) ((16.0 + Depth) / 15.0);
            Stretch = 1.0f + Near * 1.4f;
            Scale = Scale * (1.0f - 0.45f * Near);
        }
        Vec3 Position = Part.Position();
        float Yaw = Part.Yaw();
        float Pitch = Part.Pitch();
        if (BindProgress > 0.0f || Banishing) {
            Vec3 Toward = Portal.subtract(Position);
            if (Toward.lengthSqr() > 1.0E-4) {
                Vec3 Pull = Toward.normalize();
                float Weight = Mth.clamp((float) (1.0 - (-Depth) / 60.0), 0.0f, 1.0f) * 0.6f;
                float Turned = (float) Math.atan2(Pull.x, Pull.z);
                Yaw = Yaw + Mth.wrapDegrees((Turned - Yaw) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD * Weight;
                Pitch = Mth.lerp(Weight, Pitch, (float) -Math.asin(Mth.clamp(Pull.y, -1.0, 1.0)));
            }
        }
        if (Banishing) {
            float Left = 1.0f - Mth.clamp(BanishAge / (float) BanishTicks, 0.0f, 1.0f);
            Scale *= Left;
            Position = Position.lerp(Portal, 1.0f - Left);
        }
        return new Placement(Part.Kind(), Position, Yaw, Pitch, Part.Roll(), Scale, Stretch, Part.Index());
    }

    public static double nearest(Vec3 Point, float Partial) {
        double Best = Double.MAX_VALUE;
        for (Placement Each : placements(Partial)) {
            if (Each.Scale() > 0.0f) {
                Best = Math.min(Best, Each.Position().distanceToSqr(Point));
            }
        }
        return Math.sqrt(Best);
    }
}
