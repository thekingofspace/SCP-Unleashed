package net.rai.scp_unleashed.z0002;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class Z0002BarrierBlock extends Block {
    public Z0002BarrierBlock(Properties Settings) {
        super(Settings);
    }

    public static boolean is(BlockState State) {
        return State.getBlock() instanceof Z0002BarrierBlock;
    }
}
