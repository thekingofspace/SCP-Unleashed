package net.rai.scp_unleashed.props;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.VoxelShape;

import static net.rai.scp_unleashed.props.PropShapes.box;

public enum DeskKind implements StringRepresentable {
    OfficeDesk("office_desk", true, PropShapes.of(box(0, 14, 0, 16, 16, 16)), PropShapes.of(box(14, 0, 1, 15, 14, 15)), PropShapes.of(box(1, 0, 1, 7, 14, 15))),
    MetalDesk("metal_desk", false, PropShapes.of(box(0, 14, 0, 16, 16, 16)), PropShapes.of(box(14, 0, 1, 16, 14, 16)), PropShapes.of(box(0, 0, 1, 6, 14, 16))),
    LabBenchDesk("lab_bench_desk", false, PropShapes.of(box(0, 14, 0, 16, 16, 16)), PropShapes.of(box(14, 0, 1, 15, 14, 15)), PropShapes.of(box(1, 0, 1, 7, 14, 15))),
    LabTable("lab_table", false, PropShapes.of(box(0, 13, 0, 16, 16, 16)), PropShapes.of(box(13, 0, 1, 15, 13, 3), box(13, 0, 13, 15, 13, 15)),
            PropShapes.of(box(1, 0, 1, 3, 13, 3), box(1, 0, 13, 3, 13, 15)));

    public static final Codec<DeskKind> KindCodec = StringRepresentable.fromEnum(DeskKind::values);

    private final String Name;
    private final boolean JoinsCorners;
    private final VoxelShape Top;
    private final VoxelShape LeftEnd;
    private final VoxelShape RightEnd;

    DeskKind(String Name, boolean JoinsCorners, VoxelShape Top, VoxelShape LeftEnd, VoxelShape RightEnd) {
        this.Name = Name;
        this.JoinsCorners = JoinsCorners;
        this.Top = Top;
        this.LeftEnd = LeftEnd;
        this.RightEnd = RightEnd;
    }

    public boolean joinsCorners() {
        return JoinsCorners;
    }

    public VoxelShape getTop() {
        return Top;
    }

    public VoxelShape getLeftEnd() {
        return LeftEnd;
    }

    public VoxelShape getRightEnd() {
        return RightEnd;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
