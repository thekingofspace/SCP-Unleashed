package net.rai.scp_unleashed.door;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class BlastGateBlock extends PanelDoorBlock {
    public static final MapCodec<BlastGateBlock> BlockCodec = simpleCodec(BlastGateBlock::new);
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, DoorKind.BlastGate.getCellCount() - 1);

    public BlastGateBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    public DoorKind getKind() {
        return DoorKind.BlastGate;
    }

    @Override
    public IntegerProperty partProperty() {
        return Part;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return BlockCodec;
    }
}
