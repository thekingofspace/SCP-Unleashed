package net.rai.scp_unleashed.scp5001.world.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.IronBarsBlock;

public class Scp5001RailingBlock extends IronBarsBlock {
    public static final MapCodec<Scp5001RailingBlock> BlockCodec = simpleCodec(Scp5001RailingBlock::new);

    public Scp5001RailingBlock(Properties Settings) {
        super(Settings);
    }

    @Override
    public MapCodec<? extends IronBarsBlock> codec() {
        return BlockCodec;
    }
}
