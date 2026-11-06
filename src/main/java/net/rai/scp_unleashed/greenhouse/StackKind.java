package net.rai.scp_unleashed.greenhouse;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropShapes;

import static net.rai.scp_unleashed.props.PropShapes.box;

public enum StackKind implements StringRepresentable {
    GrowRack("grow_rack", PropShapes.of(box(1, 0, 2, 15, 16, 14))),
    HydroponicWall("hydroponic_wall", PropShapes.of(box(0, 0, 7, 16, 16, 16))),
    WaterTank("water_tank", PropShapes.of(box(2, 0, 0, 14, 16, 14))),
    SeedCabinet("seed_cabinet", PropShapes.of(box(1, 0, 3, 15, 16, 16)));

    public static final Codec<StackKind> KindCodec = StringRepresentable.fromEnum(StackKind::values);

    private final String Name;
    private final VoxelShape Shape;

    StackKind(String Name, VoxelShape Shape) {
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
