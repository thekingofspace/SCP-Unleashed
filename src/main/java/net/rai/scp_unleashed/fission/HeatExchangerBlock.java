package net.rai.scp_unleashed.fission;

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
import net.minecraft.world.level.Level;
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
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.rai.scp_unleashed.block.FluidHands;
import net.rai.scp_unleashed.gas.GasHandler;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.pressure.FluidPort;
import net.rai.scp_unleashed.pressure.GasPort;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import net.rai.scp_unleashed.pressure.RestrictedFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class HeatExchangerBlock extends BaseEntityBlock implements FluidPort, GasPort {
    public static final MapCodec<HeatExchangerBlock> BlockCodec = simpleCodec(HeatExchangerBlock::new);
    public static final BooleanProperty Active = BooleanProperty.create("active");

    public HeatExchangerBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Active, false));
    }

    @Nullable
    public static IFluidHandler capabilityAt(Level World, BlockPos Pos, @Nullable Direction Side) {
        if (Side == Direction.UP || !(World.getBlockEntity(Pos) instanceof HeatExchangerBlockEntity Exchanger)) {
            return null;
        }
        return new RestrictedFluidHandler(Exchanger.getTank(), PortRole.Input);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Active);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
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
            PressureNetworkManager.blockChanged(ServerWorld, Pos);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (FluidUtil.getFluidHandler(Stack).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!World.isClientSide && World.getBlockEntity(Pos) instanceof HeatExchangerBlockEntity Exchanger) {
            FluidHands.interact(User, Hand, new RestrictedFluidHandler(Exchanger.getTank(), PortRole.Input));
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Nullable
    @Override
    public PortRole portRole(BlockState State, Direction Side) {
        return Side == Direction.UP ? null : PortRole.Input;
    }

    @Nullable
    @Override
    public IFluidHandler portHandler(Level World, BlockPos Pos, BlockState State) {
        return World.getBlockEntity(Pos) instanceof HeatExchangerBlockEntity Exchanger ? Exchanger.getTank() : null;
    }

    @Override
    public BlockPos portOwner(Level World, BlockPos Pos, BlockState State) {
        return Pos;
    }

    @Nullable
    @Override
    public PortRole gasRole(BlockState State, Direction Side) {
        return Side == Direction.UP ? null : PortRole.Output;
    }

    @Nullable
    @Override
    public GasHandler gasHandler(Level World, BlockPos Pos, BlockState State) {
        return World.getBlockEntity(Pos) instanceof HeatExchangerBlockEntity Exchanger ? GasHandler.drainOnly(Exchanger.getSteam()) : null;
    }

    @Override
    public boolean exhausts(BlockState State) {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.fission_heat_exchanger.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.fission_heat_exchanger.placement").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.fission_heat_exchanger.water", HeatExchangerBlockEntity.Capacity).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.fission_heat_exchanger.flow").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.fission_heat_exchanger.shared").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.fission_heat_exchanger.steam").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new HeatExchangerBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, FissionRegistry.HeatExchangerEntity.get(), HeatExchangerBlockEntity::serverTick);
    }
}
