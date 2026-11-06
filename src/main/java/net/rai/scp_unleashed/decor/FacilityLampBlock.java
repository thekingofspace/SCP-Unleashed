package net.rai.scp_unleashed.decor;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.rai.scp_unleashed.block.DecoratableBlock;

import java.util.List;

public class FacilityLampBlock extends DecoratableBlock {
    public static final MapCodec<FacilityLampBlock> LampCodec = simpleCodec(FacilityLampBlock::new);
    public static final BooleanProperty Lit = BlockStateProperties.LIT;
    public static final int Brightness = 15;
    private static final int SwitchOffDelay = 4;

    public FacilityLampBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(defaultBlockState().setValue(Lit, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return LampCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Lit);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return defaultBlockState().setValue(Lit, Context.getLevel().hasNeighborSignal(Context.getClickedPos()));
    }

    @Override
    protected void neighborChanged(BlockState State, Level World, BlockPos Pos, Block Neighbor, BlockPos NeighborPos, boolean MovedByPiston) {
        super.neighborChanged(State, World, Pos, Neighbor, NeighborPos, MovedByPiston);
        if (World.isClientSide) {
            return;
        }
        boolean Powered = World.hasNeighborSignal(Pos);
        if (Powered == State.getValue(Lit)) {
            return;
        }
        if (Powered) {
            World.setBlock(Pos, State.setValue(Lit, true), Block.UPDATE_CLIENTS);
        } else {
            World.scheduleTick(Pos, this, SwitchOffDelay);
        }
    }

    @Override
    protected void tick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        if (State.getValue(Lit) && !World.hasNeighborSignal(Pos)) {
            World.setBlock(Pos, State.setValue(Lit, false), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void appendHoverText(ItemStack Stack, Item.TooltipContext Context, List<Component> Lines, TooltipFlag Flag) {
        Lines.add(Component.translatable("block.scp_unleashed.facility_lamp.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
