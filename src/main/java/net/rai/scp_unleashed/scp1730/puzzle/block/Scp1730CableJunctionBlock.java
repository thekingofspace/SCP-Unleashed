package net.rai.scp_unleashed.scp1730.puzzle.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class Scp1730CableJunctionBlock extends Scp1730DeviceBlock {
    public static final MapCodec<Scp1730CableJunctionBlock> Codec = simpleCodec(Scp1730CableJunctionBlock::new);
    public static final BooleanProperty Fixed = BooleanProperty.create("fixed");

    public Scp1730CableJunctionBlock(Properties Props) {
        super(Props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Fixed, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return Codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Fixed);
    }
}
