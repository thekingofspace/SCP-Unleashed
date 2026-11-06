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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.decor.PillarPart;
import net.rai.scp_unleashed.props.PropShapes;
import net.rai.scp_unleashed.props.PropTooltips;
import net.rai.scp_unleashed.props.PropWater;

import java.util.List;
import java.util.Map;

public class GreenhouseStackBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {
    public static final MapCodec<GreenhouseStackBlock> BlockCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            StackKind.KindCodec.fieldOf("kind").forGetter(GreenhouseStackBlock::getKind), propertiesCodec()).apply(Instance, GreenhouseStackBlock::new));
    public static final EnumProperty<PillarPart> Part = EnumProperty.create("part", PillarPart.class);

    private final StackKind Kind;
    private final Map<Direction, VoxelShape> Outlines;

    public GreenhouseStackBlock(StackKind Kind, Properties Properties) {
        super(Properties);
        this.Kind = Kind;
        this.Outlines = PropShapes.horizontal(Kind.getShape());
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Part, PillarPart.Single).setValue(PropWater.Waterlogged, false));
    }

    public StackKind getKind() {
        return Kind;
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Part, PropWater.Waterlogged);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState Placed = defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
        BlockState Below = Context.getLevel().getBlockState(Context.getClickedPos().below());
        if (Below.is(this)) {
            Placed = Placed.setValue(FACING, Below.getValue(FACING));
        }
        return PropWater.placed(withPart(Placed, Context.getLevel(), Context.getClickedPos()), Context);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        PropWater.update(State, World, Pos);
        if (Side.getAxis() != Direction.Axis.Y) {
            return State;
        }
        return withPart(State, World, Pos);
    }

    private BlockState withPart(BlockState State, BlockGetter World, BlockPos Pos) {
        return State.setValue(Part, PillarPart.of(joins(State, World.getBlockState(Pos.below())), joins(State, World.getBlockState(Pos.above()))));
    }

    private boolean joins(BlockState State, BlockState Other) {
        return Other.is(this) && Other.getValue(FACING) == State.getValue(FACING);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines.get(State.getValue(FACING));
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
