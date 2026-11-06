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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class DecorPipeBlock extends UtilityWaterloggedBlock {
    public static final MapCodec<DecorPipeBlock> BlockCodec = simpleCodec(DecorPipeBlock::new);
    public static final EnumProperty<Direction.Axis> Along = BlockStateProperties.AXIS;
    private static final Map<Direction.Axis, VoxelShape> ShapeByAxis = new EnumMap<>(Direction.Axis.class);

    static {
        for (Direction.Axis Line : Direction.Axis.values()) {
            int[] Turn = DecorPipes.axisTurn(Line);
            ShapeByAxis.put(Line, UtilityShapes.union(Turn[0], Turn[1], DecorPipes.StraightParts));
        }
    }

    public DecorPipeBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(defaultBlockState().setValue(Along, Direction.Axis.Y));
    }

    @Override
    protected MapCodec<? extends DecorPipeBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Along);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return watered(defaultBlockState(), Context).setValue(Along, Context.getClickedFace().getAxis());
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return ShapeByAxis.get(State.getValue(Along));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        if (Turn == Rotation.CLOCKWISE_90 || Turn == Rotation.COUNTERCLOCKWISE_90) {
            Direction.Axis Current = State.getValue(Along);
            if (Current != Direction.Axis.Y) {
                return State.setValue(Along, Current == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
            }
        }
        return State;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        UtilityTooltips.describe(this, Lines, "block.scp_unleashed.utility.decorative_pipe", "placement");
    }
}
