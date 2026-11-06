package net.rai.scp_unleashed.rifttech;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
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
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.upgrade.UpgradeType;
import net.rai.scp_unleashed.wire.WireHostBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RiftCollectorBlock extends BaseEntityBlock implements WireHostBlock, LargeBlock {
    public static final MapCodec<RiftCollectorBlock> BlockCodec = simpleCodec(RiftCollectorBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> Half = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<CollectorMode> Mode = EnumProperty.create("mode", CollectorMode.class);
    private static final VoxelShape TurretShape = Shapes.or(box(2.0, 0.0, 2.0, 14.0, 5.0, 14.0), box(3.0, 5.0, 3.0, 13.0, 14.0, 13.0));

    public RiftCollectorBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Half, DoubleBlockHalf.LOWER).setValue(Mode, CollectorMode.Off));
    }

    public static int lightOf(BlockState State) {
        boolean Upper = State.getValue(Half) == DoubleBlockHalf.UPPER;
        return switch (State.getValue(Mode)) {
            case Active -> Upper ? 9 : 4;
            case Idle -> Upper ? 0 : 2;
            default -> 0;
        };
    }

    public static void setMode(Level World, BlockPos LowerPos, BlockState LowerState, CollectorMode Next) {
        if (LowerState.getValue(Mode) != Next) {
            World.setBlock(LowerPos, LowerState.setValue(Mode, Next), Block.UPDATE_CLIENTS);
        }
        BlockState UpperState = World.getBlockState(LowerPos.above());
        if (UpperState.getBlock() instanceof RiftCollectorBlock && UpperState.getValue(Half) == DoubleBlockHalf.UPPER && UpperState.getValue(Mode) != Next) {
            World.setBlock(LowerPos.above(), UpperState.setValue(Mode, Next), Block.UPDATE_CLIENTS);
        }
    }

    @Nullable
    public static RiftCollectorBlockEntity masterEntity(BlockGetter World, BlockPos Pos, BlockState State) {
        if (!(State.getBlock() instanceof RiftCollectorBlock Collector)) {
            return null;
        }
        return World.getBlockEntity(Collector.devicePos(State, Pos)) instanceof RiftCollectorBlockEntity Found ? Found : null;
    }

    @Nullable
    public static IItemHandler itemHandlerAt(Level World, BlockPos Pos, BlockState State) {
        RiftCollectorBlockEntity Collector = masterEntity(World, Pos, State);
        return Collector == null ? null : Collector.getOutput();
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
        Builder.add(Facing, Half, Mode);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean isPathfindable(BlockState State, PathComputationType Type) {
        return false;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return State.getValue(Half) == DoubleBlockHalf.UPPER ? TurretShape : Shapes.block();
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
        if (!State.is(NewState.getBlock()) && State.getValue(Half) == DoubleBlockHalf.LOWER && World.getBlockEntity(Pos) instanceof RiftCollectorBlockEntity Collector) {
            Collector.dropContents(World, Pos);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide && masterEntity(World, Pos, State) instanceof RiftCollectorBlockEntity Collector) {
            User.openMenu(Collector);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState State) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState State, Level World, BlockPos Pos) {
        RiftCollectorBlockEntity Collector = masterEntity(World, Pos, State);
        return Collector == null ? 0 : ItemHandlerHelper.calcRedstoneFromInventory(Collector.getBuffer());
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
        Lines.add(Component.translatable("block.scp_unleashed.rift_collector.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.rift_collector.range", CollectorTuning.BaseRange, CollectorTuning.maxRange()).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.rift_collector.draw", CollectorTuning.IdleDraw,
                    CollectorTuning.drawFor(FragmentRarity.Faint, false), CollectorTuning.drawFor(FragmentRarity.Abyssal, false)).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.rift_collector.sight").withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("block.scp_unleashed.rift_collector.buffer", CollectorTuning.BufferSlots).withStyle(ChatFormatting.GRAY));
            Lines.add(Component.translatable("block.scp_unleashed.rift_collector.upgrades", UpgradeType.Range.getLimit(), CollectorTuning.RangePerUpgrade).withStyle(ChatFormatting.DARK_AQUA));
            Lines.add(Component.translatable("block.scp_unleashed.rift_collector.wiring").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Half) == DoubleBlockHalf.LOWER ? new RiftCollectorBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (State.getValue(Half) != DoubleBlockHalf.LOWER) {
            return null;
        }
        return World.isClientSide ? createTickerHelper(Type, RiftTechRegistry.RiftCollectorEntity.get(), RiftCollectorBlockEntity::clientTick)
                : createTickerHelper(Type, RiftTechRegistry.RiftCollectorEntity.get(), RiftCollectorBlockEntity::serverTick);
    }
}
