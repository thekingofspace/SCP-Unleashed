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

public class DecorPipeElbowBlock extends UtilityWaterloggedBlock {
    public static final MapCodec<DecorPipeElbowBlock> BlockCodec = simpleCodec(DecorPipeElbowBlock::new);
    public static final EnumProperty<ElbowOrientation> Orientation = EnumProperty.create("orientation", ElbowOrientation.class);
    public static final double[][] ElbowParts = {
            {3, 0, 3, 13, 1, 13},
            {3, 3, 0, 13, 13, 1},
            {4, 1, 4, 12, 4, 12},
            {4, 4, 1, 12, 12, 4},
            {4, 4, 4, 12, 8, 12},
            {4, 8, 4, 12, 9, 11},
            {4, 9, 4, 12, 10, 10},
            {4, 10, 4, 12, 11, 9},
            {4, 11, 4, 12, 12, 8}
    };
    private static final Map<ElbowOrientation, VoxelShape> ShapeByOrientation = new EnumMap<>(ElbowOrientation.class);

    static {
        for (ElbowOrientation Bend : ElbowOrientation.values()) {
            ShapeByOrientation.put(Bend, UtilityShapes.union(Bend.getPreTurnY(), Bend.getTurnX(), Bend.getTurnY(), ElbowParts));
        }
    }

    public DecorPipeElbowBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(defaultBlockState().setValue(Orientation, ElbowOrientation.DownNorth));
    }

    @Override
    protected MapCodec<? extends DecorPipeElbowBlock> codec() {
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
        Direction First = Context.getClickedFace().getOpposite();
        Direction Second = null;
        for (Direction Look : Context.getNearestLookingDirections()) {
            if (Look.getAxis() != First.getAxis() && DecorPipes.pipeAt(World, Pos, Look)) {
                Second = Look;
                break;
            }
        }
        if (Second == null) {
            for (Direction Look : Context.getNearestLookingDirections()) {
                if (Look.getAxis() != First.getAxis()) {
                    Second = Look;
                    break;
                }
            }
        }
        return watered(defaultBlockState(), Context).setValue(Orientation, ElbowOrientation.of(First, Second));
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
