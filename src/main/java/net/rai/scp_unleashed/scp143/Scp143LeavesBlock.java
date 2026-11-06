package net.rai.scp_unleashed.scp143;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public class Scp143LeavesBlock extends LeavesBlock {
    public static final MapCodec<Scp143LeavesBlock> BlockCodec = simpleCodec(Scp143LeavesBlock::new);

    public Scp143LeavesBlock(Properties Settings) {
        super(Settings);
    }

    @Override
    public MapCodec<? extends LeavesBlock> codec() {
        return BlockCodec;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState State) {
        return true;
    }

    @Override
    protected void randomTick(BlockState State, ServerLevel World, BlockPos Pos, RandomSource Random) {
        if (decaying(State)) {
            super.randomTick(State, World, Pos, Random);
            return;
        }
        Scp143Petals.shed(World, Pos, State, Random);
    }

    @Override
    public BlockState playerWillDestroy(Level World, BlockPos Pos, BlockState State, Player Breaker) {
        if (World instanceof ServerLevel ServerWorld) {
            Scp143Petals.shake(ServerWorld, Pos, 4 + ServerWorld.getRandom().nextInt(4), ServerWorld.getRandom());
        }
        return super.playerWillDestroy(World, Pos, State, Breaker);
    }
}
