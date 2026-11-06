package net.rai.scp_unleashed.fluiddump;

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
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.rai.scp_unleashed.block.FluidHands;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.pressure.FluidPort;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import net.rai.scp_unleashed.pressure.RestrictedFluidHandler;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FluidDumpBlock extends BaseEntityBlock implements FluidPort {
    public static final MapCodec<FluidDumpBlock> BlockCodec = simpleCodec(FluidDumpBlock::new);

    public FluidDumpBlock(Properties Properties) {
        super(Properties);
    }

    @Nullable
    public static IFluidHandler capabilityAt(Level World, BlockPos Pos) {
        return World.getBlockEntity(Pos) instanceof FluidDumpBlockEntity Dump ? new RestrictedFluidHandler(Dump.getBuffer(), PortRole.Input) : null;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
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
        if (FluidUtil.getFluidContained(Stack).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (World.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        IFluidHandler Inlet = capabilityAt(World, Pos);
        if (Inlet != null && FluidHands.interact(User, Hand, Inlet)) {
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Nullable
    @Override
    public PortRole portRole(BlockState State, Direction Side) {
        return PortRole.Input;
    }

    @Nullable
    @Override
    public IFluidHandler portHandler(Level World, BlockPos Pos, BlockState State) {
        return capabilityAt(World, Pos);
    }

    @Override
    public BlockPos portOwner(Level World, BlockPos Pos, BlockState State) {
        return Pos;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.fluid_dump.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.fluid_dump.dry", DryPour.Reach, DryPour.Depth).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.fluid_dump.water", FluidDumpBlockEntity.FlowPerTick * 20).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.fluid_dump.lava").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.fluid_dump.waste").withStyle(ChatFormatting.RED));
            Lines.add(Component.translatable("block.scp_unleashed.fluid_dump.recover").withStyle(ChatFormatting.GRAY));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new FluidDumpBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return createTickerHelper(Type, FluidDumpRegistry.FluidDumpEntity.get(), World.isClientSide ? FluidDumpBlockEntity::clientTick : FluidDumpBlockEntity::serverTick);
    }
}
