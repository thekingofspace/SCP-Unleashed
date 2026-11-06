package net.rai.scp_unleashed.scp3008.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

public class Scp3008CeilingBlock extends Scp3008SealedBlock {
    public static final MapCodec<Scp3008CeilingBlock> CeilingCodec = simpleCodec(Scp3008CeilingBlock::new);

    public Scp3008CeilingBlock(Properties Settings) {
        super(Settings);
    }

    @Override
    protected MapCodec<? extends Scp3008CeilingBlock> codec() {
        return CeilingCodec;
    }

    @Override
    protected RenderShape getRenderShape(BlockState State) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState State, BlockGetter World, BlockPos Pos) {
        return true;
    }
}
