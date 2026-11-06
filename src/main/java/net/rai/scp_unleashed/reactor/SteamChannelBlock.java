package net.rai.scp_unleashed.reactor;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.rai.scp_unleashed.reactor.core.ReactorTuning;
import net.rai.scp_unleashed.reactor.core.TowerType;

public class SteamChannelBlock extends SteamTowerBlock {
    public static final MapCodec<SteamChannelBlock> BlockCodec = simpleCodec(SteamChannelBlock::new);

    public SteamChannelBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    public TowerType getTowerType() {
        return TowerType.Channel;
    }

    @Override
    protected int detailLines() {
        return 4;
    }

    @Override
    protected Object[] detailArguments(int Line) {
        return switch (Line) {
            case 2 -> new Object[]{Math.round(ReactorTuning.ChannelPeak)};
            case 3 -> new Object[]{ReactorTuning.WaterPerSegment};
            default -> new Object[0];
        };
    }
}
