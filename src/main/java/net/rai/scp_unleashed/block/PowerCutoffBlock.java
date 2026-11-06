package net.rai.scp_unleashed.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.rai.scp_unleashed.item.ItemTooltips;
import net.rai.scp_unleashed.registry.ModSounds;
import net.rai.scp_unleashed.wire.PowerSwitch;
import net.rai.scp_unleashed.wire.WireNetworkManager;

import java.util.List;

public class PowerCutoffBlock extends Block implements PowerSwitch {
    public static final MapCodec<PowerCutoffBlock> BlockCodec = simpleCodec(PowerCutoffBlock::new);
    public static final DirectionProperty Facing = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty Open = BooleanProperty.create("open");

    public PowerCutoffBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(stateDefinition.any().setValue(Facing, Direction.NORTH).setValue(Open, false));
    }

    @Override
    public boolean isClosed(BlockState State) {
        return !State.getValue(Open);
    }

    @Override
    public boolean tripsOnOverload() {
        return false;
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return BlockCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Open);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Facing, Context.getHorizontalDirection().getOpposite())
                .setValue(Open, Context.getLevel().hasNeighborSignal(Context.getClickedPos()));
    }

    @Override
    protected void neighborChanged(BlockState State, Level World, BlockPos Pos, Block Neighbor, BlockPos NeighborPos, boolean MovedByPiston) {
        super.neighborChanged(State, World, Pos, Neighbor, NeighborPos, MovedByPiston);
        if (World.isClientSide) {
            return;
        }
        boolean Powered = World.hasNeighborSignal(Pos);
        if (Powered != State.getValue(Open)) {
            World.setBlock(Pos, State.setValue(Open, Powered), Block.UPDATE_ALL);
            World.playSound(null, Pos, Powered ? ModSounds.CutoffOpen.get() : ModSounds.CutoffClose.get(), SoundSource.BLOCKS, 1.0f, 1.0f);
        }
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
        Lines.add(Component.translatable("block.scp_unleashed.power_cutoff.tooltip").withStyle(ChatFormatting.GRAY));
        if (ItemTooltips.showDetails(Lines)) {
            Lines.add(Component.translatable("block.scp_unleashed.switchgear.line").withStyle(ChatFormatting.GOLD));
            Lines.add(Component.translatable("block.scp_unleashed.switchgear.data").withStyle(ChatFormatting.GOLD));
        }
    }
}
