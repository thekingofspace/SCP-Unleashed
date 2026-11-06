package net.rai.scp_unleashed.reactor;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.rai.scp_unleashed.reactor.core.ReactorTuning;
import net.rai.scp_unleashed.reactor.core.TowerType;

public class CoolantTowerBlock extends SteamTowerBlock {
    public static final MapCodec<CoolantTowerBlock> BlockCodec = simpleCodec(CoolantTowerBlock::new);

    public CoolantTowerBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }

    @Override
    public TowerType getTowerType() {
        return TowerType.Coolant;
    }

    @Override
    protected int detailLines() {
        return 4;
    }

    @Override
    protected Object[] detailArguments(int Line) {
        return switch (Line) {
            case 2 -> new Object[]{Math.round(ReactorTuning.CoolantCap)};
            case 3 -> new Object[]{ReactorTuning.WaterPerSegment};
            default -> new Object[0];
        };
    }
}
