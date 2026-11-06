package net.rai.scp_unleashed.scp076;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.PoiTypeTags;
import net.minecraft.tags.StructureTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import net.rai.scp_unleashed.archive.AnomalySpawners;

import java.util.Optional;

public final class Scp076Vault {
    public static final int Half = 4;
    public static final int Top = 8;
    public static final int Inner = 3;
    public static final int FoundationDepth = 24;
    public static final int Margin = 1;
    public static final int SearchAttempts = 64;
    public static final int StrictSlope = 2;
    public static final int LooseSlope = 4;
    public static final int PoiClearance = 40;
    public static final long KilledSeal = 168000L;
    public static final long ReturnSeal = 24000L;

    private Scp076Vault() {
    }

    public static Rotation turnFor(Direction Front) {
        return switch (Front) {
            case WEST -> Rotation.CLOCKWISE_90;
            case NORTH -> Rotation.CLOCKWISE_180;
            case EAST -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };
    }

    public static BlockPos toWorld(BlockPos Centre, Direction Front, int X, int Y, int Z) {
        return Centre.offset(new BlockPos(X, Y, Z).rotate(turnFor(Front)));
    }

    public static BlockPos doorOrigin(BlockPos Centre, Direction Front) {
        return toWorld(Centre, Front, -1, 1, Half);
    }

    public static BlockPos centreFromDoor(BlockPos Origin, Direction Front) {
        return Origin.relative(Scp076DoorBlock.across(Front), 1).below().relative(Front.getOpposite(), Half);
    }

    public static BlockPos coffinPos(BlockPos Centre) {
        return Centre.above();
    }

    public static AABB interior(BlockPos Centre) {
        return new AABB(Centre.getX() - Inner, Centre.getY() + 1, Centre.getZ() - Inner, Centre.getX() + Inner + 1, Centre.getY() + Top, Centre.getZ() + Inner + 1);
    }

    public static AABB doorway(BlockPos Origin, Direction Front) {
        BlockPos Far = Scp076DoorBlock.partPos(Origin, Front, Scp076DoorBlock.Columns - 1, Scp076DoorBlock.Rows - 1);
        return new AABB(Vec3.atLowerCornerOf(Origin), Vec3.atLowerCornerOf(Far).add(1.0, 1.0, 1.0));
    }

    public static Direction facing(BlockPos Site, Vec3 Toward) {
        Direction Found = Direction.getNearest(Toward.x - (Site.getX() + 0.5), 0.0, Toward.z - (Site.getZ() + 0.5));
        return Found.getAxis().isHorizontal() ? Found : Direction.SOUTH;
    }

    public static void build(ServerLevel World, BlockPos Centre, Direction Front) {
        BlockState Stone = Scp076Registry.VaultStone.get().defaultBlockState();
        BlockState Empty = Blocks.AIR.defaultBlockState();
        for (int X = -Half; X <= Half; X++) {
            for (int Z = -Half; Z <= Half; Z++) {
                for (int Y = 0; Y <= Top; Y++) {
                    BlockPos Cell = toWorld(Centre, Front, X, Y, Z);
                    if (World.isOutsideBuildHeight(Cell)) {
                        continue;
                    }
                    boolean Shell = Math.abs(X) == Half || Math.abs(Z) == Half || Y == 0 || Y == Top;
                    World.setBlock(Cell, Shell ? Stone : Empty, Block.UPDATE_CLIENTS);
                }
                for (int Depth = 1; Depth <= FoundationDepth; Depth++) {
                    BlockPos Below = toWorld(Centre, Front, X, -Depth, Z);
                    BlockState There = World.getBlockState(Below);
                    if (World.isOutsideBuildHeight(Below) || There.isSolidRender(World, Below) && There.getFluidState().isEmpty()) {
                        break;
                    }
                    World.setBlock(Below, Stone, Block.UPDATE_CLIENTS);
                }
            }
        }
        BlockPos Origin = doorOrigin(Centre, Front);
        clearPorch(World, Centre, Front);
        Scp076DoorBlock.place(World, Origin, Front);
        Scp076CoffinBlock.place(World, coffinPos(Centre), Front);
        if (World.getBlockEntity(Origin) instanceof Scp076DoorBlockEntity Door) {
            Door.bindCoffin(coffinPos(Centre));
        }
        Scp076Vaults.get(World.getServer()).record(World.dimension(), Origin, Front, Centre);
    }

    public static boolean naturalGround(BlockState State) {
        return State.is(BlockTags.DIRT) || State.is(BlockTags.SAND) || State.is(BlockTags.BASE_STONE_OVERWORLD) || State.is(BlockTags.TERRACOTTA)
                || State.is(Blocks.GRAVEL) || State.is(Blocks.CLAY) || State.is(Blocks.SNOW_BLOCK) || State.is(Blocks.POWDER_SNOW) || State.is(Blocks.SANDSTONE)
                || State.is(Blocks.RED_SANDSTONE) || State.is(Blocks.PACKED_ICE) || State.is(Blocks.CALCITE) || State.is(Blocks.TUFF) || State.is(BlockTags.COAL_ORES)
                || State.is(BlockTags.IRON_ORES) || State.is(BlockTags.COPPER_ORES);
    }

    public static boolean naturalCover(BlockState State) {
        if (State.isAir()) {
            return true;
        }
        if (!State.getFluidState().isEmpty()) {
            return false;
        }
        return State.is(BlockTags.REPLACEABLE) || State.is(BlockTags.FLOWERS) || State.is(BlockTags.SAPLINGS) || State.is(BlockTags.LEAVES) || State.is(Blocks.SNOW)
                || State.is(Blocks.SWEET_BERRY_BUSH) || State.is(Blocks.MOSS_CARPET) || State.is(Blocks.PINK_PETALS) || State.is(Blocks.BROWN_MUSHROOM)
                || State.is(Blocks.RED_MUSHROOM) || State.is(Blocks.CACTUS) || State.is(Blocks.SUGAR_CANE) || naturalGround(State);
    }

    public static boolean acceptableBiome(Holder<Biome> Found) {
        return !Found.is(BiomeTags.IS_OCEAN) && !Found.is(BiomeTags.IS_DEEP_OCEAN) && !Found.is(BiomeTags.IS_RIVER) && !Found.is(BiomeTags.IS_BEACH)
                && !Found.is(Tags.Biomes.IS_SWAMP) && !Found.is(Tags.Biomes.IS_MOUNTAIN_PEAK) && !Found.is(Tags.Biomes.IS_MUSHROOM);
    }

    private static int surface(ServerLevel World, int X, int Z) {
        return World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z) - 1;
    }

    private static boolean loaded(ServerLevel World, BlockPos Centre) {
        int Reach = Half + Margin + 1;
        for (int ChunkX = (Centre.getX() - Reach) >> 4; ChunkX <= (Centre.getX() + Reach) >> 4; ChunkX++) {
            for (int ChunkZ = (Centre.getZ() - Reach) >> 4; ChunkZ <= (Centre.getZ() + Reach) >> 4; ChunkZ++) {
                if (World.getChunkSource().getChunkNow(ChunkX, ChunkZ) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    public static int score(ServerLevel World, BlockPos Column, int MaxSlope) {
        if (!loaded(World, Column) || !acceptableBiome(World.getBiome(Column.atY(World.getSeaLevel())))) {
            return -1;
        }
        int Reach = Half + Margin;
        int Low = Integer.MAX_VALUE;
        int High = Integer.MIN_VALUE;
        for (int X = -Reach; X <= Reach; X++) {
            for (int Z = -Reach; Z <= Reach; Z++) {
                int Px = Column.getX() + X;
                int Pz = Column.getZ() + Z;
                int Height = surface(World, Px, Pz);
                BlockPos Ground = new BlockPos(Px, Height, Pz);
                BlockState Top = World.getBlockState(Ground);
                if (!Top.getFluidState().isEmpty() || !World.getFluidState(Ground.above()).isEmpty() || !naturalGround(Top) && !naturalCover(Top)) {
                    return -1;
                }
                Low = Math.min(Low, Height);
                High = Math.max(High, Height);
            }
        }
        if (High - Low > MaxSlope) {
            return -1;
        }
        return (High - Low) * 4;
    }

    public static int floorLevel(ServerLevel World, BlockPos Column) {
        int Sum = 0;
        int Count = 0;
        for (int X = -Half; X <= Half; X++) {
            for (int Z = -Half; Z <= Half; Z++) {
                Sum += surface(World, Column.getX() + X, Column.getZ() + Z);
                Count++;
            }
        }
        return Mth.floor(Sum / (double) Count + 0.5);
    }

    public static boolean clearVolume(ServerLevel World, BlockPos Centre) {
        if (World.isOutsideBuildHeight(Centre) || World.isOutsideBuildHeight(Centre.above(Top + 1))) {
            return false;
        }
        for (int X = -Half - Margin; X <= Half + Margin; X++) {
            for (int Z = -Half - Margin; Z <= Half + Margin; Z++) {
                boolean Footprint = Math.abs(X) <= Half && Math.abs(Z) <= Half;
                for (int Y = -2; Y <= Top + 1; Y++) {
                    BlockPos Cell = Centre.offset(X, Y, Z);
                    BlockState State = World.getBlockState(Cell);
                    if (!State.getFluidState().isEmpty()) {
                        return false;
                    }
                    if ((Footprint || Y >= 0) && !naturalCover(State)) {
                        return false;
                    }
                    if (World.getBlockEntity(Cell) != null) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static boolean settled(ServerLevel World, BlockPos Centre) {
        PoiManager Points = World.getPoiManager();
        if (Points.getCountInRange(Type -> Type.is(PoiTypeTags.VILLAGE) || Type.is(PoiTypes.NETHER_PORTAL) || Type.is(PoiTypes.LODESTONE), Centre, PoiClearance,
                PoiManager.Occupancy.ANY) > 0) {
            return true;
        }
        for (int Dx = -2; Dx <= 2; Dx++) {
            for (int Dz = -2; Dz <= 2; Dz++) {
                BlockPos Probe = Centre.offset(Dx * 16, 0, Dz * 16);
                if (World.getChunkSource().getChunkNow(Probe.getX() >> 4, Probe.getZ() >> 4) != null
                        && World.structureManager().getStructureWithPieceAt(Probe, StructureTags.VILLAGE).isValid()) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean occupied(ServerLevel World, BlockPos Centre) {
        AABB Box = new AABB(Centre.getX() - Half - 2, Centre.getY() - 1, Centre.getZ() - Half - 2, Centre.getX() + Half + 3, Centre.getY() + Top + 2, Centre.getZ() + Half + 3);
        return !World.getEntitiesOfClass(LivingEntity.class, Box, LivingEntity::isAlive).isEmpty();
    }

    public static void clearPorch(ServerLevel World, BlockPos Centre, Direction Front) {
        for (int X = -1; X <= 1; X++) {
            for (int Z = Half + 1; Z <= Half + 3; Z++) {
                for (int Y = 1; Y <= 4; Y++) {
                    BlockPos Cell = toWorld(Centre, Front, X, Y, Z);
                    BlockState State = World.getBlockState(Cell);
                    if (!State.isAir() && State.getFluidState().isEmpty() && naturalCover(State) && World.getBlockEntity(Cell) == null) {
                        World.setBlock(Cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                    }
                }
            }
        }
    }

    public static Optional<BlockPos> findSite(ServerLevel World, BlockPos Around, int Radius, int MaxSlope, RandomSource Random) {
        BlockPos Best = null;
        int BestScore = Integer.MAX_VALUE;
        for (int Attempt = 0; Attempt < SearchAttempts; Attempt++) {
            double Angle = Random.nextDouble() * Mth.TWO_PI;
            double Distance = Attempt == 0 ? 0.0 : Math.sqrt(Random.nextDouble()) * Radius;
            BlockPos Column = new BlockPos(Around.getX() + Mth.floor(Math.cos(Angle) * Distance), 0, Around.getZ() + Mth.floor(Math.sin(Angle) * Distance));
            int Score = score(World, Column, MaxSlope);
            if (Score < 0) {
                continue;
            }
            Score += (int) (Distance / 8.0);
            if (Score >= BestScore) {
                continue;
            }
            BlockPos Centre = Column.atY(floorLevel(World, Column));
            if (!clearVolume(World, Centre) || settled(World, Centre) || occupied(World, Centre)) {
                continue;
            }
            Best = Centre;
            BestScore = Score;
        }
        return Optional.ofNullable(Best);
    }

    public static boolean spawn(AnomalySpawners.Discovery Found) {
        ServerLevel World = Found.World();
        Scp076Vaults Data = Scp076Vaults.get(World.getServer());
        if (Data.nearest(World.dimension(), Found.Pos(), 96).isPresent()) {
            return true;
        }
        if (World.dimension() != Level.OVERWORLD) {
            return false;
        }
        BlockPos Centre = Found.Pos();
        if (!clearVolume(World, Centre) || settled(World, Centre) || occupied(World, Centre)) {
            Optional<BlockPos> Other = findSite(World, Centre, 24, LooseSlope, World.getRandom());
            if (Other.isEmpty()) {
                return false;
            }
            Centre = Other.get();
        }
        build(World, Centre, facing(Centre, Found.Investigator().position()));
        return true;
    }

    public static void abelGone(MinecraftServer Server, ResourceKey<Level> Dimension, BlockPos Door, boolean Killed) {
        long Ticks = Killed ? KilledSeal : ReturnSeal;
        ServerLevel World = Server.getLevel(Dimension);
        if (World != null && World.isLoaded(Door) && World.getBlockEntity(Door) instanceof Scp076DoorBlockEntity Found) {
            Found.reseal(Ticks);
            return;
        }
        Scp076Vaults.get(Server).requestReseal(Dimension, Door, Ticks);
    }
}
