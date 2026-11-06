package net.rai.scp_unleashed.scp1730.puzzle.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class Scp1730LockdownPanelBlock extends Scp1730DeviceBlock {
    public static final MapCodec<Scp1730LockdownPanelBlock> Codec = simpleCodec(Scp1730LockdownPanelBlock::new);
    public static final IntegerProperty Lit = IntegerProperty.create("lit", 0, 4);
    public static final IntegerProperty Steps = IntegerProperty.create("steps", 2, 4);

    public Scp1730LockdownPanelBlock(Properties Props) {
        super(Props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Lit, 0).setValue(Steps, 3));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return Codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Lit, Steps);
    }
}
