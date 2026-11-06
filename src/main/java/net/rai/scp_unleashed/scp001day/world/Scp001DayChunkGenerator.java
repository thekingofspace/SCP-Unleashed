package net.rai.scp_unleashed.scp001day.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.storage.loot.LootTable;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp001day.plan.Scp001DayCode;
import net.rai.scp_unleashed.scp001day.plan.Scp001DayDressing;
import net.rai.scp_unleashed.scp001day.plan.Scp001DayPlan;

public class Scp001DayChunkGenerator extends ChunkGenerator {
    public static final MapCodec<Scp001DayChunkGenerator> GeneratorCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(Generator -> Generator.biomeSource)
    ).apply(Instance, Instance.stable(Scp001DayChunkGenerator::new)));
    public static final ResourceKey<LootTable> RoomLoot = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "chests/scp001_day/room"));
    public static final ResourceKey<LootTable> CampLoot = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "chests/scp001_day/camp"));
    public static final ResourceKey<LootTable> SewerLoot = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "chests/scp001_day/sewer"));

    public Scp001DayChunkGenerator(BiomeSource Source) {
        super(Source);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return GeneratorCodec;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender Blending, RandomState Random, StructureManager Structures, ChunkAccess Chunk) {
        ChunkPos Pos = Chunk.getPos();
        int MinY = Math.max(0, Chunk.getMinBuildHeight());
        int MaxY = Math.min(Chunk.getMaxBuildHeight(), Scp001DayPlan.Height);
        Heightmap Floor = Chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap Surface = Chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        int[][] Columns = codes(Pos.x, Pos.z);
        boolean[] Kept = occupied(Chunk);
        for (int Lx = 0; Lx < 16; Lx++) {
            for (int Lz = 0; Lz < 16; Lz++) {
                int[] Column = Columns[Lx * 16 + Lz];
                int Highest = -1;
                for (int Y = MinY; Y < MaxY; Y++) {
                    int Code = Column[Y];
                    if (Code == Scp001DayCode.Air) {
                        continue;
                    }
                    BlockState State = Scp001DayPalette.state(Code);
                    if (State.hasBlockEntity()) {
                        continue;
                    }
                    int Index = Chunk.getSectionIndex(Y);
                    LevelChunkSection Section = Chunk.getSection(Index);
                    if (Kept[Index] && !Section.getBlockState(Lx, Y & 15, Lz).isAir()) {
                        continue;
                    }
                    Section.setBlockState(Lx, Y & 15, Lz, State, false);
                    Highest = Y;
                }
                if (Highest >= 0) {
                    BlockState Top = Scp001DayPalette.state(Column[Highest]);
                    Floor.update(Lx, Highest, Lz, Top);
                    Surface.update(Lx, Highest, Lz, Top);
                }
            }
        }
        return CompletableFuture.completedFuture(Chunk);
    }

    public static boolean[] occupied(ChunkAccess Chunk) {
        boolean[] Found = new boolean[Chunk.getSectionsCount()];
        for (int Index = 0; Index < Found.length; Index++) {
            Found[Index] = !Chunk.getSection(Index).hasOnlyAir();
        }
        return Found;
    }

    public static int[][] codes(int ChunkX, int ChunkZ) {
        return Scp001DayDressing.columns(ChunkX, ChunkZ);
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel Level, ChunkAccess Chunk, StructureManager Structures) {
        ChunkPos Pos = Chunk.getPos();
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        int[][] Columns = codes(Pos.x, Pos.z);
        for (int Lx = 0; Lx < 16; Lx++) {
            for (int Lz = 0; Lz < 16; Lz++) {
                int[] Column = Columns[Lx * 16 + Lz];
                for (int Y = 1; Y < Column.length; Y++) {
                    if (Column[Y] == Scp001DayCode.Air) {
                        continue;
                    }
                    BlockState State = Scp001DayPalette.state(Column[Y]);
                    if (State.hasBlockEntity() && Chunk.getBlockState(Cursor.set(Pos.getMinBlockX() + Lx, Y, Pos.getMinBlockZ() + Lz)).isAir()) {
                        Level.setBlock(Cursor, State, 2);
                    }
                }
            }
        }
        Scp001DayPlan.containers(Pos.x, Pos.z, (X, Y, Z, Kind) -> {
            Cursor.set(X, Y, Z);
            if (!Chunk.getBlockState(Cursor).isAir() || Chunk.getBlockState(Cursor.below()).isAir()) {
                return;
            }
            Direction Facing = Kind == 1 ? Direction.UP : Direction.Plane.HORIZONTAL.getRandomDirection(RandomSource.create(seedOf(X, Y, Z)));
            Level.setBlock(Cursor, Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Facing), 2);
            RandomizableContainer.setBlockEntityLootTable(Level, RandomSource.create(seedOf(X, Y, Z) ^ 0x5DEECE66DL), Cursor.immutable(), lootFor(Kind));
        });
        Scp001DayDressing.dress(Pos.x, Pos.z, (X, Y, Z, Code, Mode) -> {
        }, Item -> Scp001DayExtras.place(Level, Chunk, Item));
    }

    public static ResourceKey<LootTable> lootFor(int Kind) {
        return Kind == 1 ? SewerLoot : (Kind == 2 ? CampLoot : RoomLoot);
    }

    public static long seedOf(int X, int Y, int Z) {
        return ((long) X * 3129871L) ^ ((long) Z * 116129781L) ^ ((long) Y * 0x9E3779B9L);
    }

    @Override
    public void applyCarvers(WorldGenRegion Region, long Seed, RandomState Random, BiomeManager Biomes, StructureManager Structures, ChunkAccess Chunk, GenerationStep.Carving Step) {
    }

    @Override
    public void buildSurface(WorldGenRegion Region, StructureManager Structures, RandomState Random, ChunkAccess Chunk) {
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion Region) {
    }

    @Override
    public int getGenDepth() {
        return Scp001DayPlan.Height;
    }

    @Override
    public int getSeaLevel() {
        return -63;
    }

    @Override
    public int getMinY() {
        return 0;
    }

    @Override
    public int getSpawnHeight(LevelHeightAccessor Level) {
        return Scp001DayPlan.arrivalY();
    }

    public static BlockState[] states(int X, int Z, int Size) {
        int[] Column = new int[Scp001DayPlan.Height];
        Scp001DayPlan.column(X, Z, Column);
        BlockState[] Result = new BlockState[Size];
        Arrays.fill(Result, Blocks.AIR.defaultBlockState());
        for (int Y = 0; Y < Math.min(Size, Column.length); Y++) {
            Result[Y] = Scp001DayPalette.state(Column[Y]);
        }
        return Result;
    }

    @Override
    public int getBaseHeight(int X, int Z, Heightmap.Types Type, LevelHeightAccessor Level, RandomState Random) {
        BlockState[] Column = states(X, Z, Level.getHeight());
        for (int Y = Column.length - 1; Y >= 0; Y--) {
            if (Type.isOpaque().test(Column[Y])) {
                return Level.getMinBuildHeight() + Y + 1;
            }
        }
        return Level.getMinBuildHeight();
    }

    @Override
    public NoiseColumn getBaseColumn(int X, int Z, LevelHeightAccessor Level, RandomState Random) {
        return new NoiseColumn(Level.getMinBuildHeight(), states(X, Z, Level.getHeight()));
    }

    @Override
    public void addDebugScreenInfo(List<String> Info, RandomState Random, BlockPos Pos) {
        Scp001DayPlan.Tower Here = Scp001DayPlan.tower(Scp001DayPlan.cellOf(Pos.getX()), Scp001DayPlan.cellOf(Pos.getZ()));
        Info.add("SCP-001 city lot " + Here.CellX() + ", " + Here.CellZ() + " floors " + Here.Floors() + (Here.Collapse() > 0 ? " collapsed " + Here.Collapse() : ""));
    }
}
