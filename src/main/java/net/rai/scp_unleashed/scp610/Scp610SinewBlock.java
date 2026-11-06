package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class Scp610SinewBlock extends RotatedPillarBlock {
    public Scp610SinewBlock(BlockBehaviour.Properties Properties) {
        super(Properties);
        registerDefaultState(defaultBlockState().setValue(Scp610FleshBlock.Wild, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Scp610FleshBlock.Wild);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState Placed = super.getStateForPlacement(Context);
        return (Placed == null ? defaultBlockState() : Placed).setValue(Scp610FleshBlock.Wild, false);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState State) {
        return State.getValue(Scp610FleshBlock.Wild);
    }

    @Override
    protected void randomTick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        Scp610FleshBlock.decay(State, World, Pos, Random);
    }

    @Override
    protected void onRemove(BlockState State, Level World, BlockPos Pos, BlockState Replaced, boolean Moving) {
        if (!State.is(Replaced.getBlock()) && World instanceof ServerLevel Server) {
            Scp610Hives.released(Server, Pos);
        }
        super.onRemove(State, World, Pos, Replaced, Moving);
    }
}
