package net.rai.scp_unleashed.scp131;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public final class Scp131Play {
    public static final byte None = 0;
    public static final byte Greet = 1;
    public static final byte Tag = 2;
    public static final byte Orbit = 3;
    public static final byte Pair = 4;
    public static final byte Chasing = 5;
    public static final double SeekRange = 10.0;
    public static final double LoseRange = 18.0;
    public static final int CheckInterval = 40;
    public static final int StartChance = 3;
    public static final int CooldownMin = 700;
    public static final int CooldownSpread = 900;
    public static final int JoinTicks = 20;
    public static final int GreetTicks = 120;
    public static final int CallTick = 46;
    public static final int ReplyTick = 60;
    public static final int BumpStart = 74;
    public static final int BumpEnd = 96;
    public static final double GreetGap = 0.6;
    public static final double BumpReach = 0.56;
    public static final double BumpPush = 0.12;
    public static final int TagTicks = 380;
    public static final double TagFleeModifier = 0.92;
    public static final double TagChaseModifier = 1.02;
    public static final double TagReach = 0.75;
    public static final int TagFreeze = 16;
    public static final int TagSwaps = 4;
    public static final double TagArena = 8.0;
    public static final double TagStride = 5.0;
    public static final float TagArc = 50.0f;
    public static final int OrbitTicks = 190;
    public static final int OrbitSettle = 24;
    public static final double OrbitRadius = 1.25;
    public static final double OrbitStep = 0.13;
    public static final double OrbitModifier = 0.62;
    public static final int PairTicks = 220;
    public static final double PairOffset = 0.95;
    public static final double PairLead = 0.2;
    public static final double PairArrive = 1.6;
    public static final double PairStray = 6.0;
    public static final int PairMin = 8;
    public static final int PairMax = 15;

    private final byte Kind;
    private final Scp131Pod First;
    private final Scp131Pod Second;
    private final long Start;
    private final Vec3 Centre;
    private final Vec3 Line;
    private Scp131Pod Chaser;
    private long FreezeUntil;
    private int Swaps;
    private int Steer;
    private double Angle;
    private int Side;
    @Nullable
    private Vec3 Goal;
    private boolean Called;
    private boolean Replied;
    private boolean Bumped;
    private boolean FirstJoined;
    private boolean SecondJoined;
    private boolean Over;

    private Scp131Play(byte Kind, Scp131Pod First, Scp131Pod Second, @Nullable Vec3 Goal) {
        this.Kind = Kind;
        this.First = First;
        this.Second = Second;
        this.Start = First.level().getGameTime();
        this.Centre = First.position().add(Second.position()).scale(0.5);
        Vec3 Across = Second.position().subtract(First.position()).multiply(1.0, 0.0, 1.0);
        this.Line = Across.lengthSqr() < 1.0e-4 ? new Vec3(1.0, 0.0, 0.0) : Across.normalize();
        RandomSource Random = First.getRandom();
        this.Chaser = Random.nextBoolean() ? First : Second;
        this.Steer = Random.nextBoolean() ? 1 : -1;
        this.Angle = Math.atan2(First.getZ() - Centre.z, First.getX() - Centre.x);
        Vec3 Heading = Vec3.directionFromRotation(0.0f, First.getYRot());
        this.Side = Heading.x * Line.z - Heading.z * Line.x > 0.0 ? 1 : -1;
        this.Goal = Goal;
    }

    public static boolean tryStart(Scp131Pod Seeker) {
        List<Scp131Pod> Near = Seeker.level().getEntitiesOfClass(Scp131Pod.class, Seeker.getBoundingBox().inflate(SeekRange, 3.0, SeekRange),
                Other -> Other != Seeker && Other.canPlay() && Seeker.hasLineOfSight(Other));
        Scp131Pod Partner = Near.stream().min(Comparator.comparingDouble(Other -> Other.distanceToSqr(Seeker))).orElse(null);
        if (Partner == null) {
            return false;
        }
        RandomSource Random = Seeker.getRandom();
        int Roll = Random.nextInt(9);
        byte Kind = Roll < 3 ? Greet : Roll < 5 ? Tag : Roll < 7 ? Orbit : Pair;
        Vec3 Goal = null;
        if (Kind == Orbit && !roomToOrbit(Seeker, Seeker.position().add(Partner.position()).scale(0.5))) {
            Kind = Greet;
        }
        if (Kind == Pair) {
            Goal = pairGoal(Seeker);
            if (Goal == null) {
                Kind = Greet;
            }
        }
        Scp131Play Session = new Scp131Play(Kind, Seeker, Partner, Goal);
        Seeker.setSession(Session);
        Partner.setSession(Session);
        Seeker.getNavigation().stop();
        Partner.getNavigation().stop();
        if (Kind == Tag || Kind == Pair) {
            Seeker.chirp(Scp131Registry.ZoomLayers, Scp131Pod.ChirpVolume);
        }
        return true;
    }

    private static boolean roomToOrbit(Scp131Pod Seeker, Vec3 Middle) {
        for (int Step = 0; Step < 8; Step++) {
            double Turn = Step * Math.PI / 4.0;
            BlockPos Spot = BlockPos.containing(Middle.x + Math.cos(Turn) * OrbitRadius, Seeker.getY() + 0.2, Middle.z + Math.sin(Turn) * OrbitRadius);
            if (!Scp131MoveControl.safeColumn(Seeker.level(), Spot) || !Seeker.level().getBlockState(Spot).getCollisionShape(Seeker.level(), Spot).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static Vec3 pairGoal(Scp131Pod Seeker) {
        for (int Attempt = 0; Attempt < 6; Attempt++) {
            Vec3 Found = LandRandomPos.getPos(Seeker, PairMax, 4);
            if (Found == null || Found.distanceToSqr(Seeker.position()) < PairMin * PairMin) {
                continue;
            }
            Path Route = Seeker.getNavigation().createPath(BlockPos.containing(Found), 0);
            if (Route != null && Route.canReach()) {
                return Found;
            }
        }
        return null;
    }

    public byte kindFor(Scp131Pod Pod) {
        if (Kind == Tag) {
            return Pod == Chaser ? Chasing : Tag;
        }
        return Kind;
    }

    public byte kind() {
        return Kind;
    }

    public boolean isOver() {
        return Over;
    }

    public Scp131Pod partnerOf(Scp131Pod Pod) {
        return Pod == First ? Second : First;
    }

    private boolean valid() {
        for (Scp131Pod Pod : List.of(First, Second)) {
            if (!Pod.isAlive() || Pod.isRemoved() || Pod.session() != this || Pod.isSad() || Pod.isStartled() || Pod.isInWater()) {
                return false;
            }
        }
        return First.level() == Second.level() && First.distanceToSqr(Second) < LoseRange * LoseRange;
    }

    public void end(boolean Graceful) {
        if (Over) {
            return;
        }
        Over = true;
        for (Scp131Pod Pod : List.of(First, Second)) {
            if (Pod.session() == this) {
                Pod.setSession(null);
            }
            Pod.setZooming(false);
            Pod.restAfterPlay(Pod.getRandom());
            if (Graceful && Pod.isAlive()) {
                Pod.cheer(Scp131Pod.HappyTicks);
                Pod.getNavigation().stop();
            }
        }
        if (Graceful && First.isAlive()) {
            First.chirp(Scp131Registry.ChirpLayers, Scp131Pod.ChirpVolume);
        }
    }

    public void tick(Scp131Pod Self) {
        if (Over) {
            return;
        }
        if (Self == First) {
            FirstJoined = true;
        } else {
            SecondJoined = true;
        }
        long Age = Self.level().getGameTime() - Start;
        if (!valid() || Age > JoinTicks && (!FirstJoined || !SecondJoined)) {
            end(false);
            return;
        }
        Scp131Pod Other = partnerOf(Self);
        Self.setFocus(Other);
        switch (Kind) {
            case Greet -> greet(Self, Other, Age);
            case Tag -> tag(Self, Other, Age);
            case Orbit -> orbit(Self, Other, Age);
            default -> pair(Self, Other, Age);
        }
    }

    private void face(Scp131Pod Self, Scp131Pod Other) {
        Self.getLookControl().setLookAt(Other.getX(), Other.getY() + Scp131Pod.EyeHeight, Other.getZ(), 30.0f, 30.0f);
    }

    private void greet(Scp131Pod Self, Scp131Pod Other, long Age) {
        if (Age >= GreetTicks) {
            end(true);
            return;
        }
        face(Self, Other);
        if (Age < BumpStart) {
            Vec3 Spot = Centre.add(Line.scale(Self == First ? -GreetGap : GreetGap));
            if (Self.position().distanceToSqr(Spot.x, Self.getY(), Spot.z) > 0.04) {
                Self.getMoveControl().setWantedPosition(Spot.x, Self.getY(), Spot.z, Scp131Pod.CruiseModifier * 0.7);
            }
        } else if (Age < BumpEnd && !Bumped) {
            Self.getMoveControl().setWantedPosition(Other.getX(), Other.getY(), Other.getZ(), 0.32);
            if (Self.distanceTo(Other) < BumpReach + 0.5) {
                Bumped = true;
                First.voice(Scp131Registry.BumpLayers, 0.6f, 1.0f);
                for (Scp131Pod Pod : List.of(First, Second)) {
                    Pod.event(Scp131Pod.EventBump);
                    Vec3 Away = Pod.position().subtract(partnerOf(Pod).position()).multiply(1.0, 0.0, 1.0);
                    if (Away.lengthSqr() > 1.0e-4) {
                        Pod.push(Away.normalize().x * BumpPush, 0.0, Away.normalize().z * BumpPush);
                    }
                }
            }
        }
        if (Self == First && Age >= CallTick && !Called) {
            Called = true;
            First.voice(Scp131Registry.GreetLayers, Scp131Pod.ChirpVolume, 1.0f);
            First.event(Scp131Pod.EventBob);
        }
        if (Self == Second && Age >= ReplyTick && !Replied) {
            Replied = true;
            Second.voice(Scp131Registry.ReplyLayers, Scp131Pod.ChirpVolume, 1.06f);
            Second.event(Scp131Pod.EventBob);
        }
    }

    private void tag(Scp131Pod Self, Scp131Pod Other, long Age) {
        if (Age >= TagTicks) {
            end(true);
            return;
        }
        long Now = Self.level().getGameTime();
        Self.setZooming(true);
        if (Self == Chaser) {
            if (Now < FreezeUntil) {
                Self.getNavigation().stop();
                face(Self, Other);
                return;
            }
            if (Self.tickCount % 4 == 0 || Self.getNavigation().isDone()) {
                Self.getNavigation().moveTo(Other, TagChaseModifier);
            }
            if (Self.distanceTo(Other) < TagReach + 0.25) {
                Chaser = Other;
                FreezeUntil = Now + TagFreeze;
                Swaps++;
                Self.setPlay(kindFor(Self));
                Other.setPlay(kindFor(Other));
                Other.chirp(Scp131Registry.ZoomLayers, Scp131Pod.ChirpVolume);
                Self.event(Scp131Pod.EventBump);
                Other.event(Scp131Pod.EventBump);
                Self.getNavigation().stop();
                if (Swaps >= TagSwaps) {
                    end(true);
                }
            }
            return;
        }
        if (Self.tickCount % 10 == 0 || Self.getNavigation().isDone()) {
            fleeArc(Self, Other);
        }
    }

    private void fleeArc(Scp131Pod Self, Scp131Pod Other) {
        Vec3 Away = Self.position().subtract(Other.position()).multiply(1.0, 0.0, 1.0);
        if (Away.lengthSqr() < 1.0e-4) {
            Away = Vec3.directionFromRotation(0.0f, Self.getYRot());
        }
        Away = Away.normalize();
        for (int Attempt = 0; Attempt < 2; Attempt++) {
            Vec3 Direction = Away.yRot(Steer * TagArc * Mth.DEG_TO_RAD);
            Vec3 Home = Centre.subtract(Self.position()).multiply(1.0, 0.0, 1.0);
            if (Home.length() > TagArena) {
                Direction = Direction.add(Home.normalize().scale(1.4)).normalize();
            }
            Vec3 Target = Self.position().add(Direction.scale(TagStride));
            BlockPos Spot = BlockPos.containing(Target.x, Self.getY() + 0.2, Target.z);
            if (Scp131MoveControl.safeColumn(Self.level(), Spot) && Self.getNavigation().moveTo(Target.x, Self.getY(), Target.z, TagFleeModifier)) {
                return;
            }
            Steer = -Steer;
        }
    }

    private void orbit(Scp131Pod Self, Scp131Pod Other, long Age) {
        if (Age >= OrbitTicks) {
            end(true);
            return;
        }
        face(Self, Other);
        Self.getNavigation().stop();
        double Phase = Angle + Math.max(0, Age - OrbitSettle) * OrbitStep + (Self == First ? 0.0 : Math.PI);
        double X = Centre.x + Math.cos(Phase) * OrbitRadius;
        double Z = Centre.z + Math.sin(Phase) * OrbitRadius;
        Self.getMoveControl().setWantedPosition(X, Self.getY(), Z, Age < OrbitSettle ? Scp131Pod.WanderModifier : OrbitModifier);
        if (Self == First && Age == OrbitTicks / 2) {
            First.chirp(Scp131Registry.ChirpLayers, Scp131Pod.ChirpVolume);
        }
    }

    private void pair(Scp131Pod Self, Scp131Pod Other, long Age) {
        if (Age >= PairTicks || Goal == null) {
            end(true);
            return;
        }
        Self.setZooming(true);
        if (Self == First) {
            if (First.position().distanceToSqr(Goal) < PairArrive * PairArrive || First.distanceTo(Second) > PairStray) {
                end(true);
                return;
            }
            if (First.getNavigation().isDone() || First.tickCount % 20 == 0) {
                First.getNavigation().moveTo(Goal.x, Goal.y, Goal.z, Scp131Pod.ZoomModifier * 0.85);
            }
            return;
        }
        Vec3 Heading = Vec3.directionFromRotation(0.0f, First.getYRot());
        Vec3 Right = new Vec3(-Heading.z, 0.0, Heading.x);
        Vec3 Spot = First.position().add(Right.scale(Side * PairOffset)).add(Heading.scale(PairLead));
        double Lag = Spot.subtract(Self.position()).horizontalDistance();
        double FirstSpeed = First.getDeltaMovement().horizontalDistance();
        double Base = FirstSpeed / Math.max(0.01, Self.getAttributeValue(Attributes.MOVEMENT_SPEED) * 1.2);
        double Modifier = Mth.clamp(Base + Lag * 0.6, 0.3, Scp131Pod.ZoomModifier * 1.1);
        Self.getNavigation().stop();
        Self.getMoveControl().setWantedPosition(Spot.x, First.getY(), Spot.z, Modifier);
    }
}
