package net.rai.scp_unleashed.scp458;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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

public class Scp458BoxBlock extends BaseEntityBlock {
    public static final MapCodec<Scp458BoxBlock> BlockCodec = simpleCodec(Scp458BoxBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Open = BlockStateProperties.OPEN;
    private static final VoxelShape ClosedNorth = PropShapes.box(0, 0, 0, 16, 4, 16);
    private static final VoxelShape OpenNorth = PropShapes.of(PropShapes.box(0, 0, 0, 16, 3, 16), PropShapes.box(0, 3, 13, 16, 16, 16));
    private static final VoxelShape TrayNorth = PropShapes.box(0, 0, 0, 16, 3, 16);
    private static final Map<Direction, VoxelShape> ClosedShapes = PropShapes.horizontal(ClosedNorth);
    private static final Map<Direction, VoxelShape> OpenShapes = PropShapes.horizontal(OpenNorth);
    private static final Map<Direction, VoxelShape> TrayShapes = PropShapes.horizontal(TrayNorth);

    public Scp458BoxBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Open, false));
    }

    public static boolean isOpen(BlockState State) {
        return State.hasProperty(Open) && State.getValue(Open);
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
        ItemStack Stack = Context.getItemInHand();
        Scp458Contents Carried = Stack.getOrDefault(Scp458Registry.Contents.get(), Scp458Contents.Sealed);
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite()).setValue(Open, Carried.Open());
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
        return (isOpen(State) ? TrayShapes : ClosedShapes).get(State.getValue(Facing));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!(World.getBlockEntity(Pos) instanceof Scp458BoxBlockEntity Carton) || User.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!Scp458Box.canUse(User)) {
            return InteractionResult.PASS;
        }
        Direction Turned = State.getValue(Facing);
        boolean Closing = User.isSecondaryUseActive() || (isOpen(State) && Scp458Box.onLid(Turned, Vec3.atLowerCornerOf(Pos), Hit.getLocation()));
        Scp458Box.Step Done = use((ServerLevel) World, Pos, User, Closing);
        return Done.Result() == Scp458Box.Outcome.Refused ? InteractionResult.PASS : InteractionResult.CONSUME;
    }

    public static Scp458Box.Step use(ServerLevel World, BlockPos Pos, LivingEntity User, boolean Closing) {
        BlockState State = World.getBlockState(Pos);
        if (!(World.getBlockEntity(Pos) instanceof Scp458BoxBlockEntity Carton) || !State.hasProperty(Facing)) {
            return new Scp458Box.Step(Scp458Box.Outcome.Refused, Scp458Contents.Sealed, ItemStack.EMPTY);
        }
        Vec3 Centre = Vec3.atBottomCenterOf(Pos);
        float Angle = Scp458Box.facingAngle(State.getValue(Facing), Centre, User.position());
        Scp458Box.Step Done = Scp458Box.act(World, User, Carton.contents(), Closing, Angle);
        if (Done.changed()) {
            Carton.store(Done.After());
        }
        Scp458Box.give(User, Done.Given());
        Scp458Box.sound(World, Vec3.atCenterOf(Pos).subtract(0.0, 0.3, 0.0), Done.Result());
        switch (Done.Result()) {
            case Opened -> World.gameEvent(User, GameEvent.CONTAINER_OPEN, Pos);
            case Closed -> World.gameEvent(User, GameEvent.CONTAINER_CLOSE, Pos);
            case Took -> World.gameEvent(User, GameEvent.ITEM_INTERACT_FINISH, Pos);
            default -> {
            }
        }
        return Done;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new Scp458BoxBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? createTickerHelper(Type, Scp458Registry.BoxEntity.get(), Scp458BoxBlockEntity::clientTick) : null;
    }
}
