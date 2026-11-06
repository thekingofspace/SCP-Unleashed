package net.rai.scp_unleashed.scp024;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class Scp024SignBlock extends Scp024MountBlock {
    public static final MapCodec<Scp024SignBlock> SignCodec = simpleCodec(Scp024SignBlock::new);
    public static final int Parts = 5;
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, Parts - 1);

    public Scp024SignBlock(Properties Settings) {
        super(Settings, 0.0, 1.0, 14.0, 16.0, 15.0, 16.0);
        registerDefaultState(defaultBlockState().setValue(Part, 0));
    }

    @Override
    protected MapCodec<? extends Scp024SignBlock> codec() {
        return SignCodec;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> Builder) {
        Builder.add(Facing, Part);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext Context) {
        BlockState State = super.getStateForPlacement(Context);
        return State == null ? null : State.setValue(Part, 0);
    }
}
