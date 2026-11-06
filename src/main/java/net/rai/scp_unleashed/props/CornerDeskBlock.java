package net.rai.scp_unleashed.props;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Map;

public class CornerDeskBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<CornerDeskBlock> BlockCodec = simpleCodec(CornerDeskBlock::new);
    private static final Map<Direction, VoxelShape> Outlines = PropShapes.horizontal(PropShapes.of(
            box(0, 14, 0, 16, 16, 16), box(0, 4, 14, 14, 14, 15), box(14, 4, 0, 15, 14, 14), box(14, 0, 14, 16, 14, 16)));

    public CornerDeskBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PropWater.Waterlogged, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, PropWater.Waterlogged);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return PropWater.placed(defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite()), Context);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines.get(State.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        PropWater.update(State, World, Pos);
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    protected FluidState getFluidState(BlockState State) {
        return PropWater.fluid(State);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        PropTooltips.add(this, Lines, "joins");
    }
}
