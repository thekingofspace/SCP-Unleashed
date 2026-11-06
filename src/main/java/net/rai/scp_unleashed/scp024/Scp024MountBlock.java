package net.rai.scp_unleashed.scp024;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public class Scp024MountBlock extends Scp024SolidBlock {
    public static final MapCodec<Scp024MountBlock> MountCodec = simpleCodec(Properties -> new Scp024MountBlock(Properties, 4.0, 4.0, 10.0, 12.0, 12.0, 16.0));
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    private final Map<Direction, VoxelShape> Shapes = new EnumMap<>(Direction.class);

    public Scp024MountBlock(Properties Settings, double X1, double Y1, double Z1, double X2, double Y2, double Z2) {
        super(Settings);
        for (Direction Side : Direction.Plane.HORIZONTAL) {
            Shapes.put(Side, rotated(Side, X1, Y1, Z1, X2, Y2, Z2));
        }
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH));
    }

    public static VoxelShape rotated(Direction Side, double X1, double Y1, double Z1, double X2, double Y2, double Z2) {
        return switch (Side) {
            case SOUTH -> Block.box(16.0 - X2, Y1, 16.0 - Z2, 16.0 - X1, Y2, 16.0 - Z1);
            case EAST -> Block.box(16.0 - Z2, Y1, X1, 16.0 - Z1, Y2, X2);
            case WEST -> Block.box(Z1, Y1, 16.0 - X2, Z2, Y2, 16.0 - X1);
            default -> Block.box(X1, Y1, Z1, X2, Y2, Z2);
        };
    }

    @Override
    protected MapCodec<? extends Scp024MountBlock> codec() {
        return MountCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Direction Side = Context.getClickedFace().getAxis().isHorizontal() ? Context.getClickedFace() : Context.getHorizontalDirection().getOpposite();
        return defaultBlockState().setValue(Facing, Side);
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.get(State.getValue(Facing));
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(Facing)));
    }
}
