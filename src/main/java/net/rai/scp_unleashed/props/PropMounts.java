package net.rai.scp_unleashed.props;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;

public final class PropMounts {
    private PropMounts() {
    }

    public static BlockPos supportOf(BlockPos Pos, Direction Facing) {
        return Pos.relative(Facing.getOpposite());
    }

    public static boolean onWall(LevelReader World, BlockPos Pos, Direction Facing) {
        BlockPos Support = supportOf(Pos, Facing);
        return World.getBlockState(Support).isFaceSturdy(World, Support, Facing);
    }

    public static boolean underCeiling(LevelReader World, BlockPos Pos) {
        return Block.canSupportCenter(World, Pos.above(), Direction.DOWN);
    }

    @Nullable
    public static Direction wallFacing(BlockPlaceContext Context) {
        for (Direction Side : Context.getNearestLookingDirections()) {
            if (Side.getAxis().isHorizontal() && onWall(Context.getLevel(), Context.getClickedPos(), Side.getOpposite())) {
                return Side.getOpposite();
            }
        }
        return null;
    }
}
