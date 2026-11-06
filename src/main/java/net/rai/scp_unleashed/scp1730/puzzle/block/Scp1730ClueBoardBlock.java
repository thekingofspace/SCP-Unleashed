package net.rai.scp_unleashed.scp1730.puzzle.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class Scp1730ClueBoardBlock extends Scp1730DeviceBlock {
    public static final MapCodec<Scp1730ClueBoardBlock> Codec = simpleCodec(Scp1730ClueBoardBlock::new);
    public static final IntegerProperty Clue = IntegerProperty.create("clue", 0, 11);
    public static final IntegerProperty Mark = IntegerProperty.create("mark", 0, 3);

    public Scp1730ClueBoardBlock(Properties Props) {
        super(Props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Clue, 0).setValue(Mark, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return Codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Clue, Mark);
    }
}
