package net.rai.scp_unleashed.storage;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.rai.scp_unleashed.door.Screwdrivable;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class StorageContainerBlock extends BaseEntityBlock implements Screwdrivable {
    public static final MapCodec<StorageContainerBlock> BlockCodec = simpleCodec(StorageContainerBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Open = BlockStateProperties.OPEN;
    public static final EnumProperty<StorageEnvironment> Environment = EnumProperty.create("environment", StorageEnvironment.class);
    public static final BooleanProperty Stacked = BooleanProperty.create("stacked");
    public static final int FullSignal = 15;
    private static final Map<Direction, VoxelShape> Outlines = StorageShapes.horizontal(box(0, 0, 1, 16, 16, 16));

    public StorageContainerBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Open, false).setValue(Environment, StorageEnvironment.Normal)
                .setValue(Stacked, false));
    }

    public boolean isTall() {
        return false;
    }

    public BlockPos mainPos(BlockState State, BlockPos Pos) {
        return Pos;
    }

    public List<BlockPos> cells(BlockState State, BlockPos Pos) {
        return List.of(Pos);
    }

    @Nullable
    public static StorageContainerBlockEntity containerAt(BlockGetter World, BlockPos Pos) {
        BlockState State = World.getBlockState(Pos);
        if (!(State.getBlock() instanceof StorageContainerBlock Owner)) {
            return null;
        }
        return World.getBlockEntity(Owner.mainPos(State, Pos)) instanceof StorageContainerBlockEntity Locker ? Locker : null;
    }

    public static boolean isContainer(BlockState State) {
        return State.getBlock() instanceof StorageContainerBlock;
    }

    public static boolean isGlass(ItemStack Stack) {
        return !Stack.isEmpty() && (Stack.is(Tags.Items.GLASS_BLOCKS) || Stack.is(Tags.Items.GLASS_PANES)) && Stack.getItem() instanceof BlockItem;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Open, Environment, Stacked);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Outlines.get(State.getValue(Facing));
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite())
                .setValue(Stacked, isContainer(Context.getLevel().getBlockState(Context.getClickedPos().below())));
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (Side == Direction.DOWN && mainPos(State, Pos).equals(Pos)) {
            return State.setValue(Stacked, isContainer(NeighborState));
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        StorageContainerBlockEntity Locker = containerAt(World, Pos);
        if (Locker == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (User.isSecondaryUseActive()) {
            if (Hit.getDirection() != State.getValue(Facing)) {
                return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            }
            return handleStorage(Locker, Stack, World, User, Hand);
        }
        if (isGlass(Stack) && !Locker.hasGlassFront()) {
            if (!World.isClientSide) {
                Locker.fitGlass(User, Stack);
                World.gameEvent(User, GameEvent.BLOCK_CHANGE, Pos);
            }
            return ItemInteractionResult.sidedSuccess(World.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private ItemInteractionResult handleStorage(StorageContainerBlockEntity Locker, ItemStack Stack, Level World, Player User, InteractionHand Hand) {
        if (!Locker.isOpen()) {
            if (!World.isClientSide) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.storage.closed"), true);
                Locker.refuse();
            }
            return ItemInteractionResult.sidedSuccess(World.isClientSide);
        }
        if (Stack.isEmpty()) {
            if (Locker.hasContents() && Hand == InteractionHand.MAIN_HAND) {
                if (!World.isClientSide && !(User.isSecondaryUseActive() && Locker.takeOne(User, Hand))) {
                    Locker.take(User, Hand);
                }
                return ItemInteractionResult.sidedSuccess(World.isClientSide);
            }
            if (Hand == InteractionHand.MAIN_HAND && !User.getOffhandItem().isEmpty() && !Locker.hasContents()) {
                return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            }
            if (!World.isClientSide) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.storage.empty"), true);
            }
            return ItemInteractionResult.sidedSuccess(World.isClientSide);
        }
        if (Locker.hasContents()) {
            if (!World.isClientSide && !Locker.topUp(User, Stack)) {
                Component Full = Locker.occupiedMessage(Stack);
                User.displayClientMessage(Full != null ? Full : Component.translatable("message.scp_unleashed.storage.occupied", Locker.getContents().getHoverName()), true);
                Locker.refuse();
            }
            return ItemInteractionResult.sidedSuccess(World.isClientSide);
        }
        if (!World.isClientSide) {
            Locker.store(User, Stack);
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        StorageContainerBlockEntity Locker = containerAt(World, Pos);
        if (Locker == null) {
            return InteractionResult.PASS;
        }
        if (!World.isClientSide) {
            toggle(World, Pos, State, User, Locker);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    public void toggle(Level World, BlockPos Pos, BlockState State, @Nullable Player User, StorageContainerBlockEntity Locker) {
        boolean Opening = !State.getValue(Open);
        if (Opening && isDoorBlocked(World, Pos, State)) {
            if (User != null) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.storage.blocked"), true);
            }
            Locker.refuse();
            return;
        }
        setOpen(World, Pos, State, Opening);
        Locker.doorToggled(Opening);
        World.gameEvent(User, Opening ? GameEvent.BLOCK_OPEN : GameEvent.BLOCK_CLOSE, mainPos(State, Pos));
    }

    public boolean isDoorBlocked(Level World, BlockPos Pos, BlockState State) {
        Direction Front = State.getValue(Facing);
        for (BlockPos Cell : cells(State, Pos)) {
            BlockPos Ahead = Cell.relative(Front);
            BlockState Blocking = World.getBlockState(Ahead);
            if (Blocking.isCollisionShapeFullBlock(World, Ahead)) {
                return true;
            }
        }
        return false;
    }

    public void setOpen(Level World, BlockPos Pos, BlockState State, boolean Opened) {
        for (BlockPos Cell : cells(State, Pos)) {
            BlockState Current = World.getBlockState(Cell);
            if (Current.is(this) && Current.getValue(Open) != Opened) {
                World.setBlock(Cell, Current.setValue(Open, Opened), Block.UPDATE_ALL);
            }
        }
    }

    public void setEnvironment(Level World, BlockPos Pos, BlockState State, StorageEnvironment Wanted) {
        for (BlockPos Cell : cells(State, Pos)) {
            BlockState Current = World.getBlockState(Cell);
            if (Current.is(this) && Current.getValue(Environment) != Wanted) {
                World.setBlock(Cell, Current.setValue(Environment, Wanted), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public InteractionResult screwdrive(Level World, BlockPos Pos, BlockState State, @Nullable Player User, boolean Sneaking) {
        StorageContainerBlockEntity Locker = containerAt(World, Pos);
        if (Locker == null) {
            return InteractionResult.PASS;
        }
        if (Sneaking) {
            if (!Locker.hasGlassFront()) {
                if (User != null) {
                    User.displayClientMessage(Component.translatable("message.scp_unleashed.storage.no_glass"), true);
                }
                return InteractionResult.FAIL;
            }
            ItemStack Removed = Locker.removeGlass();
            give(World, Pos, User, Removed);
            if (User != null) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.storage.glass_removed"), true);
            }
            return InteractionResult.SUCCESS;
        }
        StorageEnvironment Current = State.getValue(Environment);
        if (!Current.isConditioned()) {
            if (User != null) {
                User.displayClientMessage(Component.translatable("message.scp_unleashed.storage.no_upgrade"), true);
            }
            return InteractionResult.FAIL;
        }
        setEnvironment(World, Pos, State, StorageEnvironment.Normal);
        Locker.environmentChanged();
        give(World, Pos, User, Current.upgradeStack());
        if (User != null) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.storage.upgrade_removed", Current.upgradeStack().getHoverName()), true);
        }
        return InteractionResult.SUCCESS;
    }

    public static void give(Level World, BlockPos Pos, @Nullable Player User, ItemStack Stack) {
        if (Stack.isEmpty()) {
            return;
        }
        if (User != null) {
            ItemHandlerHelper.giveItemToPlayer(User, Stack);
        } else {
            Containers.dropItemStack(World, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Stack);
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && mainPos(State, Pos).equals(Pos)) {
            if (World.getBlockEntity(Pos) instanceof StorageContainerBlockEntity Locker) {
                Locker.dropAll(World, Pos);
            }
            ItemStack Upgrade = State.getValue(Environment).upgradeStack();
            if (!Upgrade.isEmpty()) {
                Containers.dropItemStack(World, Pos.getX() + 0.5, Pos.getY() + 0.5, Pos.getZ() + 0.5, Upgrade);
            }
            World.updateNeighbourForOutputSignal(Pos, this);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState State) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState State, Level World, BlockPos Pos) {
        StorageContainerBlockEntity Locker = containerAt(World, Pos);
        return Locker != null && Locker.hasContents() ? FullSignal : 0;
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
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable(getDescriptionId() + ".tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.storage_container.toggle").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.storage_container.store").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.storage_container.accepts_catalogued").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.storage_container.glass").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.storage_container.upgrades").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.storage_container.screwdriver").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new StorageContainerBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? createTickerHelper(Type, StorageRegistry.ContainerEntity.get(), StorageContainerBlockEntity::clientTick)
                : createTickerHelper(Type, StorageRegistry.ContainerEntity.get(), StorageContainerBlockEntity::serverTick);
    }
}
