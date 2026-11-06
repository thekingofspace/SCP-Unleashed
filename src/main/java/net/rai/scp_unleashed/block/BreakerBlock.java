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
import net.minecraft.world.level.BlockGetter;
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
import net.rai.scp_unleashed.block.entity.BreakerBlockEntity;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.registry.ModBlockEntityTypes;
import net.rai.scp_unleashed.wire.PowerSwitch;
import net.rai.scp_unleashed.wire.WireNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BreakerBlock extends BaseEntityBlock implements PowerSwitch {
    public static final MapCodec<BreakerBlock> BlockCodec = simpleCodec(BreakerBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Open = BooleanProperty.create("open");

    public BreakerBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Open, false));
    }

    @Override
    public boolean isClosed(BlockState State) {
        return !State.getValue(Open);
    }

    @Override
    public boolean tripsOnOverload() {
        return true;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Open);
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
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (World instanceof ServerLevel ServerWorld && World.getBlockEntity(Pos) instanceof BreakerBlockEntity Breaker) {
            if (State.getValue(Open)) {
                Breaker.reset(ServerWorld);
            } else {
                Breaker.trip(ServerWorld);
            }
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected boolean isSignalSource(BlockState State) {
        return true;
    }

    @Override
    protected int getSignal(BlockState State, BlockGetter World, BlockPos Pos, Direction Side) {
        return State.getValue(Open) ? 15 : 0;
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (World instanceof ServerLevel ServerWorld) {
            WireNetworkManager.get(ServerWorld).markDirty();
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World instanceof ServerLevel ServerWorld) {
            if (World.getBlockEntity(Pos) instanceof BreakerBlockEntity Breaker) {
                Breaker.dropUpgrades(World, Pos);
            }
            WireNetworkManager.get(ServerWorld).markDirty();
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
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
        Lines.add(Component.translatable("block.scp_unleashed.breaker.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.switchgear.line").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.breaker.reset").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.switchgear.data").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.breaker.upgrades").withStyle(ChatFormatting.GOLD));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new BreakerBlockEntity(Pos, State);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level World, BlockState State, BlockEntityType<T> Type) {
        return World.isClientSide ? null : createTickerHelper(Type, ModBlockEntityTypes.Breaker.get(), BreakerBlockEntity::serverTick);
    }
}
