package net.rai.scp_unleashed.matrix;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.wire.WireHostBlock;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InstabilityMatrixBlock extends BaseEntityBlock implements WireHostBlock, LargeBlock {
    public static final MapCodec<InstabilityMatrixBlock> BlockCodec = simpleCodec(InstabilityMatrixBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> Half = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<MatrixMode> Mode = EnumProperty.create("mode", MatrixMode.class);
    private static final VoxelShape CapSupport = box(0.0, 13.0, 0.0, 16.0, 16.0, 16.0);

    public InstabilityMatrixBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Half, DoubleBlockHalf.LOWER).setValue(Mode, MatrixMode.Off));
    }

    public static int lightOf(BlockState State) {
        boolean Upper = State.getValue(Half) == DoubleBlockHalf.UPPER;
        return switch (State.getValue(Mode)) {
            case Active -> Upper ? 13 : 5;
            case Idle -> Upper ? 4 : 2;
            default -> 0;
        };
    }

    public static void setMode(Level World, BlockPos LowerPos, BlockState LowerState, MatrixMode Next) {
        if (LowerState.getValue(Mode) != Next) {
            World.setBlock(LowerPos, LowerState.setValue(Mode, Next), Block.UPDATE_CLIENTS);
        }
        BlockState UpperState = World.getBlockState(LowerPos.above());
        if (UpperState.getBlock() instanceof InstabilityMatrixBlock && UpperState.getValue(Half) == DoubleBlockHalf.UPPER && UpperState.getValue(Mode) != Next) {
            World.setBlock(LowerPos.above(), UpperState.setValue(Mode, Next), Block.UPDATE_CLIENTS);
        }
    }

    @Nullable
    public static InstabilityMatrixBlockEntity masterEntity(Level World, BlockPos Pos, BlockState State) {
        if (!(State.getBlock() instanceof InstabilityMatrixBlock Matrix)) {
            return null;
        }
        return World.getBlockEntity(Matrix.devicePos(State, Pos)) instanceof InstabilityMatrixBlockEntity Found ? Found : null;
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
    protected VoxelShape getBlockSupportShape(BlockState State, BlockGetter World, BlockPos Pos) {
        return State.getValue(Half) == DoubleBlockHalf.UPPER ? CapSupport : Shapes.block();
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
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide && masterEntity(World, Pos, State) instanceof InstabilityMatrixBlockEntity Matrix) {
            User.openMenu(Matrix);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState State) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState State, Level World, BlockPos Pos) {
        InstabilityMatrixBlockEntity Matrix = masterEntity(World, Pos, State);
        return Matrix == null ? 0 : Matrix.getComparatorSignal();
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
        Lines.add(Component.translatable("block.scp_unleashed.instability_matrix.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.instability_matrix.field", MatrixTuning.FieldRadius).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.instability_matrix.draws").withStyle(ChatFormatting.GOLD));
            for (FragmentRarity Tier : FragmentRarity.values()) {
                MutableComponent Name = Component.translatable(tierKey(Tier)).withStyle(Style.EMPTY.withColor(Tier.getColour() & 0xFFFFFF));
                Lines.add(Component.translatable("block.scp_unleashed.instability_matrix.draw_line", Name, MatrixTuning.grouped(MatrixTuning.drawFor(Tier)),
                        MatrixTuning.clock(MatrixTuning.fullPowerSeconds(Tier))).withStyle(ChatFormatting.GRAY));
            }
            Lines.add(Component.translatable("block.scp_unleashed.instability_matrix.standby_free").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.instability_matrix.cap").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.instability_matrix.wiring").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.instability_matrix.comparator").withStyle(ChatFormatting.GOLD));
        }
    }

    public static String tierKey(FragmentRarity Tier) {
        return "gui.scp_unleashed.instability_matrix.tier." + Tier.getSerializedName();
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Half) == DoubleBlockHalf.LOWER ? new InstabilityMatrixBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (State.getValue(Half) != DoubleBlockHalf.LOWER) {
            return null;
        }
        return World.isClientSide ? createTickerHelper(Type, MatrixRegistry.MatrixEntity.get(), InstabilityMatrixBlockEntity::clientTick)
                : createTickerHelper(Type, MatrixRegistry.MatrixEntity.get(), InstabilityMatrixBlockEntity::serverTick);
    }
}
