package net.rai.scp_unleashed.containment;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.item.ItemTooltips;

import java.util.List;

public class ReinforcedPaneBlock extends IronBarsBlock {
    public static final MapCodec<ReinforcedPaneBlock> PaneCodec = simpleCodec(ReinforcedPaneBlock::new);
    public static final BooleanProperty Up = BlockStateProperties.UP;
    public static final BooleanProperty Down = BlockStateProperties.DOWN;
    public static final float FrameHalfWidth = 2.0f;

    private final VoxelShape[] FrameShapes;

    public ReinforcedPaneBlock(Properties Properties) {
        super(Properties);
        FrameShapes = makeShapes(FrameHalfWidth, FrameHalfWidth, 16.0f, 0.0f, 16.0f);
        registerDefaultState(defaultBlockState().setValue(Up, false).setValue(Down, false));
    }

    @Override
    public MapCodec<? extends IronBarsBlock> codec() {
        return PaneCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Up, Down);
    }

    public boolean sameRun(BlockState State, BlockState Other) {
        if (!Other.is(this)) {
            return false;
        }
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            BooleanProperty Arm = PROPERTY_BY_DIRECTION.get(Side);
            if (State.getValue(Arm) != Other.getValue(Arm)) {
                return false;
            }
        }
        return true;
    }

    private BlockState stacked(BlockState State, BlockGetter World, BlockPos Pos) {
        return State.setValue(Up, sameRun(State, World.getBlockState(Pos.above()))).setValue(Down, sameRun(State, World.getBlockState(Pos.below())));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return stacked(super.getStateForPlacement(Context), Context.getLevel(), Context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        return stacked(super.updateShape(State, Side, Neighbour, World, Pos, NeighbourPos), World, Pos);
    }

    @Override
    protected boolean skipRendering(BlockState State, BlockState Adjacent, Direction Side) {
        if (!Adjacent.is(this)) {
            return false;
        }
        if (Side == Direction.UP) {
            return State.getValue(Up);
        }
        if (Side == Direction.DOWN) {
            return State.getValue(Down);
        }
        return State.getValue(PROPERTY_BY_DIRECTION.get(Side)) && Adjacent.getValue(PROPERTY_BY_DIRECTION.get(Side.getOpposite()))
                && State.getValue(Up) == Adjacent.getValue(Up) && State.getValue(Down) == Adjacent.getValue(Down);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return FrameShapes[getAABBIndex(State)];
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return FrameShapes[getAABBIndex(State)];
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.reinforced_glass_pane.merging").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.containment.blast_proof").withStyle(ChatFormatting.GOLD));
        }
    }
}
