package net.rai.scp_unleashed.scp087;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class Scp087Entrance {
    public static final int SearchAttempts = 40;
    public static final int MaxSlope = 2;
    public static final int LenientSlope = 6;
    public static final int GrowthDepth = 40;
    public static final int CanopyClear = 32;
    public static final int Strict = 0;
    public static final int Lenient = 1;
    public static final int Forced = 2;
    public static final int FoundationDepth = 12;
    public static final int ClearHeight = 9;
    public static final int VisiblePenalty = 6;
    public static final int AheadPenalty = 3;
    public static final int HalfWidth = 2;
    public static final int Depth = 4;
    public static final int Roof = 4;
    public static final int ClearLow = -3;
    public static final int ClearHigh = 5;
    public static final int ClearSide = 3;
    public static final int ApronDepth = 2;

    private Scp087Entrance() {
    }

    public static Direction across(Direction Front) {
        return Front.getCounterClockWise();
    }

    public static BlockPos toWorld(BlockPos Origin, Direction Front, int X, int Y, int Z) {
        Direction Side = across(Front);
        return Origin.offset(Side.getStepX() * X - Front.getStepX() * Z, Y, Side.getStepZ() * X - Front.getStepZ() * Z);
    }

    public static int[] toLocal(BlockPos Origin, Direction Front, BlockPos Pos) {
        Direction Side = across(Front);
        int Dx = Pos.getX() - Origin.getX();
        int Dz = Pos.getZ() - Origin.getZ();
        return new int[]{Dx * Side.getStepX() + Dz * Side.getStepZ(), Pos.getY() - Origin.getY(), -(Dx * Front.getStepX() + Dz * Front.getStepZ())};
    }

    public static BlockPos doorLower(BlockPos Origin, Direction Front) {
        return toWorld(Origin, Front, 0, 1, 0);
    }

    public static Direction facing(BlockPos Site, Vec3 Toward) {
        return Direction.getNearest(Toward.x - (Site.getX() + 0.5), 0.0, Toward.z - (Site.getZ() + 0.5));
    }

    private static boolean isShell(int X, int Y, int Z) {
        boolean Inside = Math.abs(X) <= HalfWidth && Z >= 0 && Z <= Depth;
        return Inside && (Y >= 0 && Y <= Roof || Y == Roof + 1 && isRim(X, Z));
    }

    private static boolean isRim(int X, int Z) {
        return Math.abs(X) == HalfWidth || Z == 0 || Z == Depth;
    }

    private static boolean isHollow(int X, int Y, int Z) {
        return Math.abs(X) < HalfWidth && Z > 0 && Z < Depth && Y > 0 && Y < Roof;
    }

    private static boolean isApron(int X, int Y, int Z) {
        return Y == 0 && Math.abs(X) <= 1 && Z < 0 && Z >= -ApronDepth;
    }

    public static boolean isInterior(Level World, BlockPos Pos) {
        if (World.isClientSide || World.getServer() == null) {
            return false;
        }
        Scp087World Data = Scp087World.get(World.getServer());
        BlockPos Door = Data.door();
        if (Door == null || World.dimension() != Data.doorDimension() || Door.distManhattan(Pos) > 16) {
            return false;
        }
        int[] Local = toLocal(Door.below(), Data.front(), Pos);
        return isHollow(Local[0], Local[1], Local[2]);
    }

    public static boolean isStructure(Level World, BlockPos Pos) {
        if (World.isClientSide || World.getServer() == null) {
            return false;
        }
        Scp087World Data = Scp087World.get(World.getServer());
        BlockPos Door = Data.door();
        if (Door == null || World.dimension() != Data.doorDimension() || Door.distManhattan(Pos) > 32) {
            return false;
        }
        int[] Local = toLocal(Door.below(), Data.front(), Pos);
        int X = Local[0];
        int Y = Local[1];
        int Z = Local[2];
        return isShell(X, Y, Z) || isApron(X, Y, Z) || Y < 0 && Y >= -FoundationDepth && (Math.abs(X) <= HalfWidth && Z >= 0 && Z <= Depth || Math.abs(X) <= 1 && Z < 0 && Z >= -ApronDepth);
    }

    public static BlockState stateFor(int X, int Y, int Z) {
        if (isApron(X, Y, Z)) {
            return Scp087Registry.Floor.get().defaultBlockState();
        }
        if (!isShell(X, Y, Z) || isHollow(X, Y, Z)) {
            return Blocks.AIR.defaultBlockState();
        }
        if (Y == 0 || Y == Roof && !isRim(X, Z)) {
            return Scp087Registry.Floor.get().defaultBlockState();
        }
        return Scp087Registry.Concrete.get().defaultBlockState();
    }

    public static void build(ServerLevel World, BlockPos Origin, Direction Front) {
        BlockState Empty = Blocks.AIR.defaultBlockState();
        for (int X = -ClearSide; X <= ClearSide; X++) {
            for (int Z = ClearLow; Z <= ClearHigh; Z++) {
                for (int Y = 1; Y <= ClearHeight; Y++) {
                    BlockPos Cell = toWorld(Origin, Front, X, Y, Z);
                    if (!World.isOutsideBuildHeight(Cell) && !World.getBlockState(Cell).isAir()) {
                        World.setBlock(Cell, Empty, Block.UPDATE_CLIENTS);
                    }
                }
                for (int Y = ClearHeight + 1; Y <= ClearHeight + CanopyClear; Y++) {
                    BlockPos Cell = toWorld(Origin, Front, X, Y, Z);
                    if (World.isOutsideBuildHeight(Cell)) {
                        break;
                    }
                    BlockState There = World.getBlockState(Cell);
                    if (!There.isAir() && isGrowth(There)) {
                        World.setBlock(Cell, Empty, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        for (int X = -HalfWidth; X <= HalfWidth; X++) {
            for (int Z = -ApronDepth; Z <= Depth; Z++) {
                for (int Y = 0; Y <= Roof + 1; Y++) {
                    BlockPos Cell = toWorld(Origin, Front, X, Y, Z);
                    if (World.isOutsideBuildHeight(Cell)) {
                        continue;
                    }
                    BlockState Wanted = stateFor(X, Y, Z);
                    if (Z < 0 && !isApron(X, Y, Z)) {
                        continue;
                    }
                    World.setBlock(Cell, Wanted, Block.UPDATE_CLIENTS);
                    if (Y == 0 && !Wanted.isAir()) {
                        fill(World, Cell);
                    }
                }
            }
        }
        Scp087DoorBlock.place(World, doorLower(Origin, Front), Front, Block.UPDATE_CLIENTS);
    }

    private static void fill(ServerLevel World, BlockPos Cell) {
        BlockState Fill = Scp087Registry.Concrete.get().defaultBlockState();
        for (int Down = 1; Down <= FoundationDepth; Down++) {
            BlockPos Below = Cell.below(Down);
            BlockState There = World.getBlockState(Below);
            if (World.isOutsideBuildHeight(Below) || There.isSolidRender(World, Below) && There.getFluidState().isEmpty()) {
                break;
            }
            World.setBlock(Below, Fill, Block.UPDATE_CLIENTS);
        }
    }

    public static boolean spawn(AnomalySpawners.Discovery Found) {
        Scp087World Data = Scp087World.get(Found.World().getServer());
        if (Data.hasEntrance()) {
            return true;
        }
        ServerLevel World = Found.World();
        if (World.dimension() != Level.OVERWORLD) {
            return false;
        }
        Direction Front = facing(Found.Pos(), Found.Investigator().position());
        place(World, Found.Pos(), Front);
        return true;
    }

    public static void place(ServerLevel World, BlockPos Origin, Direction Front) {
        build(World, Origin, Front);
        Scp087World.get(World.getServer()).recordEntrance(World.dimension(), doorLower(Origin, Front), Front);
    }

    private static int surface(Level World, int X, int Z) {
        return World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z) - 1;
    }

    public static int ground(Level World, int X, int Z) {
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos(X, surface(World, X, Z), Z);
        int Floor = Math.max(World.getMinBuildHeight(), Cursor.getY() - GrowthDepth);
        while (Cursor.getY() > Floor && isGrowth(World.getBlockState(Cursor))) {
            Cursor.move(Direction.DOWN);
        }
        return Cursor.getY();
    }

    public static boolean isGrowth(BlockState State) {
        return State.isAir() || State.is(BlockTags.LOGS) || State.is(BlockTags.LEAVES) || State.is(BlockTags.REPLACEABLE_BY_TREES) || State.is(Blocks.BAMBOO)
                || State.is(Blocks.BAMBOO_SAPLING) || State.is(Blocks.CACTUS) || State.is(Blocks.SUGAR_CANE) || State.is(Blocks.COCOA) || State.is(Blocks.MUSHROOM_STEM)
                || State.is(Blocks.RED_MUSHROOM_BLOCK) || State.is(Blocks.BROWN_MUSHROOM_BLOCK) || State.is(Blocks.SNOW) || State.is(Blocks.BEE_NEST);
    }

    private static int slopeLimit(int Mode) {
        return Mode == Strict ? MaxSlope : Mode == Lenient ? LenientSlope : Integer.MAX_VALUE;
    }

    private static int wetLimit(int Mode, int Probes) {
        return Mode == Strict ? 0 : Mode == Lenient ? Probes / 3 : Probes * 2 / 3;
    }

    private static int groundAt(ServerLevel World, BlockPos Column, Direction Front, int Mode) {
        for (int[] Corner : new int[][]{{-ClearSide, ClearLow}, {ClearSide, ClearLow}, {-ClearSide, ClearHigh}, {ClearSide, ClearHigh}, {0, 0}}) {
            if (!World.isLoaded(toWorld(Column, Front, Corner[0], 0, Corner[1]))) {
                return Integer.MIN_VALUE;
            }
        }
        List<Integer> Heights = new ArrayList<>();
        int Wet = 0;
        for (int X = -HalfWidth; X <= HalfWidth; X += 2) {
            for (int Z = -ApronDepth; Z <= Depth; Z += 2) {
                BlockPos Probe = toWorld(Column, Front, X, 0, Z);
                int Top = ground(World, Probe.getX(), Probe.getZ());
                BlockPos Surface = new BlockPos(Probe.getX(), Top, Probe.getZ());
                if (!World.getFluidState(Surface).isEmpty() || !World.getFluidState(Surface.above()).isEmpty()) {
                    Wet++;
                }
                Heights.add(Top);
            }
        }
        if (Wet > wetLimit(Mode, Heights.size())) {
            return Integer.MIN_VALUE;
        }
        Integer[] Sorted = Heights.toArray(new Integer[0]);
        Arrays.sort(Sorted);
        int Median = Sorted[Sorted.length / 2];
        if (Median <= World.getMinBuildHeight() + FoundationDepth || Median + ClearHeight >= World.getMaxBuildHeight()) {
            return Integer.MIN_VALUE;
        }
        return Median;
    }

    private static int slope(ServerLevel World, BlockPos Site, Direction Front) {
        int Min = Integer.MAX_VALUE;
        int Max = Integer.MIN_VALUE;
        for (int X = -HalfWidth; X <= HalfWidth; X += 2) {
            for (int Z = -ApronDepth; Z <= Depth; Z += 2) {
                BlockPos Probe = toWorld(Site, Front, X, 0, Z);
                int Top = ground(World, Probe.getX(), Probe.getZ());
                Min = Math.min(Min, Top);
                Max = Math.max(Max, Top);
            }
        }
        return Min == Integer.MAX_VALUE ? -1 : Max - Min;
    }

    private static boolean crowded(ServerLevel World, BlockPos Site) {
        int MinX = Site.getX() - ClearHigh - 1;
        int MaxX = Site.getX() + ClearHigh + 1;
        int MinZ = Site.getZ() - ClearHigh - 1;
        int MaxZ = Site.getZ() + ClearHigh + 1;
        for (int ChunkX = MinX >> 4; ChunkX <= MaxX >> 4; ChunkX++) {
            for (int ChunkZ = MinZ >> 4; ChunkZ <= MaxZ >> 4; ChunkZ++) {
                LevelChunk Chunk = World.getChunkSource().getChunkNow(ChunkX, ChunkZ);
                if (Chunk == null) {
                    return true;
                }
                for (BlockEntity Entity : Chunk.getBlockEntities().values()) {
                    BlockPos Pos = Entity.getBlockPos();
                    if (Pos.getX() >= MinX && Pos.getX() <= MaxX && Pos.getZ() >= MinZ && Pos.getZ() <= MaxZ && Pos.getY() >= Site.getY() - 2 && Pos.getY() <= Site.getY() + ClearHeight) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public static Optional<BlockPos> findSite(ServerLevel World, ServerPlayer Reader, Vec3 Origin, int MinDistance, int MaxDistance, int Mode, RandomSource Random) {
        int Low = Math.min(MinDistance, MaxDistance);
        int High = Math.max(MinDistance, MaxDistance);
        Vec3 Eye = Reader.getEyePosition();
        Vec3 Look = Reader.getLookAngle();
        BlockPos Best = null;
        int BestScore = Integer.MAX_VALUE;
        for (int Attempt = 0; Attempt < SearchAttempts; Attempt++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Radius = Low + Random.nextDouble() * (High - Low);
            int Cx = Mth.floor(Origin.x + Math.cos(Angle) * Radius);
            int Cz = Mth.floor(Origin.z + Math.sin(Angle) * Radius);
            BlockPos Column = new BlockPos(Cx, 0, Cz);
            Direction Front = facing(Column, Reader.position());
            int Ground = groundAt(World, Column, Front, Mode);
            if (Ground == Integer.MIN_VALUE) {
                continue;
            }
            BlockPos Site = new BlockPos(Cx, Ground, Cz);
            int Spread = slope(World, Site, Front);
            if (Spread < 0 || Spread > slopeLimit(Mode) || crowded(World, Site)) {
                continue;
            }
            int Score = Spread * 2;
            Vec3 Top = Vec3.atCenterOf(Site.above(3));
            if (World.clip(new ClipContext(Eye, Top, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Reader)).getType() == HitResult.Type.MISS) {
                Score += VisiblePenalty;
            }
            if (Top.subtract(Eye).normalize().dot(Look) > 0.5) {
                Score += AheadPenalty;
            }
            if (Score < BestScore) {
                BestScore = Score;
                Best = Site;
            }
        }
        return Optional.ofNullable(Best);
    }
}
