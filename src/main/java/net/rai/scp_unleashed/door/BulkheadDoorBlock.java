package net.rai.scp_unleashed.door;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

public class BulkheadDoorBlock extends PanelDoorBlock {
    public static final MapCodec<BulkheadDoorBlock> BlockCodec = RecordCodecBuilder.mapCodec(Instance -> Instance.group(
            DoorKind.KindCodec.fieldOf("kind").forGetter(BulkheadDoorBlock::getKind),
            propertiesCodec()
    ).apply(Instance, BulkheadDoorBlock::new));
    public static final int Cells = DoorSize.Bulkhead.getCellCount();
    public static final IntegerProperty Part = IntegerProperty.create("part", 0, Cells - 1);

    private final DoorKind Kind;

    public BulkheadDoorBlock(DoorKind Kind, Properties Properties) {
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
