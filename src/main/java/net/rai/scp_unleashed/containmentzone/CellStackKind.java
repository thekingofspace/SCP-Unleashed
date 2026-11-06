package net.rai.scp_unleashed.containmentzone;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.decor.PillarPart;
import net.rai.scp_unleashed.props.PropShapes;

import java.util.EnumMap;
import java.util.Map;

import static net.rai.scp_unleashed.props.PropShapes.box;

public enum CellStackKind implements StringRepresentable {
    ObservationBay("observation_bay", false,
            PropShapes.of(box(0, 0, 2, 16, 5, 16), box(0, 5, 0, 16, 7, 16), box(0, 7, 6, 16, 12, 10), box(0, 12, 4, 16, 16, 12), box(11, 7, 1, 14, 10, 4)),
            PropShapes.of(box(0, 0, 2, 16, 8, 16), box(0, 8, 0, 16, 10, 16), box(0, 10, 6, 16, 16, 10), box(11, 10, 1, 14, 13, 4)),
            PropShapes.of(box(0, 0, 6, 16, 16, 10)),
            PropShapes.of(box(0, 0, 6, 16, 8, 10), box(0, 8, 4, 16, 16, 12)), "stack", "row"),
    SpecimenTank("specimen_tank", false,
            PropShapes.of(box(1, 0, 1, 15, 5, 15), box(2, 5, 2, 14, 13, 14), box(1, 13, 1, 15, 16, 15), box(5, 1, 0, 11, 3, 1)),
            PropShapes.of(box(1, 0, 1, 15, 6, 15), box(2, 6, 2, 14, 16, 14), box(5, 1, 0, 11, 4, 1)),
            PropShapes.of(box(2, 0, 2, 14, 16, 14)),
            PropShapes.of(box(2, 0, 2, 14, 11, 14), box(1, 11, 1, 15, 16, 15)), "stack"),
    PaddedPanel("padded_panel", true,
            PropShapes.of(box(0, 0, 12, 16, 16, 16)),
            PropShapes.of(box(0, 0, 12, 16, 16, 16)),
            PropShapes.of(box(0, 0, 12, 16, 16, 16)),
            PropShapes.of(box(0, 0, 12, 16, 16, 16)), "stack"),
    LockdownShutter("lockdown_shutter", false,
            PropShapes.of(box(0, 0, 5, 16, 11, 11), box(0, 11, 4, 16, 16, 12)),
            PropShapes.of(box(0, 0, 5, 16, 16, 11)),
            PropShapes.of(box(0, 0, 5, 16, 16, 11)),
            PropShapes.of(box(0, 0, 5, 16, 10, 11), box(0, 10, 4, 16, 16, 12)), "stack", "row");

    public static final Codec<CellStackKind> KindCodec = StringRepresentable.fromEnum(CellStackKind::values);

    private final String Name;
    private final boolean WallMounted;
    private final Map<PillarPart, VoxelShape> Shapes = new EnumMap<>(PillarPart.class);
    private final String[] Details;

    CellStackKind(String Name, boolean WallMounted, VoxelShape Single, VoxelShape Bottom, VoxelShape Middle, VoxelShape Top, String... Details) {
        this.Name = Name;
        this.WallMounted = WallMounted;
        this.Details = Details;
        Shapes.put(PillarPart.Single, Single);
        Shapes.put(PillarPart.Bottom, Bottom);
        Shapes.put(PillarPart.Middle, Middle);
        Shapes.put(PillarPart.Top, Top);
    }

    public boolean isWallMounted() {
        return WallMounted;
    }

    public VoxelShape getShape(PillarPart Part) {
        return Shapes.get(Part);
    }

    public String[] getDetails() {
        return Details;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
