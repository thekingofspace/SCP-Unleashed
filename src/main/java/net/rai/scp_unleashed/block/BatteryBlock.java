package net.rai.scp_unleashed.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.rai.scp_unleashed.block.entity.BatteryBlockEntity;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.wire.BatteryBank;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class BatteryBlock extends BaseEntityBlock {
    public static final MapCodec<BatteryBlock> BlockCodec = simpleCodec(BatteryBlock::new);
    public static final Map<Direction, BooleanProperty> Connections = PipeBlock.PROPERTY_BY_DIRECTION;

    public BatteryBlock(Properties Properties) {
        super(Properties);
        BlockState Default = stateDefinition.any();
        for (BooleanProperty Connection : Connections.values()) {
            Default = Default.setValue(Connection, false);
        }
        registerDefaultState(Default);
    }

    private BlockState connect(BlockGetter World, BlockPos Pos, BlockState State) {
        for (Map.Entry<Direction, BooleanProperty> Entry : Connections.entrySet()) {
            State = State.setValue(Entry.getValue(), World.getBlockState(Pos.relative(Entry.getKey())).getBlock() instanceof BatteryBlock);
        }
        return State;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Connections.values().toArray(new BooleanProperty[0]));
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return connect(Context.getLevel(), Context.getClickedPos(), defaultBlockState());
    }

    @Override
    protected BlockState updateShape(BlockState State, Direction Side, BlockState NeighborState, LevelAccessor World, BlockPos Pos, BlockPos NeighborPos) {
        return State.setValue(Connections.get(Side), NeighborState.getBlock() instanceof BatteryBlock);
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (!OldState.is(this) && World instanceof ServerLevel ServerWorld) {
            WireNetworkManager.get(ServerWorld).markDirty();
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World instanceof ServerLevel ServerWorld) {
            WireNetworkManager.get(ServerWorld).markDirty();
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.battery.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.battery.capacity", BatteryBank.CapacityPerCell).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.battery.charge_rate", BatteryBank.TransferPerCell).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.battery.discharge").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.battery.bank").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new BatteryBlockEntity(Pos, State);
    }
}
