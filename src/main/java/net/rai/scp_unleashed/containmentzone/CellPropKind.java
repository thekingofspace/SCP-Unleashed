package net.rai.scp_unleashed.containmentzone;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropShapes;

import static net.rai.scp_unleashed.props.PropShapes.box;

public enum CellPropKind implements StringRepresentable {
    CellPlaque("cell_plaque", true, PropShapes.of(box(0, 2, 14, 16, 14, 16)), "use"),
    RestraintAnchor("restraint_anchor", false, PropShapes.of(box(4, 0, 4, 12, 4, 12), box(10, 0, 7, 16, 2, 15))),
    SedationVent("sedation_vent", true, PropShapes.of(box(1, 2, 12, 15, 14, 16), box(7, 14, 13, 9, 16, 15))),
    StatusPanel("status_panel", true, PropShapes.of(box(3, 1, 12, 13, 15, 16)), "redstone"),
    AlarmLight("alarm_light", true, PropShapes.of(box(2, 3, 10, 14, 13, 16)), "redstone");

    public static final Codec<CellPropKind> KindCodec = StringRepresentable.fromEnum(CellPropKind::values);

    private final String Name;
    private final boolean WallMounted;
    private final VoxelShape Shape;
    private final String[] Details;

    CellPropKind(String Name, boolean WallMounted, VoxelShape Shape, String... Details) {
        this.Name = Name;
        this.WallMounted = WallMounted;
        this.Shape = Shape;
        this.Details = Details;
    }

    public boolean isWallMounted() {
        return WallMounted;
    }

    public VoxelShape getShape() {
        return Shape;
    }

    public String[] getDetails() {
        return Details;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
