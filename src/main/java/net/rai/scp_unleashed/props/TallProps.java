package net.rai.scp_unleashed.props;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class TallProps {
    public static final EnumProperty<DoubleBlockHalf> Half = BlockStateProperties.DOUBLE_BLOCK_HALF;

    private TallProps() {
    }

    public static List<BlockPos> footprint(BlockPlaceContext Context) {
        BlockPos Pos = Context.getClickedPos();
        return List.of(Pos, Pos.above());
    }

    public static BlockPos lowerPos(BlockState State, BlockPos Pos) {
        return State.getValue(Half) == DoubleBlockHalf.UPPER ? Pos.below() : Pos;
    }

    @Nullable
    public static BlockState partnerCheck(Block Owner, BlockState State, Direction Side, BlockState NeighborState) {
        DoubleBlockHalf ThisHalf = State.getValue(Half);
        if (Side.getAxis() == Direction.Axis.Y && (ThisHalf == DoubleBlockHalf.LOWER) == (Side == Direction.UP)) {
            boolean Partner = NeighborState.is(Owner) && NeighborState.getValue(Half) != ThisHalf;
            return Partner ? State : Blocks.AIR.defaultBlockState();
        }
        return null;
    }

    public static void placeUpper(Level World, BlockPos Pos, BlockState State) {
        World.setBlock(Pos.above(), State.setValue(Half, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    public static void creativeBreak(Block Owner, Level World, BlockPos Pos, BlockState State, Player User) {
        if (World.isClientSide || !User.isCreative() || State.getValue(Half) != DoubleBlockHalf.UPPER) {
            return;
        }
        BlockPos LowerPos = Pos.below();
        BlockState LowerState = World.getBlockState(LowerPos);
        if (LowerState.is(Owner) && LowerState.getValue(Half) == DoubleBlockHalf.LOWER) {
            World.setBlock(LowerPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            World.levelEvent(User, 2001, LowerPos, Block.getId(LowerState));
        }
    }
}
