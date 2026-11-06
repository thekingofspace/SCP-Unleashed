package net.rai.scp_unleashed.scp143;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.state.BlockState;

public class Scp143SaplingBlock extends SaplingBlock {
    public static final MapCodec<Scp143SaplingBlock> BlockCodec = simpleCodec(Scp143SaplingBlock::new);

    public Scp143SaplingBlock(Properties Settings) {
        super(Scp143Registry.Grower, Settings);
    }

    @Override
    public MapCodec<? extends SaplingBlock> codec() {
        return BlockCodec;
    }

    @Override
    public void advanceTree(ServerLevel World, BlockPos Pos, BlockState State, RandomSource Random) {
        if (State.getValue(STAGE) == 0) {
            World.setBlock(Pos, State.cycle(STAGE), 4);
            return;
        }
        Scp143Trees.growYoung(World, Pos, Random.nextLong());
    }
}
