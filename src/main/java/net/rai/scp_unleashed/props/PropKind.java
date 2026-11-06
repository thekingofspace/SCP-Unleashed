package net.rai.scp_unleashed.props;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.VoxelShape;

import static net.rai.scp_unleashed.props.PropShapes.box;

public enum PropKind implements StringRepresentable {
    OfficeChair("office_chair", PropShapes.of(box(3, 0, 3, 13, 9, 13), box(3, 9, 12, 13, 21, 14))),
    LabShelf("lab_shelf", PropShapes.of(box(1, 0, 8, 15, 16, 16))),
    ChemicalBarrel("chemical_barrel", PropShapes.of(box(2, 0, 3, 14, 16, 13), box(3, 0, 2, 13, 16, 14))),
    BiohazardBin("biohazard_bin", PropShapes.of(box(2, 0, 2, 14, 13, 14))),
    WetFloorSign("wet_floor_sign", PropShapes.of(box(4, 0, 2, 12, 16, 14)));

    public static final Codec<PropKind> KindCodec = StringRepresentable.fromEnum(PropKind::values);

    private final String Name;
    private final VoxelShape Shape;

    PropKind(String Name, VoxelShape Shape) {
        this.Name = Name;
        this.Shape = Shape;
    }

    public VoxelShape getShape() {
        return Shape;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
