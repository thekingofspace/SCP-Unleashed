package net.rai.scp_unleashed.scp465;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropShapes;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class Scp465BoxBlock extends BaseEntityBlock {
    public static final MapCodec<Scp465BoxBlock> BlockCodec = simpleCodec(Scp465BoxBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Open = BlockStateProperties.OPEN;
    public static final float SealedResistance = 3600000.0f;
    private static final VoxelShape ClosedNorth = PropShapes.box(1, 0, 1, 15, 14, 15);
    private static final VoxelShape BodyNorth = PropShapes.box(2, 0, 2, 14, 11, 14);
    private static final VoxelShape OpenNorth = PropShapes.of(BodyNorth, PropShapes.box(1, 11, 14, 15, 16, 16));
    private static final Map<Direction, VoxelShape> ClosedShapes = PropShapes.horizontal(ClosedNorth);
    private static final Map<Direction, VoxelShape> OpenShapes = PropShapes.horizontal(OpenNorth);
    private static final Map<Direction, VoxelShape> BodyShapes = PropShapes.horizontal(BodyNorth);

    public Scp465BoxBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Open, false));
    }

    public static boolean isOpen(BlockState State) {
        return State.getBlock() instanceof Scp465BoxBlock && State.getValue(Open);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Open);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite()).setValue(Open, false);
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State.setValue(Facing, Flip.mirror(State.getValue(Facing)));
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return (State.getValue(Open) ? OpenShapes : ClosedShapes).get(State.getValue(Facing));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return (State.getValue(Open) ? BodyShapes : ClosedShapes).get(State.getValue(Facing));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected float getDestroyProgress(BlockState State, Player User, BlockGetter World, BlockPos Pos) {
        return State.getValue(Open) ? 0.0f : super.getDestroyProgress(State, User, World, Pos);
    }

    @Override
    public float getExplosionResistance(BlockState State, BlockGetter World, BlockPos Pos, Explosion Blast) {
        return State.getValue(Open) ? SealedResistance : super.getExplosionResistance(State, World, Pos, Blast);
    }

    @Override
    public boolean canEntityDestroy(BlockState State, BlockGetter World, BlockPos Pos, Entity Breaker) {
        return !State.getValue(Open) && super.canEntityDestroy(State, World, Pos, Breaker);
    }

    @Override
    public boolean canDropFromExplosion(BlockState State, BlockGetter World, BlockPos Pos, Explosion Blast) {
        return !State.getValue(Open);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (User.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(World.getBlockEntity(Pos) instanceof Scp465BoxBlockEntity)) {
            return InteractionResult.PASS;
        }
        Scp465Celebration.Outcome Done = State.getValue(Open) ? Scp465Celebration.closeBy((ServerLevel) World, Pos, User) : Scp465Celebration.open((ServerLevel) World, Pos, User);
        return Done == Scp465Celebration.Outcome.Ignored ? InteractionResult.PASS : InteractionResult.CONSUME;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp465BoxBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? createTickerHelper(Type, Scp465Registry.BoxEntity.get(), Scp465BoxBlockEntity::clientTick) : null;
    }
}
