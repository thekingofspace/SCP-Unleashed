package net.rai.scp_unleashed.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.rai.scp_unleashed.block.entity.CreativeTankBlockEntity;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.gas.GasHandler;
import net.rai.scp_unleashed.gas.GasVoid;
import net.rai.scp_unleashed.pressure.FluidPort;
import net.rai.scp_unleashed.pressure.GasPort;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class CreativeTankBlock extends BaseEntityBlock implements FluidPort, GasPort {
    public static final MapCodec<CreativeTankBlock> BlockCodec = simpleCodec(CreativeTankBlock::new);

    public CreativeTankBlock(Properties Properties) {
        super(Properties);
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
    protected ItemInteractionResult useItemOn(ItemStack Stack, BlockState State, Level World, BlockPos Pos, Player User, InteractionHand Hand, BlockHitResult Hit) {
        if (!(World.getBlockEntity(Pos) instanceof CreativeTankBlockEntity Tank)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        Optional<FluidStack> Contained = FluidUtil.getFluidContained(Stack);
        if (Contained.isPresent() && !Contained.get().isEmpty()) {
            if (!World.isClientSide) {
                Tank.setLiquid(Contained.get());
                SoundEvent Sound = Contained.get().getFluid().getFluidType().getSound(SoundActions.BUCKET_EMPTY);
                if (Sound != null) {
                    World.playSound(null, Pos, Sound, SoundSource.BLOCKS, 1.0f, 1.0f);
                }
            }
            return ItemInteractionResult.sidedSuccess(World.isClientSide);
        }
        if (FluidUtil.getFluidHandler(Stack).isEmpty() || Tank.getLiquid().isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!World.isClientSide) {
            FluidHands.interact(User, Hand, Tank.getHandler());
        }
        return ItemInteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!User.isShiftKeyDown() || !(World.getBlockEntity(Pos) instanceof CreativeTankBlockEntity Tank) || Tank.getLiquid().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!World.isClientSide) {
            Tank.setLiquid(FluidStack.EMPTY);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
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

    @Nullable
    @Override
    public PortRole portRole(BlockState State, Direction Side) {
        return PortRole.Both;
    }

    @Nullable
    @Override
    public PortRole gasRole(BlockState State, Direction Side) {
        return PortRole.Input;
    }

    @Nullable
    @Override
    public GasHandler gasHandler(Level World, BlockPos Pos, BlockState State) {
        return GasVoid.Instance;
    }

    @Nullable
    @Override
    public IFluidHandler portHandler(Level World, BlockPos Pos, BlockState State) {
        return World.getBlockEntity(Pos) instanceof CreativeTankBlockEntity Tank ? Tank.getHandler() : null;
    }

    @Override
    public BlockPos portOwner(Level World, BlockPos Pos, BlockState State) {
        return Pos;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.creative_tank.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.creative_tank.set").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.creative_tank.valves").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.creative_tank.void").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new CreativeTankBlockEntity(Pos, State);
    }
}
