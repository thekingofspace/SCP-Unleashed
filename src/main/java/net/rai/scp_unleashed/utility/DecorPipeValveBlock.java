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
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class DecorPipeValveBlock extends UtilityWaterloggedBlock {
    public static final MapCodec<DecorPipeValveBlock> BlockCodec = simpleCodec(DecorPipeValveBlock::new);
    public static final EnumProperty<AxisFacing> Orientation = EnumProperty.create("orientation", AxisFacing.class);
    public static final double[][] ValveParts = {
            {0, 4, 4, 16, 12, 12},
            {0, 3, 3, 1, 13, 13},
            {15, 3, 3, 16, 13, 13},
            {4, 3, 3, 12, 13, 13},
            {6, 13, 6, 10, 14, 10},
            {3, 14, 3, 13, 16, 13}
    };
    private static final Map<AxisFacing, VoxelShape> ShapeByOrientation = new EnumMap<>(AxisFacing.class);

    static {
        for (AxisFacing Mount : AxisFacing.values()) {
            ShapeByOrientation.put(Mount, UtilityShapes.union(Mount.getPreTurnY(), Mount.getTurnX(), Mount.getTurnY(), ValveParts));
        }
    }

    public DecorPipeValveBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(defaultBlockState().setValue(Orientation, AxisFacing.UpX));
    }

    @Override
    protected MapCodec<? extends DecorPipeValveBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Orientation);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction.Axis Line = Context.getClickedFace().getAxis();
        Direction Wheel = Line.isHorizontal() ? Context.getNearestLookingVerticalDirection().getOpposite() : Context.getHorizontalDirection().getOpposite();
        return watered(defaultBlockState(), Context).setValue(Orientation, AxisFacing.of(Wheel, Line));
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return ShapeByOrientation.get(State.getValue(Orientation));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Orientation, State.getValue(Orientation).rotated(Turn));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.setValue(Orientation, State.getValue(Orientation).mirrored(Flip));
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        UtilityTooltips.describe(this, Lines, "block.scp_unleashed.utility.decorative_pipe", "placement");
    }
}
