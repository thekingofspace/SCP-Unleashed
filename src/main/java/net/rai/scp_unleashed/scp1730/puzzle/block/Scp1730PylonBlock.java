package net.rai.scp_unleashed.scp1730.puzzle.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class Scp1730PylonBlock extends Scp1730FloorDeviceBlock {
    public static final MapCodec<Scp1730PylonBlock> Codec = simpleCodec(Scp1730PylonBlock::new);
    public static final IntegerProperty Glyph = IntegerProperty.create("glyph", 0, 5);
    public static final IntegerProperty Mark = IntegerProperty.create("mark", 1, 3);
    public static final BooleanProperty Lit = BooleanProperty.create("lit");

    public Scp1730PylonBlock(Properties Props) {
        super(Props, Block.box(3, 0, 3, 13, 16, 13));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Glyph, 0).setValue(Mark, 1).setValue(Lit, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return Codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Glyph, Mark, Lit);
    }
}
