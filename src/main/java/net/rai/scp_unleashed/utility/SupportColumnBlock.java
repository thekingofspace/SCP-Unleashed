package net.rai.scp_unleashed.utility;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class SupportColumnBlock extends UtilityWaterloggedBlock {
    public static final MapCodec<SupportColumnBlock> BlockCodec = simpleCodec(SupportColumnBlock::new);
    public static final BooleanProperty Up = BlockStateProperties.UP;
    public static final BooleanProperty Down = BlockStateProperties.DOWN;
    private static final VoxelShape Core = Block.box(2, 0, 2, 14, 16, 14);
    private static final VoxelShape Capital = Block.box(0, 14, 0, 16, 16, 16);
    private static final VoxelShape Footing = Block.box(0, 0, 0, 16, 2, 16);
    private static final VoxelShape[] ShapeByEnds = new VoxelShape[4];

    static {
        for (int Index = 0; Index < ShapeByEnds.length; Index++) {
            VoxelShape Shape = Core;
            if ((Index & 1) == 0) {
                Shape = Shapes.or(Shape, Capital);
            }
            if ((Index & 2) == 0) {
                Shape = Shapes.or(Shape, Footing);
            }
            ShapeByEnds[Index] = Shape.optimize();
        }
    }

    public SupportColumnBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(defaultBlockState().setValue(Up, false).setValue(Down, false));
    }

    @Override
    protected MapCodec<? extends SupportColumnBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Up, Down);
    }

    private static boolean isColumn(BlockState State) {
        return State.getBlock() instanceof SupportColumnBlock;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockPos Pos = Context.getClickedPos();
        return watered(defaultBlockState(), Context)
                .setValue(Up, isColumn(Context.getLevel().getBlockState(Pos.above())))
                .setValue(Down, isColumn(Context.getLevel().getBlockState(Pos.below())));
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        BlockState Updated = super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
        if (Side == Direction.UP) {
            return Updated.setValue(Up, isColumn(NeighborState));
        }
        if (Side == Direction.DOWN) {
            return Updated.setValue(Down, isColumn(NeighborState));
        }
        return Updated;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return ShapeByEnds[(State.getValue(Up) ? 1 : 0) | (State.getValue(Down) ? 2 : 0)];
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        UtilityTooltips.describe(this, Lines, "stacking");
    }
}
