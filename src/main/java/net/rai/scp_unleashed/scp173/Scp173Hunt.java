package net.rai.scp_unleashed.scp173;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.health.LethalDamage;
import net.rai.scp_unleashed.scp131.Scp131Pod;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;

public final class Scp173Hunt {
    public static final double DashStep = 1.2;
    public static final double ClimbStep = 0.9;
    public static final double DropStep = 2.5;
    public static final int DashTicks = 3;
    public static final int RestTicks = 3;
    public static final int RetargetTicks = 10;
    public static final int RepathTicks = 6;
    public static final int ClimbReplan = 20;
    public static final int ClimbSpread = 10;
    public static final int ClimbFail = 80;
    public static final int BudgetRetry = 3;
    public static final int Candidates = 3;
    public static final double ReachInflate = 0.36;
    public static final double PodBias = 2.25;
    public static final double GoalSlack = 2.0;
    public static final int IdleMin = 200;
    public static final int IdleSpread = 400;
    public static final int IdleRadius = 7;
    public static final int TurnInterval = 20;
    public static final double TurnRange = 24.0;
    public static final float TurnThreshold = 30.0f;
    public static final int ScrapeGap = 10;
    public static final float ResidueChance = 0.06f;
    public static final int SeepInterval = 1200;
    public static final float SeepChance = 0.25f;
    public static final int CloneCheck = 3600;
    public static final int CloneRetry = 20;
    public static final int ResidueRadius = 12;
    public static final int ResidueHeight = 4;
    public static final int FreeResidue = 10;
    public static final double PerResidue = 0.01;
    public static final double MaxCloneChance = 0.08;
    public static final int UnreachableTicks = 100;
    private final Scp173Statue Statue;
    private final Map<UUID, Long> Spared = new HashMap<>();
    private final Map<UUID, Long> Unreachable = new HashMap<>();
    @Nullable
    private LivingEntity Target;
    @Nullable
    private Scp173Course Course;
    private Vec3 Anchor;
    private int DashLeft;
    private int RestLeft;
    private int RetargetLeft;
    private int RepathLeft;
    private int ClimbLeft;
    private int IdleLeft = IdleMin;
    private int TurnLeft = TurnInterval;
    private int PauseLeft;
    private int ScrapeLeft;
    private int SeepLeft = SeepInterval;
    private int CloneLeft;
    private boolean Moved;
    private boolean Seeded;

    public Scp173Hunt(Scp173Statue Statue) {
        this.Statue = Statue;
        this.Anchor = Statue.position();
        this.CloneLeft = CloneCheck / 2 + Statue.getRandom().nextInt(CloneCheck);
    }

    @Nullable
    public LivingEntity target() {
        return Target;
    }

    @Nullable
    public Scp173Course course() {
        return Course;
    }

    public boolean moved() {
        return Moved;
    }

    public void markMoved() {
        Moved = true;
    }

    public void tick(ServerLevel World, boolean Watched) {
        long Now = World.getGameTime();
        if (Statue.tickCount % 40 == 0) {
            Spared.values().removeIf(Until -> Until <= Now);
            Unreachable.values().removeIf(Until -> Until <= Now);
        }
        ScrapeLeft = Math.max(0, ScrapeLeft - 1);
        RepathLeft = Math.max(0, RepathLeft - 1);
        ClimbLeft = Math.max(0, ClimbLeft - 1);
        if (Watched) {
            if (Moved) {
                snap(World);
                Moved = false;
            }
            DashLeft = 0;
            RestLeft = RestTicks;
            hold(World);
            return;
        }
        if (PauseLeft > 0) {
            PauseLeft--;
            hold(World);
            return;
        }
        if (--RetargetLeft <= 0 || Target != null && !isPrey(World, Target)) {
            RetargetLeft = RetargetTicks;
            acquire(World);
        }
        if (Target != null) {
            chase(World);
        } else {
            idle(World);
        }
        if (DashLeft == 0) {
            seep(World);
        }
        if (--CloneLeft <= 0) {
            cloneCheck(World);
        }
    }

    private boolean supported(ServerLevel World) {
        return Scp173Climb.supported(World, Statue.getBoundingBox());
    }

    private void hold(ServerLevel World) {
        if (Statue.isClinging()) {
            if (supported(World) || Scp173Climb.wallSide(World, Statue.getBoundingBox()) == null) {
                Statue.setClinging(false);
            } else {
                Statue.setDeltaMovement(Vec3.ZERO);
                return;
            }
        }
        Vec3 Motion = Statue.getDeltaMovement();
        Statue.setDeltaMovement(0.0, Math.min(0.0, Motion.y), 0.0);
    }

    private boolean ready(@Nullable Scp173Course Plan) {
        return Plan != null && !Plan.isDone();
    }

    private boolean stale() {
        if (Target == null || !ready(Course)) {
            return true;
        }
        BlockPos Goal = Course.goal();
        BlockPos Where = Target.blockPosition();
        if (Goal.distSqr(Where) > GoalSlack * GoalSlack) {
            return true;
        }
        return !Course.climbing() && RepathLeft <= 0;
    }

    public static boolean reaches(@Nullable Path Route, LivingEntity Body) {
        if (Route == null) {
            return false;
        }
        if (Route.canReach()) {
            return true;
        }
        Node End = Route.getEndNode();
        if (End == null) {
            return false;
        }
        BlockPos Where = Body.blockPosition();
        return Math.abs(End.x - Where.getX()) + Math.abs(End.z - Where.getZ()) <= 1 && Math.abs(End.y - Where.getY()) <= 1;
    }

    @Nullable
    private Scp173Course groundCourse(ServerLevel World, LivingEntity Body) {
        if (Statue.isClinging() || !Scp173Budget.takePath(World)) {
            return null;
        }
        Path Route = Statue.getNavigation().createPath(Body, 0);
        RepathLeft = RepathTicks;
        return reaches(Route, Body) ? Scp173Course.walk(World, Route) : null;
    }

    @Nullable
    private Scp173Course climbCourse(ServerLevel World, LivingEntity Body) {
        if (ClimbLeft > 0) {
            return null;
        }
        Scp173Course Found = Scp173Climb.find(World, Statue, Body);
        ClimbLeft = (Found == null ? ClimbFail : ClimbReplan) + Statue.getRandom().nextInt(ClimbSpread);
        return Found;
    }

    private void plan(ServerLevel World) {
        if (Target == null) {
            return;
        }
        Scp173Course Found = groundCourse(World, Target);
        if (Found == null) {
            Found = climbCourse(World, Target);
        }
        if (Found != null) {
            Course = Found;
        } else if (!ready(Course)) {
            Course = null;
        }
    }

    private void chase(ServerLevel World) {
        if (Target == null) {
            return;
        }
        if (reached(Target)) {
            strike(World, Target);
            return;
        }
        if (DashLeft > 0) {
            dashStep(World);
            return;
        }
        if (RestLeft > 0) {
            RestLeft--;
            hold(World);
            return;
        }
        if (stale()) {
            plan(World);
        }
        if (!ready(Course)) {
            if (!Statue.isClinging()) {
                Statue.faceTowards(Target.position());
            }
            RestLeft = RestTicks * 4;
            hold(World);
            return;
        }
        startDash(World);
    }

    private void idle(ServerLevel World) {
        if (DashLeft > 0) {
            dashStep(World);
            return;
        }
        if (RestLeft > 0) {
            RestLeft--;
            hold(World);
            return;
        }
        if (ready(Course)) {
            startDash(World);
            return;
        }
        if (Statue.isClinging()) {
            Statue.setClinging(false);
            Moved = true;
            return;
        }
        if (!supported(World)) {
            return;
        }
        if (Statue.getPoseIndex() >= Scp173Statue.StandPoses) {
            Statue.pickNewPose(false);
            Moved = true;
        }
        if (--TurnLeft <= 0) {
            TurnLeft = TurnInterval;
            faceNearest(World);
        }
        if (--IdleLeft > 0) {
            return;
        }
        IdleLeft = IdleMin + Statue.getRandom().nextInt(IdleSpread);
        Vec3 Spot = LandRandomPos.getPos(Statue, IdleRadius, 3);
        if (Spot != null && Scp173Budget.takePath(World)) {
            Path Route = Statue.getNavigation().createPath(BlockPos.containing(Spot), 0);
            if (Route != null && !Route.isDone()) {
                Course = Scp173Course.walk(World, Route);
                startDash(World);
            }
        }
    }

    @Nullable
    public static LivingEntity nearestPerson(ServerLevel World, Vec3 From, double Range) {
        LivingEntity Best = World.getNearestPlayer(From.x, From.y, From.z, Range, Found -> !Found.isSpectator() && Targeting.isTargetable(Found));
        double BestDistance = Best == null ? Range * Range : Best.distanceToSqr(From);
        for (LivingEntity Body : World.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(From, Range * 2.0, Range * 2.0, Range * 2.0),
                Found -> !(Found instanceof Player) && Found.isAlive() && Targeting.isTargetable(Found) && ControlBodies.isPersonLike(Found) && !ControlSessions.isBoundToSession(Found)
                        && !(Found instanceof DClassLike Person && (Person.isSpecimen() || Person.isPuppet())))) {
            double Distance = Body.distanceToSqr(From);
            if (Distance < BestDistance) {
                BestDistance = Distance;
                Best = Body;
            }
        }
        return Best;
    }

    private void faceNearest(ServerLevel World) {
        LivingEntity Nearest = nearestPerson(World, Statue.position(), TurnRange);
        if (Nearest == null) {
            return;
        }
        float Before = Statue.getYRot();
        Statue.faceTowards(Nearest.position());
        if (Math.abs(Mth.wrapDegrees(Statue.getYRot() - Before)) < TurnThreshold) {
            Statue.setYRot(Before);
            return;
        }
        Statue.pickNewPose(false);
        Moved = true;
    }

    private void startDash(ServerLevel World) {
        DashLeft = DashTicks;
        Anchor = Statue.position();
        Moved = true;
        if (ScrapeLeft <= 0) {
            ScrapeLeft = ScrapeGap;
            Statue.play(Scp173Registry.ScrapeLayers, Statue.position().add(0.0, 0.6, 0.0), Scp173Statue.SoundVolume, 0.85f + Statue.getRandom().nextFloat() * 0.3f);
        }
        dashStep(World);
    }

    private void faceWall(Direction Wall) {
        Statue.setYRot(Wall.toYRot());
    }

    private static double speedOf(byte Kind) {
        return Kind == Scp173Course.Climb ? ClimbStep : Kind == Scp173Course.Drop ? DropStep : DashStep;
    }

    private void blocked() {
        Course = null;
        RepathLeft = 0;
        DashLeft = 0;
    }

    private void dashStep(ServerLevel World) {
        DashLeft--;
        double Time = 1.0;
        Scp173Passage Way = Scp173Passage.remembering(World);
        while (Time > 1.0e-4 && ready(Course)) {
            Scp173Course.Step Next = Course.next();
            double Speed = speedOf(Next.Kind());
            Vec3 Here = Statue.position();
            Vec3 Delta = Next.Where().subtract(Here);
            double Length = Delta.length();
            double Can = Time * Speed;
            List<Vec3> Route = Way.route(Here, Next.Where());
            if (Route == null) {
                blocked();
                break;
            }
            openDoors(World, Next.Cell());
            if (Next.Kind() == Scp173Course.Climb && Next.Wall() != null) {
                faceWall(Next.Wall());
            } else if (Delta.horizontalDistanceSqr() > 1.0e-4) {
                Statue.faceTowards(Next.Where());
            }
            Statue.setClinging(Next.Kind() == Scp173Course.Climb);
            if (Length <= Can) {
                Statue.setPos(Next.Where().x, Next.Where().y, Next.Where().z);
                Anchor = Next.Where();
                Course.advance();
                Time -= Length / Speed;
            } else {
                Vec3 Step = Scp173Passage.along(Here, Route, Can / Length);
                Statue.setPos(Step.x, Step.y, Step.z);
                Time = 0.0;
            }
            Statue.setDeltaMovement(Vec3.ZERO);
            if (Target != null && reached(Target)) {
                strike(World, Target);
                return;
            }
        }
        if (DashLeft <= 0 || !ready(Course)) {
            DashLeft = 0;
            endDash(World);
        }
    }

    private void endDash(ServerLevel World) {
        Scp173Passage Way = Scp173Passage.of(World);
        Vec3 Here = Statue.position();
        boolean Stuck = !Way.open(Scp173Passage.box(Here)) || !World.getEntityCollisions(Statue, Statue.getBoundingBox().deflate(0.02)).isEmpty();
        if (Stuck && Way.passes(Here, Anchor)) {
            Statue.setPos(Anchor.x, Anchor.y, Anchor.z);
        }
        boolean Supported = supported(World);
        Direction Wall = Supported ? null : Scp173Climb.wallSide(World, Statue.getBoundingBox());
        boolean Dropping = ready(Course) && Course.next().Kind() == Scp173Course.Drop;
        boolean Cling = !Supported && Wall != null && !Dropping;
        Statue.setClinging(Cling);
        if (Cling) {
            faceWall(Wall);
        } else if (Target != null && Supported) {
            Statue.faceTowards(Target.position());
        }
        Statue.pickNewPose(Cling);
        RestLeft = RestTicks;
        if (Supported && Statue.getRandom().nextFloat() < ResidueChance) {
            Scp173Residue.place(World, Statue.blockPosition());
        }
    }

    private void openDoors(ServerLevel World, BlockPos Cell) {
        for (BlockPos Spot : List.of(Cell, Cell.above())) {
            BlockState State = World.getBlockState(Spot);
            if (State.getBlock() instanceof DoorBlock Door && Scp173Passage.plainDoor(State) && !Door.isOpen(State)) {
                Door.setOpen(Statue, World, State, Spot, true);
                World.playSound(null, Spot, Scp173Registry.Door.get(), SoundSource.HOSTILE, 1.0f, 0.8f + Statue.getRandom().nextFloat() * 0.2f);
            }
        }
    }

    private boolean reached(LivingEntity Body) {
        if (Body.level() != Statue.level()) {
            return false;
        }
        AABB Reach = Statue.getBoundingBox().inflate(ReachInflate, 0.1, ReachInflate);
        return Reach.intersects(Body.getBoundingBox()) && Scp173Passage.of(Statue.level()).touches(Scp173Passage.box(Statue.position()), Body.getBoundingBox(), Reach);
    }

    private void strike(ServerLevel World, LivingEntity Victim) {
        if (!Statue.isClinging()) {
            Statue.faceTowards(Victim.position());
        }
        Vec3 At = Victim.position().add(0.0, Victim.getBbHeight() * 0.8, 0.0);
        Statue.play(Scp173Registry.CrackLayers, At, 1.4f, 0.92f + Statue.getRandom().nextFloat() * 0.16f);
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SMOOTH_SANDSTONE.defaultBlockState()), At.x, At.y, At.z, 6, 0.15, 0.15, 0.15, 0.02);
        Spared.put(Victim.getUUID(), World.getGameTime() + Scp173Statue.RecentKill);
        DamageSource Source = Statue.damageSources().source(Scp173Registry.Snap, Statue);
        Target = null;
        Course = null;
        DashLeft = 0;
        PauseLeft = Scp173Statue.KillPause;
        Moved = true;
        Victim.hurt(Source, LethalDamage.against(Victim));
    }

    private void snap(ServerLevel World) {
        Statue.play(Scp173Registry.PoseLayers, Statue.position().add(0.0, 1.0, 0.0), Scp173Statue.SoundVolume, 0.9f + Statue.getRandom().nextFloat() * 0.2f);
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SANDSTONE.defaultBlockState()), Statue.getX(), Statue.getY() + 0.1, Statue.getZ(), 5, 0.25, 0.02, 0.25, 0.02);
    }

    private boolean unreachable(LivingEntity Body, long Now) {
        Long Until = Unreachable.get(Body.getUUID());
        return Until != null && Until > Now;
    }

    private void acquire(ServerLevel World) {
        long Now = World.getGameTime();
        List<LivingEntity> Found = World.getEntitiesOfClass(LivingEntity.class, Statue.getBoundingBox().inflate(Scp173Statue.HuntRange, 16.0, Scp173Statue.HuntRange),
                Body -> isPrey(World, Body));
        Found.sort(Comparator.<LivingEntity, Boolean>comparing(Body -> unreachable(Body, Now)).thenComparingDouble(Body -> Body.distanceToSqr(Statue) * (Body instanceof Scp131Pod ? PodBias : 1.0)));
        int Tried = 0;
        boolean Climbed = false;
        for (LivingEntity Body : Found) {
            if (Tried++ >= Candidates) {
                break;
            }
            if (Body == Target && ready(Course)) {
                return;
            }
            if (!Statue.isClinging() && !Scp173Budget.takePath(World)) {
                RetargetLeft = BudgetRetry;
                return;
            }
            if (!Statue.isClinging()) {
                Path Trial = Statue.getNavigation().createPath(Body, 0);
                if (reaches(Trial, Body)) {
                    Unreachable.remove(Body.getUUID());
                    Target = Body;
                    Course = Scp173Course.walk(World, Trial);
                    RepathLeft = RepathTicks;
                    return;
                }
            }
            boolean Attempted = !Statue.isClinging();
            if (!Climbed) {
                Climbed = true;
                Attempted = Attempted || ClimbLeft <= 0;
                Scp173Course Up = climbCourse(World, Body);
                if (Up != null) {
                    Unreachable.remove(Body.getUUID());
                    Target = Body;
                    Course = Up;
                    return;
                }
            }
            if (Attempted) {
                Unreachable.put(Body.getUUID(), Now + UnreachableTicks);
            }
        }
        if (Target != null && Found.contains(Target) && (ready(Course) || !unreachable(Target, Now))) {
            return;
        }
        Course = null;
        Target = null;
    }

    public boolean isPrey(ServerLevel World, LivingEntity Body) {
        if (Body == Statue || Body instanceof Scp173Statue || !Body.isAlive() || Body.isRemoved() || Body.isDeadOrDying() || Body.isSpectator() || Body.level() != World || !Targeting.isTargetable(Body)) {
            return false;
        }
        Long Until = Spared.get(Body.getUUID());
        if (Until != null && Until > World.getGameTime()) {
            return false;
        }
        if (Body instanceof Player User) {
            return !User.isCreative() && World.getDifficulty() != Difficulty.PEACEFUL && !Scp268.isNeglected(User);
        }
        if (ControlSessions.isBoundToSession(Body)) {
            return false;
        }
        if (ControlBodies.isPersonLike(Body)) {
            return !(Body instanceof DClassLike Person) || !Person.isSpecimen() && !Person.isPuppet();
        }
        if (Body instanceof AbstractVillager) {
            return true;
        }
        if (Body instanceof Scp131Pod Pod) {
            return !Scp173Sight.podSees(Pod, Statue);
        }
        return extraPrey(Statue, Body);
    }

    private static final List<BiPredicate<Scp173Statue, LivingEntity>> ExtraPrey = new CopyOnWriteArrayList<>();

    public static void addPrey(BiPredicate<Scp173Statue, LivingEntity> Test) {
        ExtraPrey.add(Test);
    }

    private static boolean extraPrey(Scp173Statue Statue, LivingEntity Body) {
        for (BiPredicate<Scp173Statue, LivingEntity> Test : ExtraPrey) {
            if (Test.test(Statue, Body)) {
                return true;
            }
        }
        return false;
    }

    private void seep(ServerLevel World) {
        if (--SeepLeft > 0) {
            return;
        }
        SeepLeft = SeepInterval;
        if (Statue.getRandom().nextFloat() < SeepChance && supported(World)) {
            Scp173Residue.placeNear(World, Statue.blockPosition(), Statue.position(), Statue.getRandom());
        }
    }

    public static double cloneChance(int Residue) {
        return Math.min(MaxCloneChance, Math.max(0, Residue - FreeResidue) * PerResidue);
    }

    private void cloneCheck(ServerLevel World) {
        if (!Scp173Budget.takeCloneCheck(World)) {
            CloneLeft = 1 + Statue.getRandom().nextInt(CloneRetry);
            return;
        }
        CloneLeft = CloneCheck;
        if (!Seeded && Scp173Budget.takeSeed(World)) {
            Seeded = true;
            Scp173Residue.seed(World, Statue.blockPosition(), ResidueRadius, ResidueHeight);
        }
        int Count = Scp173Residue.count(World, Statue.blockPosition(), ResidueRadius, ResidueHeight);
        if (Statue.getRandom().nextDouble() < cloneChance(Count)) {
            Scp173Clones.spawnFrom(World, Statue, false);
        }
    }

    public void save(CompoundTag Tag) {
        Tag.putBoolean("Moved", Moved);
        Tag.putInt("SeepLeft", SeepLeft);
        Tag.putInt("CloneLeft", CloneLeft);
    }

    public void load(CompoundTag Tag) {
        Moved = Tag.getBoolean("Moved");
        SeepLeft = Tag.contains("SeepLeft") ? Math.max(1, Tag.getInt("SeepLeft")) : SeepInterval;
        CloneLeft = Tag.contains("CloneLeft") ? Math.max(1, Tag.getInt("CloneLeft")) : CloneCheck / 2 + Statue.getRandom().nextInt(CloneCheck);
        Anchor = Statue.position();
    }
}
