package net.rai.scp_unleashed.scp024;

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
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.archive.AnomalySpawners;
import net.rai.scp_unleashed.scp024.plan.Scp024Layouts;
import net.rai.scp_unleashed.scp024.plan.Scp024Mat;
import net.rai.scp_unleashed.scp024.plan.Scp024Plan;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class Scp024Warehouse {
    public static final int SearchAttempts = 40;
    public static final int MaxSlope = 3;
    public static final int LenientSlope = 8;
    public static final int GrowthDepth = 40;
    public static final int CanopyClear = 32;
    public static final int Strict = 0;
    public static final int Lenient = 1;
    public static final int Forced = 2;
    public static final int FoundationDepth = 12;
    public static final int ClearHeight = 14;
    public static final int VisiblePenalty = 6;
    public static final int AheadPenalty = 3;
    private static Scp024Plan Layout;

    private Scp024Warehouse() {
    }

    public static Scp024Plan layout() {
        if (Layout == null) {
            Layout = Scp024Layouts.warehouse();
        }
        return Layout;
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
        return Direction.getNearest(Toward.x - (Site.getX() + 0.5), 0.0, Toward.z - (Site.getZ() + 0.5));
    }

    public static BlockPos doorOrigin(BlockPos Origin, Direction Front) {
        int[] Door = layout().mark("door");
        return toWorld(Origin, Front, Door[0], Door[1], Door[2]);
    }

    private static BlockState rotated(BlockState State, Direction Front) {
        return State.rotate(turnFor(Front));
    }

    public static void build(ServerLevel World, BlockPos Origin, Direction Front) {
        Scp024Plan Plan = layout();
        int[] Low = Plan.low();
        int[] High = Plan.high();
        BlockState Empty = Blocks.AIR.defaultBlockState();
        for (int X = Low[0]; X <= High[0]; X++) {
            for (int Z = Low[2]; Z <= High[2]; Z++) {
                for (int Y = 1; Y <= ClearHeight; Y++) {
                    BlockPos Cell = toWorld(Origin, Front, X, Y, Z);
                    if (!World.getBlockState(Cell).isAir() && !World.isOutsideBuildHeight(Cell)) {
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
        List<Runnable> Later = new ArrayList<>();
        Plan.forEach((X, Y, Z, Mat, Facing, Variant) -> {
            BlockPos Cell = toWorld(Origin, Front, X, Y, Z);
            if (World.isOutsideBuildHeight(Cell)) {
                return;
            }
            BlockState State = rotated(Scp024Palette.state(Mat, Facing, Variant), Front);
            if (Scp024Palette.attached(Mat)) {
                Later.add(() -> World.setBlock(Cell, State, Block.UPDATE_CLIENTS));
            } else {
                World.setBlock(Cell, State, Block.UPDATE_CLIENTS);
            }
            if (Y == 0 && Mat == Scp024Mat.Concrete) {
                BlockState Fill = Scp024Registry.Concrete.get().defaultBlockState();
                for (int Depth = 1; Depth <= FoundationDepth; Depth++) {
                    BlockPos Below = Cell.below(Depth);
                    BlockState There = World.getBlockState(Below);
                    if (World.isOutsideBuildHeight(Below) || There.isSolidRender(World, Below) && There.getFluidState().isEmpty()) {
                        break;
                    }
                    World.setBlock(Below, Fill, Block.UPDATE_CLIENTS);
                }
            }
        });
        Later.forEach(Runnable::run);
    }

    public static Optional<BlockPos> findSite(ServerLevel World, ServerPlayer Reader, int MinDistance, int MaxDistance, RandomSource Random) {
        return findSite(World, Reader, Reader.position(), MinDistance, MaxDistance, Random);
    }

    public static Optional<BlockPos> findSite(ServerLevel World, ServerPlayer Reader, Vec3 Origin, int MinDistance, int MaxDistance, RandomSource Random) {
        return findSite(World, Reader, Origin, MinDistance, MaxDistance, Strict, Random);
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
            if (Spread < 0 || Spread > slopeLimit(Mode) || crowded(World, Site, Front)) {
                continue;
            }
            int Score = Spread * 2;
            Vec3 Top = Vec3.atCenterOf(Site.above(4));
            if (World.clip(new ClipContext(Eye, Top, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Reader)).getType() == HitResult.Type.MISS) {
                Score += VisiblePenalty;
            }
            Vec3 Toward = Top.subtract(Eye).normalize();
            if (Toward.dot(Look) > 0.5) {
                Score += AheadPenalty;
            }
            if (Score < BestScore) {
                BestScore = Score;
                Best = Site;
            }
        }
        return Optional.ofNullable(Best);
    }

    private static int surface(Level World, int X, int Z) {
        return World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z) - 1;
    }

    private static int ground(Level World, int X, int Z) {
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
        int[] Low = layout().low();
        int[] High = layout().high();
        for (int[] Corner : new int[][]{{Low[0], Low[2]}, {High[0], Low[2]}, {Low[0], High[2]}, {High[0], High[2]}, {0, 0}}) {
            BlockPos Probe = toWorld(Column, Front, Corner[0], 0, Corner[1]);
            if (!World.isLoaded(Probe)) {
                return Integer.MIN_VALUE;
            }
        }
        List<Integer> Heights = new ArrayList<>();
        int Wet = 0;
        for (int X = Low[0]; X <= High[0]; X += 4) {
            for (int Z = Low[2]; Z <= High[2]; Z += 4) {
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
        if (Median <= World.getMinBuildHeight() + 2 || Median + ClearHeight >= World.getMaxBuildHeight()) {
            return Integer.MIN_VALUE;
        }
        return Median;
    }

    private static int slope(ServerLevel World, BlockPos Site, Direction Front) {
        int[] Low = layout().low();
        int[] High = layout().high();
        int Min = Integer.MAX_VALUE;
        int Max = Integer.MIN_VALUE;
        for (int X = Low[0] + 1; X <= High[0] - 1; X += 3) {
            for (int Z = Low[2] + 1; Z <= High[2] - 1; Z += 3) {
                BlockPos Probe = toWorld(Site, Front, X, 0, Z);
                int Top = ground(World, Probe.getX(), Probe.getZ());
                Min = Math.min(Min, Top);
                Max = Math.max(Max, Top);
            }
        }
        return Min == Integer.MAX_VALUE ? -1 : Max - Min;
    }

    private static boolean crowded(ServerLevel World, BlockPos Site, Direction Front) {
        int[] Low = layout().low();
        int[] High = layout().high();
        BlockPos A = toWorld(Site, Front, Low[0], -2, Low[2]);
        BlockPos B = toWorld(Site, Front, High[0], ClearHeight, High[2]);
        int MinX = Math.min(A.getX(), B.getX());
        int MaxX = Math.max(A.getX(), B.getX());
        int MinZ = Math.min(A.getZ(), B.getZ());
        int MaxZ = Math.max(A.getZ(), B.getZ());
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

    public static boolean spawn(AnomalySpawners.Discovery Found) {
        Scp024World Data = Scp024World.get(Found.World().getServer());
        if (Data.hasWarehouse()) {
            return true;
        }
        ServerLevel World = Found.World();
        if (World.dimension() != Level.OVERWORLD) {
            return false;
        }
        Direction Front = facing(Found.Pos(), Found.Investigator().position());
        build(World, Found.Pos(), Front);
        Data.recordWarehouse(doorOrigin(Found.Pos(), Front), Front);
        return true;
    }

    public static void place(ServerLevel World, BlockPos Site, Direction Front) {
        build(World, Site, Front);
        Scp024World.get(World.getServer()).recordWarehouse(doorOrigin(Site, Front), Front);
    }

    @Nullable
    public static Direction doorFacing(BlockState State) {
        return State.hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? State.getValue(BlockStateProperties.HORIZONTAL_FACING) : null;
    }
}
