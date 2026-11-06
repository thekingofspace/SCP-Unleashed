package net.rai.scp_unleashed.utility;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

public final class DecorPipes {
    public static final double[][] StraightParts = {
            {4, 4, 0, 12, 12, 16},
            {3, 3, 0, 13, 13, 1},
            {3, 3, 15, 13, 13, 16}
    };

    private DecorPipes() {
    }

    public static boolean opensToward(BlockState State, Direction Side) {
        if (State.getBlock() instanceof DecorPipeBlock) {
            return State.getValue(DecorPipeBlock.Along) == Side.getAxis();
        }
        if (State.getBlock() instanceof DecorPipeElbowBlock) {
            return State.getValue(DecorPipeElbowBlock.Orientation).opensTo(Side);
        }
        if (State.getBlock() instanceof DecorPipeValveBlock) {
            return State.getValue(DecorPipeValveBlock.Orientation).getAlong() == Side.getAxis();
        }
        return false;
    }

    public static boolean pipeAt(BlockGetter World, BlockPos Pos, Direction Side) {
        return opensToward(World.getBlockState(Pos.relative(Side)), Side.getOpposite());
    }

    public static int[] axisTurn(Direction.Axis Along) {
        return switch (Along) {
            case X -> new int[]{0, 90};
            case Y -> new int[]{90, 0};
            default -> new int[]{0, 0};
        };
    }
}
