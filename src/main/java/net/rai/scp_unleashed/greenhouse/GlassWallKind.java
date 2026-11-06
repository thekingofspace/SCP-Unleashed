package net.rai.scp_unleashed.greenhouse;

import com.mojang.serialization.Codec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public enum GlassWallKind implements StringRepresentable {
    Bottom("bottom"),
    Middle("middle"),
    Top("top");

    public static final Codec<GlassWallKind> KindCodec = StringRepresentable.fromEnum(GlassWallKind::values);
    public static final int PlanterTop = 7;
    public static final int GutterBottom = 11;
    public static final int MistPipe = 9;
    public static final int FrameFront = 6;
    public static final int FrameBack = 10;

    private final String Name;

    GlassWallKind(String Name) {
        this.Name = Name;
    }

    public boolean panelBelow() {
        return this != Bottom;
    }

    public boolean panelAbove() {
        return this != Top;
    }

    public VoxelShape shape(boolean AlongX) {
        VoxelShape Panel = AlongX ? Block.box(FrameFront, 0, 0, FrameBack, 16, 16) : Block.box(0, 0, FrameFront, 16, 16, FrameBack);
        return switch (this) {
            case Bottom -> Shapes.or(Panel, Block.box(0, 0, 0, 16, PlanterTop, 16));
            case Top -> Shapes.or(Panel, Block.box(0, GutterBottom, 0, 16, 16, 16), Block.box(0, MistPipe, 1, 16, GutterBottom, 2), Block.box(0, MistPipe, 14, 16, GutterBottom, 15));
            default -> Panel;
        };
    }

    @Override
    public String getSerializedName() {
        return Name;
    }
}
