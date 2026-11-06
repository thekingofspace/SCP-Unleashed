package net.rai.scp_unleashed.advmaterials;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.rai.scp_unleashed.door.DoorKind;

public class MagnetizedSingleDoorBlock extends MagnetizedDoorBlock {
    public static final MapCodec<MagnetizedSingleDoorBlock> BlockCodec = simpleCodec(MagnetizedSingleDoorBlock::new);
    public static final DoorKind Layout = DoorKind.HczSingle;
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, Layout.getCellCount() - 1);

    public MagnetizedSingleDoorBlock(Properties Properties) {
        super(Properties);
    }

    @Override
    public DoorKind getLayout() {
        return Layout;
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
