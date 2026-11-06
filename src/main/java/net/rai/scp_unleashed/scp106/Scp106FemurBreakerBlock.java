package net.rai.scp_unleashed.scp106;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
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
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.block.LargeBlock;
import net.rai.scp_unleashed.item.ItemTooltips;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Scp106FemurBreakerBlock extends BaseEntityBlock implements LargeBlock {
    public static final MapCodec<Scp106FemurBreakerBlock> BlockCodec = simpleCodec(Scp106FemurBreakerBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, Scp106FemurLayout.CellCount - 1);
    public static final BooleanProperty Occupied = BooleanProperty.create("occupied");
    private static boolean Dismantling;

    public Scp106FemurBreakerBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Part, Scp106FemurLayout.MasterPart).setValue(Occupied, false));
    }

    public static BlockPos originOf(BlockPos Cell, BlockState State) {
        return Scp106FemurLayout.originOf(Cell, State.getValue(Facing), State.getValue(Part));
    }

    @Nullable
    public static Scp106FemurBreakerBlockEntity masterEntity(BlockGetter World, BlockPos Cell, BlockState State) {
        if (!(State.getBlock() instanceof Scp106FemurBreakerBlock)) {
            return null;
        }
        return World.getBlockEntity(originOf(Cell, State)) instanceof Scp106FemurBreakerBlockEntity Machine ? Machine : null;
    }

    public static void setOccupied(Level World, BlockPos Origin, BlockState MasterState, boolean Taken) {
        Direction Front = MasterState.getValue(Facing);
        for (int Index = 0; Index < Scp106FemurLayout.CellCount; Index++) {
            BlockPos Cell = Scp106FemurLayout.cellPos(Origin, Front, Index);
            BlockState Current = World.getBlockState(Cell);
            if (Current.getBlock() instanceof Scp106FemurBreakerBlock && Current.getValue(Part) == Index && Current.getValue(Occupied) != Taken) {
                World.setBlock(Cell, Current.setValue(Occupied, Taken), Block.UPDATE_CLIENTS);
            }
        }
    }

    @Override
    public List<BlockPos> getFootprint(BlockPlaceContext Context) {
        Direction Front = Context.getHorizontalDirection().getOpposite();
        return Scp106FemurLayout.cells(Context.getClickedPos(), Front);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Part, Occupied);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Scp106FemurLayout.shape(State.getValue(Facing), State.getValue(Part));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState State, BlockGetter World, BlockPos Pos, CollisionContext Context) {
        return Scp106FemurLayout.shape(State.getValue(Facing), State.getValue(Part));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        if (!LargeBlock.canPlace(Context, getFootprint(Context))) {
            return null;
        }
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite()).setValue(Part, Scp106FemurLayout.MasterPart);
    }

    @Override
    public void setPlacedBy(Level World, BlockPos Pos, BlockState State, @Nullable LivingEntity Placer, ItemStack Stack) {
        if (World.isClientSide) {
            return;
        }
        Direction Front = State.getValue(Facing);
        for (int Index = 0; Index < Scp106FemurLayout.CellCount; Index++) {
            if (Index != Scp106FemurLayout.MasterPart) {
                World.setBlock(Scp106FemurLayout.cellPos(Pos, Front, Index), State.setValue(Part, Index), Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && !World.isClientSide && !Dismantling) {
            Dismantling = true;
            try {
                BlockPos Origin = originOf(Pos, State);
                Direction Front = State.getValue(Facing);
                if (World.getBlockEntity(Origin) instanceof Scp106FemurBreakerBlockEntity Machine) {
                    Machine.release(null);
                }
                for (int Index = 0; Index < Scp106FemurLayout.CellCount; Index++) {
                    BlockPos Cell = Scp106FemurLayout.cellPos(Origin, Front, Index);
                    if (Cell.equals(Pos)) {
                        continue;
                    }
                    BlockState CellState = World.getBlockState(Cell);
                    if (CellState.is(this) && CellState.getValue(Part) == Index && CellState.getValue(Facing) == Front) {
                        World.setBlock(Cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                    }
                }
            } finally {
                Dismantling = false;
            }
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!Stack.is(Items.LEAD) || !Scp106FemurLayout.isChair(State.getValue(Part))) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!World.isClientSide) {
            Scp106FemurBreakerBlockEntity Machine = masterEntity(World, Pos, State);
            if (Machine != null) {
                Machine.seatLeashed(User);
            }
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (User.isSpectator()) {
            return InteractionResult.PASS;
        }
        if (World.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Scp106FemurBreakerBlockEntity Machine = masterEntity(World, Pos, State);
        if (Machine == null) {
            return InteractionResult.CONSUME;
        }
        if (Scp106FemurLayout.isChair(State.getValue(Part))) {
            Machine.useChair(User);
        } else {
            Machine.pullLever(User);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    protected BlockState rotate(BlockState State, Rotation Turn) {
        return State.setValue(Facing, Turn.rotate(State.getValue(Facing)));
    }

    @Override
    protected BlockState mirror(BlockState State, Mirror Flip) {
        return State;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.scp106_femur_breaker.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.scp106_femur_breaker.size").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.scp106_femur_breaker.chair").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.scp106_femur_breaker.lever").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return State.getValue(Part) == Scp106FemurLayout.MasterPart ? new Scp106FemurBreakerBlockEntity(Pos, State) : null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        if (State.getValue(Part) != Scp106FemurLayout.MasterPart || World.isClientSide) {
            return null;
        }
        return createTickerHelper(Type, Scp106Registry.FemurBreakerEntity.get(), Scp106FemurBreakerBlockEntity::serverTick);
    }
}
