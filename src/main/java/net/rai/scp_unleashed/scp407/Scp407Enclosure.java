package net.rai.scp_unleashed.scp407;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

public final class Scp407Enclosure {
    public static final int Footprint = 32;
    public static final int MaxHeight = 16;
    public static final int MaxCells = Footprint * Footprint * MaxHeight;

    private Scp407Enclosure() {
    }

    public record Result(boolean Enclosed, LongOpenHashSet Cells, String Reason) {
    }

    public static boolean passable(ServerLevel World, BlockPos Pos, BlockState State) {
        if (State.getBlock() instanceof Scp407GrowthBlock) {
            return true;
        }
        if (State.getBlock() instanceof DoorBlock && State.getValue(DoorBlock.OPEN)) {
            return true;
        }
        if (State.getBlock() instanceof TrapDoorBlock && State.getValue(TrapDoorBlock.OPEN)) {
            return true;
        }
        if (State.getBlock() instanceof FenceGateBlock && State.getValue(FenceGateBlock.OPEN)) {
            return true;
        }
        return State.getCollisionShape(World, Pos).isEmpty();
    }

    public static Result check(ServerLevel World, BlockPos Speaker, @Nullable Direction Facing) {
        LongOpenHashSet Seen = new LongOpenHashSet();
        LongArrayFIFOQueue Queue = new LongArrayFIFOQueue();
        BlockPos Start = Facing == null ? null : Speaker.relative(Facing);
        if (Start == null || !World.isLoaded(Start) || !passable(World, Start, World.getBlockState(Start))) {
            for (Direction Side : Direction.values()) {
                BlockPos Next = Speaker.relative(Side);
                if (World.isLoaded(Next) && passable(World, Next, World.getBlockState(Next))) {
                    Start = Next;
                    break;
                }
            }
        }
        if (Start == null || !World.isLoaded(Start) || !passable(World, Start, World.getBlockState(Start))) {
            return new Result(false, Seen, "boxed in");
        }
        int MinX = Start.getX();
        int MaxX = MinX;
        int MinY = Start.getY();
        int MaxY = MinY;
        int MinZ = Start.getZ();
        int MaxZ = MinZ;
        Seen.add(Start.asLong());
        Queue.enqueue(Start.asLong());
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        while (!Queue.isEmpty()) {
            long Packed = Queue.dequeueLong();
            int X = BlockPos.getX(Packed);
            int Y = BlockPos.getY(Packed);
            int Z = BlockPos.getZ(Packed);
            if (Y >= World.getHeight(Heightmap.Types.MOTION_BLOCKING, X, Z)) {
                return new Result(false, Seen, "open sky at " + (X - Speaker.getX()) + " " + (Y - Speaker.getY()) + " " + (Z - Speaker.getZ()));
            }
            for (Direction Side : Direction.values()) {
                Cursor.set(X + Side.getStepX(), Y + Side.getStepY(), Z + Side.getStepZ());
                long Key = Cursor.asLong();
                if (Seen.contains(Key)) {
                    continue;
                }
                if (Cursor.equals(Speaker)) {
                    continue;
                }
                if (World.isOutsideBuildHeight(Cursor) || !World.isLoaded(Cursor)) {
                    return new Result(false, Seen, "unbounded");
                }
                if (!passable(World, Cursor, World.getBlockState(Cursor))) {
                    continue;
                }
                MinX = Math.min(MinX, Cursor.getX());
                MaxX = Math.max(MaxX, Cursor.getX());
                MinY = Math.min(MinY, Cursor.getY());
                MaxY = Math.max(MaxY, Cursor.getY());
                MinZ = Math.min(MinZ, Cursor.getZ());
                MaxZ = Math.max(MaxZ, Cursor.getZ());
                if (MaxX - MinX + 1 > Footprint || MaxZ - MinZ + 1 > Footprint) {
                    return new Result(false, Seen, "wider than " + Footprint);
                }
                if (MaxY - MinY + 1 > MaxHeight) {
                    return new Result(false, Seen, "taller than " + MaxHeight);
                }
                Seen.add(Key);
                if (Seen.size() > MaxCells) {
                    return new Result(false, Seen, "too large");
                }
                Queue.enqueue(Key);
            }
        }
        return new Result(true, Seen, "enclosed");
    }
}
