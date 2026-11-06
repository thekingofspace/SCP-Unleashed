package net.rai.scp_unleashed.containmentzone;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import net.rai.scp_unleashed.props.PropShapes;
import net.rai.scp_unleashed.props.PropTooltips;
import net.rai.scp_unleashed.props.PropWater;

import java.util.List;
import java.util.Map;

public class CellPropBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<CellPropBlock> BlockCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            CellPropKind.KindCodec.fieldOf("kind").forGetter(CellPropBlock::getKind), propertiesCodec()).apply(Instance, CellPropBlock::new));

    private final CellPropKind Kind;
    private final Map<Direction, VoxelShape> Outlines;

    public CellPropBlock(CellPropKind Kind, Properties Properties) {
        super(Properties);
        this.Kind = Kind;
        this.Outlines = PropShapes.horizontal(Kind.getShape());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PropWater.Waterlogged, false));
    }

    public CellPropKind getKind() {
        return Kind;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, PropWater.Waterlogged);
    }

    public static Direction facingFor(BlockPlaceContext Context, boolean WallMounted) {
        Direction Clicked = Context.getClickedFace();
        if (WallMounted && Clicked.getAxis().isHorizontal()) {
            return Clicked;
        }
        return Context.getHorizontalDirection().getOpposite();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return PropWater.placed(defaultBlockState().setValue(FACING, facingFor(Context, Kind.isWallMounted())), Context);
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
        PropTooltips.add(this, Lines, Kind.getDetails());
    }
}
