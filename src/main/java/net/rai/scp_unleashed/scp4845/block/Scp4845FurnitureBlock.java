package net.rai.scp_unleashed.scp4845.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropSeatEntity;
import net.rai.scp_unleashed.props.SeatBlock;
import net.rai.scp_unleashed.scp4845.staff.Scp4845Brain;

import java.util.Map;

public class Scp4845FurnitureBlock extends HorizontalDirectionalBlock implements SeatBlock {
    public static final MapCodec<Scp4845FurnitureBlock> FurnitureCodec = simpleCodec(Settings -> new Scp4845FurnitureBlock(Settings, Scp4845Shapes.Full));
    private final Map<Direction, VoxelShape> Outline;
    private final Map<Direction, VoxelShape> Collision;
    private final double SeatTop;

    public Scp4845FurnitureBlock(Properties Settings, double[][] North) {
        this(Settings, North, 0.0);
    }

    public Scp4845FurnitureBlock(Properties Settings, double[][] North, double SeatTop) {
        super(Settings);
        Outline = Scp4845Shapes.facings(North);
        Collision = Scp4845Shapes.collisions(Outline);
        this.SeatTop = SeatTop;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Scp4845FurnitureBlock> codec() {
        return FurnitureCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(FACING, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outline.get(State.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Collision.get(State.getValue(FACING));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Kind) {
        return false;
    }

    @Override
    public boolean seats(BlockState State) {
        return SeatTop > 0.0;
    }

    @Override
    public double seatHeight(BlockState State) {
        return SeatTop / 16.0;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!seats(State) || User.isSecondaryUseActive() || User.isPassenger() || PropSeatEntity.occupied(World, Pos)) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (Scp4845Brain.seatedAt(World, Pos)) {
            return InteractionResult.PASS;
        }
        return PropSeatEntity.sit(World, Pos, State, User) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (seats(State) && !World.isClientSide && !State.is(NewState.getBlock())) {
            PropSeatEntity.release(World, Pos);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }
}
