package net.rai.scp_unleashed.dclass;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.zoneprops.PairPart;
import net.rai.scp_unleashed.zoneprops.ZonePairedPropBlock;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class DClassBeds {
    public static final TagKey<Block> Beds = BlockTags.create(ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "dclass_beds"));
    public static final float VanillaMattress = 0.5625f;
    public static final float MattressLow = 0.05f;
    public static final float MattressHigh = 0.875f;
    public static final float Headroom = 0.375f;
    public static final double ColumnLow = 0.375;
    public static final double ColumnHigh = 0.625;
    public static final int ScanTicks = 200;
    public static final int ScanRise = 6;
    public static final int ScanCacheLimit = 4096;
    public static final int ClaimTicks = 100;
    public static final int RunLimit = 4;

    private static final Map<ResourceKey<Level>, Map<Long, Scan>> Scans = new HashMap<>();
    private static final Map<ResourceKey<Level>, Map<BlockPos, Claim>> Claims = new HashMap<>();

    public record Spot(BlockPos Head, @Nullable BlockPos Foot, Direction Toward, float Mattress, boolean Vanilla) {
        public List<BlockPos> cells() {
            return Foot == null ? List.of(Head) : List.of(Head, Foot);
        }

        public Vec3 centre() {
            Vec3 Middle = Vec3.atBottomCenterOf(Head);
            return Foot == null ? Middle : Middle.add(Vec3.atBottomCenterOf(Foot)).scale(0.5);
        }

        public double reachSqr(Entity Walker) {
            double Best = Double.MAX_VALUE;
            for (BlockPos Cell : cells()) {
                Best = Math.min(Best, Walker.distanceToSqr(Cell.getX() + 0.5, Cell.getY() + 0.5, Cell.getZ() + 0.5));
            }
            return Best;
        }
    }

    private record Scan(long Stamp, List<BlockPos> Found) {
    }

    private record Claim(UUID Owner, long Stamp) {
    }

    private DClassBeds() {
    }

    @Nullable
    public static Spot spotAt(BlockGetter World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        if (State.getBlock() instanceof BedBlock && State.hasProperty(BedBlock.PART) && State.hasProperty(BedBlock.FACING)) {
            Direction Facing = State.getValue(BedBlock.FACING);
            BlockPos Head = State.getValue(BedBlock.PART) == BedPart.HEAD ? Pos : Pos.relative(Facing);
            return new Spot(Head.immutable(), Head.relative(Facing.getOpposite()).immutable(), Facing, VanillaMattress, true);
        }
        if (!State.is(Beds)) {
            return null;
        }
        BlockPos Head;
        BlockPos Foot;
        Direction Toward;
        if (State.getBlock() instanceof ZonePairedPropBlock Paired && State.hasProperty(ZonePairedPropBlock.Pair)) {
            Toward = Paired.towardExtension(State.getValue(ZonePairedPropBlock.FACING));
            boolean IsHead = State.getValue(ZonePairedPropBlock.Pair) == PairPart.Extension;
            Head = IsHead ? Pos : Pos.relative(Toward);
            Foot = IsHead ? Pos.relative(Toward.getOpposite()) : Pos;
            if (!World.getBlockState(Head).is(State.getBlock()) || !World.getBlockState(Foot).is(State.getBlock())) {
                Foot = null;
                Head = Pos;
            }
        } else {
            Toward = furnitureToward(State);
            if (hasPart(State)) {
                BlockPos[] Pair = pairInRun(World, Pos, State, Toward);
                Head = Pair[0];
                Foot = Pair[1];
            } else {
                Head = Pos;
                Foot = null;
            }
        }
        float Mattress = mattress(World, Head);
        if (Mattress < MattressLow || Mattress > MattressHigh) {
            return null;
        }
        return new Spot(Head.immutable(), Foot == null ? null : Foot.immutable(), Toward, Mattress, false);
    }

    private static Direction furnitureToward(BlockState State) {
        if (State.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return State.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        }
        return Direction.NORTH;
    }

    private static boolean hasPart(BlockState State) {
        for (Property<?> Entry : State.getProperties()) {
            if (Entry instanceof IntegerProperty && Entry.getName().equals("part")) {
                return true;
            }
        }
        return false;
    }

    private static boolean sameBed(BlockGetter World, BlockPos Pos, BlockState State) {
        BlockState Other = World.getBlockState(Pos);
        if (!Other.is(State.getBlock())) {
            return false;
        }
        return !State.hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                || Other.getValue(BlockStateProperties.HORIZONTAL_FACING) == State.getValue(BlockStateProperties.HORIZONTAL_FACING);
    }

    private static BlockPos[] pairInRun(BlockGetter World, BlockPos Pos, BlockState State, Direction Toward) {
        int Ahead = 0;
        while (Ahead < RunLimit && sameBed(World, Pos.relative(Toward, Ahead + 1), State)) {
            Ahead++;
        }
        int Behind = 0;
        while (Behind < RunLimit && sameBed(World, Pos.relative(Toward.getOpposite(), Behind + 1), State)) {
            Behind++;
        }
        if (Ahead + Behind == 0) {
            return new BlockPos[]{Pos, null};
        }
        BlockPos Front = Pos.relative(Toward, Ahead);
        int FromFront = Ahead;
        int Length = Ahead + Behind + 1;
        int PairStart = FromFront - FromFront % 2;
        if (PairStart + 1 >= Length) {
            BlockPos Lone = Front.relative(Toward.getOpposite(), PairStart);
            return new BlockPos[]{Lone, null};
        }
        BlockPos Head = Front.relative(Toward.getOpposite(), PairStart);
        return new BlockPos[]{Head, Head.relative(Toward.getOpposite())};
    }

    public static float mattress(BlockGetter World, BlockPos Pos) {
        List<AABB> Column = column(World.getBlockState(Pos).getShape(World, Pos, CollisionContext.empty()), 0.0);
        BlockPos Above = Pos.above();
        BlockState Over = World.getBlockState(Above);
        if (!Over.isAir()) {
            Column.addAll(column(Over.getCollisionShape(World, Above, CollisionContext.empty()), 1.0));
        }
        List<Double> Surfaces = new ArrayList<>();
        for (AABB Box : Column) {
            if (Box.maxY <= 1.0) {
                Surfaces.add(Box.maxY);
            }
        }
        Surfaces.sort(Double::compare);
        for (double Top : Surfaces) {
            if (Top < MattressLow || Top > MattressHigh) {
                continue;
            }
            boolean Blocked = false;
            for (AABB Box : Column) {
                if (Box.maxY > Top + 1.0e-4 && Box.minY < Top + Headroom) {
                    Blocked = true;
                    break;
                }
            }
            if (!Blocked) {
                return (float) Top;
            }
        }
        return -1.0f;
    }

    private static List<AABB> column(VoxelShape Shape, double Rise) {
        List<AABB> Found = new ArrayList<>();
        for (AABB Box : Shape.toAabbs()) {
            if (Box.maxX > ColumnLow && Box.minX < ColumnHigh && Box.maxZ > ColumnLow && Box.minZ < ColumnHigh) {
                Found.add(Box.move(0.0, Rise, 0.0));
            }
        }
        return Found;
    }

    @Nullable
    public static Spot sleepingSpot(LivingEntity Sleeper) {
        BlockPos Where = Sleeper.getSleepingPos().orElse(null);
        return Where == null ? null : spotAt(Sleeper.level(), Where);
    }

    public static boolean isModBed(LivingEntity Sleeper) {
        Spot Found = sleepingSpot(Sleeper);
        return Found != null && !Found.Vanilla();
    }

    public static List<BlockPos> nearby(ServerLevel World, BlockPos Centre, int Range) {
        long Now = World.getGameTime();
        Map<Long, Scan> Cache = Scans.computeIfAbsent(World.dimension(), Key -> new HashMap<>());
        if (Cache.size() > ScanCacheLimit) {
            Cache.values().removeIf(Entry -> Now - Entry.Stamp() > ScanTicks || Now < Entry.Stamp());
        }
        List<BlockPos> Result = new ArrayList<>();
        int Low = SectionPos.blockToSectionCoord(Centre.getY() - ScanRise);
        int High = SectionPos.blockToSectionCoord(Centre.getY() + ScanRise);
        int ChunkLowX = SectionPos.blockToSectionCoord(Centre.getX() - Range);
        int ChunkHighX = SectionPos.blockToSectionCoord(Centre.getX() + Range);
        int ChunkLowZ = SectionPos.blockToSectionCoord(Centre.getZ() - Range);
        int ChunkHighZ = SectionPos.blockToSectionCoord(Centre.getZ() + Range);
        double Limit = (double) Range * Range;
        for (int ChunkX = ChunkLowX; ChunkX <= ChunkHighX; ChunkX++) {
            for (int ChunkZ = ChunkLowZ; ChunkZ <= ChunkHighZ; ChunkZ++) {
                LevelChunk Chunk = World.getChunkSource().getChunkNow(ChunkX, ChunkZ);
                if (Chunk == null) {
                    continue;
                }
                for (int SectionY = Low; SectionY <= High; SectionY++) {
                    long Key = SectionPos.asLong(ChunkX, SectionY, ChunkZ);
                    Scan Cached = Cache.get(Key);
                    if (Cached == null || Now - Cached.Stamp() > ScanTicks || Now < Cached.Stamp()) {
                        Cached = new Scan(Now, scanSection(Chunk, ChunkX, SectionY, ChunkZ));
                        Cache.put(Key, Cached);
                    }
                    for (BlockPos Found : Cached.Found()) {
                        if (Found.distSqr(Centre) <= Limit && Math.abs(Found.getY() - Centre.getY()) <= ScanRise) {
                            Result.add(Found);
                        }
                    }
                }
            }
        }
        return Result;
    }

    private static List<BlockPos> scanSection(LevelChunk Chunk, int ChunkX, int SectionY, int ChunkZ) {
        int Index = Chunk.getSectionIndexFromSectionY(SectionY);
        if (Index < 0 || Index >= Chunk.getSectionsCount()) {
            return List.of();
        }
        LevelChunkSection Section = Chunk.getSection(Index);
        if (Section.hasOnlyAir() || !Section.maybeHas(State -> State.is(Beds))) {
            return List.of();
        }
        List<BlockPos> Found = new ArrayList<>();
        int BaseX = SectionPos.sectionToBlockCoord(ChunkX);
        int BaseY = SectionPos.sectionToBlockCoord(SectionY);
        int BaseZ = SectionPos.sectionToBlockCoord(ChunkZ);
        for (int Y = 0; Y < 16; Y++) {
            for (int Z = 0; Z < 16; Z++) {
                for (int X = 0; X < 16; X++) {
                    if (Section.getBlockState(X, Y, Z).is(Beds)) {
                        Found.add(new BlockPos(BaseX + X, BaseY + Y, BaseZ + Z));
                    }
                }
            }
        }
        return Found;
    }

    public static void forget(ServerLevel World, BlockPos Pos) {
        Map<Long, Scan> Cache = Scans.get(World.dimension());
        if (Cache != null) {
            Cache.remove(SectionPos.asLong(Pos));
        }
    }

    private static boolean live(ServerLevel World, Claim Held) {
        long Age = World.getGameTime() - Held.Stamp();
        if (Age < 0 || Age > ClaimTicks) {
            return false;
        }
        Entity Owner = World.getEntity(Held.Owner());
        return Owner != null && Owner.isAlive();
    }

    public static boolean heldByOther(ServerLevel World, Spot Bed, Entity Self) {
        Map<BlockPos, Claim> Held = Claims.get(World.dimension());
        if (Held == null) {
            return false;
        }
        for (BlockPos Cell : Bed.cells()) {
            Claim Found = Held.get(Cell);
            if (Found == null) {
                continue;
            }
            if (!live(World, Found)) {
                Held.remove(Cell);
            } else if (!Found.Owner().equals(Self.getUUID())) {
                return true;
            }
        }
        return false;
    }

    public static boolean occupiedByOther(ServerLevel World, Spot Bed, Entity Self) {
        if (Bed.Vanilla()) {
            BlockState Head = World.getBlockState(Bed.Head());
            boolean Taken = Head.getBlock() instanceof BedBlock && Head.getValue(BedBlock.OCCUPIED);
            boolean Mine = Self instanceof LivingEntity Living && Living.getSleepingPos().map(Bed.Head()::equals).orElse(false);
            if (Taken && !Mine) {
                return true;
            }
        }
        AABB Area = new AABB(Bed.Head()).inflate(1.5);
        return !World.getEntitiesOfClass(LivingEntity.class, Area, Other -> Other != Self && Other.isAlive()
                && Other.getSleepingPos().map(Pos -> Bed.cells().contains(Pos)).orElse(false)).isEmpty();
    }

    public static boolean available(ServerLevel World, Spot Bed, Entity Self) {
        Spot Fresh = spotAt(World, Bed.Head());
        return Fresh != null && Fresh.Head().equals(Bed.Head()) && !heldByOther(World, Bed, Self) && !occupiedByOther(World, Bed, Self);
    }

    public static void claim(ServerLevel World, Spot Bed, Entity Self) {
        Map<BlockPos, Claim> Held = Claims.computeIfAbsent(World.dimension(), Key -> new HashMap<>());
        Claim Mine = new Claim(Self.getUUID(), World.getGameTime());
        for (BlockPos Cell : Bed.cells()) {
            Held.put(Cell, Mine);
        }
    }

    public static void release(ServerLevel World, Entity Self) {
        Map<BlockPos, Claim> Held = Claims.get(World.dimension());
        if (Held == null) {
            return;
        }
        Iterator<Map.Entry<BlockPos, Claim>> Walker = Held.entrySet().iterator();
        while (Walker.hasNext()) {
            if (Walker.next().getValue().Owner().equals(Self.getUUID())) {
                Walker.remove();
            }
        }
    }

    public static boolean claimedBy(ServerLevel World, BlockPos Cell, Entity Self) {
        Map<BlockPos, Claim> Held = Claims.get(World.dimension());
        Claim Found = Held == null ? null : Held.get(Cell);
        return Found != null && Found.Owner().equals(Self.getUUID()) && live(World, Found);
    }

    public static void clearAll() {
        Scans.clear();
        Claims.clear();
    }
}
