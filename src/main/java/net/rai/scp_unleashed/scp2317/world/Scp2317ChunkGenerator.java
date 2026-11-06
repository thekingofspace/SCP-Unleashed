package net.rai.scp_unleashed.scp2317.world;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.rai.scp_unleashed.scp2317.Scp2317DoorBlock;
import net.rai.scp_unleashed.scp2317.Scp2317Plan;
import net.rai.scp_unleashed.scp2317.Scp2317Registry;
import net.rai.scp_unleashed.scp2317.Scp2317World;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Scp2317ChunkGenerator extends ChunkGenerator {
    public static final MapCodec<Scp2317ChunkGenerator> GeneratorCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(Generator -> Generator.biomeSource)
    ).apply(Instance, Instance.stable(Scp2317ChunkGenerator::new)));

    public Scp2317ChunkGenerator(BiomeSource Source) {
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
        int MaxY = Math.min(Chunk.getMaxBuildHeight(), Scp2317Plan.Height);
        Heightmap Floor = Chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap Surface = Chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        for (int Lx = 0; Lx < 16; Lx++) {
            for (int Lz = 0; Lz < 16; Lz++) {
                int X = Pos.getMinBlockX() + Lx;
                int Z = Pos.getMinBlockZ() + Lz;
                int Highest = -1;
                BlockState TopState = Blocks.AIR.defaultBlockState();
                for (int Y = MinY; Y < MaxY; Y++) {
                    BlockState State = Scp2317Plan.state(X, Y, Z);
                    if (State.isAir() || State.hasBlockEntity()) {
                        continue;
                    }
                    LevelChunkSection Section = Chunk.getSection(Chunk.getSectionIndex(Y));
                    Section.setBlockState(Lx, Y & 15, Lz, State, false);
                    Highest = Y;
                    TopState = State;
                }
                if (Highest >= 0) {
                    Floor.update(Lx, Highest, Lz, TopState);
                    Surface.update(Lx, Highest, Lz, TopState);
                }
            }
        }
        return CompletableFuture.completedFuture(Chunk);
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel Level, ChunkAccess Chunk, StructureManager Structures) {
        if (Scp2317World.standingIn()) {
            return;
        }
        ChunkPos Pos = Chunk.getPos();
        int Flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        BlockPos Vase = Scp2317World.vasePos();
        BlockPos King = Scp2317World.kingPos();
        BlockPos Door = Scp2317World.farDoor();
        if (Pos.equals(new ChunkPos(Vase))) {
            Level.setBlock(Vase, Scp2317Registry.Vase.get().defaultBlockState(), Flags);
        }
        if (Pos.equals(new ChunkPos(King))) {
            Level.setBlock(King, Scp2317Registry.King.get().defaultBlockState(), Flags);
        }
        if (Pos.equals(new ChunkPos(Door))) {
            Scp2317DoorBlock.placeWhole(Level, Door, Scp2317Plan.DoorFront, Scp2317Registry.FarDoor.get(), Flags);
        }
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
        return Scp2317Plan.Height;
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
        return Scp2317Plan.Ground + 1;
    }

    public static BlockState[] states(int X, int Z, int Size) {
        BlockState[] Result = new BlockState[Size];
        Arrays.fill(Result, Blocks.AIR.defaultBlockState());
        for (int Y = 0; Y < Math.min(Size, Scp2317Plan.Height); Y++) {
            Result[Y] = Scp2317Plan.state(X, Y, Z);
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
        Info.add("SCP-2317 desert, surface " + Scp2317Plan.surface(Pos.getX(), Pos.getZ()));
    }
}
