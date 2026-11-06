package net.rai.scp_unleashed.scp354;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Scp354Basin {
    public static final double MinRadius = 2.3;
    public static final double MaxRadius = 3.9;
    public static final int MinDepth = 3;
    public static final int MaxDepth = 4;
    public static final int Window = 5;
    public static final int MinWidth = 5;
    public static final int MaxWidth = 8;
    public static final int ShapeAttempts = 16;
    public static final int ClearHeight = 5;
    public static final int CanopyClear = 24;
    public static final int MaxFill = 48;
    public static final int StainRing = 2;
    public static final int GrassRing = 4;
    public static final int GravityRing = 3;
    public static final int ShallowFill = 1;
    public static final int PatchPasses = 4;
    public static final Direction[] Sealed = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};

    private Scp354Basin() {
    }

    public record Plan(int CentreX, int Surface, int CentreZ, Map<Long, Integer> Depths) {
        public Set<BlockPos> liquid() {
            Set<BlockPos> Cells = new LinkedHashSet<>();
            for (Map.Entry<Long, Integer> Entry : Depths.entrySet()) {
                int X = columnX(Entry.getKey());
                int Z = columnZ(Entry.getKey());
                for (int Level = 0; Level < Entry.getValue(); Level++) {
                    Cells.add(new BlockPos(X, Surface - Level, Z));
                }
            }
            return Cells;
        }

        public int radius() {
            int Most = 0;
            for (long Key : Depths.keySet()) {
                Most = Math.max(Most, Math.max(Math.abs(columnX(Key) - CentreX), Math.abs(columnZ(Key) - CentreZ)));
            }
            return Most;
        }

        public int width(boolean AlongX) {
            int Low = Integer.MAX_VALUE;
            int High = Integer.MIN_VALUE;
            for (long Key : Depths.keySet()) {
                int Value = AlongX ? columnX(Key) : columnZ(Key);
                Low = Math.min(Low, Value);
                High = Math.max(High, Value);
            }
            return High - Low + 1;
        }

        public int deepest() {
            int Most = 0;
            for (int Value : Depths.values()) {
                Most = Math.max(Most, Value);
            }
            return Most;
        }

        public BlockPos centre() {
            return new BlockPos(CentreX, Surface, CentreZ);
        }
    }

    public record Result(Plan Shape, Set<BlockPos> Liquid, List<BlockPos> Exposed, int Placed) {
    }

    public static long column(int X, int Z) {
        return ((long) X << 32) | (Z & 0xFFFFFFFFL);
    }

    public static int columnX(long Key) {
        return (int) (Key >> 32);
    }

    public static int columnZ(long Key) {
        return (int) Key;
    }

    public static Plan plan(BlockPos Centre, RandomSource Random) {
        for (int Attempt = 0; Attempt < ShapeAttempts; Attempt++) {
            Plan Made = shape(Centre, Random);
            int Wide = Made.width(true);
            int Deep = Made.width(false);
            if (Wide >= MinWidth && Wide <= MaxWidth && Deep >= MinWidth && Deep <= MaxWidth) {
                return Made;
            }
        }
        Map<Long, Integer> Depths = new LinkedHashMap<>();
        for (int Dx = -2; Dx <= 2; Dx++) {
            for (int Dz = -2; Dz <= 2; Dz++) {
                double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
                if (Distance <= 2.6) {
                    Depths.put(column(Centre.getX() + Dx, Centre.getZ() + Dz), Distance < 1.5 ? MinDepth : 2);
                }
            }
        }
        return new Plan(Centre.getX(), Centre.getY(), Centre.getZ(), Depths);
    }

    private static Plan shape(BlockPos Centre, RandomSource Random) {
        double Base = MinRadius + Random.nextDouble() * (MaxRadius - MinRadius);
        int Depth = MinDepth + Random.nextInt(MaxDepth - MinDepth + 1);
        double PhaseA = Random.nextDouble() * Mth.TWO_PI;
        double PhaseB = Random.nextDouble() * Mth.TWO_PI;
        double PhaseC = Random.nextDouble() * Mth.TWO_PI;
        double Stretch = 0.88 + Random.nextDouble() * 0.24;
        double OffsetX = Random.nextBoolean() ? 0.5 : 0.0;
        double OffsetZ = Random.nextBoolean() ? 0.5 : 0.0;
        Map<Long, Integer> Depths = new LinkedHashMap<>();
        for (int Dx = -Window; Dx <= Window; Dx++) {
            for (int Dz = -Window; Dz <= Window; Dz++) {
                double Px = (Dx - OffsetX) / Stretch;
                double Pz = (Dz - OffsetZ) * Stretch;
                double Angle = Math.atan2(Pz, Px);
                double Edge = Mth.clamp(Base * (1.0 + 0.16 * Math.sin(2.0 * Angle + PhaseA) + 0.1 * Math.sin(3.0 * Angle + PhaseB) + 0.06 * Math.sin(5.0 * Angle + PhaseC)),
                        MinRadius, MaxRadius);
                double Distance = Math.sqrt((Dx - OffsetX) * (Dx - OffsetX) + (Dz - OffsetZ) * (Dz - OffsetZ));
                double Shape = Math.sqrt(Px * Px + Pz * Pz);
                if (Shape > Edge || Distance > MaxRadius + 0.1) {
                    continue;
                }
                double Share = Shape / Edge;
                int Here = Mth.clamp((int) Math.round(Depth * (1.0 - 0.6 * Share * Share) + 0.3), 1, Depth);
                Depths.put(column(Centre.getX() + Dx, Centre.getZ() + Dz), Here);
            }
        }
        return new Plan(Centre.getX(), Centre.getY(), Centre.getZ(), Depths);
    }

    public static Result build(Level World, BlockPos Centre, RandomSource Random) {
        return build(World, plan(Centre, Random), Random);
    }

    public static Result build(Level World, Plan Shape, RandomSource Random) {
        int Surface = Shape.Surface();
        Set<BlockPos> Liquid = Shape.liquid();
        Set<Long> Columns = Shape.Depths().keySet();
        int[] Placed = {0};
        BlockState Empty = Blocks.AIR.defaultBlockState();
        for (long Key : Columns) {
            int X = columnX(Key);
            int Z = columnZ(Key);
            for (int Y = Surface + 1; Y <= Surface + ClearHeight; Y++) {
                BlockPos Cell = new BlockPos(X, Y, Z);
                if (!World.isOutsideBuildHeight(Cell) && !World.getBlockState(Cell).isAir() && World.getBlockEntity(Cell) == null) {
                    set(World, Cell, Empty, Placed);
                }
            }
            for (int Y = Surface + ClearHeight + 1; Y <= Surface + CanopyClear; Y++) {
                BlockPos Cell = new BlockPos(X, Y, Z);
                if (World.isOutsideBuildHeight(Cell)) {
                    break;
                }
                BlockState There = World.getBlockState(Cell);
                if (!There.isAir() && isGrowth(There) && World.getBlockEntity(Cell) == null) {
                    set(World, Cell, Empty, Placed);
                }
            }
        }
        Set<Long> Near = ring(Columns, GravityRing);
        for (long Key : Near) {
            for (int Y = Surface - MaxDepth - 3; Y <= Surface + ClearHeight + 2; Y++) {
                BlockPos Cell = new BlockPos(columnX(Key), Y, columnZ(Key));
                BlockState There = World.getBlockState(Cell);
                if (There.getBlock() instanceof FallingBlock && World.getBlockEntity(Cell) == null) {
                    set(World, Cell, stainFor(There, Random), Placed);
                } else if (!There.getFluidState().isEmpty() && !Scp354Fluid.isRed(There.getFluidState()) && Y <= Surface + 1) {
                    set(World, Cell, Scp354Registry.StainedStone.get().defaultBlockState(), Placed);
                }
            }
        }
        BlockState Red = Scp354Registry.RedLiquid.get().defaultBlockState();
        for (BlockPos Cell : Liquid) {
            if (!World.isOutsideBuildHeight(Cell)) {
                set(World, Cell, Red, Placed);
            }
        }
        for (int Pass = 0; Pass < PatchPasses; Pass++) {
            boolean Changed = false;
            for (BlockPos Cell : List.copyOf(Liquid)) {
                for (Direction Side : Sealed) {
                    BlockPos Next = Cell.relative(Side);
                    if (Liquid.contains(Next) || isSealing(World, Next)) {
                        continue;
                    }
                    if (World.getBlockEntity(Next) != null || World.isOutsideBuildHeight(Next)) {
                        Liquid.remove(Cell);
                        set(World, Cell, Scp354Registry.StainedStone.get().defaultBlockState(), Placed);
                        Changed = true;
                        break;
                    }
                    set(World, Next, rimFor(Next, Surface, Random), Placed);
                    support(World, Next, Surface, Random, Placed);
                    Changed = true;
                }
            }
            if (!Changed) {
                break;
            }
        }
        decorate(World, Shape, Liquid, Random, Placed);
        return new Result(Shape, Liquid, exposed(World, Liquid), Placed[0]);
    }

    private static void support(Level World, BlockPos Top, int Surface, RandomSource Random, int[] Placed) {
        for (int Depth = 1; Depth <= MaxFill; Depth++) {
            BlockPos Cell = Top.below(Depth);
            if (World.isOutsideBuildHeight(Cell) || isSealing(World, Cell) || World.getBlockEntity(Cell) != null) {
                return;
            }
            BlockState Fill = Surface - Cell.getY() <= ShallowFill + MaxDepth ? rimFor(Cell, Surface, Random) : Blocks.STONE.defaultBlockState();
            set(World, Cell, Fill, Placed);
        }
    }

    private static void decorate(Level World, Plan Shape, Set<BlockPos> Liquid, RandomSource Random, int[] Placed) {
        int Surface = Shape.Surface();
        Set<Long> Columns = Shape.Depths().keySet();
        for (BlockPos Cell : Liquid) {
            for (Direction Side : Sealed) {
                BlockPos Next = Cell.relative(Side);
                if (!Liquid.contains(Next)) {
                    stain(World, Next, Random, Placed);
                }
            }
        }
        for (long Key : Columns) {
            for (Direction Side : Direction.Plane.HORIZONTAL) {
                for (int Y = Surface + 1; Y <= Surface + ClearHeight; Y++) {
                    BlockPos Wall = new BlockPos(columnX(Key) + Side.getStepX(), Y, columnZ(Key) + Side.getStepZ());
                    if (!Columns.contains(column(Wall.getX(), Wall.getZ()))) {
                        stain(World, Wall, Random, Placed);
                    }
                }
            }
        }
        Map<Long, Integer> Distance = distances(Columns, GrassRing);
        for (Map.Entry<Long, Integer> Entry : Distance.entrySet()) {
            int Ring = Entry.getValue();
            int X = columnX(Entry.getKey());
            int Z = columnZ(Entry.getKey());
            BlockPos Top = topSolid(World, X, Z, Surface + ClearHeight + 2, Surface - 4);
            if (Top == null) {
                continue;
            }
            double Chance = Ring <= StainRing ? 1.0 : 0.75 * (GrassRing + 1 - Ring) / (double) (GrassRing + 1 - StainRing);
            if (Random.nextDouble() > Chance) {
                continue;
            }
            BlockState There = World.getBlockState(Top);
            BlockPos Above = Top.above();
            BlockState Over = World.getBlockState(Above);
            if (isPlant(Over)) {
                World.destroyBlock(Above, false);
                Placed[0]++;
            }
            if (There.is(Blocks.GRASS_BLOCK) || There.is(Blocks.PODZOL) || There.is(Blocks.MYCELIUM) || There.is(Blocks.MOSS_BLOCK)
                    || Ring <= StainRing && isEarthy(There)) {
                set(World, Top, Ring <= 1 && Random.nextInt(3) == 0 ? Scp354Registry.StainedEarth.get().defaultBlockState()
                        : Scp354Registry.DeadGrass.get().defaultBlockState(), Placed);
            } else if (Ring <= StainRing) {
                stain(World, Top, Random, Placed);
            }
            if (Ring <= StainRing) {
                stain(World, Top.below(), Random, Placed);
            }
        }
    }

    private static Map<Long, Integer> distances(Set<Long> Columns, int Reach) {
        Map<Long, Integer> Found = new LinkedHashMap<>();
        Set<Long> Frontier = new HashSet<>(Columns);
        Set<Long> Seen = new HashSet<>(Columns);
        for (int Step = 1; Step <= Reach; Step++) {
            Set<Long> Next = new HashSet<>();
            for (long Key : Frontier) {
                for (Direction Side : Direction.Plane.HORIZONTAL) {
                    long Around = column(columnX(Key) + Side.getStepX(), columnZ(Key) + Side.getStepZ());
                    if (Seen.add(Around)) {
                        Next.add(Around);
                        Found.put(Around, Step);
                    }
                }
            }
            Frontier = Next;
        }
        return Found;
    }

    private static Set<Long> ring(Set<Long> Columns, int Reach) {
        Set<Long> Around = new HashSet<>(distances(Columns, Reach).keySet());
        Around.addAll(Columns);
        return Around;
    }

    private static BlockPos topSolid(Level World, int X, int Z, int From, int To) {
        for (int Y = From; Y >= To; Y--) {
            BlockPos Cell = new BlockPos(X, Y, Z);
            BlockState There = World.getBlockState(Cell);
            if (Scp354Fluid.isRed(There.getFluidState())) {
                return null;
            }
            if (There.isSolidRender(World, Cell)) {
                return Cell;
            }
        }
        return null;
    }

    private static void stain(Level World, BlockPos Cell, RandomSource Random, int[] Placed) {
        BlockState There = World.getBlockState(Cell);
        if (World.getBlockEntity(Cell) != null || isOwn(There)) {
            return;
        }
        if (isEarthy(There) || isStony(There)) {
            set(World, Cell, stainFor(There, Random), Placed);
        }
    }

    private static BlockState stainFor(BlockState There, RandomSource Random) {
        if (isStony(There)) {
            return Scp354Registry.StainedStone.get().defaultBlockState();
        }
        return Scp354Registry.StainedEarth.get().defaultBlockState();
    }

    private static BlockState rimFor(BlockPos Cell, int Surface, RandomSource Random) {
        int Below = Surface - Cell.getY();
        if (Below <= 1) {
            return Random.nextInt(3) == 0 ? Scp354Registry.StainedStone.get().defaultBlockState() : Scp354Registry.StainedEarth.get().defaultBlockState();
        }
        return Random.nextInt(4) == 0 ? Scp354Registry.StainedEarth.get().defaultBlockState() : Scp354Registry.StainedStone.get().defaultBlockState();
    }

    private static void set(Level World, BlockPos Cell, BlockState State, int[] Placed) {
        if (World.setBlock(Cell, State, Block.UPDATE_CLIENTS)) {
            Placed[0]++;
        }
    }

    public static boolean isOwn(BlockState State) {
        return State.is(Scp354Registry.StainedEarth.get()) || State.is(Scp354Registry.StainedStone.get()) || State.is(Scp354Registry.DeadGrass.get())
                || Scp354Fluid.isRed(State.getFluidState());
    }

    public static boolean isEarthy(BlockState State) {
        return State.is(BlockTags.DIRT) || State.is(BlockTags.SAND) || State.is(Blocks.GRAVEL) || State.is(Blocks.CLAY) || State.is(Blocks.SNOW_BLOCK)
                || State.is(Blocks.FARMLAND) || State.is(Blocks.DIRT_PATH) || State.is(Blocks.SUSPICIOUS_SAND) || State.is(Blocks.SUSPICIOUS_GRAVEL);
    }

    public static boolean isStony(BlockState State) {
        return State.is(BlockTags.BASE_STONE_OVERWORLD) || State.is(Blocks.COBBLESTONE) || State.is(Blocks.MOSSY_COBBLESTONE) || State.is(Blocks.SANDSTONE)
                || State.is(Blocks.RED_SANDSTONE) || State.is(BlockTags.TERRACOTTA) || State.is(Blocks.CALCITE) || State.is(Blocks.SMOOTH_BASALT)
                || State.is(BlockTags.COAL_ORES) || State.is(BlockTags.IRON_ORES) || State.is(BlockTags.COPPER_ORES);
    }

    public static boolean isPlant(BlockState State) {
        return State.is(BlockTags.REPLACEABLE_BY_TREES) || State.is(BlockTags.FLOWERS) || State.is(BlockTags.SAPLINGS) || State.is(Blocks.SWEET_BERRY_BUSH)
                || State.is(Blocks.DEAD_BUSH) || State.is(Blocks.SNOW);
    }

    public static boolean isGrowth(BlockState State) {
        return State.isAir() || State.is(BlockTags.LOGS) || State.is(BlockTags.LEAVES) || State.is(BlockTags.REPLACEABLE_BY_TREES) || State.is(Blocks.BAMBOO)
                || State.is(Blocks.BAMBOO_SAPLING) || State.is(Blocks.CACTUS) || State.is(Blocks.SUGAR_CANE) || State.is(Blocks.COCOA) || State.is(Blocks.MUSHROOM_STEM)
                || State.is(Blocks.RED_MUSHROOM_BLOCK) || State.is(Blocks.BROWN_MUSHROOM_BLOCK) || State.is(Blocks.SNOW) || State.is(Blocks.BEE_NEST)
                || State.is(BlockTags.FLOWERS) || State.is(Blocks.VINE);
    }

    public static boolean isSealing(BlockGetter World, BlockPos Cell) {
        BlockState State = World.getBlockState(Cell);
        return State.isSolidRender(World, Cell) && State.getFluidState().isEmpty() && !isGrowth(State) && !(State.getBlock() instanceof FallingBlock);
    }

    public static List<BlockPos> exposed(BlockGetter World, Collection<BlockPos> Liquid) {
        List<BlockPos> Found = new ArrayList<>();
        for (BlockPos Cell : Liquid) {
            if (!Scp354Fluid.isRed(World.getFluidState(Cell))) {
                continue;
            }
            for (Direction Side : Sealed) {
                BlockPos Next = Cell.relative(Side);
                BlockState There = World.getBlockState(Next);
                if (Scp354Fluid.isRed(There.getFluidState())) {
                    continue;
                }
                if (!There.isSolidRender(World, Next)) {
                    Found.add(Cell);
                    break;
                }
            }
        }
        return Found;
    }

    public static Set<BlockPos> scan(BlockGetter World, BlockPos Centre, int Radius, int Down, int Up) {
        Set<BlockPos> Found = new LinkedHashSet<>();
        for (BlockPos Cell : BlockPos.betweenClosed(Centre.offset(-Radius, -Down, -Radius), Centre.offset(Radius, Up, Radius))) {
            if (Scp354Fluid.isRed(World.getFluidState(Cell))) {
                Found.add(Cell.immutable());
            }
        }
        return Found;
    }
}
