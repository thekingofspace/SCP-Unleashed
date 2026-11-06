package net.rai.scp_unleashed.faction;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class FactionGround {
    public static final int Strict = 0;
    public static final int Lenient = 1;
    public static final int Forced = 2;
    public static final int SearchAttempts = 36;
    public static final int GrowthDepth = 40;
    public static final int PresetAttempts = 64;
    public static final int BorderMargin = 64;

    public record Footprint(int MinX, int MinZ, int MaxX, int MaxZ, int Height, int Slope, int LenientSlope) {
        public int reach() {
            return Math.max(Math.max(-MinX, MaxX), Math.max(-MinZ, MaxZ));
        }
    }

    public record Placement(BlockPos Origin, Direction Front, int Slope) {
    }

    private FactionGround() {
    }

    public static boolean areaLoaded(ServerLevel World, BlockPos Centre, int Radius) {
        return World.hasChunksAt(Centre.getX() - Radius, Centre.getZ() - Radius, Centre.getX() + Radius, Centre.getZ() + Radius);
    }

    public static Rotation turnFor(Direction Front) {
        return switch (Front) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static BlockPos toWorld(BlockPos Origin, Direction Front, int X, int Y, int Z) {
        return Origin.offset(new BlockPos(X, Y, Z).rotate(turnFor(Front)));
    }

    public static Direction facing(BlockPos Site, Vec3 Toward) {
        Direction Found = Direction.getNearest(Toward.x - (Site.getX() + 0.5), 0.0, Toward.z - (Site.getZ() + 0.5));
        return Found.getAxis().isHorizontal() ? Found : Direction.SOUTH;
    }

    public static boolean isDryLand(ServerLevel World, int X, int Z) {
        Holder<Biome> Found = World.getUncachedNoiseBiome(QuartPos.fromBlock(X), QuartPos.fromBlock(World.getSeaLevel()), QuartPos.fromBlock(Z));
        return !Found.is(BiomeTags.IS_OCEAN) && !Found.is(BiomeTags.IS_DEEP_OCEAN) && !Found.is(BiomeTags.IS_RIVER) && !Found.is(BiomeTags.IS_BEACH);
    }

    public static BlockPos preset(ServerLevel World, BlockPos Origin, int MinDistance, int MaxDistance, RandomSource Random, Predicate<BlockPos> Accept) {
        int Loaded = (World.getServer().getPlayerList().getViewDistance() + 2) * 16;
        int Low = Math.max(MinDistance, Loaded);
        int Span = Math.max(16, MaxDistance - MinDistance);
        WorldBorder Border = World.getWorldBorder();
        BlockPos Fallback = null;
        for (int Attempt = 0; Attempt < PresetAttempts; Attempt++) {
            int Reach = Low + Attempt / 16 * 24;
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Distance = Reach + Random.nextDouble() * Span;
            int X = Origin.getX() + Mth.floor(Math.cos(Angle) * Distance);
            int Z = Origin.getZ() + Mth.floor(Math.sin(Angle) * Distance);
            if (!Border.isWithinBounds(X, Z) || Border.getDistanceToBorder(X, Z) < BorderMargin) {
                continue;
            }
            BlockPos Target = new BlockPos(X, World.getSeaLevel(), Z);
            if (World.getChunkSource().hasChunk(X >> 4, Z >> 4) || !Accept.test(Target)) {
                continue;
            }
            if (Fallback == null) {
                Fallback = Target;
            }
            if (isDryLand(World, X, Z)) {
                return Target;
            }
        }
        if (Fallback != null) {
            return Fallback;
        }
        double Angle = Random.nextDouble() * Mth.TWO_PI;
        return new BlockPos(Origin.getX() + Mth.floor(Math.cos(Angle) * Low), World.getSeaLevel(), Origin.getZ() + Mth.floor(Math.sin(Angle) * Low));
    }

    public static boolean isGrowth(BlockState State) {
        if (State.isAir()) {
            return true;
        }
        if (!State.getFluidState().isEmpty()) {
            return false;
        }
        return State.is(BlockTags.LOGS) || State.is(BlockTags.LEAVES) || State.is(BlockTags.REPLACEABLE_BY_TREES) || State.is(BlockTags.REPLACEABLE)
                || State.is(Blocks.BAMBOO) || State.is(Blocks.BAMBOO_SAPLING) || State.is(Blocks.CACTUS) || State.is(Blocks.SUGAR_CANE) || State.is(Blocks.COCOA)
                || State.is(Blocks.MUSHROOM_STEM) || State.is(Blocks.RED_MUSHROOM_BLOCK) || State.is(Blocks.BROWN_MUSHROOM_BLOCK) || State.is(Blocks.SNOW)
                || State.is(Blocks.BEE_NEST) || State.is(BlockTags.FLOWERS) || State.is(BlockTags.SAPLINGS);
    }

    public static boolean madeByHand(BlockState State) {
        return State.is(BlockTags.PLANKS) || State.is(BlockTags.WOOL) || State.is(BlockTags.WOOL_CARPETS) || State.is(BlockTags.BEDS) || State.is(BlockTags.DOORS)
                || State.is(BlockTags.ALL_SIGNS) || State.is(BlockTags.RAILS) || State.is(BlockTags.BANNERS) || State.is(BlockTags.STAIRS) || State.is(BlockTags.SLABS)
                || State.is(BlockTags.WALLS) || State.is(BlockTags.FENCES) || State.is(BlockTags.FENCE_GATES) || State.is(BlockTags.TRAPDOORS) || State.is(BlockTags.CANDLES)
                || State.is(BlockTags.STONE_BRICKS) || State.is(Tags.Blocks.GLASS_BLOCKS) || State.is(Tags.Blocks.GLASS_PANES) || State.is(Blocks.TORCH)
                || State.is(Blocks.WALL_TORCH) || State.is(Blocks.LANTERN) || State.is(Blocks.BRICKS) || State.is(Blocks.CRAFTING_TABLE) || State.is(Blocks.FARMLAND)
                || State.is(Blocks.DIRT_PATH) || State.is(Blocks.COBBLESTONE) || State.is(Blocks.HAY_BLOCK);
    }

    public static int surface(Level World, int X, int Z) {
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

    private static int slopeLimit(Footprint Print, int Mode) {
        return Mode == Strict ? Print.Slope() : Mode == Lenient ? Print.LenientSlope() : Integer.MAX_VALUE;
    }

    private static int wetLimit(int Mode, int Probes) {
        return Mode == Strict ? 0 : Mode == Lenient ? Probes / 4 : Probes / 2;
    }

    public static Optional<Placement> find(ServerLevel World, BlockPos Near, int Spread, Footprint Print, int Mode, RandomSource Random, @Nullable Vec3 FaceToward,
                                           Predicate<BlockState> Forbidden, Predicate<BlockPos> Accept) {
        Placement Best = null;
        int BestScore = Integer.MAX_VALUE;
        for (int Attempt = 0; Attempt < SearchAttempts; Attempt++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Radius = Attempt == 0 ? 0.0 : Math.sqrt(Random.nextDouble()) * Spread;
            int Cx = Near.getX() + Mth.floor(Math.cos(Angle) * Radius);
            int Cz = Near.getZ() + Mth.floor(Math.sin(Angle) * Radius);
            BlockPos Column = new BlockPos(Cx, 0, Cz);
            Direction Front = FaceToward != null ? facing(Column, FaceToward) : Direction.from2DDataValue(Random.nextInt(4));
            int Floor = groundAt(World, Column, Front, Print, Mode);
            if (Floor == Integer.MIN_VALUE) {
                continue;
            }
            BlockPos Site = new BlockPos(Cx, Floor, Cz);
            int Spread0 = slope(World, Site, Front, Print);
            if (Spread0 < 0 || Spread0 > slopeLimit(Print, Mode) || !Accept.test(Site) || crowded(World, Site, Front, Print, Forbidden)) {
                continue;
            }
            int Score = Spread0 * 2 + (int) (Radius / 8.0);
            if (Score < BestScore) {
                BestScore = Score;
                Best = new Placement(Site, Front, Spread0);
            }
        }
        return Optional.ofNullable(Best);
    }

    private static int groundAt(ServerLevel World, BlockPos Column, Direction Front, Footprint Print, int Mode) {
        int[][] Corners = {{Print.MinX(), Print.MinZ()}, {Print.MaxX(), Print.MinZ()}, {Print.MinX(), Print.MaxZ()}, {Print.MaxX(), Print.MaxZ()}, {0, 0}};
        for (int[] Corner : Corners) {
            BlockPos Probe = toWorld(Column, Front, Corner[0], 0, Corner[1]);
            if (!World.isLoaded(Probe)) {
                return Integer.MIN_VALUE;
            }
        }
        List<Integer> Heights = new ArrayList<>();
        int Wet = 0;
        for (int X = Print.MinX(); X <= Print.MaxX(); X += 4) {
            for (int Z = Print.MinZ(); Z <= Print.MaxZ(); Z += 4) {
                BlockPos Probe = toWorld(Column, Front, X, 0, Z);
                int Top = ground(World, Probe.getX(), Probe.getZ());
                BlockPos Surface = new BlockPos(Probe.getX(), Top, Probe.getZ());
                if (!World.getFluidState(Surface).isEmpty() || !World.getFluidState(Surface.above()).isEmpty()) {
                    Wet++;
                }
                Heights.add(Top);
            }
        }
        if (Heights.isEmpty() || Wet > wetLimit(Mode, Heights.size())) {
            return Integer.MIN_VALUE;
        }
        Integer[] Sorted = Heights.toArray(new Integer[0]);
        Arrays.sort(Sorted);
        int Median = Sorted[Sorted.length / 2];
        if (Median <= World.getMinBuildHeight() + 4 || Median + Print.Height() >= World.getMaxBuildHeight()) {
            return Integer.MIN_VALUE;
        }
        return Median;
    }

    private static int slope(ServerLevel World, BlockPos Site, Direction Front, Footprint Print) {
        int Min = Integer.MAX_VALUE;
        int Max = Integer.MIN_VALUE;
        for (int X = Print.MinX() + 1; X <= Print.MaxX() - 1; X += 3) {
            for (int Z = Print.MinZ() + 1; Z <= Print.MaxZ() - 1; Z += 3) {
                BlockPos Probe = toWorld(Site, Front, X, 0, Z);
                int Top = ground(World, Probe.getX(), Probe.getZ());
                Min = Math.min(Min, Top);
                Max = Math.max(Max, Top);
            }
        }
        return Min == Integer.MAX_VALUE ? -1 : Max - Min;
    }

    public static boolean crowded(ServerLevel World, BlockPos Site, Direction Front, Footprint Print, Predicate<BlockState> Forbidden) {
        BlockPos A = toWorld(Site, Front, Print.MinX(), 0, Print.MinZ());
        BlockPos B = toWorld(Site, Front, Print.MaxX(), 0, Print.MaxZ());
        int MinX = Math.min(A.getX(), B.getX());
        int MaxX = Math.max(A.getX(), B.getX());
        int MinZ = Math.min(A.getZ(), B.getZ());
        int MaxZ = Math.max(A.getZ(), B.getZ());
        int Low = Site.getY() - 4;
        int High = Site.getY() + Print.Height();
        for (int ChunkX = MinX >> 4; ChunkX <= MaxX >> 4; ChunkX++) {
            for (int ChunkZ = MinZ >> 4; ChunkZ <= MaxZ >> 4; ChunkZ++) {
                LevelChunk Chunk = World.getChunkSource().getChunkNow(ChunkX, ChunkZ);
                if (Chunk == null) {
                    return true;
                }
                for (BlockEntity Tile : Chunk.getBlockEntities().values()) {
                    BlockPos Pos = Tile.getBlockPos();
                    if (Pos.getX() >= MinX && Pos.getX() <= MaxX && Pos.getZ() >= MinZ && Pos.getZ() <= MaxZ && Pos.getY() >= Low && Pos.getY() <= High) {
                        return true;
                    }
                }
            }
        }
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        for (int X = MinX; X <= MaxX; X += 2) {
            for (int Z = MinZ; Z <= MaxZ; Z += 2) {
                int Top = surface(World, X, Z);
                for (int Y = Math.min(Low, Top - 3); Y <= Math.max(Top + 2, Site.getY() + 6); Y++) {
                    Cursor.set(X, Y, Z);
                    BlockState State = World.getBlockState(Cursor);
                    if (!State.isAir() && (madeByHand(State) || Forbidden.test(State))) {
                        return true;
                    }
                }
            }
        }
        for (int[] Probe : new int[][]{{0, 0}, {Print.MinX(), Print.MinZ()}, {Print.MaxX(), Print.MaxZ()}, {Print.MinX(), Print.MaxZ()}, {Print.MaxX(), Print.MinZ()}}) {
            BlockPos Point = toWorld(Site, Front, Probe[0], 1, Probe[1]);
            if (World.structureManager().getStructureWithPieceAt(Point, StructureTags.VILLAGE).isValid()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public static BlockPos standable(ServerLevel World, BlockPos Centre, int MinRadius, int MaxRadius, RandomSource Random, int Tries) {
        for (int Attempt = 0; Attempt < Tries; Attempt++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Radius = MinRadius + Random.nextDouble() * Math.max(0, MaxRadius - MinRadius);
            int X = Centre.getX() + Mth.floor(Math.cos(Angle) * Radius);
            int Z = Centre.getZ() + Mth.floor(Math.sin(Angle) * Radius);
            if (!World.isLoaded(new BlockPos(X, Centre.getY(), Z))) {
                continue;
            }
            int Top = ground(World, X, Z);
            BlockPos Feet = new BlockPos(X, Top + 1, Z);
            BlockState Under = World.getBlockState(Feet.below());
            if (!Under.isFaceSturdy(World, Feet.below(), Direction.UP) || !World.getFluidState(Feet).isEmpty() || !World.getFluidState(Feet.below()).isEmpty()) {
                continue;
            }
            if (Math.abs(Feet.getY() - Centre.getY()) > 12) {
                continue;
            }
            if (World.getBlockState(Feet).getCollisionShape(World, Feet).isEmpty() && World.getBlockState(Feet.above()).getCollisionShape(World, Feet.above()).isEmpty()) {
                return Feet;
            }
        }
        return null;
    }
}
