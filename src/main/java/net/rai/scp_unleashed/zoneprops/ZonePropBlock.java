package net.rai.scp_unleashed.zoneprops;

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
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class ZonePropBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<ZonePropBlock> BlockCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            ZonePropKind.KindCodec.fieldOf("kind").forGetter(ZonePropBlock::getKind), propertiesCodec()).apply(Instance, ZonePropBlock::new));

    protected final ZonePropKind Kind;
    private final Map<Direction, VoxelShape> Outlines;

    public ZonePropBlock(ZonePropKind Kind, Properties Properties) {
        super(Properties);
        this.Kind = Kind;
        this.Outlines = PropShapes.horizontal(Kind.getShape(0));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PropWater.Waterlogged, false));
    }

    public ZonePropKind getKind() {
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

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return PropWater.placed(defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite()), Context);
    }

    protected VoxelShape outline(BlockState State) {
        return Outlines.get(State.getValue(FACING));
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return outline(State);
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
        PropTooltips.add(this, Lines, Kind.details());
    }
}
