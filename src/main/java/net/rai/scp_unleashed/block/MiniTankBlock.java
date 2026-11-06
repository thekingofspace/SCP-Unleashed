package net.rai.scp_unleashed.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.rai.scp_unleashed.block.entity.MiniTankBlockEntity;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.pressure.FluidPort;
import net.rai.scp_unleashed.pressure.MiniTankColumn;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MiniTankBlock extends BaseEntityBlock implements FluidPort {
    public static final MapCodec<MiniTankBlock> BlockCodec = simpleCodec(MiniTankBlock::new);
    public static final BooleanProperty Top = BooleanProperty.create("top");
    public static final BooleanProperty Bottom = BooleanProperty.create("bottom");
    public static final int Capacity = 4 * FluidType.BUCKET_VOLUME;
    public static final int MaxStack = 32;

    public MiniTankBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Top, true).setValue(Bottom, true));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Top, Bottom);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        return defaultBlockState()
                .setValue(Top, !(World.getBlockState(Pos.above()).getBlock() instanceof MiniTankBlock))
                .setValue(Bottom, !(World.getBlockState(Pos.below()).getBlock() instanceof MiniTankBlock));
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        if (Side == Direction.UP) {
            return State.setValue(Top, !(NeighborState.getBlock() instanceof MiniTankBlock));
        }
        if (Side == Direction.DOWN) {
            return State.setValue(Bottom, !(NeighborState.getBlock() instanceof MiniTankBlock));
        }
        return State;
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (World instanceof ServerLevel ServerWorld) {
            PressureNetworkManager.blockChanged(ServerWorld, Pos);
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World instanceof ServerLevel ServerWorld) {
            PressureNetworkManager.blockChanged(ServerWorld, Pos);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (FluidUtil.getFluidHandler(Stack).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!World.isClientSide) {
            FluidHands.interact(User, Hand, MiniTankColumn.of(World, Pos));
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Nullable
    @Override
    public PortRole portRole(BlockState State, Direction Side) {
        if (Side == Direction.UP && State.getValue(Top)) {
            return PortRole.Input;
        }
        if (Side == Direction.DOWN && State.getValue(Bottom)) {
            return PortRole.Output;
        }
        return null;
    }

    @Nullable
    @Override
    public IFluidHandler portHandler(Level World, BlockPos Pos, BlockState State) {
        return MiniTankColumn.of(World, Pos);
    }

    @Override
    public BlockPos portOwner(Level World, BlockPos Pos, BlockState State) {
        return MiniTankColumn.bottomOf(World, Pos);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.mini_tank.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.tank.capacity", Capacity / FluidType.BUCKET_VOLUME).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.mini_tank.stacking").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.mini_tank.valves").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new MiniTankBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? createTickerHelper(Type, ModBlockEntityTypes.MiniTank.get(), MiniTankBlockEntity::clientTick)
                : createTickerHelper(Type, ModBlockEntityTypes.MiniTank.get(), MiniTankBlockEntity::serverTick);
    }
}
