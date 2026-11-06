package net.rai.scp_unleashed.scp163;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Scp163Survey {
    public static final int MaxCells = 1024;
    public static final int Radius = 16;
    public static final int Budget = 256;
    public static final int MaxSpots = 48;
    public static final int Inspections = 2;
    public static final int InspectReach = 12;
    public static final double LowShape = 0.5;
    public static final int SkyScan = 32;

    public enum Outcome {
        Unsurveyed,
        Enclosed,
        Sky,
        Large,
        Far,
        Unloaded,
        Breathless;

        public String key() {
            return name().toLowerCase(Locale.ROOT);
        }

        public static Outcome byName(String Name) {
            for (Outcome Candidate : values()) {
                if (Candidate.name().equals(Name)) {
                    return Candidate;
                }
            }
            return Unsurveyed;
        }
    }

    public record Spot(BlockPos Cell, Direction Wall, boolean Corner) {
    }

    private final BlockPos Origin;
    private final LongArrayFIFOQueue Queue = new LongArrayFIFOQueue();
    private final LongOpenHashSet Seen = new LongOpenHashSet();
    private final List<Spot> Spots = new ArrayList<>();
    private Outcome Result;

    public Scp163Survey(BlockPos Origin) {
        this.Origin = Origin.immutable();
        Seen.add(this.Origin.asLong());
        Queue.enqueue(this.Origin.asLong());
    }

    public BlockPos origin() {
        return Origin;
    }

    public boolean isDone() {
        return Result != null;
    }

    public Outcome result() {
        return Result == null ? Outcome.Unsurveyed : Result;
    }

    public int cells() {
        return Seen.size();
    }

    public LongOpenHashSet cellSet() {
        return Seen;
    }

    public static boolean isPassable(BlockGetter World, BlockPos Pos, BlockState State) {
        if (State.getBlock() instanceof DoorBlock) {
            return State.getValue(DoorBlock.OPEN);
        }
        if (State.getBlock() instanceof TrapDoorBlock) {
            return State.getValue(TrapDoorBlock.OPEN);
        }
        if (State.getBlock() instanceof FenceGateBlock) {
            return State.getValue(FenceGateBlock.OPEN);
        }
        VoxelShape Shape = State.getCollisionShape(World, Pos);
        return Shape.isEmpty() || Shape.max(Direction.Axis.Y) <= LowShape;
    }

    private static boolean isPassable(ServerLevel World, BlockPos Pos) {
        return isPassable(World, Pos, World.getBlockState(Pos));
    }

    private static boolean isOpenAbove(ServerLevel World, BlockPos Cell) {
        BlockPos.MutableBlockPos Cursor = Cell.mutable();
        int Top = Math.min(World.getMaxBuildHeight(), Cell.getY() + SkyScan);
        while (Cursor.getY() < Top) {
            Cursor.move(Direction.UP);
            if (!isPassable(World, Cursor)) {
                return false;
            }
        }
        return true;
    }

    public boolean step(ServerLevel World, int Limit) {
        BlockPos.MutableBlockPos Next = new BlockPos.MutableBlockPos();
        int Processed = 0;
        while (Result == null && Processed < Limit) {
            if (Queue.isEmpty()) {
                Result = Outcome.Enclosed;
                break;
            }
            BlockPos Cell = BlockPos.of(Queue.dequeueLong());
            Processed++;
            if (Cell.getY() >= World.getHeight(Heightmap.Types.MOTION_BLOCKING, Cell.getX(), Cell.getZ()) && isOpenAbove(World, Cell)) {
                Result = Outcome.Sky;
                break;
            }
            if (Math.abs(Cell.getX() - Origin.getX()) > Radius || Math.abs(Cell.getY() - Origin.getY()) > Radius || Math.abs(Cell.getZ() - Origin.getZ()) > Radius) {
                Result = Outcome.Far;
                break;
            }
            for (Direction Side : Direction.values()) {
                Next.setWithOffset(Cell, Side);
                if (Seen.contains(Next.asLong())) {
                    continue;
                }
                if (!World.isLoaded(Next)) {
                    Result = Outcome.Unloaded;
                    break;
                }
                if (isPassable(World, Next)) {
                    Seen.add(Next.asLong());
                    Queue.enqueue(Next.asLong());
                    if (Seen.size() > MaxCells) {
                        Result = Outcome.Large;
                        break;
                    }
                }
            }
            noteWalls(World, Cell);
        }
        return Result != null;
    }

    private void noteWalls(ServerLevel World, BlockPos Cell) {
        if (Spots.size() >= MaxSpots || isPassable(World, Cell.below()) || !isPassable(World, Cell.above()) || Scp163Footing.isInstrumentSpot(World, Cell)) {
            return;
        }
        int Blocked = 0;
        Direction Found = null;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            BlockPos Wall = Cell.relative(Side);
            if (isPassable(World, Wall) || isPassable(World, Wall.above())) {
                continue;
            }
            Blocked++;
            BlockPos Back = Cell.relative(Side.getOpposite());
            if (Found == null && isPassable(World, Back) && isPassable(World, Back.above())) {
                Found = Side;
            }
        }
        if (Found != null) {
            Spots.add(new Spot(Cell.immutable(), Found, Blocked >= 2));
        }
    }

    public List<Spot> plan(BlockPos From, RandomSource Random) {
        List<Spot> Near = new ArrayList<>();
        for (Spot Candidate : Spots) {
            if (Candidate.Cell().closerThan(From, InspectReach)) {
                Near.add(Candidate);
            }
        }
        List<Spot> Chosen = new ArrayList<>();
        if (Near.isEmpty()) {
            return Chosen;
        }
        List<Spot> Corners = Near.stream().filter(Spot::Corner).toList();
        List<Spot> Pool = Corners.isEmpty() ? Near : Corners;
        Spot First = Pool.get(Random.nextInt(Pool.size()));
        Chosen.add(First);
        Spot Far = null;
        double Best = 4.0;
        for (Spot Candidate : Near) {
            double Distance = Candidate.Cell().distSqr(First.Cell());
            if (Distance > Best) {
                Best = Distance;
                Far = Candidate;
            }
        }
        if (Far != null && Chosen.size() < Inspections) {
            Chosen.add(Far);
        }
        return Chosen;
    }
}
