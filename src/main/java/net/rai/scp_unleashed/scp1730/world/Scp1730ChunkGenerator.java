package net.rai.scp_unleashed.scp1730.world;

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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.rai.scp_unleashed.scp1730.Scp1730World;
import net.rai.scp_unleashed.scp1730.plan.Scp1730Plan;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Scp1730ChunkGenerator extends ChunkGenerator {
    public static final MapCodec<Scp1730ChunkGenerator> GeneratorCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(Generator -> Generator.biomeSource)
    ).apply(Instance, Instance.stable(Scp1730ChunkGenerator::new)));
    public static final int Height = 256;

    public Scp1730ChunkGenerator(BiomeSource Source) {
        super(Source);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return GeneratorCodec;
    }

    private static int runOf(ChunkPos Pos) {
        int Run = Scp1730World.runAt(Pos.getMinBlockX());
        if (Run < 0 || !Scp1730World.inPlanRange(Run, Pos)) {
            return -1;
        }
        return Run;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender Blending, RandomState Random, StructureManager Structures, ChunkAccess Chunk) {
        ChunkPos Pos = Chunk.getPos();
        int Run = runOf(Pos);
        if (Run < 0) {
            return CompletableFuture.completedFuture(Chunk);
        }
        Scp1730Plan Plan = Scp1730World.plan(Run);
        int Ox = Scp1730World.originX(Run);
        int Oz = Scp1730World.originZ();
        Heightmap Floor = Chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap Surface = Chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        Plan.paint(Pos.getMinBlockX() - Ox, Pos.getMinBlockZ() - Oz, Pos.getMaxBlockX() - Ox, Pos.getMaxBlockZ() - Oz, (X, Y, Z, Text) -> {
            if (Y < Chunk.getMinBuildHeight() || Y >= Chunk.getMaxBuildHeight()) {
                return;
            }
            BlockState State = Scp1730World.state(Text);
            if (State.isAir() || State.hasBlockEntity()) {
                return;
            }
            int Wx = Ox + X;
            int Wz = Oz + Z;
            Chunk.setBlockState(Cursor.set(Wx, Y, Wz), State, false);
            Floor.update(Wx & 15, Y, Wz & 15, State);
            Surface.update(Wx & 15, Y, Wz & 15, State);
        });
        return CompletableFuture.completedFuture(Chunk);
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel Level, ChunkAccess Chunk, StructureManager Structures) {
        ChunkPos Pos = Chunk.getPos();
        int Run = runOf(Pos);
        if (Run < 0) {
            return;
        }
        Scp1730Plan Plan = Scp1730World.plan(Run);
        int Ox = Scp1730World.originX(Run);
        int Oz = Scp1730World.originZ();
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        Plan.paint(Pos.getMinBlockX() - Ox, Pos.getMinBlockZ() - Oz, Pos.getMaxBlockX() - Ox, Pos.getMaxBlockZ() - Oz, (X, Y, Z, Text) -> {
            if (Y < Chunk.getMinBuildHeight() || Y >= Chunk.getMaxBuildHeight()) {
                return;
            }
            BlockState State = Scp1730World.state(Text);
            if (State.hasBlockEntity()) {
                Level.setBlock(Cursor.set(Ox + X, Y, Oz + Z), State, Scp1730World.PaintFlags);
            }
        });
        Scp1730World.decorate(Level, Run, Pos.getMinBlockX(), Pos.getMinBlockZ(), Pos.getMaxBlockX(), Pos.getMaxBlockZ(), Level.getRandom());
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
        return Height;
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
        return 200;
    }

    public static BlockState[] column(int X, int Z, int Size) {
        BlockState[] Column = new BlockState[Size];
        Arrays.fill(Column, Blocks.AIR.defaultBlockState());
        int Run = Scp1730World.runAt(X);
        if (Run < 0 || !Scp1730World.inPlan(Run, X, Z)) {
            return Column;
        }
        int Ox = Scp1730World.originX(Run);
        int Oz = Scp1730World.originZ();
        Scp1730World.plan(Run).paint(X - Ox, Z - Oz, X - Ox, Z - Oz, (Px, Y, Pz, Text) -> {
            if (Y >= 0 && Y < Size) {
                Column[Y] = Scp1730World.state(Text);
            }
        });
        return Column;
    }

    @Override
    public int getBaseHeight(int X, int Z, Heightmap.Types Type, LevelHeightAccessor Level, RandomState Random) {
        BlockState[] Column = column(X, Z, Level.getHeight());
        for (int Y = Column.length - 1; Y >= 0; Y--) {
            if (Type.isOpaque().test(Column[Y])) {
                return Level.getMinBuildHeight() + Y + 1;
            }
        }
        return Level.getMinBuildHeight();
    }

    @Override
    public NoiseColumn getBaseColumn(int X, int Z, LevelHeightAccessor Level, RandomState Random) {
        return new NoiseColumn(Level.getMinBuildHeight(), column(X, Z, Level.getHeight()));
    }

    @Override
    public void addDebugScreenInfo(List<String> Info, RandomState Random, BlockPos Pos) {
        int Run = Scp1730World.runAt(Pos.getX());
        Info.add("SCP-1730 run " + Run + " zone " + Scp1730World.zoneOf(Run, Pos));
    }
}
