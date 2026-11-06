package net.rai.scp_unleashed.scp5001.xk;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class YaldabaothFlight {
    public enum Mode { Absent, Rise, Roam, Dive, Gather, Arena, Attack, React, Bind, Banish }

    public enum Pattern { Circle, Ellipse, Eight, Spiral }

    public static final double Cruise = 0.62;
    public static final double Transit = 0.9;
    public static final double DiveSpeed = 0.95;
    public static final double DuelSpeed = 1.3;
    public static final double DashSpeed = 2.2;
    public static final double CoilSpeed = 2.0;
    public static final double RoamTurn = 0.016;
    public static final double DiveTurn = 0.032;
    public static final double DuelTurn = 0.075;
    public static final double DashTurn = 0.09;
    public static final double SweepTurn = 0.11;
    public static final double CoilTurn = 0.19;
    public static final double ReactTurn = 0.09;
    public static final double FacingTurn = 0.14;
    public static final double RoamClimb = 0.4;
    public static final double DuelClimb = 0.62;
    public static final double MinRadius = 150.0;
    public static final double MaxRadius = 260.0;
    public static final int AnchorTicks = 3000;
    public static final int TrailStep = 4;
    public static final int TrailKeep = 64;
    public static final double BodyLength = 80.0;
    public static final double DiveClearance = 24.0;
    public static final int DiveTicks = 720;
    public static final double DiveLow = 46.0;
    public static final int BanishTicks = 30;
    public static final double LeadArc = 42.0;
    public static final int BlendMin = 400;
    public static final int BlendMax = 1800;
    public static final double DuelReach = 115.0;
    public static final double DuelFloor = -40.0;
    public static final double DuelCeiling = 125.0;
    public static final double OrbitRadius = 46.0;
    public static final int ImpactAt = 40;
    public static final int VoiceRoar = 1;
    public static final int VoiceGreat = 2;
    public static final int VoiceSnap = 4;
    public static final int VoiceLunge = 8;
    public static final int VoiceCoil = 16;
    public static final int VoiceFlinch = 32;
    public static final int VoiceDrag = 64;
    public static final int VoiceBanish = 128;
    public static final int VoiceDive = 256;
    public static final int VoiceSweep = 512;
    public static final double DashLimit = 3.4;
    private boolean Present;
    private boolean Dismissed;
    private boolean Banished;
    private Mode Current = Mode.Absent;
    private int ModeTicks;
    private Vec3 Head = Vec3.ZERO;
    private Vec3 Heading = new Vec3(1.0, 0.0, 0.0);
    private Vec3 Facing = new Vec3(1.0, 0.0, 0.0);
    private double Speed = Cruise;
    private Vec3 Impulse = Vec3.ZERO;
    private Vec3 Anchor = Vec3.ZERO;
    private double Radius = 200.0;
    private double Altitude = 180.0;
    private Pattern Shape = Pattern.Circle;
    private double Spin;
    private int Turning = 1;
    private double Phase;
    private int AnchorAge;
    private int NextDive = 2400;
    private int NextPattern = 1500;
    private Vec3 GlideFrom = Vec3.ZERO;
    private Vec3 GlideWay = new Vec3(1.0, 0.0, 0.0);
    private int BlendAge;
    private int BlendTicks;
    @Nullable
    private UUID DiveTarget;
    private Vec3 DiveCentre = Vec3.ZERO;
    private double DiveAltitude;
    private double DiveRadius;
    private double DiveAngle;
    private int DiveTurning = 1;
    private Vec3 Arena = Vec3.ZERO;
    @Nullable
    private Vec3 LastCentre;
    private int OpponentId = -1;
    private Vec3 MoveTarget = Vec3.ZERO;
    private Vec3 MoveOffset = Vec3.ZERO;
    private Vec3 Approach = new Vec3(1.0, 0.0, 0.0);
    private Vec3 AttackSide = new Vec3(0.0, 0.0, 1.0);
    private int Move = -1;
    private double SweepReach = 24.0;
    private double Orbit;
    private int OrbitTurning = 1;
    private int DuelClock;
    private boolean Held;
    private Vec3 ReactFrom = Vec3.ZERO;
    private float ReactPower;
    private Vec3 Portal = Vec3.ZERO;
    private Vec3 BindStart = Vec3.ZERO;
    private Vec3 BindControl = Vec3.ZERO;
    private Vec3 BindDirection = new Vec3(1.0, 0.0, 0.0);
    private float BindProgress;
    private float Jaw;
    private float JawTarget;
    private int Flinch;
    private int Roared;
    private int Voice;
    private float VoicePower;
    private double LastTurn;
    private double LastCap = RoamTurn;
    private final ArrayDeque<Vec3> Trail = new ArrayDeque<>();
    private int TrailClock;

    public void load(CompoundTag Tag) {
        Present = Tag.getBoolean("Present");
        Dismissed = Tag.getBoolean("Dismissed");
        Banished = Tag.getBoolean("Banished");
        String Stored = Tag.getString("Mode");
        Current = Present ? Mode.Roam : Mode.Absent;
        for (Mode Each : Mode.values()) {
            if (Each.name().equals(Stored) && (Each == Mode.Rise || Each == Mode.Roam || Each == Mode.Gather || Each == Mode.Absent)) {
                Current = Each;
            }
        }
        Head = new Vec3(Tag.getDouble("X"), Tag.getDouble("Y"), Tag.getDouble("Z"));
        Vec3 Stored0 = new Vec3(Tag.getDouble("Hx"), Tag.getDouble("Hy"), Tag.getDouble("Hz"));
        Heading = Stored0.lengthSqr() > 1.0E-6 ? Stored0.normalize() : new Vec3(1.0, 0.0, 0.0);
        Facing = Heading;
        Anchor = new Vec3(Tag.getDouble("Ax"), 0.0, Tag.getDouble("Az"));
        Phase = Tag.getDouble("Orbit");
        Radius = Tag.contains("Radius") ? Tag.getDouble("Radius") : 200.0;
        Altitude = Tag.contains("Altitude") ? Tag.getDouble("Altitude") : 180.0;
        Arena = new Vec3(Tag.getDouble("ArenaX"), Tag.getDouble("ArenaY"), Tag.getDouble("ArenaZ"));
        Shape = Pattern.values()[Mth.clamp(Tag.getInt("Pattern"), 0, Pattern.values().length - 1)];
        Spin = Tag.getDouble("Spin");
        Turning = Tag.getInt("Turning") < 0 ? -1 : 1;
        seedTrail();
        glideFromHere(BlendMin);
    }

    public CompoundTag save() {
        CompoundTag Tag = new CompoundTag();
        Tag.putBoolean("Present", Present);
        Tag.putBoolean("Dismissed", Dismissed);
        Tag.putBoolean("Banished", Banished);
        Tag.putString("Mode", Current.name());
        Tag.putDouble("X", Head.x);
        Tag.putDouble("Y", Head.y);
        Tag.putDouble("Z", Head.z);
        Tag.putDouble("Hx", Heading.x);
        Tag.putDouble("Hy", Heading.y);
        Tag.putDouble("Hz", Heading.z);
        Tag.putDouble("Ax", Anchor.x);
        Tag.putDouble("Az", Anchor.z);
        Tag.putDouble("Orbit", Phase);
        Tag.putDouble("Radius", Radius);
        Tag.putDouble("Altitude", Altitude);
        Tag.putDouble("ArenaX", Arena.x);
        Tag.putDouble("ArenaY", Arena.y);
        Tag.putDouble("ArenaZ", Arena.z);
        Tag.putInt("Pattern", Shape.ordinal());
        Tag.putDouble("Spin", Spin);
        Tag.putInt("Turning", Turning);
        return Tag;
    }

    public void clear() {
        Present = false;
        Dismissed = false;
        Banished = false;
        Current = Mode.Absent;
        ModeTicks = 0;
        Trail.clear();
        Move = -1;
        OpponentId = -1;
        LastCentre = null;
        Jaw = 0.0f;
        JawTarget = 0.0f;
        Flinch = 0;
        Impulse = Vec3.ZERO;
    }

    public boolean present() {
        return Present;
    }

    public boolean dismissed() {
        return Dismissed;
    }

    public boolean banished() {
        return Banished;
    }

    public void dismiss() {
        Dismissed = true;
        Present = false;
        Current = Mode.Absent;
        Trail.clear();
    }

    public Mode mode() {
        return Current;
    }

    public int modeTicks() {
        return ModeTicks;
    }

    public Vec3 head() {
        return Head;
    }

    public Vec3 heading() {
        return Heading;
    }

    public Vec3 facing() {
        return Facing;
    }

    public double speed() {
        return Speed;
    }

    public double lastTurn() {
        return LastTurn;
    }

    public double lastCap() {
        return LastCap;
    }

    public float jaw() {
        return Jaw;
    }

    public int flinch() {
        return Flinch;
    }

    public Vec3 portal() {
        return Portal;
    }

    public Vec3 bindDirection() {
        return BindDirection;
    }

    public float bindProgress() {
        return BindProgress;
    }

    public boolean roaredNow() {
        boolean Was = Roared > 0;
        Roared = 0;
        return Was;
    }

    public int voice() {
        int Was = Voice;
        Voice = 0;
        return Was;
    }

    public float voicePower() {
        float Was = VoicePower;
        VoicePower = 0.0f;
        return Was;
    }

    private void say(int Kind, float Power) {
        Voice |= Kind;
        VoicePower = Math.max(VoicePower, Power);
    }

    public int opponent() {
        return OpponentId;
    }

    public List<Vec3> trail() {
        return new ArrayList<>(Trail);
    }

    private void mode(Mode Next) {
        Current = Next;
        ModeTicks = 0;
    }

    public void manifest(Vec3 Start, Vec3 Toward, boolean Rising, double Height) {
        Present = true;
        Dismissed = false;
        Banished = false;
        Head = Start;
        Vec3 Flat = new Vec3(Toward.x, 0.0, Toward.z);
        Heading = Flat.lengthSqr() > 1.0E-6 ? Flat.normalize() : new Vec3(1.0, 0.0, 0.0);
        Facing = Heading;
        Anchor = new Vec3(Start.x, 0.0, Start.z);
        Altitude = Height;
        Speed = Cruise;
        AnchorAge = 0;
        Impulse = Vec3.ZERO;
        LastCentre = null;
        mode(Rising ? Mode.Rise : Mode.Roam);
        seedTrail();
        alignCurve();
        glideFromHere(BlendMin);
    }

    public void park(Vec3 At) {
        Head = At;
        Anchor = new Vec3(At.x, 0.0, At.z);
        Altitude = At.y;
        Radius = MinRadius;
        seedTrail();
        alignCurve();
        glideFromHere(BlendMin);
    }

    private void seedTrail() {
        Trail.clear();
        Vec3 Back = Heading.scale(-1.0);
        for (int Index = 0; Index < TrailKeep; Index++) {
            Trail.addLast(Head.add(Back.scale(Index * Cruise * TrailStep)));
        }
    }

    public void gather(Vec3 Point) {
        if (Current == Mode.Arena || Current == Mode.Attack || Current == Mode.React || Current == Mode.Bind || Current == Mode.Banish) {
            return;
        }
        if (Current != Mode.Gather) {
            mode(Mode.Gather);
            Orbit = Math.atan2(Head.z - Point.z, Head.x - Point.x);
            OrbitTurning = turningAround(Point);
            glideFromHere(160);
        }
        Arena = Point;
    }

    public void hold(Vec3 Where) {
        Head = Where;
        engage(-1, Where);
        Held = true;
        ModeTicks = 400;
        seedTrail();
    }

    public void engage(int Opponent, Vec3 Where) {
        OpponentId = Opponent;
        Arena = Where;
        Held = false;
        enterOrbit(Where, 120);
    }

    public void attack(int Which, Vec3 Target) {
        Move = Which;
        MoveTarget = Target;
        if (Current == Mode.Absent || Current == Mode.Banish || Current == Mode.Bind) {
            return;
        }
        Vec3 Centre = LastCentre != null ? LastCentre : Arena;
        MoveOffset = Target.subtract(Centre);
        Vec3 Toward = Target.subtract(Head);
        Approach = Toward.lengthSqr() > 1.0E-4 ? Toward.normalize() : Heading;
        Vec3 Side = Approach.cross(new Vec3(0.0, 1.0, 0.0));
        AttackSide = Side.lengthSqr() > 1.0E-4 ? Side.normalize().scale(Heading.dot(Side) >= 0.0 ? 1.0 : -1.0) : new Vec3(0.0, 0.0, 1.0);
        mode(Mode.Attack);
        if (Which == 2 || Which == 4) {
            Orbit = Math.atan2(Head.z - Centre.z, Head.x - Centre.x);
            OrbitTurning = turningAround(Centre);
            SweepReach = Math.max(24.0, new Vec3(Head.x - Centre.x, 0.0, Head.z - Centre.z).length());
            glideFromHere(40);
        }
    }

    public void react(Vec3 From, float Power) {
        if (Current == Mode.Absent || Current == Mode.Banish || Current == Mode.Bind) {
            return;
        }
        ReactFrom = From;
        ReactPower = Mth.clamp(Power, 0.2f, 2.5f);
        Vec3 Away = Head.subtract(From);
        Away = Away.lengthSqr() > 1.0E-4 ? Away.normalize() : Heading.scale(-1.0);
        Impulse = Impulse.add(Away.scale(ReactPower * 0.8));
        Flinch = 10;
        JawTarget = 1.0f;
        Roared = 1;
        say(ReactPower >= 0.8f ? VoiceFlinch | VoiceRoar | (ReactPower >= 1.4f ? VoiceGreat : 0) : VoiceFlinch, ReactPower);
        mode(Mode.React);
    }

    public void bind(Vec3 Where, float Progress) {
        if (Current != Mode.Bind) {
            say(VoiceDrag, 1.0f);
            BindStart = Head;
            BindControl = Head.add(Heading.scale(28.0));
            Vec3 Toward = Where.subtract(BindControl);
            BindDirection = Toward.lengthSqr() > 1.0E-4 ? Toward.normalize() : Facing;
            Impulse = Vec3.ZERO;
            mode(Mode.Bind);
        }
        Portal = Where;
        BindProgress = Mth.clamp(Progress, 0.0f, 1.0f);
    }

    public void banish() {
        if (Current != Mode.Bind) {
            BindStart = Head;
            BindDirection = Facing;
            Portal = Head;
        }
        mode(Mode.Banish);
        Banished = true;
        say(VoiceBanish, 1.0f);
    }

    public boolean finishedBanish() {
        return Current == Mode.Banish && ModeTicks >= BanishTicks;
    }

    public void vanish() {
        Present = false;
        Current = Mode.Absent;
        Trail.clear();
    }

    public double nearest(Vec3 Point) {
        double Best = Head.distanceToSqr(Point);
        int Index = 0;
        for (Vec3 Each : Trail) {
            if (Index++ % 2 == 0) {
                Best = Math.min(Best, Each.distanceToSqr(Point));
            }
        }
        return Math.sqrt(Best);
    }

    public void retarget(Vec3 Centre, double Ring, double Height) {
        glideFromHere(blendFor(Centre));
        Anchor = new Vec3(Centre.x, 0.0, Centre.z);
        Radius = Ring;
        Altitude = Height;
        AnchorAge = 0;
        alignCurve();
    }

    public void shape(Pattern Kind, double Turn) {
        Shape = Kind;
        Spin = Turn;
        glideFromHere(BlendMin);
        alignCurve();
    }

    public void startDive(Vec3 Point) {
        DiveTarget = null;
        beginDive(Point);
    }

    public void tick(@Nullable ServerLevel World, RandomSource Random) {
        if (!Present) {
            return;
        }
        ModeTicks++;
        if (Flinch > 0) {
            Flinch--;
        }
        JawTarget = 0.12f + 0.08f * (float) Math.sin(ModeTicks * 0.05);
        switch (Current) {
            case Rise -> {
                advancePhase();
                Vec3 Point = blended(curve(Phase + lead()).add(0.0, -Math.max(0.0, Altitude - Head.y - 30.0) * 0.6, 0.0));
                steer(Point, Cruise, RoamTurn, RoamClimb, 0.02);
                if (Head.y >= Altitude - 8.0 || ModeTicks > 1200) {
                    mode(Mode.Roam);
                }
            }
            case Roam -> roam(World, Random);
            case Dive -> dive(World);
            case Gather -> {
                Orbit += OrbitTurning * Transit / 70.0;
                double Lead = Orbit + OrbitTurning * 0.5;
                Vec3 Spot = Arena.add(Math.cos(Lead) * 70.0, 10.0 * Math.sin(ModeTicks * 0.015), Math.sin(Lead) * 70.0);
                steer(blended(Spot), Transit, DuelTurn * 0.5, RoamClimb, 0.03);
            }
            case Arena -> orbit(World);
            case Attack -> strike(World);
            case React -> {
                Vec3 Away = Head.subtract(ReactFrom);
                Vec3 Flee = Away.lengthSqr() > 1.0E-4 ? Away.normalize() : Heading;
                steer(contain(Head.add(Flee.scale(40.0)).add(0.0, 10.0, 0.0)), DuelSpeed, ReactTurn, DuelClimb, 0.08);
                JawTarget = 1.0f;
                look(opponentPoint(World), FacingTurn);
                if (ModeTicks >= 34) {
                    enterOrbit(Arena, 90);
                }
            }
            case Bind -> {
                Vec3 End = Portal.add(BindDirection.scale(BodyLength + 12.0));
                double Share = BindProgress;
                Vec3 Before = Heading;
                Vec3 Next = bezier(BindStart, BindControl, End, Share);
                Vec3 Ahead = bezier(BindStart, BindControl, End, Math.min(1.0, Share + 0.01));
                Vec3 Tangent = Ahead.subtract(Next);
                if (Tangent.lengthSqr() > 1.0E-8) {
                    Heading = rotateToward(Heading, Tangent.normalize(), DashTurn);
                }
                Head = Next;
                Facing = rotateToward(Facing, Heading, FacingTurn);
                LastTurn = angle(Before, Heading);
                LastCap = DashTurn;
                JawTarget = 1.0f;
            }
            case Banish -> {
                Head = Head.add(BindDirection.scale(1.5));
                LastTurn = 0.0;
                JawTarget = 0.0f;
            }
            default -> {
            }
        }
        Jaw += (JawTarget - Jaw) * 0.25f;
        if (++TrailClock >= TrailStep) {
            TrailClock = 0;
            Trail.addFirst(Head);
            while (Trail.size() > TrailKeep) {
                Trail.removeLast();
            }
        }
    }

    private static double smooth(double Value) {
        double Clamped = Mth.clamp(Value, 0.0, 1.0);
        return Clamped * Clamped * (3.0 - 2.0 * Clamped);
    }

    public static double angle(Vec3 First, Vec3 Second) {
        return Math.acos(Mth.clamp(First.dot(Second), -1.0, 1.0));
    }

    private static Vec3 bezier(Vec3 Start, Vec3 Control, Vec3 End, double Share) {
        double Rest = 1.0 - Share;
        return Start.scale(Rest * Rest).add(Control.scale(2.0 * Rest * Share)).add(End.scale(Share * Share));
    }

    private static Vec3 limitClimb(Vec3 Way, double Climb, Vec3 Fallback) {
        if (Math.abs(Way.y) <= Climb) {
            return Way;
        }
        Vec3 Flat = new Vec3(Way.x, 0.0, Way.z);
        if (Flat.lengthSqr() < 1.0E-8) {
            Flat = new Vec3(Fallback.x, 0.0, Fallback.z);
        }
        if (Flat.lengthSqr() < 1.0E-8) {
            Flat = new Vec3(1.0, 0.0, 0.0);
        }
        double Across = Math.sqrt(1.0 - Climb * Climb);
        return Flat.normalize().scale(Across).add(0.0, Math.signum(Way.y) * Climb, 0.0);
    }

    public static Vec3 rotateToward(Vec3 From, Vec3 To, double Limit) {
        double Between = angle(From, To);
        if (Between <= Limit) {
            return To;
        }
        Vec3 Axis = From.cross(To);
        if (Axis.lengthSqr() < 1.0E-10) {
            Axis = From.cross(new Vec3(0.0, 1.0, 0.0));
            if (Axis.lengthSqr() < 1.0E-10) {
                Axis = new Vec3(1.0, 0.0, 0.0);
            }
        }
        Axis = Axis.normalize();
        double Cos = Math.cos(Limit);
        double Sin = Math.sin(Limit);
        Vec3 Turned = From.scale(Cos).add(Axis.cross(From).scale(Sin)).add(Axis.scale(Axis.dot(From) * (1.0 - Cos)));
        return Turned.normalize();
    }

    private void steer(Vec3 Point, double TargetSpeed, double Turn, double Climb, double Ease) {
        Vec3 Before = Heading;
        Vec3 Toward = Point.subtract(Head);
        if (Toward.lengthSqr() > 1.0E-6) {
            Vec3 Desired = limitClimb(Toward.normalize(), Climb, Heading);
            Vec3 Turned = limitClimb(rotateToward(Heading, Desired, Turn), Climb, Before);
            Heading = angle(Before, Turned) > Turn ? rotateToward(Before, Turned, Turn) : Turned;
        }
        Speed += (TargetSpeed - Speed) * Ease;
        Head = Head.add(Heading.scale(Speed)).add(Impulse);
        Impulse = Impulse.scale(0.86);
        if (Impulse.lengthSqr() < 1.0E-4) {
            Impulse = Vec3.ZERO;
        }
        LastTurn = angle(Before, Heading);
        LastCap = Turn;
        if (Current == Mode.Rise || Current == Mode.Roam || Current == Mode.Dive || Current == Mode.Gather) {
            Facing = rotateToward(Facing, Heading, FacingTurn);
        }
    }

    private void look(@Nullable Vec3 Point, double Turn) {
        Vec3 Wanted = Heading;
        if (Point != null) {
            Vec3 Toward = Point.subtract(Head);
            if (Toward.lengthSqr() > 1.0E-4 && Toward.lengthSqr() < 90.0 * 90.0) {
                Wanted = Toward.normalize().add(Heading.scale(0.35)).normalize();
            }
        }
        Facing = rotateToward(Facing, Wanted, Turn);
    }

    @Nullable
    private Vec3 opponentPoint(@Nullable ServerLevel World) {
        if (OpponentId < 0 || World == null) {
            return null;
        }
        var Found = World.getEntity(OpponentId);
        return Found == null ? null : Found.position().add(0.0, Found.getBbHeight() * 0.68, 0.0);
    }

    private Vec3 centreOf(@Nullable ServerLevel World) {
        Vec3 Found = opponentPoint(World);
        if (Found != null) {
            return Found;
        }
        return LastCentre != null && OpponentId >= 0 ? LastCentre : Arena;
    }

    private Vec3 contain(Vec3 Point) {
        Vec3 Flat = new Vec3(Point.x - Arena.x, 0.0, Point.z - Arena.z);
        double Reach = Flat.length();
        Vec3 Kept = Point;
        if (Reach > DuelReach) {
            Flat = Flat.scale(DuelReach / Reach);
            Kept = new Vec3(Arena.x + Flat.x, Point.y, Arena.z + Flat.z);
        }
        return new Vec3(Kept.x, Mth.clamp(Kept.y, Arena.y + DuelFloor, Arena.y + DuelCeiling), Kept.z);
    }

    private int turningAround(Vec3 Centre) {
        Vec3 Radial = new Vec3(Head.x - Centre.x, 0.0, Head.z - Centre.z);
        double Cross = Radial.z * Heading.x - Radial.x * Heading.z;
        return Cross >= 0.0 ? -1 : 1;
    }

    private double lead() {
        return Turning * LeadArc / Math.max(40.0, Radius);
    }

    private Vec3 curve(double At) {
        return curve(Shape, Anchor, Radius, Altitude, Spin, At);
    }

    public static Vec3 curve(Pattern Kind, Vec3 Centre, double Ring, double Height, double Turn, double At) {
        double X;
        double Z;
        double Y = Height + 14.0 * Math.sin(At * 0.7);
        switch (Kind) {
            case Ellipse -> {
                X = Ring * Math.cos(At);
                Z = Ring * 0.62 * Math.sin(At);
            }
            case Eight -> {
                X = Ring * Math.cos(At);
                Z = Ring * 0.55 * Math.sin(2.0 * At);
            }
            case Spiral -> {
                double Swell = Ring * (0.72 + 0.28 * Math.sin(At * 0.23));
                X = Swell * Math.cos(At);
                Z = Swell * Math.sin(At);
                Y += 10.0 * Math.sin(At * 0.41);
            }
            default -> {
                X = Ring * Math.cos(At);
                Z = Ring * Math.sin(At);
            }
        }
        double Cos = Math.cos(Turn);
        double Sin = Math.sin(Turn);
        return new Vec3(Centre.x + X * Cos - Z * Sin, Y, Centre.z + X * Sin + Z * Cos);
    }

    private void advancePhase() {
        double Window = ModeTicks % 20 == 0 ? Math.PI * 0.5 : 0.35;
        double Chosen = Phase;
        double Best = Head.distanceToSqr(curve(Phase));
        for (int Step = 1; Step <= 24; Step++) {
            double At = Phase + Turning * Window * Step / 24.0;
            double Distance = Head.distanceToSqr(curve(At));
            if (Distance < Best) {
                Best = Distance;
                Chosen = At;
            }
        }
        Phase = Chosen + Turning * Math.min(Speed, 1.0) / Math.max(40.0, Radius) * 0.25;
    }

    private void alignCurve() {
        double Best = Double.MAX_VALUE;
        double Chosen = Phase;
        int ChosenTurn = Turning;
        for (int Direction = -1; Direction <= 1; Direction += 2) {
            for (int Step = 0; Step < 36; Step++) {
                double At = Step * Math.PI * 2.0 / 36.0;
                Vec3 Point = curve(Shape, Anchor, Radius, Altitude, Spin, At + Direction * LeadArc / Math.max(40.0, Radius));
                Vec3 Toward = Point.subtract(Head);
                double Distance = Toward.length();
                if (Distance < 1.0E-3) {
                    continue;
                }
                Vec3 Tangent = curve(Shape, Anchor, Radius, Altitude, Spin, At + Direction * 0.05).subtract(curve(Shape, Anchor, Radius, Altitude, Spin, At));
                double Align = Tangent.lengthSqr() > 1.0E-8 ? Tangent.normalize().dot(Heading) : 0.0;
                double Score = Distance * (1.6 - Toward.normalize().dot(Heading)) * (1.3 - 0.3 * Align);
                if (Score < Best) {
                    Best = Score;
                    Chosen = At;
                    ChosenTurn = Direction;
                }
            }
        }
        Phase = Chosen;
        Turning = ChosenTurn;
    }

    private int blendFor(Vec3 Centre) {
        double Distance = new Vec3(Centre.x - Anchor.x, 0.0, Centre.z - Anchor.z).length();
        return Mth.clamp((int) (Distance / 0.7), BlendMin, BlendMax);
    }

    private void glideFromHere(int Ticks) {
        GlideFrom = Head.add(Heading.scale(LeadArc));
        GlideWay = Heading;
        BlendAge = 0;
        BlendTicks = Math.max(1, Ticks);
    }

    private Vec3 blended(Vec3 Wanted) {
        if (BlendAge >= BlendTicks) {
            return Wanted;
        }
        BlendAge++;
        Vec3 Glide = GlideFrom.add(GlideWay.scale(BlendAge * Speed));
        return Glide.lerp(Wanted, smooth(BlendAge / (double) BlendTicks));
    }

    private void roam(@Nullable ServerLevel World, RandomSource Random) {
        AnchorAge++;
        List<ServerPlayer> Players = World == null ? List.of() : World.players().stream().filter(Player -> !Player.isSpectator()).toList();
        if (!Players.isEmpty()) {
            boolean Near = false;
            for (ServerPlayer Player : Players) {
                if (Player.position().subtract(Anchor).horizontalDistance() < 500.0) {
                    Near = true;
                    break;
                }
            }
            if (!Near || AnchorAge > AnchorTicks) {
                ServerPlayer Chosen = Players.get(Random.nextInt(Players.size()));
                int Ground = World.getHeight(Heightmap.Types.MOTION_BLOCKING, Chosen.getBlockX(), Chosen.getBlockZ());
                double Height = Mth.clamp(Math.max(Ground + 90.0, 150.0) + Random.nextDouble() * 40.0, World.getMinBuildHeight() + 40.0, World.getMaxBuildHeight() - 20.0);
                Shape = Pattern.values()[Random.nextInt(Pattern.values().length)];
                Spin = Random.nextDouble() * Math.PI * 2.0;
                retarget(new Vec3(Chosen.getX(), 0.0, Chosen.getZ()), MinRadius + Random.nextDouble() * (MaxRadius - MinRadius), Height);
            }
            if (--NextDive <= 0) {
                NextDive = 2400 + Random.nextInt(2400);
                ServerPlayer Target = null;
                double Best = 320.0 * 320.0;
                for (ServerPlayer Player : Players) {
                    double Distance = Player.position().subtract(Head).horizontalDistanceSqr();
                    if (Distance < Best && Random.nextFloat() < 0.7f) {
                        Best = Distance;
                        Target = Player;
                    }
                }
                if (Target != null) {
                    DiveTarget = Target.getUUID();
                    beginDive(Target.position());
                    return;
                }
            }
        }
        if (--NextPattern <= 0 && BlendAge >= BlendTicks) {
            NextPattern = 1200 + Random.nextInt(1200);
            Shape = Pattern.values()[Random.nextInt(Pattern.values().length)];
            glideFromHere(BlendMin);
            alignCurve();
        }
        advancePhase();
        double Wanted = BlendAge < BlendTicks ? Transit : Cruise;
        steer(blended(curve(Phase + lead())), Wanted, RoamTurn, RoamClimb, 0.02);
    }

    private void beginDive(Vec3 Point) {
        DiveCentre = Point;
        DiveAltitude = Head.y;
        Vec3 Radial = new Vec3(Head.x - Point.x, 0.0, Head.z - Point.z);
        DiveRadius = Mth.clamp(Radial.length(), 70.0, 220.0);
        DiveAngle = Math.atan2(Radial.z, Radial.x);
        DiveTurning = turningAround(Point);
        Roared = 1;
        say(VoiceDive, 1.0f);
        mode(Mode.Dive);
        glideFromHere(80);
    }

    private void dive(@Nullable ServerLevel World) {
        if (DiveTarget != null && World != null) {
            ServerPlayer Target = (ServerPlayer) World.getPlayerByUUID(DiveTarget);
            if (Target != null) {
                DiveCentre = DiveCentre.lerp(Target.position(), 0.02);
            }
        }
        double Share = ModeTicks / (double) DiveTicks;
        double Ring = Share < 0.5 ? Mth.lerp(smooth(Share / 0.5), DiveRadius, DiveLow)
                : Mth.lerp(smooth((Share - 0.5) / 0.5), DiveLow, Math.max(DiveRadius, MinRadius * 0.7));
        double Low = DiveCentre.y + DiveClearance;
        double Height = DiveAltitude - (DiveAltitude - Low) * Math.pow(Math.sin(Math.PI * Mth.clamp(Share, 0.0, 1.0)), 1.3);
        DiveAngle += DiveTurning * DiveSpeed / Math.max(DiveLow, Ring);
        double Lead = DiveAngle + DiveTurning * 32.0 / Math.max(DiveLow, Ring);
        Vec3 Spot = new Vec3(DiveCentre.x + Math.cos(Lead) * Ring, Height, DiveCentre.z + Math.sin(Lead) * Ring);
        steer(blended(Spot), DiveSpeed, DiveTurn, RoamClimb, 0.02);
        JawTarget = Share > 0.35 && Share < 0.65 ? 0.6f : 0.2f;
        if (ModeTicks == DiveTicks / 2) {
            Roared = 1;
            say(VoiceRoar, 1.0f);
        }
        if (ModeTicks >= DiveTicks) {
            DiveTarget = null;
            mode(Mode.Roam);
            glideFromHere(BlendMin);
            alignCurve();
        }
    }

    private void enterOrbit(Vec3 Where, int Blend) {
        Vec3 Centre = LastCentre != null && OpponentId >= 0 ? LastCentre : Where;
        Orbit = Math.atan2(Head.z - Centre.z, Head.x - Centre.x);
        OrbitTurning = turningAround(Centre);
        mode(Mode.Arena);
        glideFromHere(Blend);
    }

    private void orbit(@Nullable ServerLevel World) {
        DuelClock++;
        Vec3 Centre = centreOf(World);
        LastCentre = Centre;
        if (DuelClock % 700 == 0) {
            OrbitTurning = -OrbitTurning;
            glideFromHere(140);
        }
        double Retreat = !Held && DuelClock % 520 < 140 ? Math.sin(Math.PI * (DuelClock % 520) / 140.0) : 0.0;
        double Ring = Held ? 12.0 : OrbitRadius + 12.0 * Math.sin(DuelClock * 0.011) + 30.0 * Retreat;
        double Lift = Held ? 2.0 * Math.sin(DuelClock * 0.03) : 16.0 * Math.sin(DuelClock * 0.017) + 9.0 * Math.sin(DuelClock * 0.041 + 1.0);
        double Pace = Held ? 0.5 : DuelSpeed;
        Orbit += OrbitTurning * Pace / Ring;
        double Lead = Orbit + OrbitTurning * 0.42;
        Vec3 Spot = contain(Centre.add(Math.cos(Lead) * Ring, Lift, Math.sin(Lead) * Ring));
        steer(blended(Spot), Pace, DuelTurn, DuelClimb, 0.05);
        look(Centre, FacingTurn);
    }

    private void strike(@Nullable ServerLevel World) {
        Vec3 Centre = centreOf(World);
        LastCentre = Centre;
        Vec3 Point = Centre.add(MoveOffset);
        int Time = ModeTicks;
        int Length;
        switch (Move) {
            case 0, 1 -> {
                boolean Bite = Move == 1;
                int Wind = Bite ? 8 : 12;
                Length = ImpactAt + (Bite ? 20 : 28);
                if (Time < Wind) {
                    steer(contain(Point.subtract(Approach.scale(Bite ? 30.0 : 40.0)).add(AttackSide.scale(22.0)).add(0.0, 10.0, 0.0)), 1.3, DuelTurn, DuelClimb, 0.1);
                    JawTarget = 0.6f;
                } else if (Time <= ImpactAt) {
                    if (Time == Wind) {
                        say(VoiceLunge, Bite ? 0.8f : 1.0f);
                    }
                    if (Time == ImpactAt - 3) {
                        say(VoiceSnap, Bite ? 1.0f : 0.8f);
                    }
                    if (Time == Wind) {
                        Vec3 Toward = Point.subtract(Head);
                        Approach = Toward.lengthSqr() > 1.0E-4 ? Toward.normalize() : Heading;
                    }
                    double Remaining = Math.max(1.0, ImpactAt - Time + 1.0);
                    double Need = Mth.clamp(Head.distanceTo(Point) / Remaining, DashSpeed * 0.7, DashLimit);
                    steer(Point.add(Approach.scale(14.0)), Need, DashTurn, DuelClimb, 0.35);
                    JawTarget = Time < ImpactAt - 3 ? 1.0f : 0.0f;
                } else {
                    steer(contain(Point.add(Approach.scale(50.0)).add(AttackSide.scale(28.0)).add(0.0, 10.0, 0.0)), DuelSpeed, DuelTurn, DuelClimb, 0.06);
                    JawTarget = 0.2f;
                }
                look(Point, FacingTurn * 1.4);
            }
            case 2 -> {
                Length = 84;
                Vec3 Below = Point.subtract(0.0, 18.0, 0.0);
                if (Time == 1) {
                    say(VoiceSweep, 1.0f);
                }
                if (Time == 34) {
                    say(VoiceSnap, 0.8f);
                }
                if (Time < 64) {
                    double Ring = Mth.lerp(smooth(Time / 34.0), SweepReach, 24.0);
                    Orbit += OrbitTurning * DashSpeed * 0.9 / Ring;
                    double Lead = Orbit + OrbitTurning * 0.5;
                    steer(blended(Below.add(Math.cos(Lead) * Ring, 0.0, Math.sin(Lead) * Ring)), DashSpeed * 0.9 + (Ring - 24.0) * 0.02, SweepTurn, DuelClimb, 0.1);
                    JawTarget = 0.9f;
                } else {
                    steer(contain(Head.add(Heading.scale(40.0)).add(0.0, 14.0, 0.0)), DuelSpeed, DuelTurn, DuelClimb, 0.06);
                    JawTarget = 0.2f;
                }
                look(Point, FacingTurn);
            }
            case 3 -> {
                Length = 64;
                Vec3 Perch = contain(Point.add(Approach.scale(-34.0)).add(0.0, 26.0, 0.0));
                steer(Perch.add(AttackSide.scale(8.0 * Math.sin(Time * 0.1))), Time < 24 ? 1.0 : 0.45, DuelTurn, DuelClimb, 0.06);
                JawTarget = Time >= 12 && Time < 52 ? 1.0f : 0.2f;
                if (Time == 12) {
                    Roared = 1;
                    say(VoiceRoar | VoiceGreat, 1.0f);
                }
                look(Point.add(0.0, 14.0, 0.0), FacingTurn);
            }
            case 4 -> {
                Length = Integer.MAX_VALUE;
                if (Time % 150 == 1) {
                    say(VoiceCoil, Time == 1 ? 1.0f : 0.7f);
                }
                double Ring = 13.0 + 3.0 * Math.sin(Time * 0.05);
                double Lift = 22.0 * Math.cos(Time * 0.035);
                Orbit += OrbitTurning * CoilSpeed / Ring;
                double Lead = Orbit + OrbitTurning * 0.4;
                Vec3 Spot = Centre.add(Math.cos(Lead) * Ring, Lift - 10.0, Math.sin(Lead) * Ring);
                steer(blended(Spot), CoilSpeed, CoilTurn, 0.85, 0.08);
                JawTarget = 0.7f;
                look(Centre, FacingTurn);
            }
            default -> {
                Length = 40;
                steer(contain(Point), DuelSpeed, DuelTurn, DuelClimb, 0.05);
            }
        }
        if (Time >= Length) {
            enterOrbit(Arena, 60);
        }
    }
}
