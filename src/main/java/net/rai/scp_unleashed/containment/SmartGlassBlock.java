package net.rai.scp_unleashed.containment;

import com.mojang.serialization.MapCodec;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import java.util.List;

public class SmartGlassBlock extends OneWayGlassBlock {
    public static final MapCodec<SmartGlassBlock> SmartCodec = simpleCodec(SmartGlassBlock::new);
    public static final BooleanProperty Active = BooleanProperty.create("active");
    public static final BooleanProperty Powered = BlockStateProperties.POWERED;

    public SmartGlassBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(defaultBlockState().setValue(Active, false).setValue(Powered, false));
    }

    @Override
    protected MapCodec<? extends OneWayGlassBlock> codec() {
        return SmartCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Active, Powered);
    }

    @Override
    public boolean blocksLight(BlockState State) {
        return State.getValue(Active);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        Level World = Context.getLevel();
        BlockPos Pos = Context.getClickedPos();
        boolean Signal = World.hasNeighborSignal(Pos);
        return super.getStateForPlacement(Context).setValue(Powered, Signal).setValue(Active, Signal || SmartGlassGroups.touchesActive(World, Pos));
    }

    @Override
    protected void neighborChanged(BlockState State, Level World, BlockPos Pos, Block Source, BlockPos SourcePos, boolean MovedByPiston) {
        if (World instanceof ServerLevel ServerWorld) {
            boolean Signal = ServerWorld.hasNeighborSignal(Pos);
            if (Signal != State.getValue(Powered)) {
                ServerWorld.setBlock(Pos, State.setValue(Powered, Signal), Block.UPDATE_CLIENTS);
                SmartGlassGroups.markDirty(ServerWorld, Pos);
            }
        }
    }

    @Override
    protected void onPlace(BlockState State, Level World, BlockPos Pos, BlockState OldState, boolean MovedByPiston) {
        super.onPlace(State, World, Pos, OldState, MovedByPiston);
        if (!OldState.is(this) && World instanceof ServerLevel ServerWorld) {
            SmartGlassGroups.markDirty(ServerWorld, Pos);
        }
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState NewState, boolean MovedByPiston) {
        if (!State.is(NewState.getBlock()) && World instanceof ServerLevel ServerWorld) {
            for (Direction Side : Direction.values()) {
                SmartGlassGroups.markDirty(ServerWorld, Pos.relative(Side));
            }
        }
        super.onRemove(State, World, Pos, NewState, MovedByPiston);
    }

    @Override
    protected void appendDetails(List<Component> Lines) {
        Lines.add(Component.translatable("block.scp_unleashed.smart_one_way_glass.signal").withStyle(ChatFormatting.GOLD));
        Lines.add(Component.translatable("block.scp_unleashed.smart_one_way_glass.group", SmartGlassGroups.GroupLimit).withStyle(ChatFormatting.GOLD));
        super.appendDetails(Lines);
    }
}
