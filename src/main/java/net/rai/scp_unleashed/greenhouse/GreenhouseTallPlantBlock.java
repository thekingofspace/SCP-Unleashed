package net.rai.scp_unleashed.greenhouse;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.props.PropShapes;
import net.rai.scp_unleashed.props.PropTooltips;
import net.rai.scp_unleashed.props.PropWater;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

import static net.rai.scp_unleashed.props.PropShapes.box;

public class GreenhouseTallPlantBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock, LargeBlock {
    public static final MapCodec<GreenhouseTallPlantBlock> BlockCodec = simpleCodec(GreenhouseTallPlantBlock::new);
    public static final EnumProperty<DoubleBlockHalf> Half = BlockStateProperties.DOUBLE_BLOCK_HALF;
    private static final VoxelShape LowerShape = PropShapes.of(box(4, 0, 4, 12, 10, 12), box(2, 10, 2, 14, 16, 14));
    private static final VoxelShape UpperShape = PropShapes.of(box(2, 0, 2, 14, 16, 14));

    private final Map<Direction, VoxelShape> LowerOutlines;
    private final Map<Direction, VoxelShape> UpperOutlines;

    public GreenhouseTallPlantBlock(Properties Properties) {
        super(Properties);
        this.LowerOutlines = PropShapes.horizontal(LowerShape);
        this.UpperOutlines = PropShapes.horizontal(UpperShape);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Half, DoubleBlockHalf.LOWER).setValue(PropWater.Waterlogged, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Half, PropWater.Waterlogged);
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        BlockPos Pos = Context.getClickedPos();
        return List.of(Pos, Pos.above());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            return null;
        }
        return PropWater.placed(defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite()), Context);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        BlockPos Above = Pos.above();
        BlockState Placed = State.setValue(Half, DoubleBlockHalf.UPPER).setValue(PropWater.Waterlogged, World.getFluidState(Above).is(Fluids.WATER));
        World.setBlock(Above, Placed, Block.UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        PropWater.update(State, World, Pos);
        DoubleBlockHalf ThisHalf = State.getValue(Half);
        if (Side.getAxis() == Direction.Axis.Y && (ThisHalf == DoubleBlockHalf.LOWER) == (Side == Direction.UP)) {
            return Neighbour.is(this) && Neighbour.getValue(Half) != ThisHalf ? State : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, Neighbour, World, Pos, NeighbourPos);
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        if (!World.isClientSide && User.isCreative() && State.getValue(Half) == DoubleBlockHalf.UPPER) {
            BlockPos LowerPos = Pos.below();
            BlockState LowerState = World.getBlockState(LowerPos);
            if (LowerState.is(this) && LowerState.getValue(Half) == DoubleBlockHalf.LOWER) {
                World.setBlock(LowerPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                World.levelEvent(User, 2001, LowerPos, Block.getId(LowerState));
            }
        }
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return (State.getValue(Half) == DoubleBlockHalf.LOWER ? LowerOutlines : UpperOutlines).get(State.getValue(FACING));
    }

    @Override
    protected FluidState getFluidState(BlockState State) {
        return PropWater.fluid(State);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        PropTooltips.add(this, Lines);
    }
}
