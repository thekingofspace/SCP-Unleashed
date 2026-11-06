package net.rai.scp_unleashed.scp343;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class Scp343Enclosure {
    public static final int Radius = 16;
    public static final int Height = 10;
    public static final int MaxCells = 1200;
    public static final int MaxDrop = 3;
    public static final double LowShape = 0.5;

    private Scp343Enclosure() {
    }

    public record Survey(boolean Enclosed, int Cells, @Nullable BlockPos Stand, @Nullable BlockPos Wall) {
        public static final Survey Open = new Survey(false, 0, null, null);
    }

    public static boolean passable(BlockGetter World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
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

    private static boolean floats(Level World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        return !State.getFluidState().isEmpty() || State.is(BlockTags.CLIMBABLE);
    }

    public static boolean standable(Level World, BlockPos Pos) {
        if (World.isOutsideBuildHeight(Pos) || World.isOutsideBuildHeight(Pos.above())) {
            return false;
        }
        if (!passable(World, Pos) || !passable(World, Pos.above())) {
            return false;
        }
        if (floats(World, Pos)) {
            return true;
        }
        BlockPos Below = Pos.below();
        return !passable(World, Below) || !World.getBlockState(Pos).getCollisionShape(World, Pos).isEmpty() || floats(World, Below);
    }

    private static boolean blocker(Level World, BlockPos Pos) {
        return !passable(World, Pos) && !passable(World, Pos.above());
    }

    @Nullable
    public static BlockPos startOf(Level World, BlockPos Feet) {
        for (BlockPos Candidate : new BlockPos[]{Feet, Feet.above(), Feet.below()}) {
            if (World.isLoaded(Candidate) && standable(World, Candidate)) {
                return Candidate;
            }
        }
        return null;
    }

    public static Survey survey(Level World, BlockPos Feet) {
        BlockPos Start = startOf(World, Feet);
        if (Start == null) {
            return Survey.Open;
        }
        LongArrayFIFOQueue Queue = new LongArrayFIFOQueue();
        LongOpenHashSet Seen = new LongOpenHashSet();
        Queue.enqueue(Start.asLong());
        Seen.add(Start.asLong());
        BlockPos Stand = null;
        BlockPos Wall = null;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        while (!Queue.isEmpty()) {
            BlockPos Cell = BlockPos.of(Queue.dequeueLong());
            if (Math.abs(Cell.getX() - Start.getX()) > Radius || Math.abs(Cell.getZ() - Start.getZ()) > Radius || Math.abs(Cell.getY() - Start.getY()) > Height) {
                return Survey.Open;
            }
            if (Seen.size() > MaxCells) {
                return Survey.Open;
            }
            for (Direction Side : Direction.Plane.HORIZONTAL) {
                Cursor.setWithOffset(Cell, Side);
                if (!World.isLoaded(Cursor)) {
                    return Survey.Open;
                }
                if (Wall == null && blocker(World, Cursor)) {
                    Stand = Cell;
                    Wall = Cursor.immutable();
                }
                BlockPos Next = step(World, Cell, Side);
                if (Next != null && Seen.add(Next.asLong())) {
                    Queue.enqueue(Next.asLong());
                }
            }
            if (floats(World, Cell)) {
                BlockPos Up = Cell.above();
                if (standable(World, Up) && Seen.add(Up.asLong())) {
                    Queue.enqueue(Up.asLong());
                }
            }
        }
        return new Survey(true, Seen.size(), Stand, Wall);
    }

    @Nullable
    private static BlockPos step(Level World, BlockPos Cell, Direction Side) {
        BlockPos Ahead = Cell.relative(Side);
        if (standable(World, Ahead)) {
            return Ahead;
        }
        BlockPos Up = Ahead.above();
        if (!passable(World, Ahead) && passable(World, Cell.above(2)) && standable(World, Up)) {
            BlockState Step = World.getBlockState(Ahead);
            VoxelShape Shape = Step.getCollisionShape(World, Ahead);
            if (Shape.isEmpty() || Shape.max(Direction.Axis.Y) <= 1.0) {
                return Up;
            }
        }
        if (passable(World, Ahead) && passable(World, Ahead.above())) {
            BlockPos Down = Ahead;
            for (int Drop = 1; Drop <= MaxDrop; Drop++) {
                Down = Down.below();
                if (standable(World, Down)) {
                    return Down;
                }
                if (!passable(World, Down)) {
                    break;
                }
            }
        }
        return null;
    }
}
