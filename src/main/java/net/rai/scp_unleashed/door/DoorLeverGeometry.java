package net.rai.scp_unleashed.door;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public final class DoorLeverGeometry {
    public static final double PivotY = 2.0;
    private static final VoxelShape[] Outlines = new VoxelShape[AttachFace.values().length * 4 * 2];

    static {
        for (AttachFace Face : AttachFace.values()) {
            for (Direction Facing : Direction.Plane.HORIZONTAL) {
                for (boolean On : new boolean[]{false, true}) {
                    int RotX = rotationX(Face);
                    int RotY = rotationY(Face, Facing);
                    VoxelShape Plate = Shapes.or(rotatedBox(RotX, RotY, 4, 0, 2, 12, 1, 14), rotatedBox(RotX, RotY, 5, 1, 4, 11, 3, 12));
                    VoxelShape Handle = On ? rotatedBox(RotX, RotY, 6, 3, 8, 10, 11, 15) : rotatedBox(RotX, RotY, 6, 3, 1, 10, 11, 8);
                    Outlines[outlineIndex(Face, Facing, On)] = Shapes.or(Plate, Handle);
                }
            }
        }
    }

    private DoorLeverGeometry() {
    }

    public static int rotationX(AttachFace Face) {
        return switch (Face) {
            case FLOOR -> 0;
            case WALL -> 90;
            case CEILING -> 180;
        };
    }

    public static int rotationY(AttachFace Face, Direction Facing) {
        return Face == AttachFace.CEILING ? Facing.get2DDataValue() * 90 : (Facing.get2DDataValue() * 90 + 180) % 360;
    }

    public static VoxelShape outlineOf(AttachFace Face, Direction Facing, boolean On) {
        return Outlines[outlineIndex(Face, Facing, On)];
    }

    private static int outlineIndex(AttachFace Face, Direction Facing, boolean On) {
        return (Face.ordinal() * 4 + Facing.get2DDataValue()) * 2 + (On ? 1 : 0);
    }

    private static VoxelShape rotatedBox(int RotX, int RotY, double X0, double Y0, double Z0, double X1, double Y1, double Z1) {
        double[] Low = {Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE};
        double[] High = {-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE};
        for (double X : new double[]{X0, X1}) {
            for (double Y : new double[]{Y0, Y1}) {
                for (double Z : new double[]{Z0, Z1}) {
                    double[] Point = turned(RotX, RotY, X - 8.0, Y - 8.0, Z - 8.0);
                    for (int Axis = 0; Axis < 3; Axis++) {
                        Low[Axis] = Math.min(Low[Axis], Point[Axis] + 8.0);
                        High[Axis] = Math.max(High[Axis], Point[Axis] + 8.0);
                    }
                }
            }
        }
        return Shapes.box(Low[0] / 16.0, Low[1] / 16.0, Low[2] / 16.0, High[0] / 16.0, High[1] / 16.0, High[2] / 16.0);
    }

    private static double[] turned(int RotX, int RotY, double X, double Y, double Z) {
        double AngleX = Math.toRadians(-RotX);
        double CosX = Math.round(Math.cos(AngleX));
        double SinX = Math.round(Math.sin(AngleX));
        double TiltedY = Y * CosX - Z * SinX;
        double TiltedZ = Y * SinX + Z * CosX;
        double AngleY = Math.toRadians(-RotY);
        double CosY = Math.round(Math.cos(AngleY));
        double SinY = Math.round(Math.sin(AngleY));
        return new double[]{X * CosY + TiltedZ * SinY, TiltedY, -X * SinY + TiltedZ * CosY};
    }
}
