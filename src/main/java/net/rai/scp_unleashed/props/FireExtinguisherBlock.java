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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;
import java.util.Map;

public class FireExtinguisherBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<FireExtinguisherBlock> BlockCodec = simpleCodec(FireExtinguisherBlock::new);
    public static final BooleanProperty Wall = BooleanProperty.create("wall");
    private static final Map<Direction, VoxelShape> WallOutlines = PropShapes.horizontal(PropShapes.of(box(6, 1, 11, 11, 15, 16)));
    private static final Map<Direction, VoxelShape> FloorOutlines = PropShapes.horizontal(PropShapes.of(box(6, 0, 6, 11, 14, 10)));

    public FireExtinguisherBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Wall, true).setValue(PropWater.Waterlogged, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Wall, PropWater.Waterlogged);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Clicked = Context.getClickedFace();
        if (Clicked.getAxis().isHorizontal() && PropMounts.onWall(Context.getLevel(), Context.getClickedPos(), Clicked)) {
            return PropWater.placed(defaultBlockState().setValue(FACING, Clicked).setValue(Wall, true), Context);
        }
        return PropWater.placed(defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite()).setValue(Wall, false), Context);
    }

    @Override
    protected boolean canSurvive(BlockState State, LevelReader World, BlockPos Pos) {
        return !State.getValue(Wall) || PropMounts.onWall(World, Pos, State.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (State.getValue(Wall) && Side == State.getValue(FACING).getOpposite() && !State.canSurvive(World, Pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        PropWater.update(State, World, Pos);
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return (State.getValue(Wall) ? WallOutlines : FloorOutlines).get(State.getValue(FACING));
    }

    @Override
    protected FluidState getFluidState(BlockState State) {
        return PropWater.fluid(State);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        PropTooltips.add(this, Lines, "mount");
    }
}
