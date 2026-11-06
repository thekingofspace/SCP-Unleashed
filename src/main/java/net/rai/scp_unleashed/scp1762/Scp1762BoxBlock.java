package net.rai.scp_unleashed.scp1762;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

public class Scp1762BoxBlock extends BaseEntityBlock {
    public static final MapCodec<Scp1762BoxBlock> BlockCodec = simpleCodec(Scp1762BoxBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Open = BlockStateProperties.OPEN;
    public static final float GuardedResistance = 3600000.0f;
    private static final VoxelShape ClosedNorth = PropShapes.box(0.33, 0, 3, 15.67, 6.5, 13);
    private static final VoxelShape BodyNorth = PropShapes.of(PropShapes.box(0.83, 0, 3.5, 15.17, 6, 4.17), PropShapes.box(0.83, 0, 11.83, 15.17, 6, 12.5),
            PropShapes.box(0.83, 0, 3.5, 1.5, 6, 12.5), PropShapes.box(14.5, 0, 3.5, 15.17, 6, 12.5), PropShapes.box(0.83, 0, 3.5, 15.17, 1.17, 12.5));
    private static final Map<Direction, VoxelShape> ClosedShapes = PropShapes.horizontal(ClosedNorth);
    private static final Map<Direction, VoxelShape> BodyShapes = PropShapes.horizontal(BodyNorth);

    public Scp1762BoxBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Open, false));
    }

    public static boolean isOpen(BlockState State) {
        return State.getBlock() instanceof Scp1762BoxBlock && State.getValue(Open);
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
        return (State.getValue(Open) ? BodyShapes : ClosedShapes).get(State.getValue(Facing));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    public static boolean guarded(BlockGetter World, BlockPos Pos) {
        return World.getBlockEntity(Pos) instanceof Scp1762BoxBlockEntity Box && Box.guarded();
    }

    @Override
    protected float getDestroyProgress(BlockState State, Player User, BlockGetter World, BlockPos Pos) {
        return guarded(World, Pos) ? 0.0f : super.getDestroyProgress(State, User, World, Pos);
    }

    @Override
    public float getExplosionResistance(BlockState State, BlockGetter World, BlockPos Pos, Explosion Blast) {
        return guarded(World, Pos) ? GuardedResistance : super.getExplosionResistance(State, World, Pos, Blast);
    }

    @Override
    public boolean canEntityDestroy(BlockState State, BlockGetter World, BlockPos Pos, Entity Breaker) {
        return !guarded(World, Pos) && super.canEntityDestroy(State, World, Pos, Breaker);
    }

    @Override
    public boolean canDropFromExplosion(BlockState State, BlockGetter World, BlockPos Pos, Explosion Blast) {
        return !guarded(World, Pos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!Stack.is(Items.PAPER) || User.isSpectator() || !(World instanceof ServerLevel Server) || !(World.getBlockEntity(Pos) instanceof Scp1762BoxBlockEntity Box)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!Box.revive(Server, User)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        Stack.consume(1, User);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (User.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(World.getBlockEntity(Pos) instanceof Scp1762BoxBlockEntity Box)) {
            return InteractionResult.PASS;
        }
        Box.interact((ServerLevel) World, User);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState Replaced, boolean Moved) {
        if (!State.is(Replaced.getBlock()) && World instanceof ServerLevel Server && World.getBlockEntity(Pos) instanceof Scp1762BoxBlockEntity Box) {
            Box.vanishAll(Server);
        }
        super.onRemove(State, World, Pos, Replaced, Moved);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp1762BoxBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, Scp1762Registry.BoxEntity.get(), Scp1762BoxBlockEntity::serverTick);
    }
}
