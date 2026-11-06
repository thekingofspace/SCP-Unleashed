package net.rai.scp_unleashed.gas;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.fission.FissionRegistry;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.pressure.GasPort;
import net.rai.scp_unleashed.pressure.PortRole;
import net.rai.scp_unleashed.pressure.PressureNetworkManager;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GasTankBlock extends BaseEntityBlock implements GasPort {
    public static final MapCodec<GasTankBlock> BlockCodec = simpleCodec(GasTankBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final int MaxFill = 8;
    public static final IntegerProperty Fill = IntegerProperty.create("fill", 0, MaxFill);

    public GasTankBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Fill, 0));
    }

    public static boolean isInlet(BlockState State, Direction Side) {
        return Side == Direction.UP || Side == State.getValue(Facing).getClockWise();
    }

    public static boolean isOutlet(BlockState State, Direction Side) {
        return Side == Direction.DOWN || Side == State.getValue(Facing).getCounterClockWise();
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Fill);
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
            if (World.getBlockEntity(Pos) instanceof GasTankBlockEntity Tank && !Tank.getTank().isEmpty()) {
                GasStack Held = Tank.getTank().getGas();
                Vec3 Centre = Vec3.atCenterOf(Pos);
                Held.getGas().release(ServerWorld, Centre, Held.getAmount());
                World.playSound(null, Pos, FissionRegistry.GasHiss.get(), SoundSource.BLOCKS, 1.0f, 0.8f);
            }
            PressureNetworkManager.blockChanged(ServerWorld, Pos);
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState State, Level World, BlockPos Pos, Player User, BlockHitResult Hit) {
        if (!(World.getBlockEntity(Pos) instanceof GasTankBlockEntity Tank)) {
            return InteractionResult.PASS;
        }
        if (!World.isClientSide) {
            GasStack Held = Tank.getTank().getGas();
            Component Line = Held.isEmpty()
                    ? Component.translatable("message.scp_unleashed.gas_tank.empty", GasTankBlockEntity.Capacity)
                    : Component.translatable("message.scp_unleashed.gas_tank.contents", Held.getGas().getName(), Held.getAmount(), GasTankBlockEntity.Capacity);
            User.displayClientMessage(Line, true);
        }
        return InteractionResult.sidedSuccess(World.isClientSide);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState State) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState State, Level World, BlockPos Pos) {
        if (!(World.getBlockEntity(Pos) instanceof GasTankBlockEntity Tank) || Tank.getTank().isEmpty()) {
            return 0;
        }
        return 1 + Math.round(Tank.getTank().getAmount() * 14.0f / GasTankBlockEntity.Capacity);
    }

    @Nullable
    @Override
    public PortRole gasRole(BlockState State, Direction Side) {
        if (isInlet(State, Side)) {
            return PortRole.Input;
        }
        return isOutlet(State, Side) ? PortRole.Output : null;
    }

    @Nullable
    @Override
    public GasHandler gasHandler(Level World, BlockPos Pos, BlockState State) {
        return World.getBlockEntity(Pos) instanceof GasTankBlockEntity Tank ? Tank.getTank() : null;
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.gas_tank.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.gas_tank.capacity", GasTankBlockEntity.Capacity).withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.gas_tank.ports").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.gas_tank.single").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.gas_tank.breaking").withStyle(ChatFormatting.RED));
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos Pos, BlockState State) {
        return new GasTankBlockEntity(Pos, State);
    }
}
