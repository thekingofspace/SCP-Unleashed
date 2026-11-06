package net.rai.scp_unleashed.scp327;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Scp327Waters {
    public static final int NoSurface = Integer.MIN_VALUE;
    public static final int MaxDepthScan = 12;
    public static final int PlanBudget = 6000;
    public static final double LandWidth = 1.2;
    public static final double LandHeight = 0.9;
    private static final int[][] Steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private Scp327Waters() {
    }

    public static boolean isWater(Level World, BlockPos Pos) {
        return World.isLoaded(Pos) && World.getFluidState(Pos).is(FluidTags.WATER);
    }

    public static int surfaceAt(Level World, BlockPos Pos) {
        BlockPos.MutableBlockPos Cursor = Pos.mutable();
        if (!isWater(World, Cursor)) {
            boolean Found = false;
            for (int Down = 1; Down <= 2; Down++) {
                Cursor.setY(Pos.getY() - Down);
                if (isWater(World, Cursor)) {
                    Found = true;
                    break;
                }
            }
            if (!Found) {
                return NoSurface;
            }
        }
        int Limit = Cursor.getY() + 24;
        while (Cursor.getY() < Limit && isWater(World, Cursor.above())) {
            Cursor.move(Direction.UP);
        }
        return Cursor.getY();
    }

    public static int depthAt(Level World, int X, int SurfaceY, int Z) {
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos(X, SurfaceY, Z);
        int Depth = 0;
        while (Depth < MaxDepthScan && isWater(World, Cursor)) {
            Depth++;
            Cursor.move(Direction.DOWN);
        }
        return Depth;
    }

    public static double floorAt(Level World, int X, int SurfaceY, int Z) {
        return SurfaceY + 1 - depthAt(World, X, SurfaceY, Z);
    }

    public static double waterline(int SurfaceY) {
        return SurfaceY + 0.89;
    }

    private static int columnSurface(Level World, int X, int Z, int Near) {
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos(X, Near, Z);
        for (int Offset : new int[]{0, -1, 1}) {
            Cursor.setY(Near + Offset);
            if (isWater(World, Cursor) && !isWater(World, Cursor.above())) {
                return Near + Offset;
            }
        }
        return NoSurface;
    }

    public static long pack(int X, int Z) {
        return ((long) X << 32) ^ (Z & 0xFFFFFFFFL);
    }

    public static int unpackX(long Key) {
        return (int) (Key >> 32);
    }

    public static int unpackZ(long Key) {
        return (int) Key;
    }

    public static boolean standable(Level World, BlockPos Pos) {
        if (!World.isLoaded(Pos)) {
            return false;
        }
        BlockState Below = World.getBlockState(Pos.below());
        if (!Below.isFaceSturdy(World, Pos.below(), Direction.UP) || !World.getFluidState(Pos.below()).isEmpty()) {
            return false;
        }
        AABB Box = AABB.ofSize(Vec3.atBottomCenterOf(Pos).add(0.0, LandHeight / 2.0, 0.0), LandWidth, LandHeight, LandWidth);
        return World.noCollision(Box) && !World.containsAnyLiquid(Box);
    }

    public record Plan(Vec3 Goal, @Nullable BlockPos Beach, @Nullable BlockPos Edge, List<Vec3> Route, int SurfaceY, double TargetDistance, boolean Reached, int Visited) {
    }

    public static Optional<Plan> plan(ServerLevel World, Vec3 From, Vec3 Target, boolean AllowBeach, double BeachReach, int Budget) {
        BlockPos Start = BlockPos.containing(From);
        int Surface = surfaceAt(World, Start.above());
        if (Surface == NoSurface) {
            Surface = surfaceAt(World, Start);
        }
        if (Surface == NoSurface) {
            return Optional.empty();
        }
        Map<Long, Long> Parents = new HashMap<>();
        Map<Long, Integer> Heights = new HashMap<>();
        ArrayDeque<Long> Queue = new ArrayDeque<>();
        long First = pack(Start.getX(), Start.getZ());
        Parents.put(First, First);
        Heights.put(First, Surface);
        Queue.add(First);
        long Best = First;
        double BestDistance = flat(Start.getX(), Start.getZ(), Target);
        BlockPos BestBeach = null;
        long BeachEdge = First;
        double BeachDistance = Double.MAX_VALUE;
        while (!Queue.isEmpty() && Parents.size() < Budget) {
            long Key = Queue.poll();
            int X = unpackX(Key);
            int Z = unpackZ(Key);
            int Height = Heights.get(Key);
            double Distance = flat(X, Z, Target) + Math.abs(Height + 1 - Target.y) * 0.25;
            if (Distance < BestDistance) {
                BestDistance = Distance;
                Best = Key;
            }
            for (int[] Step : Steps) {
                int Nx = X + Step[0];
                int Nz = Z + Step[1];
                long Next = pack(Nx, Nz);
                if (Parents.containsKey(Next)) {
                    continue;
                }
                int NextHeight = columnSurface(World, Nx, Nz, Height);
                if (NextHeight != NoSurface) {
                    Parents.put(Next, Key);
                    Heights.put(Next, NextHeight);
                    Queue.add(Next);
                    continue;
                }
                if (!AllowBeach) {
                    continue;
                }
                BlockPos Land = shore(World, Nx, Height, Nz);
                if (Land == null) {
                    continue;
                }
                double LandDistance = Vec3.atBottomCenterOf(Land).distanceTo(Target);
                if (LandDistance < BeachDistance) {
                    BeachDistance = LandDistance;
                    BestBeach = Land;
                    BeachEdge = Key;
                }
            }
        }
        boolean Beached = BestBeach != null && BeachDistance <= BeachReach && BeachDistance + 0.5 < BestDistance;
        long Goal = Beached ? BeachEdge : Best;
        List<Long> Chain = new ArrayList<>();
        long Cursor = Goal;
        int Guard = 0;
        while (Guard++ < Budget) {
            Chain.add(Cursor);
            long Parent = Parents.get(Cursor);
            if (Parent == Cursor) {
                break;
            }
            Cursor = Parent;
        }
        Collections.reverse(Chain);
        List<Vec3> Route = pull(Chain, Heights, Parents);
        int GoalHeight = Heights.get(Goal);
        Vec3 GoalPoint = new Vec3(unpackX(Goal) + 0.5, GoalHeight, unpackZ(Goal) + 0.5);
        double Final = Beached ? BeachDistance : BestDistance;
        return Optional.of(new Plan(GoalPoint, Beached ? BestBeach : null, Beached ? new BlockPos(unpackX(Goal), GoalHeight, unpackZ(Goal)) : null, Route, Surface, Final,
                Queue.isEmpty(), Parents.size()));
    }

    @Nullable
    public static BlockPos shore(Level World, int X, int SurfaceY, int Z) {
        BlockPos Flush = new BlockPos(X, SurfaceY, Z);
        if (standable(World, Flush)) {
            return Flush;
        }
        BlockPos Raised = Flush.above();
        if (standable(World, Raised)) {
            return Raised;
        }
        return null;
    }

    private static List<Vec3> pull(List<Long> Chain, Map<Long, Integer> Heights, Map<Long, Long> Known) {
        List<Vec3> Route = new ArrayList<>();
        if (Chain.isEmpty()) {
            return Route;
        }
        int Anchor = 0;
        for (int Index = 1; Index < Chain.size(); Index++) {
            if (!clear(Chain.get(Anchor), Chain.get(Index), Known)) {
                long Corner = Chain.get(Index - 1);
                Route.add(new Vec3(unpackX(Corner) + 0.5, Heights.get(Corner), unpackZ(Corner) + 0.5));
                Anchor = Index - 1;
            }
        }
        long Last = Chain.get(Chain.size() - 1);
        Route.add(new Vec3(unpackX(Last) + 0.5, Heights.get(Last), unpackZ(Last) + 0.5));
        return Route;
    }

    private static boolean clear(long From, long To, Map<Long, Long> Known) {
        double X0 = unpackX(From) + 0.5;
        double Z0 = unpackZ(From) + 0.5;
        double X1 = unpackX(To) + 0.5;
        double Z1 = unpackZ(To) + 0.5;
        double Length = Math.sqrt((X1 - X0) * (X1 - X0) + (Z1 - Z0) * (Z1 - Z0));
        int Count = Math.max(1, (int) Math.ceil(Length * 3.0));
        for (int Index = 0; Index <= Count; Index++) {
            double Share = Index / (double) Count;
            double X = X0 + (X1 - X0) * Share;
            double Z = Z0 + (Z1 - Z0) * Share;
            for (double Dx : new double[]{-0.45, 0.45}) {
                for (double Dz : new double[]{-0.45, 0.45}) {
                    if (!Known.containsKey(pack(Mth.floor(X + Dx), Mth.floor(Z + Dz)))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static double flat(double X, double Z, Vec3 Target) {
        double Dx = X + 0.5 - Target.x;
        double Dz = Z + 0.5 - Target.z;
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    public static Optional<BlockPos> nearestWater(ServerLevel World, BlockPos From, int Radius, int Vertical) {
        BlockPos Best = null;
        double BestDistance = Double.MAX_VALUE;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int Dx = -Radius; Dx <= Radius; Dx++) {
            for (int Dz = -Radius; Dz <= Radius; Dz++) {
                double Flat = Dx * Dx + Dz * Dz;
                if (Flat > Radius * Radius || Flat >= BestDistance) {
                    continue;
                }
                for (int Dy = -Vertical; Dy <= Vertical; Dy++) {
                    Cursor.set(From.getX() + Dx, From.getY() + Dy, From.getZ() + Dz);
                    if (isWater(World, Cursor)) {
                        double Distance = Flat + Dy * Dy * 0.5;
                        if (Distance < BestDistance) {
                            BestDistance = Distance;
                            Best = Cursor.immutable();
                        }
                    }
                }
            }
        }
        return Optional.ofNullable(Best);
    }

    public record Body(BlockPos Centre, int SurfaceY, List<BlockPos> Columns) {
    }

    public static Optional<Body> body(ServerLevel World, BlockPos Seed, int MinDepth, int MinArea, int Limit) {
        int Surface = surfaceAt(World, Seed);
        if (Surface == NoSurface || depthAt(World, Seed.getX(), Surface, Seed.getZ()) < MinDepth) {
            return Optional.empty();
        }
        Map<Long, Integer> Seen = new HashMap<>();
        ArrayDeque<Long> Queue = new ArrayDeque<>();
        List<BlockPos> Columns = new ArrayList<>();
        long First = pack(Seed.getX(), Seed.getZ());
        Seen.put(First, Surface);
        Queue.add(First);
        while (!Queue.isEmpty() && Columns.size() < Limit) {
            long Key = Queue.poll();
            int X = unpackX(Key);
            int Z = unpackZ(Key);
            int Height = Seen.get(Key);
            Columns.add(new BlockPos(X, Height, Z));
            for (int[] Step : Steps) {
                long Next = pack(X + Step[0], Z + Step[1]);
                if (Seen.containsKey(Next)) {
                    continue;
                }
                int NextHeight = columnSurface(World, X + Step[0], Z + Step[1], Height);
                if (NextHeight != NoSurface && depthAt(World, X + Step[0], NextHeight, Z + Step[1]) >= MinDepth) {
                    Seen.put(Next, NextHeight);
                    Queue.add(Next);
                }
            }
        }
        if (Columns.size() < MinArea) {
            return Optional.empty();
        }
        return Optional.of(new Body(new BlockPos(Seed.getX(), Surface, Seed.getZ()), Surface, Columns));
    }
}
