package net.rai.scp_unleashed.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
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
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.rai.scp_unleashed.block.entity.CreativePressurizerBlockEntity;
import net.rai.scp_unleashed.door.Screwdrivable;
import net.rai.scp_unleashed.gas.GasHandler;
import net.rai.scp_unleashed.gas.GasVoid;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.pressure.FluidPort;
import net.rai.scp_unleashed.pressure.FluidVoid;
import net.rai.scp_unleashed.pressure.GasPort;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CreativePressurizerBlock extends BaseEntityBlock implements FluidPort, GasPort, Screwdrivable {
    public static final MapCodec<CreativePressurizerBlock> BlockCodec = simpleCodec(CreativePressurizerBlock::new);
    public static final BooleanProperty Voiding = BooleanProperty.create("voiding");

    public CreativePressurizerBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Voiding, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Voiding);
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Voiding, true);
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
    public InteractionResult screwdrive(Level World, BlockPos Pos, BlockState State, @Nullable Player User, boolean Sneaking) {
        boolean Next = !State.getValue(Voiding);
        World.setBlock(Pos, State.setValue(Voiding, Next), Block.UPDATE_ALL);
        if (World instanceof ServerLevel ServerWorld) {
            PressureNetworkManager.blockChanged(ServerWorld, Pos);
        }
        if (User != null) {
            User.displayClientMessage(Component.translatable(Next ? "message.scp_unleashed.creative_pressurizer.voiding" : "message.scp_unleashed.creative_pressurizer.pressure_only"), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public PortRole portRole(BlockState State, Direction Side) {
        return State.getValue(Voiding) ? PortRole.Input : null;
    }

    @Nullable
    @Override
    public IFluidHandler portHandler(Level World, BlockPos Pos, BlockState State) {
        return State.getValue(Voiding) ? FluidVoid.Instance : null;
    }

    @Override
    public BlockPos portOwner(Level World, BlockPos Pos, BlockState State) {
        return Pos;
    }

    @Nullable
    @Override
    public PortRole gasRole(BlockState State, Direction Side) {
        return State.getValue(Voiding) ? PortRole.Input : null;
    }

    @Nullable
    @Override
    public GasHandler gasHandler(Level World, BlockPos Pos, BlockState State) {
        return State.getValue(Voiding) ? GasVoid.Instance : null;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.creative_pressurizer.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.creative_pressurizer.detail").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.creative_pressurizer.void").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.creative_pressurizer.toggle").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new CreativePressurizerBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, ModBlockEntityTypes.CreativePressurizer.get(), CreativePressurizerBlockEntity::serverTick);
    }
}
