package net.rai.scp_unleashed.greenhouse;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropShapes;

import static net.rai.scp_unleashed.props.PropShapes.box;

public enum GreenhouseKind implements StringRepresentable {
    PottingBench("potting_bench", PropShapes.of(box(0, 0, 2, 16, 11, 16), box(1, 11, 4, 14, 16, 15), box(0, 11, 15, 16, 16, 16))),
    HangingPlanter("hanging_planter", PropShapes.of(box(3, 0, 3, 13, 14, 13), box(6, 14, 6, 10, 16, 10))),
    IrrigationPanel("irrigation_panel", PropShapes.of(box(1, 0, 11, 15, 15, 16)));

    public static final Codec<GreenhouseKind> KindCodec = StringRepresentable.fromEnum(GreenhouseKind::values);

    private final String Name;
    private final VoxelShape Shape;

    GreenhouseKind(String Name, VoxelShape Shape) {
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
