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
import net.minecraft.world.level.Level;
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

public class CableTrayBlock extends UtilityWaterloggedBlock {
    public static final MapCodec<CableTrayBlock> BlockCodec = simpleCodec(CableTrayBlock::new);
    public static final EnumProperty<AxisFacing> Orientation = EnumProperty.create("orientation", AxisFacing.class);
    public static final double[][] TrayParts = {
            {0, 0, 2, 16, 4, 14}
    };
    private static final Map<AxisFacing, VoxelShape> ShapeByOrientation = new EnumMap<>(AxisFacing.class);

    static {
        for (AxisFacing Mount : AxisFacing.values()) {
            ShapeByOrientation.put(Mount, UtilityShapes.union(Mount.getPreTurnY(), Mount.getTurnX(), Mount.getTurnY(), TrayParts));
        }
    }

    public CableTrayBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(defaultBlockState().setValue(Orientation, AxisFacing.UpX));
    }

    @Override
    protected MapCodec<? extends CableTrayBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Orientation);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        Direction Facing = Context.getClickedFace();
        Direction.Axis Line = null;
        for (Direction Side : Context.getNearestLookingDirections()) {
            if (Side.getAxis() == Facing.getAxis()) {
                continue;
            }
            BlockState Neighbor = World.getBlockState(Pos.relative(Side));
            if (Neighbor.getBlock() instanceof CableTrayBlock && Neighbor.getValue(Orientation).getFacing() == Facing && Neighbor.getValue(Orientation).getAlong() == Side.getAxis()) {
                Line = Side.getAxis();
                break;
            }
        }
        if (Line == null) {
            if (Facing.getAxis().isHorizontal()) {
                Line = Context.getNearestLookingDirection().getAxis() == Direction.Axis.Y ? Direction.Axis.Y : Facing.getClockWise().getAxis();
            } else {
                Line = Context.getHorizontalDirection().getAxis();
            }
        }
        return watered(defaultBlockState(), Context).setValue(Orientation, AxisFacing.of(Facing, Line));
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
        UtilityTooltips.describe(this, Lines, "placement", "run");
    }
}
