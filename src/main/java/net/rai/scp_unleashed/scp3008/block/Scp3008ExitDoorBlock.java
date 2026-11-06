package net.rai.scp_unleashed.scp3008.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.rai.scp_unleashed.scp3008.Scp3008Transit;

import java.util.Map;

public class Scp3008ExitDoorBlock extends Scp3008SealedBlock {
    public static final MapCodec<Scp3008ExitDoorBlock> DoorCodec = simpleCodec(Scp3008ExitDoorBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> Half = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final BooleanProperty Right = BooleanProperty.create("right");
    public static final Map<Direction, VoxelShape> Shapes = Scp3008Shapes.facings(new double[][]{{0, 0, 3, 16, 16, 16}});

    public Scp3008ExitDoorBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Half, DoubleBlockHalf.LOWER).setValue(Right, false));
    }

    @Override
    protected MapCodec<? extends Scp3008ExitDoorBlock> codec() {
        return DoorCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Half, Right);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Shapes.get(State.getValue(Facing));
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (User instanceof ServerPlayer Visitor && !(User instanceof FakePlayer) && !Visitor.isSpectator()) {
            Scp3008Transit.leave(Visitor, Pos);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Kind) {
        return false;
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.rotate(Flip.getRotation(State.getValue(Facing))).setValue(Right, !State.getValue(Right));
    }
}
