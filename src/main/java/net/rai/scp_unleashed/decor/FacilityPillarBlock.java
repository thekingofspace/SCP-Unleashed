package net.rai.scp_unleashed.decor;

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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.rai.scp_unleashed.block.DecoratableBlock;

import java.util.List;

public class FacilityPillarBlock extends DecoratableBlock {
    public static final MapCodec<FacilityPillarBlock> PillarCodec = simpleCodec(FacilityPillarBlock::new);
    public static final EnumProperty<Direction.Axis> PillarAxis = BlockStateProperties.AXIS;
    public static final EnumProperty<PillarPart> Part = EnumProperty.create("part", PillarPart.class);

    public FacilityPillarBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(defaultBlockState().setValue(PillarAxis, Direction.Axis.Y).setValue(Part, PillarPart.Single));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return PillarCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(PillarAxis, Part);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState Placed = defaultBlockState().setValue(PillarAxis, Context.getClickedFace().getAxis());
        return withPart(Placed, Context.getLevel(), Context.getClickedPos());
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState Neighbour, LevelAccessor World, BlockPos Pos, BlockPos NeighbourPos) {
        if (Side.getAxis() != State.getValue(PillarAxis)) {
            return State;
        }
        return withPart(State, World, Pos);
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return RotatedPillarBlock.rotatePillar(State, Turn);
    }

    private BlockState withPart(BlockState State, BlockGetter World, BlockPos Pos) {
        Direction.Axis Along = State.getValue(PillarAxis);
        BlockState Below = World.getBlockState(Pos.relative(Direction.fromAxisAndDirection(Along, Direction.AxisDirection.NEGATIVE)));
        BlockState Above = World.getBlockState(Pos.relative(Direction.fromAxisAndDirection(Along, Direction.AxisDirection.POSITIVE)));
        return State.setValue(Part, PillarPart.of(joins(Below, Along), joins(Above, Along)));
    }

    private boolean joins(BlockState Other, Direction.Axis Along) {
        return Other.is(this) && Other.getValue(PillarAxis) == Along;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.facility_pillar.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
