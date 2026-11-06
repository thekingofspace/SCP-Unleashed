package net.rai.scp_unleashed.scp354;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.level.pathfinder.PathFinder;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.pathfinder.PathfindingContext;
import net.minecraft.world.level.pathfinder.WalkNodeEvaluator;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Scp354Swim {
    public static final double EmergeDrift = 0.035;
    public static final double Grip = 0.22;
    public static final double Buoyancy = 0.16;
    public static final double VerticalDamp = 0.55;
    public static final double RiseLimit = 0.16;
    public static final double SinkLimit = 0.12;
    public static final double BobHeight = 0.05;
    public static final double BobRate = 0.22;
    public static final float TurnRate = 16.0f;
    public static final double AirDrag = 0.98;
    public static final double HopMargin = 0.3;
    public static final double MinHop = 0.7;
    public static final double HopGrowth = 0.15;
    public static final double ReachGrowth = 0.3;
    public static final double HaulPush = 0.13;
    public static final double ReachSlack = 0.85;
    public static final int HopTicks = 14;
    public static final int SettleTicks = 14;
    public static final int RescanTicks = 20;
    public static final int HaulCooldown = 18;
    public static final int FloodReach = 12;
    public static final int FloodCells = 320;
    public static final double TargetWeight = 0.35;
    public static final double RiseWeight = 0.8;
    public static final double SettleReach = 0.35;
    public static final double LineStep = 0.5;
    public static final float ShoreMalus = 16.0f;
    public static final int ShoreDrop = 5;
    public static final double StuckGrowth = 0.5;
    public static final double StuckReach = 2.5;

    private Scp354Swim() {
    }

    public static final class Course {
        public Vec3 Heading = Vec3.ZERO;
        public double Speed;
        public int Haul;
        @Nullable
        public Vec3 Exit;
    }

    public static boolean swimming(Entity Body) {
        return Body.isInFluidType(Scp354Registry.RedType.value());
    }

    public static boolean isRedAt(BlockGetter World, BlockPos Cell) {
        return Scp354Fluid.isRed(World.getFluidState(Cell));
    }

    public static void halt(Entity Body) {
        if (Body instanceof Scp354Swimmer Swimmer && Swimmer.swimsOut() && swimming(Body)) {
            return;
        }
        Body.setDeltaMovement(Body.getDeltaMovement().multiply(0.0, 1.0, 0.0));
    }

    public static void travel(LivingEntity Body, Scp354Swimmer Swimmer) {
        Course Route = Swimmer.course();
        double Gravity = Body.getGravity();
        Vec3 Motion = Body.getDeltaMovement();
        if (Route.Haul > 0) {
            Body.move(MoverType.SELF, Motion);
            Vec3 After = Body.getDeltaMovement();
            Body.setDeltaMovement(After.x, (After.y - Gravity) * AirDrag, After.z);
            return;
        }
        double Top = surface(Body);
        double Vertical;
        if (Double.isNaN(Top)) {
            Vertical = (Motion.y - Gravity * 0.5) * 0.8;
        } else {
            double Rest = Top - Swimmer.draft() + BobHeight * Math.sin(Body.tickCount * BobRate + Body.getId());
            Vertical = Mth.clamp(Motion.y * VerticalDamp + (Rest - Body.getY()) * Buoyancy, -SinkLimit, RiseLimit);
        }
        double WantX = 0.0;
        double WantZ = 0.0;
        if (Route.Speed > 0.0 && Route.Heading.lengthSqr() > 1.0e-6) {
            if (Body instanceof Scp354Emergent Emergent && Emergent.isEmerging()) {
                WantX = Route.Heading.x * Route.Speed;
                WantZ = Route.Heading.z * Route.Speed;
            } else {
                float Aim = (float) (Mth.atan2(Route.Heading.z, Route.Heading.x) * Mth.RAD_TO_DEG) - 90.0f;
                float Yaw = Mth.wrapDegrees(Mth.approachDegrees(Body.getYRot(), Aim, TurnRate));
                Body.setYRot(Yaw);
                Body.yBodyRot = Yaw;
                double Off = Math.min(90.0, Math.abs(Mth.wrapDegrees(Aim - Yaw)));
                double Pace = Route.Speed * Math.max(0.25, Math.cos(Math.toRadians(Off)));
                Vec3 Facing = Vec3.directionFromRotation(0.0f, Yaw);
                WantX = Facing.x * Pace;
                WantZ = Facing.z * Pace;
            }
        }
        Body.setDeltaMovement(Motion.x + (WantX - Motion.x) * Grip, Vertical, Motion.z + (WantZ - Motion.z) * Grip);
        Body.move(MoverType.SELF, Body.getDeltaMovement());
    }

    public static double surface(Entity Body) {
        Level World = Body.level();
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos(Body.getX(), Body.getY(), Body.getZ());
        int Start = Cursor.getY();
        int Found = Integer.MIN_VALUE;
        for (int Dy = -1; Dy <= Mth.ceil(Body.getBbHeight()) + 1; Dy++) {
            Cursor.setY(Start + Dy);
            if (isRedAt(World, Cursor)) {
                Found = Cursor.getY();
            } else if (Found != Integer.MIN_VALUE) {
                break;
            }
        }
        if (Found == Integer.MIN_VALUE) {
            return Double.NaN;
        }
        Cursor.setY(Found);
        return Found + World.getFluidState(Cursor).getHeight(World, Cursor);
    }

    public static int topCell(BlockGetter World, int X, int Y, int Z) {
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Dy = 3; Dy >= -3; Dy--) {
            Cursor.set(X, Y + Dy, Z);
            if (isRedAt(World, Cursor) && !isRedAt(World, Cursor.above())) {
                return Y + Dy;
            }
        }
        return Integer.MIN_VALUE;
    }

    @Nullable
    public static Vec3 exit(Mob Body, Scp354Swimmer Swimmer, @Nullable Entity Target) {
        return exit(Body, Swimmer, Target, 0.0);
    }

    @Nullable
    public static Vec3 exit(Mob Body, Scp354Swimmer Swimmer, @Nullable Entity Target, double Slack) {
        Level World = Body.level();
        BlockPos Feet = Body.blockPosition();
        int Top = topCell(World, Feet.getX(), Feet.getY(), Feet.getZ());
        if (Top == Integer.MIN_VALUE) {
            return null;
        }
        double FloatY = Top + Scp354Fluid.SurfaceHeight - Swimmer.draft();
        Set<Long> Surface = new HashSet<>();
        ArrayDeque<BlockPos> Frontier = new ArrayDeque<>();
        BlockPos Origin = new BlockPos(Feet.getX(), Top, Feet.getZ());
        Surface.add(Origin.asLong());
        Frontier.add(Origin);
        while (!Frontier.isEmpty() && Surface.size() < FloodCells) {
            BlockPos Cell = Frontier.poll();
            for (Direction Side : Direction.Plane.HORIZONTAL) {
                BlockPos Next = Cell.relative(Side);
                if (Math.abs(Next.getX() - Origin.getX()) > FloodReach || Math.abs(Next.getZ() - Origin.getZ()) > FloodReach || Surface.contains(Next.asLong())) {
                    continue;
                }
                if (isRedAt(World, Next) && !isRedAt(World, Next.above())) {
                    Surface.add(Next.asLong());
                    Frontier.add(Next);
                }
            }
        }
        Set<Long> Tried = new HashSet<>();
        Vec3 Best = null;
        double BestScore = Double.MAX_VALUE;
        for (long Key : Surface) {
            int X = BlockPos.getX(Key);
            int Z = BlockPos.getZ(Key);
            for (int Dx = -1; Dx <= 1; Dx++) {
                for (int Dz = -1; Dz <= 1; Dz++) {
                    long Column = BlockPos.asLong(X + Dx, Top, Z + Dz);
                    if (Surface.contains(Column) || !Tried.add(Column)) {
                        continue;
                    }
                    Vec3 Spot = standAt(World, Body, X + Dx, Z + Dz, Top);
                    if (Spot == null) {
                        continue;
                    }
                    double Rise = Spot.y - FloatY;
                    if (Rise > Swimmer.haulReach() + Slack) {
                        continue;
                    }
                    double Score = flat(Body.position(), Spot) + RiseWeight * Math.max(0.0, Rise) + (Target != null ? TargetWeight * flat(Spot, Target.position()) : 0.0);
                    if (Score < BestScore) {
                        BestScore = Score;
                        Best = Spot;
                    }
                }
            }
        }
        return Best;
    }

    @Nullable
    public static Vec3 standAt(Level World, Entity Body, int X, int Z, int Top) {
        BlockPos.MutableBlockPos Floor = new BlockPos.MutableBlockPos();
        for (int Y = Top + 3; Y >= Top; Y--) {
            Floor.set(X, Y - 1, Z);
            if (isRedAt(World, Floor)) {
                return null;
            }
            VoxelShape Outline = World.getBlockState(Floor).getCollisionShape(World, Floor);
            if (Outline.isEmpty()) {
                continue;
            }
            Vec3 Spot = new Vec3(X + 0.5, Floor.getY() + Outline.max(Direction.Axis.Y), Z + 0.5);
            AABB Box = Body.getDimensions(Body.getPose()).makeBoundingBox(Spot).deflate(1.0e-3);
            if (!World.noCollision(Body, Box) || touchesRed(World, Box)) {
                return null;
            }
            return Spot;
        }
        return null;
    }

    public static boolean touchesRed(Level World, AABB Box) {
        for (BlockPos Cell : BlockPos.betweenClosed(Mth.floor(Box.minX), Mth.floor(Box.minY), Mth.floor(Box.minZ), Mth.floor(Box.maxX), Mth.floor(Box.maxY),
                Mth.floor(Box.maxZ))) {
            if (isRedAt(World, Cell) && Cell.getY() + World.getFluidState(Cell).getHeight(World, Cell) > Box.minY) {
                return true;
            }
        }
        return false;
    }

    public static double flat(Vec3 From, Vec3 To) {
        double Dx = To.x - From.x;
        double Dz = To.z - From.z;
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    public static double launchSpeed(double Apex, double Gravity) {
        double Speed = 0.2;
        while (Speed < 1.5) {
            double Height = 0.0;
            double Rise = Speed;
            while (Rise > 0.0) {
                Height += Rise;
                Rise = (Rise - Gravity) * AirDrag;
            }
            if (Height >= Apex) {
                return Speed;
            }
            Speed += 0.01;
        }
        return Speed;
    }

    public static boolean crossesPool(Level World, Vec3 From, Vec3 To) {
        double Length = flat(From, To);
        int Steps = Math.max(1, Mth.ceil(Length / LineStep));
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Index = 0; Index <= Steps; Index++) {
            double Share = Index / (double) Steps;
            double X = Mth.lerp(Share, From.x, To.x);
            double Y = Mth.lerp(Share, From.y, To.y);
            double Z = Mth.lerp(Share, From.z, To.z);
            for (int Dy = -2; Dy <= 0; Dy++) {
                Cursor.set(X, Y + Dy, Z);
                if (isRedAt(World, Cursor)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean brink(Entity Body, Vec3 Forward) {
        double Reach = Body.getBbWidth() * 0.5 + 0.8;
        return crossesPool(Body.level(), Body.position(), Body.position().add(Forward.x * Reach, 0.0, Forward.z * Reach));
    }

    private static void splash(Mob Body) {
        if (!(Body.level() instanceof ServerLevel World)) {
            return;
        }
        double Top = surface(Body);
        double Y = Double.isNaN(Top) ? Body.getY() + 0.5 : Top;
        double Spread = Body.getBbWidth() * 0.45;
        World.sendParticles(Scp354Registry.Splash.get(), Body.getX(), Y + 0.1, Body.getZ(), 14, Spread, 0.05, Spread, 0.18);
        Scp354Sounds.play(World, new Vec3(Body.getX(), Y, Body.getZ()), Scp354Registry.SplashLayers, 1.0f, 0.65f + Body.getRandom().nextFloat() * 0.15f);
    }

    public static final class Escape extends Goal {
        private final Mob Body;
        private final Scp354Swimmer Swimmer;
        private int Rescan;
        private int Settle;
        private int Cooldown;
        private int Tries;
        private int Stuck;

        public Escape(Mob Body, Scp354Swimmer Swimmer) {
            this.Body = Body;
            this.Swimmer = Swimmer;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Swimmer.swimsOut() && Body.isAlive() && swimming(Body);
        }

        @Override
        public boolean canContinueToUse() {
            return Body.isAlive() && (swimming(Body) || Swimmer.course().Haul > 0 || Settle > 0);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            Body.getNavigation().stop();
            Rescan = 0;
            Settle = 0;
            Cooldown = 0;
            Tries = 0;
            Stuck = 0;
        }

        @Override
        public void stop() {
            Course Route = Swimmer.course();
            Route.Speed = 0.0;
            Route.Haul = 0;
            Settle = 0;
        }

        @Override
        public void tick() {
            Course Route = Swimmer.course();
            boolean Wet = swimming(Body);
            if (Route.Haul > 0) {
                Route.Haul--;
                steer(Route, Math.max(HaulPush, Swimmer.swimSpeed()));
                if (Route.Haul == 0 && !Wet) {
                    Settle = SettleTicks;
                }
                return;
            }
            if (!Wet) {
                Route.Speed = 0.0;
                if (Settle > 0) {
                    Settle--;
                    settle(Route);
                }
                return;
            }
            Body.getNavigation().stop();
            Cooldown = Math.max(0, Cooldown - 1);
            LivingEntity Target = Body.getTarget();
            if (Route.Exit == null || --Rescan <= 0) {
                Vec3 Found = exit(Body, Swimmer, Target, Math.min(StuckReach, StuckGrowth * Stuck));
                if (Found != null) {
                    Route.Exit = Found;
                } else {
                    Stuck++;
                }
                Rescan = RescanTicks;
            }
            Vec3 Mark = Route.Exit != null ? Route.Exit : Target != null ? Target.position() : null;
            if (Mark == null) {
                Route.Speed = 0.0;
                return;
            }
            Vec3 Flat = new Vec3(Mark.x - Body.getX(), 0.0, Mark.z - Body.getZ());
            double Distance = Flat.length();
            Route.Heading = Distance > 1.0e-4 ? Flat.scale(1.0 / Distance) : Vec3.ZERO;
            boolean Emerging = Body instanceof Scp354Emergent Emergent && Emergent.isEmerging();
            Route.Speed = Emerging ? EmergeDrift : Swimmer.swimSpeed();
            Body.getLookControl().setLookAt(Mark.x, Mark.y + Body.getEyeHeight() * 0.5, Mark.z, 30.0f, 30.0f);
            if (Emerging || Cooldown > 0) {
                return;
            }
            double Reach = Body.getBbWidth() * 0.5 + ReachSlack;
            if (Route.Exit != null && Distance <= Reach) {
                haul(Route, Route.Exit);
            } else if (Body.horizontalCollision) {
                Vec3 Spot = ahead();
                if (Spot != null) {
                    haul(Route, Spot);
                } else {
                    Rescan = 0;
                    Cooldown = 6;
                }
            }
        }

        @Nullable
        private Vec3 ahead() {
            Vec3 Facing = Vec3.directionFromRotation(0.0f, Body.getYRot());
            int Top = topCell(Body.level(), Body.getBlockX(), Body.getBlockY(), Body.getBlockZ());
            if (Top == Integer.MIN_VALUE) {
                return null;
            }
            for (double Step : new double[]{0.6, 1.1}) {
                double Far = Body.getBbWidth() * 0.5 + Step;
                Vec3 Spot = standAt(Body.level(), Body, Mth.floor(Body.getX() + Facing.x * Far), Mth.floor(Body.getZ() + Facing.z * Far), Top);
                if (Spot != null) {
                    return Spot;
                }
            }
            return null;
        }

        private void haul(Course Route, Vec3 Spot) {
            double Rise = Spot.y - Body.getY();
            if (Rise > Swimmer.haulReach() + ReachGrowth * Tries + Math.min(StuckReach, StuckGrowth * Stuck)) {
                Tries++;
                Rescan = 0;
                Cooldown = 10;
                return;
            }
            double Up = launchSpeed(Math.max(MinHop, Rise + HopMargin + HopGrowth * Tries), Body.getGravity());
            Route.Exit = Spot;
            Route.Haul = HopTicks;
            Route.Speed = 0.0;
            Body.setDeltaMovement(Body.getDeltaMovement().x, Up, Body.getDeltaMovement().z);
            steer(Route, Math.max(HaulPush, Swimmer.swimSpeed()));
            Body.hasImpulse = true;
            Swimmer.beginHaul();
            splash(Body);
            Tries++;
            Cooldown = HaulCooldown;
        }

        private void steer(Course Route, double Push) {
            if (Route.Exit == null) {
                return;
            }
            Vec3 Flat = new Vec3(Route.Exit.x - Body.getX(), 0.0, Route.Exit.z - Body.getZ());
            double Distance = Flat.length();
            Vec3 Motion = Body.getDeltaMovement();
            if (Distance < 0.1) {
                Body.setDeltaMovement(0.0, Motion.y, 0.0);
                return;
            }
            double Pace = Math.min(Push, Distance);
            Body.setDeltaMovement(Flat.x / Distance * Pace, Motion.y, Flat.z / Distance * Pace);
            float Aim = (float) (Mth.atan2(Flat.z, Flat.x) * Mth.RAD_TO_DEG) - 90.0f;
            float Yaw = Mth.wrapDegrees(Mth.approachDegrees(Body.getYRot(), Aim, TurnRate));
            Body.setYRot(Yaw);
            Body.yBodyRot = Yaw;
        }

        private void settle(Course Route) {
            if (Route.Exit == null || !Body.onGround()) {
                return;
            }
            if (flat(Body.position(), Route.Exit) <= SettleReach) {
                Settle = 0;
                return;
            }
            Body.getMoveControl().setWantedPosition(Route.Exit.x, Route.Exit.y, Route.Exit.z, 1.0);
        }
    }

    public static final class Buoy extends FloatGoal {
        private final Mob Body;

        public Buoy(Mob Body) {
            super(Body);
            this.Body = Body;
        }

        @Override
        public boolean canUse() {
            if (Body instanceof Scp354Swimmer Swimmer && Swimmer.swimsOut() && swimming(Body)) {
                return false;
            }
            return super.canUse();
        }
    }

    @Nullable
    public static Path dry(Mob Body, @Nullable Path Found) {
        if (Found == null) {
            return null;
        }
        Level World = Body.level();
        List<Node> Kept = new ArrayList<>();
        for (int Index = 0; Index < Found.getNodeCount(); Index++) {
            Vec3 Spot = Found.getEntityPosAtNode(Body, Index);
            BlockPos Cell = BlockPos.containing(Spot.x, Spot.y + 0.1, Spot.z);
            boolean Wet = isRedAt(World, Cell) || isRedAt(World, Cell.below());
            if (Wet && !Kept.isEmpty()) {
                break;
            }
            if (!Wet) {
                Kept.add(Found.getNode(Index));
            }
        }
        if (Kept.isEmpty()) {
            return null;
        }
        if (Kept.size() == Found.getNodeCount()) {
            return Found;
        }
        return new Path(Kept, Kept.get(Kept.size() - 1).asBlockPos(), false);
    }

    public static final class DryEvaluator extends WalkNodeEvaluator {
        @Override
        public Node getStart() {
            Node Found = super.getStart();
            int Cells = Mth.floor(mob.getBbWidth() + 1.0f);
            if (Found == null || Cells < 2) {
                return Found;
            }
            int X = Mth.floor(mob.getX() - Cells * 0.5 + 0.5);
            int Z = Mth.floor(mob.getZ() - Cells * 0.5 + 0.5);
            if (X == Found.x && Z == Found.z) {
                return Found;
            }
            return getStartNode(new BlockPos(X, Found.y, Z));
        }

        @Override
        public PathType getPathType(PathfindingContext Context, int X, int Y, int Z) {
            BlockGetter World = Context.level();
            BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos(X, Y, Z);
            if (isRedAt(World, Cursor)) {
                return PathType.BLOCKED;
            }
            for (int Drop = 1; Drop <= ShoreDrop; Drop++) {
                Cursor.setY(Y - Drop);
                if (isRedAt(World, Cursor)) {
                    return PathType.DANGER_OTHER;
                }
                if (!World.getBlockState(Cursor).getCollisionShape(World, Cursor).isEmpty()) {
                    break;
                }
            }
            return super.getPathType(Context, X, Y, Z);
        }
    }

    public static final class DryNavigation extends GroundPathNavigation {
        public DryNavigation(Mob Body, Level World) {
            super(Body, World);
        }

        @Override
        @Nullable
        protected Path createPath(Set<BlockPos> Targets, int Offset, boolean Upward, int Accuracy, float Range) {
            return dry(mob, super.createPath(Targets, Offset, Upward, Accuracy, Range));
        }

        @Override
        protected PathFinder createPathFinder(int Visits) {
            nodeEvaluator = new DryEvaluator();
            nodeEvaluator.setCanPassDoors(true);
            return new PathFinder(nodeEvaluator, Visits);
        }
    }

    public static final class DryClimberNavigation extends WallClimberNavigation {
        public DryClimberNavigation(Mob Body, Level World) {
            super(Body, World);
        }

        @Override
        @Nullable
        protected Path createPath(Set<BlockPos> Targets, int Offset, boolean Upward, int Accuracy, float Range) {
            return dry(mob, super.createPath(Targets, Offset, Upward, Accuracy, Range));
        }

        @Override
        protected PathFinder createPathFinder(int Visits) {
            nodeEvaluator = new DryEvaluator();
            nodeEvaluator.setCanPassDoors(true);
            return new PathFinder(nodeEvaluator, Visits);
        }
    }
}
