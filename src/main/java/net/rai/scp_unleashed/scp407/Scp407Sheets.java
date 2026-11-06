package net.rai.scp_unleashed.scp407;

import net.minecraft.core.Direction;

import java.util.ArrayList;
import java.util.List;

public final class Scp407Sheets {
    public static final float PadLift = 1.0f;
    public static final float Hair = 0.05f;
    private static final float[][] Corners = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};

    public record Sheet(Direction Normal, float[][] Points, float[][] Uvs) {
    }

    private Scp407Sheets() {
    }

    public static float[] tangent(Direction Face, boolean Second) {
        return switch (Face.getAxis()) {
            case Y -> Second ? new float[]{0, 0, 1} : new float[]{1, 0, 0};
            case Z -> Second ? new float[]{0, 1, 0} : new float[]{1, 0, 0};
            default -> Second ? new float[]{0, 1, 0} : new float[]{0, 0, 1};
        };
    }

    public static float rimInset(Direction Face) {
        return Hair * (2 + Face.getAxis().ordinal());
    }

    public static float depth(Direction Face, float Pixels) {
        float Lift = Pixels / 16.0f;
        return Face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0f - Lift : Lift;
    }

    public static Sheet layer(Direction Face, float Pixels, int Slot) {
        return flat(Face, Face.getOpposite(), Pixels, Slot);
    }

    public static Sheet back(Direction Face) {
        return flat(Face, Face, Hair, -1);
    }

    public static List<Sheet> rims(Direction Face) {
        List<Sheet> Out = new ArrayList<>(4);
        int Axis = Face.getAxis().ordinal();
        float Near = depth(Face, Hair);
        float Far = depth(Face, PadLift);
        float Inset = rimInset(Face) / 16.0f;
        for (Direction Side : Scp407Carpets.edges(Face)) {
            int Across = Side.getAxis().ordinal();
            int Along = 3 - Axis - Across;
            float Edge = Side.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1.0f - Inset : Inset;
            float[][] Points = new float[4][3];
            float[][] Uvs = new float[4][2];
            for (int Vertex = 0; Vertex < 4; Vertex++) {
                float A = Corners[Vertex][0];
                float B = Corners[Vertex][1];
                Points[Vertex][Across] = Edge;
                Points[Vertex][Along] = A;
                Points[Vertex][Axis] = Near + (Far - Near) * B;
                Uvs[Vertex][0] = A;
                Uvs[Vertex][1] = 1.0f - (Hair + (PadLift - Hair) * B) / 16.0f;
            }
            Out.add(wind(Side, Points, Uvs));
        }
        return Out;
    }

    public static List<Sheet> pad(Direction Face) {
        List<Sheet> Out = new ArrayList<>(6);
        Out.add(layer(Face, PadLift, -1));
        Out.add(back(Face));
        Out.addAll(rims(Face));
        return Out;
    }

    private static Sheet flat(Direction Face, Direction Normal, float Pixels, int Slot) {
        float[] First = tangent(Face, false);
        float[] Second = tangent(Face, true);
        float[] Origin = new float[3];
        Origin[Face.getAxis().ordinal()] = depth(Face, Pixels);
        float[][] Points = new float[4][3];
        float[][] Uvs = new float[4][2];
        for (int Vertex = 0; Vertex < 4; Vertex++) {
            float A = Corners[Vertex][0];
            float B = Corners[Vertex][1];
            for (int Index = 0; Index < 3; Index++) {
                Points[Vertex][Index] = Origin[Index] + First[Index] * A + Second[Index] * B;
            }
            Uvs[Vertex] = switch (Slot) {
                case 0 -> new float[]{B, A};
                case 1 -> new float[]{B, 1.0f - A};
                case 2 -> new float[]{A, B};
                case 3 -> new float[]{A, 1.0f - B};
                case 4 -> new float[]{A, B};
                default -> new float[]{A, Face.getAxis() == Direction.Axis.Y ? B : 1.0f - B};
            };
        }
        return wind(Normal, Points, Uvs);
    }

    public static float facing(float[][] Points, Direction Normal) {
        float[] EdgeA = {Points[1][0] - Points[0][0], Points[1][1] - Points[0][1], Points[1][2] - Points[0][2]};
        float[] EdgeB = {Points[3][0] - Points[0][0], Points[3][1] - Points[0][1], Points[3][2] - Points[0][2]};
        float[] Cross = {EdgeA[1] * EdgeB[2] - EdgeA[2] * EdgeB[1], EdgeA[2] * EdgeB[0] - EdgeA[0] * EdgeB[2], EdgeA[0] * EdgeB[1] - EdgeA[1] * EdgeB[0]};
        return Cross[0] * Normal.getStepX() + Cross[1] * Normal.getStepY() + Cross[2] * Normal.getStepZ();
    }

    private static Sheet wind(Direction Normal, float[][] Points, float[][] Uvs) {
        if (facing(Points, Normal) >= 0) {
            return new Sheet(Normal, Points, Uvs);
        }
        int[] Order = {0, 3, 2, 1};
        float[][] Turned = new float[4][];
        float[][] TurnedUvs = new float[4][];
        for (int Index = 0; Index < 4; Index++) {
            Turned[Index] = Points[Order[Index]];
            TurnedUvs[Index] = Uvs[Order[Index]];
        }
        return new Sheet(Normal, Turned, TurnedUvs);
    }
}
