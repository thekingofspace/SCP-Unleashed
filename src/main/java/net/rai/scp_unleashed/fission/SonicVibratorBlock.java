package net.rai.scp_unleashed.fission;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.rai.scp_unleashed.block.FluidHands;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.pressure.FluidPort;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import net.rai.scp_unleashed.pressure.RestrictedFluidHandler;
import net.rai.scp_unleashed.upgrade.UpgradeType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SonicVibratorBlock extends BaseEntityBlock implements FluidPort {
    public static final MapCodec<SonicVibratorBlock> BlockCodec = simpleCodec(SonicVibratorBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Active = BooleanProperty.create("active");
    public static final int ActiveLight = 5;

    public SonicVibratorBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Active, false));
    }

    public static Direction inputSide(BlockState State) {
        return State.getValue(Facing).getClockWise();
    }

    public static Direction outputSide(BlockState State) {
        return State.getValue(Facing).getCounterClockWise();
    }

    @Nullable
    public static IFluidHandler capabilityAt(Level World, BlockPos Pos, BlockState State, @Nullable Direction Side) {
        if (!(World.getBlockEntity(Pos) instanceof SonicVibratorBlockEntity Machine)) {
            return null;
        }
        if (Side == null) {
            return Machine.getCombined();
        }
        if (Side == inputSide(State)) {
            return new RestrictedFluidHandler(Machine.getInputTank(), PortRole.Input);
        }
        return Side == outputSide(State) ? new RestrictedFluidHandler(Machine.getOutputTank(), PortRole.Output) : null;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Active);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite());
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
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (!OldState.is(this) && World instanceof ServerLevel ServerWorld) {
            PressureNetworkManager.blockChanged(ServerWorld, Pos);
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World instanceof ServerLevel ServerWorld) {
            if (World.getBlockEntity(Pos) instanceof SonicVibratorBlockEntity Machine) {
                Machine.dropUpgrades(World, Pos);
            }
            PressureNetworkManager.blockChanged(ServerWorld, Pos);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!World.isClientSide && World.getBlockEntity(Pos) instanceof SonicVibratorBlockEntity Machine) {
            User.openMenu(Machine);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (FluidUtil.getFluidHandler(Stack).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!World.isClientSide && World.getBlockEntity(Pos) instanceof SonicVibratorBlockEntity Machine) {
            FluidHands.interact(User, Hand, Machine.getCombined());
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Nullable
    @Override
    public PortRole portRole(BlockState State, Direction Side) {
        if (Side == inputSide(State)) {
            return PortRole.Input;
        }
        return Side == outputSide(State) ? PortRole.Output : null;
    }

    @Nullable
    @Override
    public IFluidHandler portHandler(Level World, BlockPos Pos, BlockState State) {
        return World.getBlockEntity(Pos) instanceof SonicVibratorBlockEntity Machine ? Machine.getCombined() : null;
    }

    @Override
    public BlockPos portOwner(Level World, BlockPos Pos, BlockState State) {
        return Pos;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.sonic_vibrator.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.sonic_vibrator.rate_active", SonicVibratorBlockEntity.CleanRate * 20, SonicVibratorBlockEntity.WorkDraw).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.machine.speed_upgrades", UpgradeType.Speed.getLimit()).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.sonic_vibrator.ports").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.sonic_vibrator.buckets").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.sonic_vibrator.wiring").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.sonic_vibrator.warm").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new SonicVibratorBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return createTickerHelper(Type, FissionRegistry.SonicVibratorEntity.get(), World.isClientSide ? SonicVibratorBlockEntity::clientTick : SonicVibratorBlockEntity::serverTick);
    }
}
