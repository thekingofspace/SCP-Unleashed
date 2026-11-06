package net.rai.scp_unleashed.door;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class SlidingDoorBlock extends PanelDoorBlock {
    public static final MapCodec<SlidingDoorBlock> BlockCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            DoorKind.KindCodec.fieldOf("kind").forGetter(SlidingDoorBlock::getKind),
            propertiesCodec()
    ).apply(Instance, SlidingDoorBlock::new));
    public static final int Cells = DoorSize.Tall.getCellCount();
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, Cells - 1);

    private final DoorKind Kind;

    public SlidingDoorBlock(DoorKind Kind, Properties Properties) {
        super(Properties);
        if (Kind.getCellCount() != Cells) {
            throw new IllegalArgumentException(Kind.getSerializedName() + " does not have " + Cells + " cells");
        }
        this.Kind = Kind;
    }

    @Override
    public DoorKind getKind() {
        return Kind;
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
