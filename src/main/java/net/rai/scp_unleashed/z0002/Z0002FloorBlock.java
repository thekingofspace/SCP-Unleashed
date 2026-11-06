package net.rai.scp_unleashed.z0002;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

public class Z0002FloorBlock extends Block {
    public static final BooleanProperty Cut = BooleanProperty.create("cut");

    public Z0002FloorBlock(Properties Settings) {
        super(Settings);
        registerDefaultState(stateDefinition.any().setValue(Cut, false));
    }

    public static boolean is(BlockState State) {
        return State.getBlock() instanceof Z0002FloorBlock;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Cut);
    }
}
