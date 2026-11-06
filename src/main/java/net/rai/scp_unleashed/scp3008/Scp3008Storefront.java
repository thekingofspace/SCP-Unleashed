package net.rai.scp_unleashed.scp3008;

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
import net.rai.scp_unleashed.scp3008.block.Scp3008EntranceBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008FacadeBlock;
import net.rai.scp_unleashed.scp3008.block.Scp3008FacadeSignBlock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public final class Scp3008Storefront {
    public static final int HalfWidth = 24;
    public static final int Depth = 32;
    public static final int WallTop = 16;
    public static final int RoofY = WallTop + 1;
    public static final int ParapetY = RoofY + 1;
    public static final int BandLow = 13;
    public static final int BandHigh = 14;
    public static final int PlazaHalf = 12;
    public static final int PlazaDepth = 7;
    public static final int CanopyHalf = 5;
    public static final int CanopyY = 5;
    public static final int CanopyDepth = 3;
    public static final int SignLowY = 9;
    public static final int FoundationDepth = 12;
    public static final int ClearHeight = ParapetY + 1;
    public static final int CanopyClear = 24;
    public static final int SearchAttempts = 40;
    public static final int MaxSlope = 3;
    public static final int LenientSlope = 7;
    public static final int GrowthDepth = 40;
    public static final int Strict = 0;
    public static final int Lenient = 1;
    public static final int Forced = 2;
    public static final int VisiblePenalty = 6;
    public static final int AheadPenalty = 3;

    private Scp3008Storefront() {
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

    public static BlockPos door(BlockPos Origin, Direction Front) {
        return toWorld(Origin, Front, 0, 1, 0);
    }

    public static BlockPos originOfDoor(BlockPos Door, Direction Front) {
        return Door.below();
    }

    public static Direction facing(BlockPos Site, Vec3 Toward) {
        return Direction.getNearest(Toward.x - (Site.getX() + 0.5), 0.0, Toward.z - (Site.getZ() + 0.5));
    }

    public static boolean inFootprint(int X, int Z) {
        return X >= -HalfWidth && X < HalfWidth && Z >= 0 && Z < Depth;
    }

    public static boolean inPlaza(int X, int Z) {
        return X >= -PlazaHalf && X < PlazaHalf && Z >= -PlazaDepth && Z < 0;
    }

    public static boolean isWall(int X, int Z) {
        return inFootprint(X, Z) && (X == -HalfWidth || X == HalfWidth - 1 || Z == 0 || Z == Depth - 1);
    }

    public static boolean isEntrance(int X, int Y, int Z) {
        return Z == 0 && X >= -2 && X <= 1 && Y >= 1 && Y <= Scp3008EntranceBlock.Height;
    }

    public static boolean isVestibule(int X, int Y, int Z) {
        return Z == 1 && X >= -3 && X <= 2 && Y >= 1 && Y <= Scp3008EntranceBlock.Height + 1;
    }

    public static boolean isSign(int X, int Y, int Z) {
        return Z == 0 && X >= -Scp3008FacadeSignBlock.Columns / 2 && X < Scp3008FacadeSignBlock.Columns / 2 && Y >= SignLowY && Y < SignLowY + Scp3008FacadeSignBlock.Rows;
    }

    public static boolean isCanopy(int X, int Y, int Z) {
        return Y == CanopyY && X >= -CanopyHalf && X < CanopyHalf && Z >= -CanopyDepth && Z < 0;
    }

    public static BlockState facade(Scp3008FacadeBlock.Finish Finish) {
        return Scp3008Registry.Facade.get().defaultBlockState().setValue(Scp3008FacadeBlock.Surface, Finish);
    }

    public static BlockState stateFor(int X, int Y, int Z, Direction Front) {
        if (Y == 0 && (inFootprint(X, Z) || inPlaza(X, Z))) {
            return facade(Scp3008FacadeBlock.Finish.Paving);
        }
        if (isCanopy(X, Y, Z)) {
            return facade(Scp3008FacadeBlock.Finish.Yellow);
        }
        if (!inFootprint(X, Z)) {
            return null;
        }
        if (Y == RoofY) {
            return facade(Scp3008FacadeBlock.Finish.Roof);
        }
        if (Y == ParapetY) {
            return isWall(X, Z) ? facade(Scp3008FacadeBlock.Finish.Blue) : null;
        }
        if (Y < 1 || Y > WallTop) {
            return null;
        }
        if (isEntrance(X, Y, Z)) {
            int Part = (Scp3008EntranceBlock.Height - Y) * Scp3008EntranceBlock.Width + (X + 2);
            return Scp3008Registry.Entrance.get().defaultBlockState().setValue(Scp3008EntranceBlock.Facing, Front).setValue(Scp3008EntranceBlock.Part, Part);
        }
        if (isVestibule(X, Y, Z)) {
            return facade(Scp3008FacadeBlock.Finish.Plinth);
        }
        if (!isWall(X, Z)) {
            return null;
        }
        if (isSign(X, Y, Z)) {
            int Tile = (SignLowY + Scp3008FacadeSignBlock.Rows - 1 - Y) * Scp3008FacadeSignBlock.Columns + (X + Scp3008FacadeSignBlock.Columns / 2);
            return Scp3008Registry.FacadeSign.get().defaultBlockState().setValue(Scp3008FacadeSignBlock.Facing, Front).setValue(Scp3008FacadeSignBlock.Tile, Tile);
        }
        if (Y == 1) {
            return facade(Scp3008FacadeBlock.Finish.Plinth);
        }
        if (Y >= BandLow && Y <= BandHigh) {
            return facade(Scp3008FacadeBlock.Finish.Yellow);
        }
        return facade(Scp3008FacadeBlock.Finish.Blue);
    }

    public static boolean isStructure(Level World, BlockPos Pos) {
        if (World.isClientSide || World.getServer() == null) {
            return false;
        }
        Scp3008State Data = Scp3008State.get(World.getServer());
        BlockPos Door = Data.door();
        if (Door == null || World.dimension() != Data.doorDimension() || Door.distManhattan(Pos) > 96) {
            return false;
        }
        int[] Local = toLocal(originOfDoor(Door, Data.front()), Data.front(), Pos);
        return Local[1] >= -FoundationDepth && Local[1] <= ParapetY && (inFootprint(Local[0], Local[2]) || inPlaza(Local[0], Local[2]));
    }

    public static void build(ServerLevel World, BlockPos Origin, Direction Front) {
        BlockState Empty = Blocks.AIR.defaultBlockState();
        for (int X = -HalfWidth - 1; X <= HalfWidth; X++) {
            for (int Z = -PlazaDepth - 1; Z <= Depth; Z++) {
                for (int Y = 1; Y <= ClearHeight + CanopyClear; Y++) {
                    BlockPos Cell = toWorld(Origin, Front, X, Y, Z);
                    if (World.isOutsideBuildHeight(Cell)) {
                        break;
                    }
                    BlockState There = World.getBlockState(Cell);
                    if (There.isAir()) {
                        continue;
                    }
                    boolean Inside = inFootprint(X, Z) || inPlaza(X, Z);
                    if (Y <= ClearHeight && (Inside || Y <= 4) || isGrowth(There) || Inside) {
                        World.setBlock(Cell, Empty, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        for (int X = -HalfWidth; X < HalfWidth; X++) {
            for (int Z = -PlazaDepth; Z < Depth; Z++) {
                for (int Y = 0; Y <= ParapetY; Y++) {
                    BlockState Wanted = stateFor(X, Y, Z, Front);
                    if (Wanted == null) {
                        continue;
                    }
                    BlockPos Cell = toWorld(Origin, Front, X, Y, Z);
                    if (World.isOutsideBuildHeight(Cell)) {
                        continue;
                    }
                    World.setBlock(Cell, Wanted, Block.UPDATE_CLIENTS);
                    if (Y == 0) {
                        fill(World, Cell);
                    }
                }
            }
        }
    }

    private static void fill(ServerLevel World, BlockPos Cell) {
        BlockState Fill = facade(Scp3008FacadeBlock.Finish.Plinth);
        for (int Down = 1; Down <= FoundationDepth; Down++) {
            BlockPos Below = Cell.below(Down);
            BlockState There = World.getBlockState(Below);
            if (World.isOutsideBuildHeight(Below) || There.isSolidRender(World, Below) && There.getFluidState().isEmpty()) {
                break;
            }
            World.setBlock(Below, Fill, Block.UPDATE_CLIENTS);
        }
    }

    public static void place(ServerLevel World, BlockPos Origin, Direction Front) {
        build(World, Origin, Front);
        Scp3008State.get(World.getServer()).recordStore(World.dimension(), door(Origin, Front), Front);
    }

    public static boolean spawn(AnomalySpawners.Discovery Found) {
        Scp3008State Data = Scp3008State.get(Found.World().getServer());
        if (Data.hasStore()) {
            return true;
        }
        ServerLevel World = Found.World();
        if (World.dimension() != Level.OVERWORLD) {
            return false;
        }
        place(World, Found.Pos(), facing(Found.Pos(), Found.Investigator().position()));
        return true;
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
        return Mode == Strict ? 0 : Mode == Lenient ? Probes / 4 : Probes * 2 / 3;
    }

    private static int groundAt(ServerLevel World, BlockPos Column, Direction Front, int Mode) {
        for (int[] Corner : new int[][]{{-HalfWidth, -PlazaDepth}, {HalfWidth - 1, -PlazaDepth}, {-HalfWidth, Depth - 1}, {HalfWidth - 1, Depth - 1}, {0, 0}}) {
            if (!World.isLoaded(toWorld(Column, Front, Corner[0], 0, Corner[1]))) {
                return Integer.MIN_VALUE;
            }
        }
        List<Integer> Heights = new ArrayList<>();
        int Wet = 0;
        for (int X = -HalfWidth; X < HalfWidth; X += 6) {
            for (int Z = -PlazaDepth; Z < Depth; Z += 6) {
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
        for (int X = -HalfWidth; X < HalfWidth; X += 6) {
            for (int Z = -PlazaDepth; Z < Depth; Z += 6) {
                BlockPos Probe = toWorld(Site, Front, X, 0, Z);
                int Top = ground(World, Probe.getX(), Probe.getZ());
                Min = Math.min(Min, Top);
                Max = Math.max(Max, Top);
            }
        }
        return Min == Integer.MAX_VALUE ? -1 : Max - Min;
    }

    private static boolean crowded(ServerLevel World, BlockPos Site) {
        int Reach = HalfWidth + Depth;
        int MinX = Site.getX() - Reach;
        int MaxX = Site.getX() + Reach;
        int MinZ = Site.getZ() - Reach;
        int MaxZ = Site.getZ() + Reach;
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
            if (Spread < 0 || Spread > slopeLimit(Mode) || Mode != Forced && crowded(World, Site)) {
                continue;
            }
            int Score = Spread * 2;
            Vec3 Top = Vec3.atCenterOf(Site.above(8));
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
