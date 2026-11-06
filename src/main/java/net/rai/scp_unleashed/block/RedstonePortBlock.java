package net.rai.scp_unleashed.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public abstract class RedstonePortBlock extends NetworkObjectBlock {
    public static final IntegerProperty Power = BlockStateProperties.POWER;

    protected RedstonePortBlock(Properties Properties) {
        super(Properties);
        registerDefaultState(defaultBlockState().setValue(Power, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        super.createBlockStateDefinition(Builder);
        Builder.add(Power);
    }
}
