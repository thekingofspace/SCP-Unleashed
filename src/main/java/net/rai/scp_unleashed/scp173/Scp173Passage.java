package net.rai.scp_unleashed.scp173;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.door.PanelDoorBlock;
import net.rai.scp_unleashed.door.PanelDoorBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class Scp173Passage {
    public static final double Skin = 1.0e-3;
    public static final double Flat = 1.0e-4;
    public static final double MinBand = 0.05;
    public static final float FullyOpen = 0.999f;
    public static final double PassWidth = 0.6;
    public static final double PassHeight = 1.8;
    private static final AABB[] Nothing = new AABB[0];
    private final Level World;
    @Nullable
    private final Long2ObjectOpenHashMap<AABB[]> Memory;
    private final BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();

    private Scp173Passage(Level World, boolean Remember) {
        this.World = World;
        this.Memory = Remember ? new Long2ObjectOpenHashMap<>() : null;
    }

    public static Scp173Passage of(Level World) {
        return new Scp173Passage(World, false);
    }

    public static Scp173Passage remembering(Level World) {
        return new Scp173Passage(World, true);
    }

    public Level level() {
        return World;
    }

    public static boolean plainDoor(BlockState State) {
        return State.getBlock() instanceof DoorBlock Door && Door.type().canOpenByHand() && State.is(BlockTags.WOODEN_DOORS);
    }

    public static boolean ignores(BlockGetter World, BlockPos Cell, BlockState State) {
        if (plainDoor(State)) {
            return true;
        }
        if (State.getBlock() instanceof PanelDoorBlock Door && State.getValue(PanelDoorBlock.Open)) {
            PanelDoorBlockEntity Master = Door.masterEntity(World, Cell, State);
            return Master == null || Master.getEased() >= FullyOpen;
        }
        return false;
    }

    public static AABB box(Vec3 Where) {
        double Half = PassWidth / 2.0;
        return new AABB(Where.x - Half, Where.y, Where.z - Half, Where.x + Half, Where.y + PassHeight, Where.z + Half);
    }

    private AABB[] partsAt(int X, int Y, int Z) {
        if (Memory == null) {
            return read(X, Y, Z);
        }
        long Key = BlockPos.asLong(X, Y, Z);
        AABB[] Found = Memory.get(Key);
        if (Found == null) {
            Found = read(X, Y, Z);
            Memory.put(Key, Found);
        }
        return Found;
    }

    private AABB[] read(int X, int Y, int Z) {
        Cursor.set(X, Y, Z);
        AABB Whole = new AABB(X, Y, Z, X + 1.0, Y + 1.0, Z + 1.0);
        if (World.isOutsideBuildHeight(Cursor)) {
            return Nothing;
        }
        if (!World.isLoaded(Cursor)) {
            return new AABB[]{Whole};
        }
        BlockState State = World.getBlockState(Cursor);
        if (State.getFluidState().is(FluidTags.LAVA)) {
            return new AABB[]{Whole};
        }
        if (ignores(World, Cursor, State)) {
            return Nothing;
        }
        VoxelShape Shape = State.getCollisionShape(World, Cursor);
        if (Shape.isEmpty()) {
            return Nothing;
        }
        if (Shape == Shapes.block()) {
            return new AABB[]{Whole};
        }
        List<AABB> Parts = Shape.toAabbs();
        AABB[] Placed = new AABB[Parts.size()];
        for (int Index = 0; Index < Placed.length; Index++) {
            Placed[Index] = Parts.get(Index).move(X, Y, Z);
        }
        return Placed;
    }

    private boolean inside(AABB Region) {
        return World.getWorldBorder().isWithinBounds(Region);
    }

    public boolean open(AABB Region) {
        AABB Inner = Region.deflate(Skin);
        if (!inside(Inner)) {
            return false;
        }
        int MinX = floor(Inner.minX);
        int MinY = floor(Inner.minY);
        int MinZ = floor(Inner.minZ);
        int MaxX = floor(Inner.maxX);
        int MaxY = floor(Inner.maxY);
        int MaxZ = floor(Inner.maxZ);
        for (int X = MinX; X <= MaxX; X++) {
            for (int Y = MinY; Y <= MaxY; Y++) {
                for (int Z = MinZ; Z <= MaxZ; Z++) {
                    for (AABB Part : partsAt(X, Y, Z)) {
                        if (Part.intersects(Inner)) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    public boolean sweep(AABB From, AABB To) {
        AABB Span = From.minmax(To).deflate(Skin);
        AABB Start = From.deflate(Skin);
        AABB End = To.deflate(Skin);
        if (!inside(Span)) {
            return false;
        }
        int MinX = floor(Span.minX);
        int MinY = floor(Span.minY);
        int MinZ = floor(Span.minZ);
        int MaxX = floor(Span.maxX);
        int MaxY = floor(Span.maxY);
        int MaxZ = floor(Span.maxZ);
        for (int X = MinX; X <= MaxX; X++) {
            for (int Y = MinY; Y <= MaxY; Y++) {
                for (int Z = MinZ; Z <= MaxZ; Z++) {
                    for (AABB Part : partsAt(X, Y, Z)) {
                        if (Part.intersects(Span) && (Part.intersects(End) || !Part.intersects(Start))) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean viaCorner(Vec3 From, Vec3 Corner, Vec3 To) {
        AABB Middle = box(Corner);
        return sweep(box(From), Middle) && sweep(Middle, box(To));
    }

    @Nullable
    public List<Vec3> route(Vec3 From, Vec3 To) {
        if (sweep(box(From), box(To))) {
            return List.of(To);
        }
        double Rise = To.y - From.y;
        if (Math.abs(Rise) <= Flat) {
            return null;
        }
        Vec3 Lift = new Vec3(From.x, To.y, From.z);
        Vec3 Shift = new Vec3(To.x, From.y, To.z);
        Vec3 First = Rise > 0.0 ? Lift : Shift;
        Vec3 Second = Rise > 0.0 ? Shift : Lift;
        if (viaCorner(From, First, To)) {
            return List.of(First, To);
        }
        if (viaCorner(From, Second, To)) {
            return List.of(Second, To);
        }
        return null;
    }

    public boolean passes(Vec3 From, Vec3 To) {
        return route(From, To) != null;
    }

    public boolean touches(AABB Body, AABB Victim, AABB Reach) {
        if (!Reach.intersects(Victim)) {
            return false;
        }
        AABB Contact = Reach.intersect(Victim);
        double Low = Math.max(Body.minY, Victim.minY);
        double High = Math.min(Body.maxY, Victim.maxY);
        if (High - Low < MinBand) {
            return false;
        }
        AABB Gap = new AABB(Math.min(Body.minX, Contact.minX), Low, Math.min(Body.minZ, Contact.minZ), Math.max(Body.maxX, Contact.maxX), High, Math.max(Body.maxZ, Contact.maxZ));
        return open(Gap);
    }

    public static Vec3 along(Vec3 Here, List<Vec3> Route, double Fraction) {
        double Total = 0.0;
        Vec3 Previous = Here;
        for (Vec3 Point : Route) {
            Total += Point.distanceTo(Previous);
            Previous = Point;
        }
        double Left = Total * Math.max(0.0, Math.min(1.0, Fraction));
        Previous = Here;
        for (Vec3 Point : Route) {
            double Leg = Point.distanceTo(Previous);
            if (Left <= Leg) {
                return Leg < 1.0e-9 ? Point : Previous.add(Point.subtract(Previous).scale(Left / Leg));
            }
            Left -= Leg;
            Previous = Point;
        }
        return Previous;
    }

    private static int floor(double Value) {
        return (int) Math.floor(Value);
    }
}
