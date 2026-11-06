package net.rai.scp_unleashed.scp143;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.List;

public final class Scp143Trees {
    public static final long OriginalSeed = 143L;
    public static final int ClearRadius = 13;
    public static final int ClearBelow = 2;
    public static final int ClearAbove = 22;
    public static final double CoreRadius = 3.6;
    public static final double StepRadius = 5.4;
    public static final double MoundRadius = 7.0;
    public static final int FillDepth = 8;
    public static final int GrowthDepth = 40;
    private static List<Scp143Shape.Cell> Original;
    private static int[] OriginalBox;

    private Scp143Trees() {
    }

    public static List<Scp143Shape.Cell> original() {
        if (Original == null) {
            Original = Scp143Shape.grand(OriginalSeed);
            OriginalBox = Scp143Shape.bounds(Original);
        }
        return Original;
    }

    public static int[] originalBounds() {
        original();
        return OriginalBox;
    }

    public static boolean withinOriginal(BlockPos Base, BlockPos Pos) {
        int[] Box = originalBounds();
        int Dx = Pos.getX() - Base.getX();
        int Dy = Pos.getY() - Base.getY();
        int Dz = Pos.getZ() - Base.getZ();
        return Dx >= Box[0] - 1 && Dx <= Box[3] + 1 && Dy >= Box[1] - 1 && Dy <= Box[4] + 1 && Dz >= Box[2] - 1 && Dz <= Box[5] + 1;
    }

    public static BlockState stateFor(Scp143Shape.Cell Cell) {
        Direction.Axis Axis = Cell.Axis() == Scp143Shape.AxisX ? Direction.Axis.X : Cell.Axis() == Scp143Shape.AxisZ ? Direction.Axis.Z : Direction.Axis.Y;
        if (Cell.Kind() == Scp143Shape.Leaves) {
            return Scp143Registry.Leaves.get().defaultBlockState().setValue(LeavesBlock.DISTANCE, Math.max(1, Math.min(6, Cell.Distance())))
                    .setValue(LeavesBlock.PERSISTENT, false);
        }
        Block Kind = Cell.Kind() == Scp143Shape.Wood ? Scp143Registry.Wood.get() : Scp143Registry.Log.get();
        return Kind.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Axis);
    }

    public static boolean isGrowth(BlockState State) {
        return State.isAir() || State.is(BlockTags.LOGS) || State.is(BlockTags.LEAVES) || State.is(BlockTags.REPLACEABLE_BY_TREES) || State.is(BlockTags.FLOWERS)
                || State.is(Blocks.BAMBOO) || State.is(Blocks.BAMBOO_SAPLING) || State.is(Blocks.CACTUS) || State.is(Blocks.SUGAR_CANE) || State.is(Blocks.COCOA)
                || State.is(Blocks.MUSHROOM_STEM) || State.is(Blocks.RED_MUSHROOM_BLOCK) || State.is(Blocks.BROWN_MUSHROOM_BLOCK) || State.is(Blocks.SNOW)
                || State.is(Blocks.BEE_NEST) || State.is(BlockTags.SAPLINGS);
    }

    private static boolean isCompetition(BlockState State) {
        if (State.getBlock() instanceof Scp143LeavesBlock || Scp143Registry.isBladewood(State.getBlock())) {
            return false;
        }
        return State.is(BlockTags.LOGS) || State.is(BlockTags.LEAVES) || State.is(Blocks.VINE) || State.is(Blocks.BEE_NEST) || State.is(Blocks.COCOA)
                || State.is(Blocks.MUSHROOM_STEM) || State.is(Blocks.RED_MUSHROOM_BLOCK) || State.is(Blocks.BROWN_MUSHROOM_BLOCK) || State.is(Blocks.BAMBOO);
    }

    public static int ground(Level World, int X, int Z) {
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos(X, World.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, X, Z) - 1, Z);
        int Floor = Math.max(World.getMinBuildHeight(), Cursor.getY() - GrowthDepth);
        while (Cursor.getY() > Floor && isGrowth(World.getBlockState(Cursor))) {
            Cursor.move(Direction.DOWN);
        }
        return Cursor.getY();
    }

    public static void placeGrand(ServerLevel World, BlockPos Base) {
        clearCompetition(World, Base);
        prepareGround(World, Base);
        int Flags = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE;
        for (Scp143Shape.Cell Cell : original()) {
            if (Cell.Kind() == Scp143Shape.Leaves) {
                continue;
            }
            BlockPos Pos = Base.offset(Cell.X(), Cell.Y(), Cell.Z());
            if (!World.isOutsideBuildHeight(Pos) && World.getBlockState(Pos).getDestroySpeed(World, Pos) >= 0.0f) {
                World.setBlock(Pos, stateFor(Cell), Flags);
            }
        }
        for (Scp143Shape.Cell Cell : original()) {
            if (Cell.Kind() != Scp143Shape.Leaves) {
                continue;
            }
            BlockPos Pos = Base.offset(Cell.X(), Cell.Y(), Cell.Z());
            if (World.isOutsideBuildHeight(Pos)) {
                continue;
            }
            BlockState There = World.getBlockState(Pos);
            if (There.isAir() || There.canBeReplaced() || isGrowth(There) && !There.is(BlockTags.LOGS)) {
                World.setBlock(Pos, stateFor(Cell), Flags);
            }
        }
    }

    private static void clearCompetition(ServerLevel World, BlockPos Base) {
        BlockPos.MutableBlockPos Cursor = new BlockPos.MutableBlockPos();
        BlockState Empty = Blocks.AIR.defaultBlockState();
        for (int Dx = -ClearRadius; Dx <= ClearRadius + 1; Dx++) {
            for (int Dz = -ClearRadius; Dz <= ClearRadius + 1; Dz++) {
                if ((Dx - 0.5) * (Dx - 0.5) + (Dz - 0.5) * (Dz - 0.5) > (ClearRadius + 0.5) * (ClearRadius + 0.5)) {
                    continue;
                }
                for (int Dy = -ClearBelow; Dy <= ClearAbove; Dy++) {
                    Cursor.set(Base.getX() + Dx, Base.getY() + Dy, Base.getZ() + Dz);
                    if (World.isOutsideBuildHeight(Cursor)) {
                        continue;
                    }
                    if (isCompetition(World.getBlockState(Cursor))) {
                        World.setBlock(Cursor, Empty, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
                    }
                }
            }
        }
    }

    private static void prepareGround(ServerLevel World, BlockPos Base) {
        double Cx = Base.getX() + 1.0;
        double Cz = Base.getZ() + 1.0;
        int Reach = (int) Math.ceil(MoundRadius) + 1;
        BlockState Grass = Blocks.GRASS_BLOCK.defaultBlockState();
        BlockState Dirt = Blocks.DIRT.defaultBlockState();
        BlockState Empty = Blocks.AIR.defaultBlockState();
        int Flags = Block.UPDATE_CLIENTS;
        for (int Dx = -Reach; Dx <= Reach + 1; Dx++) {
            for (int Dz = -Reach; Dz <= Reach + 1; Dz++) {
                int X = Base.getX() + Dx;
                int Z = Base.getZ() + Dz;
                double Distance = Math.hypot(X + 0.5 - Cx, Z + 0.5 - Cz);
                if (Distance > MoundRadius) {
                    continue;
                }
                int Ground = ground(World, X, Z);
                int Target = Distance <= CoreRadius ? Base.getY() - 1 : Distance <= StepRadius ? Base.getY() - 2 : Base.getY() - 3;
                if (Ground > Target && Distance <= CoreRadius) {
                    for (int Y = Target + 1; Y <= Ground; Y++) {
                        World.setBlock(new BlockPos(X, Y, Z), Empty, Flags);
                    }
                    World.setBlock(new BlockPos(X, Target, Z), Grass, Flags);
                } else if (Ground < Target) {
                    int Bottom = Math.max(Ground + 1, Target - FillDepth);
                    for (int Y = Bottom; Y < Target; Y++) {
                        World.setBlock(new BlockPos(X, Y, Z), Dirt, Flags);
                    }
                    World.setBlock(new BlockPos(X, Target, Z), Grass, Flags);
                } else if (Distance <= CoreRadius) {
                    World.setBlock(new BlockPos(X, Target, Z), Grass, Flags);
                }
                if (Distance <= 2.5) {
                    for (int Y = Math.max(Ground, Target) + 1; Y <= Base.getY() + 1; Y++) {
                        BlockPos Above = new BlockPos(X, Y, Z);
                        if (isGrowth(World.getBlockState(Above)) && !World.getBlockState(Above).isAir()) {
                            World.setBlock(Above, Empty, Flags);
                        }
                    }
                }
            }
        }
    }

    public static boolean growYoung(ServerLevel World, BlockPos Pos, long Seed) {
        List<Scp143Shape.Cell> Cells = Scp143Shape.young(Seed);
        for (Scp143Shape.Cell Cell : Cells) {
            BlockPos At = Pos.offset(Cell.X(), Cell.Y(), Cell.Z());
            if (World.isOutsideBuildHeight(At) || !World.isLoaded(At)) {
                return false;
            }
            if (At.equals(Pos)) {
                continue;
            }
            BlockState There = World.getBlockState(At);
            boolean Free = There.isAir() || There.is(BlockTags.REPLACEABLE_BY_TREES) || There.is(BlockTags.LEAVES) && Cell.Kind() == Scp143Shape.Leaves;
            if (!Free) {
                return false;
            }
        }
        BlockPos Below = Pos.below();
        if (World.getBlockState(Below).is(BlockTags.DIRT) && !World.getBlockState(Below).is(Blocks.MUD)) {
            World.setBlock(Below, Blocks.DIRT.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        World.setBlock(Pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_NONE);
        for (Scp143Shape.Cell Cell : Cells) {
            if (Cell.Kind() != Scp143Shape.Leaves) {
                World.setBlock(Pos.offset(Cell.X(), Cell.Y(), Cell.Z()), stateFor(Cell), Block.UPDATE_ALL);
            }
        }
        for (Scp143Shape.Cell Cell : Cells) {
            if (Cell.Kind() == Scp143Shape.Leaves) {
                BlockPos At = Pos.offset(Cell.X(), Cell.Y(), Cell.Z());
                if (World.getBlockState(At).isAir() || World.getBlockState(At).is(BlockTags.REPLACEABLE_BY_TREES)) {
                    World.setBlock(At, stateFor(Cell), Block.UPDATE_ALL);
                }
            }
        }
        World.playSound(null, Pos, Scp143Registry.Grow.get(), SoundSource.BLOCKS, 1.0f, 0.9f + World.getRandom().nextFloat() * 0.2f);
        return true;
    }
}
