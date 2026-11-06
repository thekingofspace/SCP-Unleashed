package net.rai.scp_unleashed.greenhouse;

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

public class GreenhousePropBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<GreenhousePropBlock> BlockCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            GreenhouseKind.KindCodec.fieldOf("kind").forGetter(GreenhousePropBlock::getKind), propertiesCodec()).apply(Instance, GreenhousePropBlock::new));

    private final GreenhouseKind Kind;
    private final Map<Direction, VoxelShape> Outlines;

    public GreenhousePropBlock(GreenhouseKind Kind, Properties Properties) {
        super(Properties);
        this.Kind = Kind;
        this.Outlines = PropShapes.horizontal(Kind.getShape());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PropWater.Waterlogged, false));
    }

    public GreenhouseKind getKind() {
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

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Facing = Context.getHorizontalDirection().getOpposite();
        if (Kind == GreenhouseKind.IrrigationPanel && Context.getClickedFace().getAxis().isHorizontal()) {
            Facing = Context.getClickedFace();
        }
        return PropWater.placed(defaultBlockState().setValue(FACING, Facing), Context);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines.get(State.getValue(FACING));
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        PropWater.update(State, World, Pos);
        return super.updateShape(State, Side, Neighbour, World, Pos, NeighbourPos);
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
