package net.rai.scp_unleashed.props;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.shapes.VoxelShape;

import static net.rai.scp_unleashed.props.PropShapes.box;

public enum ScreenKind implements StringRepresentable {
    ComputerTerminal("computer_terminal", 2, PropShapes.of(box(3, 0, 1, 13, 1, 5), box(2, 0, 5, 14, 12, 15), box(4, 2, 15, 12, 11, 16))),
    CrtMonitor("crt_monitor", 0, PropShapes.of(box(1, 0, 3, 15, 12, 15))),
    SecurityMonitor("security_monitor", 3, PropShapes.of(box(0, 0, 1, 16, 16, 16))),
    WallMonitor("wall_monitor", 1, PropShapes.of(box(0, 3, 14, 16, 13, 16)));

    public static final Codec<ScreenKind> KindCodec = StringRepresentable.fromEnum(ScreenKind::values);

    private final String Name;
    private final int DefaultScreen;
    private final VoxelShape Shape;

    ScreenKind(String Name, int DefaultScreen, VoxelShape Shape) {
        this.Name = Name;
        this.DefaultScreen = DefaultScreen;
        this.Shape = Shape;
    }

    public int getDefaultScreen() {
        return DefaultScreen;
    }

    public VoxelShape getShape() {
        return Shape;
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
