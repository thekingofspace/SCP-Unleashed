package net.rai.scp_unleashed.scp163;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class Scp163Footing {
    public static final double FenceHeight = 24.0;
    public static final VoxelShape Ledge = Block.box(0.0, 0.0, 0.0, 16.0, 4.0, 16.0);
    public static final int[] Levels = {0, -1, 1};
    public static final double CornerCost = 0.5;
    public static final double LevelCost = 1.0;

    private Scp163Footing() {
    }

    public static boolean isInstrument(BlockState State) {
        return State.getBlock() instanceof Scp163ApparatusBlock || State.getBlock() instanceof Scp163DeviceBlock;
    }

    public static boolean isInstrumentSpot(BlockGetter World, BlockPos Pos) {
        return isInstrument(World.getBlockState(Pos)) || isInstrument(World.getBlockState(Pos.below()));
    }

    public static boolean keepsOff(CollisionContext Context, BlockPos Pos) {
        return Context instanceof EntityCollisionContext Mover && Mover.getEntity() instanceof Scp163Castaway && !Mover.isAbove(Ledge, Pos, false);
    }

    public static boolean isClear(BlockGetter World, BlockPos Pos) {
        if (isInstrumentSpot(World, Pos) || !World.getFluidState(Pos).isEmpty()) {
            return false;
        }
        BlockState Here = World.getBlockState(Pos);
        BlockPos Head = Pos.above();
        if (!Scp163Survey.isPassable(World, Pos, Here) || !Scp163Survey.isPassable(World, Head, World.getBlockState(Head))) {
            return false;
        }
        BlockPos Under = Pos.below();
        return !Here.getCollisionShape(World, Pos).isEmpty() || !World.getBlockState(Under).getCollisionShape(World, Under).isEmpty();
    }

    public static int openness(BlockGetter World, BlockPos Pos) {
        int Open = 0;
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            if (isClear(World, Pos.relative(Side))) {
                Open++;
            }
        }
        return Open;
    }

    @Nullable
    public static BlockPos side(BlockGetter World, BlockPos Instrument, Vec3 From, boolean Corners) {
        BlockPos Best = null;
        double BestScore = Double.MAX_VALUE;
        for (int Dx = -1; Dx <= 1; Dx++) {
            for (int Dz = -1; Dz <= 1; Dz++) {
                boolean Corner = Dx != 0 && Dz != 0;
                if (Dx == 0 && Dz == 0 || Corner && !Corners) {
                    continue;
                }
                for (int Dy : Levels) {
                    BlockPos Cell = Instrument.offset(Dx, Dy, Dz);
                    if (!isClear(World, Cell)) {
                        continue;
                    }
                    if (openness(World, Cell) > 0) {
                        double Score = Vec3.atBottomCenterOf(Cell).distanceTo(From) + (Corner ? CornerCost : 0.0) + Math.abs(Dy) * LevelCost;
                        if (Score < BestScore) {
                            BestScore = Score;
                            Best = Cell;
                        }
                    }
                    break;
                }
            }
        }
        return Best;
    }
}
