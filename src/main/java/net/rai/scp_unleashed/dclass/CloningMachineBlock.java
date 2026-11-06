package net.rai.scp_unleashed.dclass;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.wire.WireHostBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CloningMachineBlock extends BaseEntityBlock implements WireHostBlock, LargeBlock {
    public static final MapCodec<CloningMachineBlock> BlockCodec = simpleCodec(CloningMachineBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> Half = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<CloningLamp> Lamp = EnumProperty.create("lamp", CloningLamp.class);
    public static final BooleanProperty Open = BlockStateProperties.OPEN;

    public CloningMachineBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Half, DoubleBlockHalf.LOWER).setValue(Lamp, CloningLamp.Off).setValue(Open, false));
    }

    public static int lightOf(BlockState State) {
        if (State.getValue(Lamp) == CloningLamp.Working) {
            return State.getValue(Half) == DoubleBlockHalf.UPPER ? 7 : 5;
        }
        return State.getValue(Lamp) == CloningLamp.Off ? 0 : 2;
    }

    public static void setLook(Level World, BlockPos LowerPos, CloningLamp NewLamp, boolean NewOpen) {
        for (BlockPos Cell : List.of(LowerPos, LowerPos.above())) {
            BlockState State = World.getBlockState(Cell);
            if (State.getBlock() instanceof CloningMachineBlock && (State.getValue(Lamp) != NewLamp || State.getValue(Open) != NewOpen)) {
                World.setBlock(Cell, State.setValue(Lamp, NewLamp).setValue(Open, NewOpen), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Nullable
    public static CloningMachineBlockEntity masterEntity(Level World, BlockPos Pos, BlockState State) {
        if (!(State.getBlock() instanceof CloningMachineBlock Machine)) {
            return null;
        }
        return World.getBlockEntity(Machine.devicePos(State, Pos)) instanceof CloningMachineBlockEntity Tank ? Tank : null;
    }

    @Override
    public BlockPos devicePos(BlockState State, BlockPos Pos) {
        return State.getValue(Half) == DoubleBlockHalf.UPPER ? Pos.below() : Pos;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Half, Lamp, Open);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        BlockPos Pos = Context.getClickedPos();
        return List.of(Pos, Pos.above());
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            return null;
        }
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        World.setBlock(Pos.above(), State.setValue(Half, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        DoubleBlockHalf ThisHalf = State.getValue(Half);
        if (Side.getAxis() == Direction.Axis.Y && (ThisHalf == DoubleBlockHalf.LOWER) == (Side == Direction.UP)) {
            boolean Partner = NeighborState.is(this) && NeighborState.getValue(Half) != ThisHalf;
            return Partner ? State : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(State, Side, NeighborState, World, Pos, NeighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player User) {
        if (!World.isClientSide && User.isCreative() && State.getValue(Half) == DoubleBlockHalf.UPPER) {
            BlockPos LowerPos = Pos.below();
            BlockState LowerState = World.getBlockState(LowerPos);
            if (LowerState.is(this) && LowerState.getValue(Half) == DoubleBlockHalf.LOWER) {
                World.setBlock(LowerPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                World.levelEvent(User, 2001, LowerPos, Block.getId(LowerState));
            }
        }
        return super.playerWillDestroy(World, Pos, State, User);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && State.getValue(Half) == DoubleBlockHalf.LOWER && World.getBlockEntity(Pos) instanceof CloningMachineBlockEntity Tank) {
            Tank.dropContents(World, Pos);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!(Stack.getItem() instanceof CloningBloodItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        CloningMachineBlockEntity Tank = masterEntity(World, Pos, State);
        if (Tank == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!World.isClientSide && !Tank.feed(Stack, User)) {
            User.displayClientMessage(Component.translatable("message.scp_unleashed.cloning_machine.full").withStyle(ChatFormatting.RED), true);
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide && User instanceof ServerPlayer Server && masterEntity(World, Pos, State) instanceof CloningMachineBlockEntity Tank) {
            Server.openMenu(Tank, Buffer -> Buffer.writeBlockPos(Tank.getBlockPos()));
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
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
        Lines.add(Component.translatable("block.scp_unleashed.cloning_machine.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.cloning_machine.draw", CloningMachineBlockEntity.WorkingDraw).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.cloning_machine.cycle", CloningMachineBlockEntity.CycleTicks / 20).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.cloning_machine.space").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.cloning_machine.wiring").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Half) == DoubleBlockHalf.LOWER ? new CloningMachineBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (State.getValue(Half) != DoubleBlockHalf.LOWER) {
            return null;
        }
        return World.isClientSide ? createTickerHelper(Type, DClassRegistry.CloningMachineEntity.get(), CloningMachineBlockEntity::clientTick)
                : createTickerHelper(Type, DClassRegistry.CloningMachineEntity.get(), CloningMachineBlockEntity::serverTick);
    }
}
