package net.rai.scp_unleashed.scp1730.puzzle.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class Scp1730GeneratorBlock extends Scp1730FloorDeviceBlock {
    public static final MapCodec<Scp1730GeneratorBlock> Codec = simpleCodec(Scp1730GeneratorBlock::new);
    public static final IntegerProperty Fuel = IntegerProperty.create("fuel", 0, 2);
    public static final BooleanProperty Running = BooleanProperty.create("running");

    public Scp1730GeneratorBlock(Properties Props) {
        super(Props, Block.box(1, 0, 1, 15, 15, 15));
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(Fuel, 0).setValue(Running, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return Codec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(FACING, Fuel, Running);
    }
}
