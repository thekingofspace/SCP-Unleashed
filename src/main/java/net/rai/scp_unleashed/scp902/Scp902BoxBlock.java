package net.rai.scp_unleashed.scp902;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropShapes;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class Scp902BoxBlock extends BaseEntityBlock {
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Open = BlockStateProperties.OPEN;
    private static final VoxelShape ClosedNorth = PropShapes.box(2, 0, 4.5, 14, 10, 11.5);
    private static final VoxelShape OpenNorth = PropShapes.of(PropShapes.box(2, 0, 5, 14, 8, 11), PropShapes.box(1.5, 8, 10.5, 14.5, 15, 12.5));
    private static final VoxelShape BodyNorth = PropShapes.box(2, 0, 5, 14, 8, 11);
    private static final Map<Direction, VoxelShape> ClosedShapes = PropShapes.horizontal(ClosedNorth);
    private static final Map<Direction, VoxelShape> OpenShapes = PropShapes.horizontal(OpenNorth);
    private static final Map<Direction, VoxelShape> BodyShapes = PropShapes.horizontal(BodyNorth);
    private final boolean Scenery;

    public Scp902BoxBlock(Properties Settings, boolean Scenery) {
        super(Settings);
        this.Scenery = Scenery;
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Open, false));
    }

    public boolean scenery() {
        return Scenery;
    }

    public static boolean isOpen(BlockState State) {
        return State.hasProperty(Open) && State.getValue(Open);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(Settings -> new Scp902BoxBlock(Settings, Scenery));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Open);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
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
        return (isOpen(State) ? OpenShapes : ClosedShapes).get(State.getValue(Facing));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return (isOpen(State) ? BodyShapes : ClosedShapes).get(State.getValue(Facing));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (Scenery || !(World.getBlockEntity(Pos) instanceof Scp902BoxBlockEntity) || User.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!Scp902Box.canUse(User)) {
            return InteractionResult.PASS;
        }
        if (User.isSecondaryUseActive() && User instanceof ServerPlayer Visitor && Scp902Trip.begin(Visitor)) {
            return InteractionResult.CONSUME;
        }
        return toggle((ServerLevel) World, Pos, User) ? InteractionResult.CONSUME : InteractionResult.PASS;
    }

    public static boolean toggle(ServerLevel World, BlockPos Pos, Player User) {
        BlockState State = World.getBlockState(Pos);
        if (!(World.getBlockEntity(Pos) instanceof Scp902BoxBlockEntity Tin) || !State.hasProperty(Open)) {
            return false;
        }
        long Now = World.getGameTime();
        if (Tin.settling(Now)) {
            return true;
        }
        boolean Opening = !State.getValue(Open);
        World.setBlock(Pos, State.setValue(Open, Opening), Block.UPDATE_ALL);
        Tin.toggled(Now);
        Scp902Box.lid(World, Vec3.atCenterOf(Pos).subtract(0.0, 0.25, 0.0), Opening);
        World.gameEvent(User, Opening ? GameEvent.CONTAINER_OPEN : GameEvent.CONTAINER_CLOSE, Pos);
        if (Opening) {
            Scp902Box.empty(User, World);
        }
        return true;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp902BoxBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? createTickerHelper(Type, Scp902Registry.BoxEntity.get(), Scp902BoxBlockEntity::clientTick)
                : createTickerHelper(Type, Scp902Registry.BoxEntity.get(), Scp902BoxBlockEntity::serverTick);
    }
}
