package net.rai.scp_unleashed.door;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class ContainmentDoorBlock extends PanelDoorBlock {
    public static final MapCodec<ContainmentDoorBlock> BlockCodec = simpleCodec(ContainmentDoorBlock::new);
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, DoorKind.Containment.getCellCount() - 1);

    public ContainmentDoorBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    public DoorKind getKind() {
        return DoorKind.Containment;
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
