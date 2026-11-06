package net.rai.scp_unleashed.scp1730.puzzle.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class Scp1730TerminalBlock extends Scp1730DeviceBlock {
    public static final MapCodec<Scp1730TerminalBlock> Codec = simpleCodec(Scp1730TerminalBlock::new);
    public static final IntegerProperty Glyph = IntegerProperty.create("glyph", 0, 5);
    public static final BooleanProperty Accepted = BooleanProperty.create("accepted");

    public Scp1730TerminalBlock(Properties Props) {
        super(Props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Glyph, 0).setValue(Accepted, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return Codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Glyph, Accepted);
    }
}
