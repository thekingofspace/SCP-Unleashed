package net.rai.scp_unleashed.containmentzone;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class CellSignalBlock extends CellPropBlock {
    public static final MapCodec<CellSignalBlock> SignalCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            CellPropKind.KindCodec.fieldOf("kind").forGetter(CellSignalBlock::getKind), propertiesCodec()).apply(Instance, CellSignalBlock::new));
    public static final BooleanProperty Powered = BlockStateProperties.POWERED;

    public CellSignalBlock(CellPropKind Kind, Properties Properties) {
        super(Kind, Properties);
        registerDefaultState(defaultBlockState().setValue(Powered, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return SignalCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Powered);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        return super.getStateForPlacement(Context).setValue(Powered, Context.getLevel().hasNeighborSignal(Context.getClickedPos()));
    }

    @Override
    protected void neighborChanged(BlockState State, Level World, BlockPos Pos, Block Neighbor, BlockPos NeighborPos, boolean MovedByPiston) {
        super.neighborChanged(State, World, Pos, Neighbor, NeighborPos, MovedByPiston);
        if (World.isClientSide) {
            return;
        }
        boolean Signal = World.hasNeighborSignal(Pos);
        if (Signal != State.getValue(Powered)) {
            World.setBlock(Pos, State.setValue(Powered, Signal), Block.UPDATE_CLIENTS);
        }
    }
}
