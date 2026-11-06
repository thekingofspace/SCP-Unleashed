package net.rai.scp_unleashed.scp224;

import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class Scp224Room {
    public static final int CellCap = 2000;
    public static final double OpenRadius = 16.0;
    public static final double FullSpan = 0.99;
    public static final double WallHeight = 0.75;

    private final BlockPos Lower;
    private final LongOpenHashSet Cells;
    private final boolean Open;
    private final int Doors;
    private final AABB Bounds;
    private final long Made;

    private Scp224Room(BlockPos Lower, LongOpenHashSet Cells, boolean Open, int Doors, AABB Bounds, long Made) {
        this.Lower = Lower;
        this.Cells = Cells;
        this.Open = Open;
        this.Doors = Doors;
        this.Bounds = Bounds;
        this.Made = Made;
    }

    public boolean isOpen() {
        return Open;
    }

    public boolean isEnclosed() {
        return !Open;
    }

    public int size() {
        return Cells.size();
    }

    public int doors() {
        return Doors;
    }

    public long made() {
        return Made;
    }

    public AABB bounds() {
        return Bounds;
    }

    public boolean touches(BlockPos Pos) {
        return Bounds.inflate(1.5).contains(Vec3.atCenterOf(Pos));
    }

    public boolean contains(BlockPos Pos) {
        if (Open) {
            return Vec3.atCenterOf(Pos).distanceToSqr(centre()) <= OpenRadius * OpenRadius;
        }
        return Cells.contains(Pos.asLong());
    }

    public boolean contains(Entity Target) {
        if (Open) {
            return Target.getEyePosition().distanceToSqr(centre()) <= OpenRadius * OpenRadius;
        }
        return Cells.contains(Target.blockPosition().asLong()) || Cells.contains(BlockPos.containing(Target.getEyePosition()).asLong());
    }

    private Vec3 centre() {
        return new Vec3(Lower.getX() + 0.5, Lower.getY() + 1.0, Lower.getZ() + 0.5);
    }

    public static boolean isDoor(BlockState State) {
        return State.hasProperty(BlockStateProperties.OPEN);
    }

    public static boolean passable(Level World, BlockPos Pos, BlockState State) {
        if (State.isAir()) {
            return true;
        }
        if (State.hasProperty(BlockStateProperties.OPEN)) {
            return State.getValue(BlockStateProperties.OPEN);
        }
        VoxelShape Shape = State.getCollisionShape(World, Pos);
        if (Shape.isEmpty()) {
            return true;
        }
        AABB Box = Shape.bounds();
        boolean SpansX = Box.getXsize() >= FullSpan;
        boolean SpansZ = Box.getZsize() >= FullSpan;
        if (SpansX && SpansZ) {
            return false;
        }
        return !(Box.getYsize() >= WallHeight && (SpansX || SpansZ));
    }

    public static Scp224Room measure(Level World, BlockPos Lower, long Now) {
        BlockPos Upper = Lower.above();
        LongOpenHashSet Seen = new LongOpenHashSet();
        LongOpenHashSet Cells = new LongOpenHashSet();
        LongArrayFIFOQueue Queue = new LongArrayFIFOQueue();
        Seen.add(Lower.asLong());
        Seen.add(Upper.asLong());
        boolean Open = false;
        int Doors = 0;
        int MinX = Lower.getX();
        int MinY = Lower.getY();
        int MinZ = Lower.getZ();
        int MaxX = MinX;
        int MaxY = Upper.getY();
        int MaxZ = MinZ;
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (BlockPos Part : new BlockPos[]{Lower, Upper}) {
            for (Direction Side : Direction.values()) {
                long Key = BlockPos.offset(Part.asLong(), Side);
                if (Seen.add(Key)) {
                    Queue.enqueue(Key);
                }
            }
        }
        while (!Queue.isEmpty()) {
            long Key = Queue.dequeueLong();
            Cursor.set(Key);
            if (World.isOutsideBuildHeight(Cursor) || !World.isLoaded(Cursor)) {
                Open = true;
                break;
            }
            BlockState State = World.getBlockState(Cursor);
            if (isDoor(State)) {
                Doors++;
            }
            if (!passable(World, Cursor, State)) {
                continue;
            }
            Cells.add(Key);
            MinX = Math.min(MinX, Cursor.getX());
            MinY = Math.min(MinY, Cursor.getY());
            MinZ = Math.min(MinZ, Cursor.getZ());
            MaxX = Math.max(MaxX, Cursor.getX());
            MaxY = Math.max(MaxY, Cursor.getY());
            MaxZ = Math.max(MaxZ, Cursor.getZ());
            if (Cells.size() >= CellCap) {
                Open = true;
                break;
            }
            for (Direction Side : Direction.values()) {
                long Next = BlockPos.offset(Key, Side);
                if (Seen.add(Next)) {
                    Queue.enqueue(Next);
                }
            }
        }
        AABB Bounds = new AABB(MinX, MinY, MinZ, MaxX + 1, MaxY + 1, MaxZ + 1);
        return new Scp224Room(Lower.immutable(), Cells, Open, Doors, Bounds, Now);
    }
}
