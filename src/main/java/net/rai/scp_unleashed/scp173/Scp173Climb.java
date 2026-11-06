package net.rai.scp_unleashed.scp173;

import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.PriorityQueue;

public final class Scp173Climb {
    public static final int MaxNodes = 800;
    public static final int MaxProbes = 2000;
    public static final int MaxDrop = 16;
    public static final int VerticalRange = 32;
    public static final int HorizontalRange = 48;
    public static final double WalkCost = 1.0;
    public static final double StepCost = 1.2;
    public static final double ClingCost = 1.4;
    public static final double ClimbUpCost = 1.4;
    public static final double ClimbDownCost = 1.25;
    public static final double DropBase = 1.0;
    public static final double DropPerBlock = 0.25;
    public static final double HalfWidth = Scp173Statue.Width / 2.0;
    public static final double Gap = 0.101;
    public static final double Reach = 0.2;
    public static final double EdgeHook = 0.5;
    public static final double LowFloor = 0.5;
    private static final Direction[] Sides = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final Spot Blocked = new Spot(BlockPos.ZERO, 0.0, false, null);

    private Scp173Climb() {
    }

    public record Spot(BlockPos Cell, double Y, boolean Supported, @Nullable Direction Wall) {
        public boolean holdable() {
            return Supported || Wall != null;
        }
    }

    private record Open(long Key, double Score, double Cost) {
    }

    public interface Probe {
        @Nullable
        Spot spot(BlockPos Cell);

        boolean passes(Vec3 From, Vec3 To);
    }

    public static Probe probe(Level World) {
        Scp173Passage Way = Scp173Passage.remembering(World);
        return new Probe() {
            @Nullable
            @Override
            public Spot spot(BlockPos Cell) {
                return inspect(Way, Cell, true);
            }

            @Override
            public boolean passes(Vec3 From, Vec3 To) {
                return Way.passes(From, To);
            }
        };
    }

    public static Vec3 centre(Spot Here) {
        return new Vec3(Here.Cell().getX() + 0.5, Here.Y(), Here.Cell().getZ() + 0.5);
    }

    public static AABB box(double X, double Y, double Z) {
        return new AABB(X - HalfWidth, Y, Z - HalfWidth, X + HalfWidth, Y + Scp173Statue.Height, Z + HalfWidth);
    }

    public static AABB box(BlockPos Cell, double Y) {
        return box(Cell.getX() + 0.5, Y, Cell.getZ() + 0.5);
    }

    private static boolean overlaps(Level World, BlockPos Cell, BlockState State, AABB Region) {
        VoxelShape Shape = State.getCollisionShape(World, Cell);
        if (Shape.isEmpty()) {
            return false;
        }
        int X = Cell.getX();
        int Y = Cell.getY();
        int Z = Cell.getZ();
        if (Shape == Shapes.block()) {
            return Region.intersects(X, Y, Z, X + 1.0, Y + 1.0, Z + 1.0);
        }
        for (AABB Part : Shape.toAabbs()) {
            if (Region.intersects(Part.minX + X, Part.minY + Y, Part.minZ + Z, Part.maxX + X, Part.maxY + Y, Part.maxZ + Z)) {
                return true;
            }
        }
        return false;
    }

    private static boolean solidIn(Level World, AABB Region) {
        for (BlockPos Cell : BlockPos.betweenClosed(BlockPos.containing(Region.minX, Region.minY, Region.minZ), BlockPos.containing(Region.maxX - 1.0e-4, Region.maxY - 1.0e-4, Region.maxZ - 1.0e-4))) {
            if (World.isLoaded(Cell) && overlaps(World, Cell, World.getBlockState(Cell), Region)) {
                return true;
            }
        }
        return false;
    }

    public static AABB sliver(AABB Box, Direction Side) {
        double Low = Box.minY - EdgeHook;
        double High = Box.maxY - 0.2;
        return switch (Side) {
            case EAST -> new AABB(Box.maxX + Gap, Low, Box.minZ + 0.1, Box.maxX + Reach, High, Box.maxZ - 0.1);
            case WEST -> new AABB(Box.minX - Reach, Low, Box.minZ + 0.1, Box.minX - Gap, High, Box.maxZ - 0.1);
            case SOUTH -> new AABB(Box.minX + 0.1, Low, Box.maxZ + Gap, Box.maxX - 0.1, High, Box.maxZ + Reach);
            default -> new AABB(Box.minX + 0.1, Low, Box.minZ - Reach, Box.maxX - 0.1, High, Box.minZ - Gap);
        };
    }

    @Nullable
    public static Direction wallSide(Level World, AABB Box) {
        for (Direction Side : Sides) {
            if (solidIn(World, sliver(Box, Side))) {
                return Side;
            }
        }
        return null;
    }

    public static boolean supported(Level World, AABB Box) {
        return solidIn(World, new AABB(Box.minX, Box.minY - 0.06, Box.minZ, Box.maxX, Box.minY, Box.maxZ));
    }

    @Nullable
    public static Spot inspect(Level World, BlockPos Cell) {
        return inspect(Scp173Passage.of(World), Cell, true);
    }

    @Nullable
    public static Vec3 stand(Scp173Passage Way, BlockPos Cell) {
        Spot Found = inspect(Way, Cell, false);
        return Found == null ? null : centre(Found);
    }

    @Nullable
    public static Spot inspect(Scp173Passage Way, BlockPos Cell, boolean Walls) {
        Level World = Way.level();
        if (!World.isLoaded(Cell) || World.isOutsideBuildHeight(Cell)) {
            return null;
        }
        BlockState Here = World.getBlockState(Cell);
        VoxelShape HereShape = Scp173Passage.ignores(World, Cell, Here) ? Shapes.empty() : Here.getCollisionShape(World, Cell);
        double Y;
        boolean Supported;
        if (!HereShape.isEmpty()) {
            double Top = HereShape.max(Direction.Axis.Y);
            if (Top > LowFloor) {
                return null;
            }
            Y = Cell.getY() + Top;
            Supported = true;
        } else {
            BlockPos Below = Cell.below();
            BlockState Under = World.getBlockState(Below);
            VoxelShape UnderShape = Under.getCollisionShape(World, Below);
            double Top = UnderShape.isEmpty() ? 0.0 : UnderShape.max(Direction.Axis.Y);
            Supported = !UnderShape.isEmpty() && Top >= LowFloor;
            Y = Supported ? Below.getY() + Top : Cell.getY();
        }
        if (!Way.open(Scp173Passage.box(new Vec3(Cell.getX() + 0.5, Y, Cell.getZ() + 0.5)))) {
            return null;
        }
        return new Spot(Cell.immutable(), Y, Supported, Supported || !Walls ? null : wallSide(World, box(Cell, Y)));
    }

    private static final class Search {
        private final Probe World;
        private final BlockPos Start;
        private final BlockPos Goal;
        private final Vec3 Origin;
        private final Long2ObjectOpenHashMap<Spot> Spots = new Long2ObjectOpenHashMap<>();
        private final Long2DoubleOpenHashMap Costs = new Long2DoubleOpenHashMap();
        private final Long2LongOpenHashMap Parents = new Long2LongOpenHashMap();
        private final Long2LongOpenHashMap Vias = new Long2LongOpenHashMap();
        private final Long2ByteOpenHashMap Kinds = new Long2ByteOpenHashMap();
        private final PriorityQueue<Open> Queue = new PriorityQueue<>((First, Second) -> Double.compare(First.Score(), Second.Score()));
        private int Expanded;
        private int Probes;

        private Search(Probe World, BlockPos Start, BlockPos Goal, Vec3 Origin) {
            this.World = World;
            this.Start = Start;
            this.Goal = Goal;
            this.Origin = Origin;
            Costs.defaultReturnValue(Double.MAX_VALUE);
        }

        private Vec3 leaving(Spot Here) {
            return Here.Cell().equals(Start) ? Origin : centre(Here);
        }

        @Nullable
        private Spot spot(BlockPos Cell) {
            if (Math.abs(Cell.getY() - Start.getY()) > VerticalRange || Math.abs(Cell.getX() - Start.getX()) > HorizontalRange || Math.abs(Cell.getZ() - Start.getZ()) > HorizontalRange) {
                return null;
            }
            long Key = Cell.asLong();
            Spot Found = Spots.get(Key);
            if (Found == null) {
                if (Probes >= MaxProbes) {
                    return null;
                }
                Probes++;
                Found = World.spot(Cell);
                Spots.put(Key, Found == null ? Blocked : Found);
                return Found;
            }
            return Found == Blocked ? null : Found;
        }

        private double guess(BlockPos Cell) {
            return Math.abs(Cell.getX() - Goal.getX()) + Math.abs(Cell.getZ() - Goal.getZ()) + Math.abs(Cell.getY() - Goal.getY());
        }

        private boolean arrived(Spot Here) {
            int Across = Math.abs(Here.Cell().getX() - Goal.getX()) + Math.abs(Here.Cell().getZ() - Goal.getZ());
            return Across <= 1 && Math.abs(Here.Cell().getY() - Goal.getY()) <= 1 && Here.holdable();
        }

        private void push(long From, Spot To, double Cost, byte Kind, long Via) {
            long Key = To.Cell().asLong();
            if (Cost >= Costs.get(Key)) {
                return;
            }
            Costs.put(Key, Cost);
            Parents.put(Key, From);
            Kinds.put(Key, Kind);
            Vias.put(Key, Via);
            Queue.add(new Open(Key, Cost + guess(To.Cell()), Cost));
        }

        private void drop(long From, Spot Over, double Cost) {
            Vec3 Edge = new Vec3(Over.Cell().getX() + 0.5, Over.Cell().getY(), Over.Cell().getZ() + 0.5);
            for (int Depth = 1; Depth <= MaxDrop; Depth++) {
                Spot Land = spot(Over.Cell().below(Depth));
                if (Land == null) {
                    return;
                }
                if (Land.Supported()) {
                    if (World.passes(Edge, centre(Land))) {
                        push(From, Land, Cost + DropBase + DropPerBlock * Depth, Scp173Course.Drop, Over.Cell().asLong());
                    }
                    return;
                }
            }
        }

        private void expand(Spot Here, double Cost) {
            long From = Here.Cell().asLong();
            Vec3 Leave = leaving(Here);
            for (Direction Side : Sides) {
                BlockPos Next = Here.Cell().relative(Side);
                Spot Beside = spot(Next);
                if (Beside != null && World.passes(Leave, centre(Beside))) {
                    if (Beside.Supported()) {
                        push(From, Beside, Cost + (Here.Supported() ? WalkCost : ClingCost), Scp173Course.Walk, Long.MIN_VALUE);
                    } else {
                        if (Beside.Wall() != null) {
                            push(From, Beside, Cost + ClingCost, Scp173Course.Climb, Long.MIN_VALUE);
                        }
                        drop(From, Beside, Cost);
                    }
                }
                if (Here.Supported()) {
                    Spot Up = spot(Next.above());
                    if (Up != null && Up.Supported() && World.passes(Leave, centre(Up))) {
                        push(From, Up, Cost + StepCost, Scp173Course.Walk, Long.MIN_VALUE);
                    }
                }
            }
            Spot Up = spot(Here.Cell().above());
            if (Up != null && !Up.Supported() && Up.Wall() != null && World.passes(Leave, centre(Up))) {
                push(From, Up, Cost + ClimbUpCost, Scp173Course.Climb, Long.MIN_VALUE);
            }
            if (!Here.Supported()) {
                Spot Down = spot(Here.Cell().below());
                if (Down != null && Down.holdable() && World.passes(Leave, centre(Down))) {
                    push(From, Down, Cost + ClimbDownCost, Down.Supported() ? Scp173Course.Walk : Scp173Course.Climb, Long.MIN_VALUE);
                }
            }
        }

        @Nullable
        private Scp173Course run() {
            Spot First = spot(Start);
            if (First == null) {
                return null;
            }
            Costs.put(Start.asLong(), 0.0);
            Queue.add(new Open(Start.asLong(), guess(Start), 0.0));
            while (!Queue.isEmpty() && Expanded < MaxNodes && Probes < MaxProbes) {
                Open Next = Queue.poll();
                if (Next.Cost() > Costs.get(Next.Key())) {
                    continue;
                }
                Spot Here = Spots.get(Next.Key());
                if (Here == null || Here == Blocked) {
                    continue;
                }
                Expanded++;
                if (Next.Key() != Start.asLong() && arrived(Here)) {
                    return build(Next.Key());
                }
                expand(Here, Next.Cost());
            }
            return null;
        }

        private Scp173Course build(long End) {
            List<Scp173Course.Step> Steps = new ArrayList<>();
            boolean Climbing = false;
            long Key = End;
            while (Key != Start.asLong()) {
                Spot Here = Spots.get(Key);
                byte Kind = Kinds.get(Key);
                Steps.add(new Scp173Course.Step(new Vec3(Here.Cell().getX() + 0.5, Here.Y(), Here.Cell().getZ() + 0.5), Here.Cell(), Kind, Here.Wall()));
                long Via = Vias.get(Key);
                if (Kind == Scp173Course.Drop && Via != Long.MIN_VALUE) {
                    BlockPos Over = BlockPos.of(Via);
                    Steps.add(new Scp173Course.Step(new Vec3(Over.getX() + 0.5, Over.getY(), Over.getZ() + 0.5), Over, Scp173Course.Drop, null));
                }
                Climbing |= Kind != Scp173Course.Walk;
                Key = Parents.get(Key);
            }
            Collections.reverse(Steps);
            return new Scp173Course(Steps, BlockPos.of(End), Climbing);
        }
    }

    public record Result(@Nullable Scp173Course Course, int Expanded, int Probes) {
    }

    public static Result search(Probe World, BlockPos Start, BlockPos Goal, Vec3 Origin) {
        Search Plan = new Search(World, Start, Goal, Origin);
        Scp173Course Found = Plan.run();
        return new Result(Found, Plan.Expanded, Plan.Probes);
    }

    @Nullable
    public static Scp173Course find(ServerLevel World, Scp173Statue Statue, LivingEntity Target) {
        if (!Scp173Budget.takeProbes(World, MaxProbes)) {
            return null;
        }
        BlockPos Start = BlockPos.containing(Statue.getX(), Statue.getY() + 0.01, Statue.getZ());
        BlockPos Goal = BlockPos.containing(Target.getX(), Target.getY() + 0.01, Target.getZ());
        Result Found = search(probe(World), Start, Goal, Statue.position());
        Scp173Budget.refundProbes(MaxProbes - Found.Probes());
        return Found.Course();
    }
}
