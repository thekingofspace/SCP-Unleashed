package net.rai.scp_unleashed.scp1730.puzzle.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class Scp1730SluiceBlock extends Scp1730DeviceBlock {
    public static final MapCodec<Scp1730SluiceBlock> Codec = simpleCodec(Scp1730SluiceBlock::new);
    public static final BooleanProperty Open = BooleanProperty.create("open");
    public static final IntegerProperty Turn = IntegerProperty.create("turn", 0, 3);

    public Scp1730SluiceBlock(Properties Props) {
        super(Props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Open, false).setValue(Turn, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return Codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Open, Turn);
    }
}
