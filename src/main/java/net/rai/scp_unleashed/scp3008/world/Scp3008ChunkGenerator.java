package net.rai.scp_unleashed.scp3008.world;

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
import net.rai.scp_unleashed.scp3008.Scp3008Department;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Canvas;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Code;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Painter;
import net.rai.scp_unleashed.scp3008.plan.Scp3008Plan;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class Scp3008ChunkGenerator extends ChunkGenerator {
    public static final MapCodec<Scp3008ChunkGenerator> GeneratorCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(Generator -> Generator.biomeSource)
    ).apply(Instance, Instance.stable(Scp3008ChunkGenerator::new)));
    public static final int Height = 384;

    public Scp3008ChunkGenerator(BiomeSource Source) {
        super(Source);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() {
        return GeneratorCodec;
    }

    private static final class ChunkCanvas implements Scp3008Canvas {
        private final ChunkAccess Chunk;
        private final int MinX;
        private final int MinZ;
        private final Heightmap Floor;
        private final Heightmap Surface;
        private final BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();

        private ChunkCanvas(ChunkAccess Chunk) {
            this.Chunk = Chunk;
            ChunkPos Pos = Chunk.getPos();
            this.MinX = Pos.getMinBlockX();
            this.MinZ = Pos.getMinBlockZ();
            this.Floor = Chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
            this.Surface = Chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        }

        @Override
        public int minX() {
            return MinX;
        }

        @Override
        public int minZ() {
            return MinZ;
        }

        @Override
        public int maxX() {
            return MinX + 15;
        }

        @Override
        public int maxZ() {
            return MinZ + 15;
        }

        @Override
        public void put(int X, int Y, int Z, int Code) {
            if (Scp3008Code.decor(Code) || Y < Chunk.getMinBuildHeight() || Y >= Chunk.getMaxBuildHeight()) {
                return;
            }
            BlockState State = Scp3008Palette.state(Code);
            if (State == null) {
                return;
            }
            Chunk.setBlockState(Cursor.set(X, Y, Z), State, false);
            if (Y >= Scp3008Plan.CeilingY) {
                Floor.update(X & 15, Y, Z & 15, State);
                Surface.update(X & 15, Y, Z & 15, State);
            }
        }
    }

    private static final class ColumnCanvas implements Scp3008Canvas {
        private final int X;
        private final int Z;
        private final BlockState[] Column;

        private ColumnCanvas(int X, int Z, BlockState[] Column) {
            this.X = X;
            this.Z = Z;
            this.Column = Column;
        }

        @Override
        public int minX() {
            return X;
        }

        @Override
        public int minZ() {
            return Z;
        }

        @Override
        public int maxX() {
            return X;
        }

        @Override
        public int maxZ() {
            return Z;
        }

        @Override
        public void put(int X0, int Y, int Z0, int Code) {
            if (Y < 0 || Y >= Column.length) {
                return;
            }
            BlockState State = Scp3008Code.decor(Code) ? null : Scp3008Palette.state(Code);
            if (State != null) {
                Column[Y] = State;
            }
        }
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender Blending, RandomState Random, StructureManager Structures, ChunkAccess Chunk) {
        ChunkCanvas Canvas = new ChunkCanvas(Chunk);
        Scp3008Painter.paintArea(Canvas.minX(), Canvas.minZ(), Canvas.maxX(), Canvas.maxZ(), Canvas);
        return CompletableFuture.completedFuture(Chunk);
    }

    @Override
    public void applyBiomeDecoration(WorldGenLevel Level, ChunkAccess Chunk, StructureManager Structures) {
        Scp3008Furnishing.decorate(Level, Chunk);
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
        return Scp3008Plan.Stand;
    }

    public static BlockState[] column(int X, int Z, int Size) {
        BlockState[] Column = new BlockState[Size];
        java.util.Arrays.fill(Column, Blocks.AIR.defaultBlockState());
        ColumnCanvas Canvas = new ColumnCanvas(X, Z, Column);
        Scp3008Painter.paintArea(X, Z, X, Z, Canvas);
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
        int Ux = Scp3008Plan.unitX(Pos.getX());
        int Uz = Scp3008Plan.unitZ(Pos.getZ());
        Info.add("SCP-3008 department: " + Scp3008Department.of(Scp3008Plan.cachedDepartment(Ux, Uz)).Key + " block " + Ux + ", " + Uz);
    }
}
