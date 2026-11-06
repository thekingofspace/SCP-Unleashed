package net.rai.scp_unleashed.powerzone;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.rai.scp_unleashed.props.PropShapes;

import static net.rai.scp_unleashed.props.PropShapes.box;

public enum PowerPropKind implements StringRepresentable {
    GeneratorBottom("generator_housing_bottom", PropShapes.of(box(0, 0, 0, 16, 16, 16)), 4, true),
    GeneratorMiddle("generator_housing_middle", PropShapes.of(box(0, 0, 0, 16, 16, 16)), 0, true),
    GeneratorTop("generator_housing_top", PropShapes.of(box(1, 0, 1, 15, 10, 15), box(0, 10, 0, 16, 12, 16), box(4, 12, 4, 12, 16, 12)), 0, true),
    BreakerBottom("breaker_panel_bottom", PropShapes.of(box(1, 0, 10, 15, 16, 16)), 0, true),
    BreakerMiddle("breaker_panel_middle", PropShapes.of(box(1, 0, 10, 15, 16, 16)), 0, true),
    BreakerTop("breaker_panel_top", PropShapes.of(box(1, 0, 10, 15, 8, 16), box(0, 8, 9, 16, 9, 16), box(3, 9, 12, 13, 16, 14)), 0, true),
    TransformerBottom("transformer_bottom", PropShapes.of(box(0, 0, 1, 16, 16, 15)), 0, true),
    TransformerMiddle("transformer_middle", PropShapes.of(box(1, 0, 1, 15, 16, 15)), 0, true),
    TransformerTop("transformer_top", PropShapes.of(box(1, 0, 1, 15, 3, 15), box(1, 3, 6, 15, 16, 10)), 0, true),
    ExhaustBottom("exhaust_stack_bottom", PropShapes.of(box(2, 0, 2, 14, 2, 14), box(4, 2, 4, 12, 14, 12), box(3, 14, 3, 13, 16, 13)), 0, true),
    ExhaustMiddle("exhaust_stack_middle", PropShapes.of(box(4, 0, 4, 12, 14, 12), box(3, 14, 3, 13, 16, 13)), 0, true),
    ExhaustTop("exhaust_stack_top", PropShapes.of(box(4, 0, 4, 12, 8, 12), box(3, 8, 3, 13, 13, 13), box(2, 13, 2, 14, 15, 14)), 0, true),
    ControlConsole("control_console", PropShapes.of(box(0, 0, 2, 16, 10, 16), box(0, 10, 9, 16, 16, 16)), 3, false),
    CableSpool("cable_spool", PropShapes.of(box(0, 0, 1, 16, 16, 15)), 0, false),
    FuseBox("fuse_box", PropShapes.of(box(0, 0, 11, 14, 16, 16)), 0, false);

    public static final Codec<PowerPropKind> KindCodec = StringRepresentable.fromEnum(PowerPropKind::values);

    private final String Name;
    private final VoxelShape Shape;
    private final int Light;
    private final boolean Stacks;

    PowerPropKind(String Name, VoxelShape Shape, int Light, boolean Stacks) {
        this.Name = Name;
        this.Shape = Shape;
        this.Light = Light;
        this.Stacks = Stacks;
    }

    public VoxelShape getShape() {
        return Shape;
    }

    public int getLight() {
        return Light;
    }

    public boolean stacks() {
        return Stacks;
    }

    public String blockName() {
        return "power_" + Name;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
