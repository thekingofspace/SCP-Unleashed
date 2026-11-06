package net.rai.scp_unleashed.scp439.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class Scp439GrowthMesh {
    public static final float Shown = 0.02f;
    public static final float SmallestScale = 0.25f;
    private static final float[][] Normals = {{-1, 0, 0}, {1, 0, 0}, {0, 0, -1}, {0, 0, 1}, {0, 1, 0}, {0, -1, 0}};
    private static final int[][] Corners = {{3, 2, 0, 1}, {4, 5, 7, 6}, {2, 4, 6, 0}, {5, 3, 1, 7}, {3, 5, 4, 2}, {0, 6, 7, 1}};

    private Scp439GrowthMesh() {
    }

    public interface Sink {
        void vertex(float X, float Y, float Z, float U, float V, float NormalX, float NormalY, float NormalZ);
    }

    public static float share(Scp439GrowthShapes.Cube Item0, float Progress) {
        float Raw = (Progress - (Item0.Tier() - 1) - Item0.Delay()) / Item0.Span();
        return Math.max(0.0f, Math.min(1.0f, Raw));
    }

    public static boolean emitPart(PoseStack Pose, Sink Out, Scp439GrowthShapes.Shape Found, String BoneName, float Progress, float Gape) {
        Scp439GrowthShapes.Part Owned = Found.Parts().get(BoneName);
        if (Owned == null) {
            return false;
        }
        boolean Hinged = BoneName.equals(Found.JawPart());
        float Hx = Found.JawPivot()[0] + Owned.Pivot()[0];
        float Hy = Found.JawPivot()[1] + Owned.Pivot()[1];
        float Hz = Found.JawPivot()[2] + Owned.Pivot()[2];
        boolean Any = false;
        for (Scp439GrowthShapes.Cube Item0 : Owned.Cubes()) {
            float Share = share(Item0, Progress);
            if (Share <= Shown) {
                continue;
            }
            Any = true;
            if (Hinged && Item0.Jaw()) {
                Pose.pushPose();
                Pose.translate(-Hx / 16.0f, Hy / 16.0f, Hz / 16.0f);
                Pose.mulPose(Axis.XP.rotationDegrees(-Gape));
                Pose.translate(Hx / 16.0f, -Hy / 16.0f, -Hz / 16.0f);
                cube(Pose, Out, Owned.Pivot(), Item0, Share);
                Pose.popPose();
                continue;
            }
            cube(Pose, Out, Owned.Pivot(), Item0, Share);
        }
        return Any;
    }

    public static void place(PoseStack Pose, float[] PartPivot, Scp439GrowthShapes.Cube Item0, float Share) {
        float Scale = SmallestScale + (1.0f - SmallestScale) * Share;
        float Ax = Item0.Anchor()[0] + PartPivot[0];
        float Ay = Item0.Anchor()[1] + PartPivot[1];
        float Az = Item0.Anchor()[2] + PartPivot[2];
        float Px = Item0.Pivot()[0] + PartPivot[0];
        float Py = Item0.Pivot()[1] + PartPivot[1];
        float Pz = Item0.Pivot()[2] + PartPivot[2];
        Pose.translate(-Ax / 16.0f, Ay / 16.0f, Az / 16.0f);
        Pose.scale(Scale, Scale, Scale);
        Pose.translate(Ax / 16.0f, -Ay / 16.0f, -Az / 16.0f);
        Pose.translate(-Px / 16.0f, Py / 16.0f, Pz / 16.0f);
        Pose.mulPose(Axis.ZP.rotationDegrees(Item0.Rotation()[2]));
        Pose.mulPose(Axis.YP.rotationDegrees(-Item0.Rotation()[1]));
        Pose.mulPose(Axis.XP.rotationDegrees(-Item0.Rotation()[0]));
        Pose.translate(Px / 16.0f, -Py / 16.0f, -Pz / 16.0f);
    }

    public static void cube(PoseStack Pose, Sink Out, float[] PartPivot, Scp439GrowthShapes.Cube Item0, float Share) {
        float Ox = Item0.Origin()[0] + PartPivot[0];
        float Oy = Item0.Origin()[1] + PartPivot[1];
        float Oz = Item0.Origin()[2] + PartPivot[2];
        float Sx = Item0.Size()[0];
        float Sy = Item0.Size()[1];
        float Sz = Item0.Size()[2];
        Pose.pushPose();
        place(Pose, PartPivot, Item0, Share);
        float X0 = -(Ox + Sx) / 16.0f;
        float X1 = -Ox / 16.0f;
        float Y0 = Oy / 16.0f;
        float Y1 = (Oy + Sy) / 16.0f;
        float Z0 = Oz / 16.0f;
        float Z1 = (Oz + Sz) / 16.0f;
        float[][] Points = {{X0, Y0, Z0}, {X0, Y0, Z1}, {X0, Y1, Z0}, {X0, Y1, Z1}, {X1, Y1, Z0}, {X1, Y1, Z1}, {X1, Y0, Z0}, {X1, Y0, Z1}};
        int U = Item0.U();
        int V = Item0.V();
        int Wx = (int) Sx;
        int Wy = (int) Sy;
        int Wz = (int) Sz;
        int[][] Rects = {{U + Wz + Wx, V + Wz, Wz, Wy}, {U, V + Wz, Wz, Wy}, {U + Wz, V + Wz, Wx, Wy}, {U + Wz + Wx + Wz, V + Wz, Wx, Wy}, {U + Wz, V, Wx, Wz},
                {U + Wz + Wx, V + Wz, Wx, -Wz}};
        PoseStack.Pose Last = Pose.last();
        Matrix4f Matrix = Last.pose();
        Vector3f Point = new Vector3f();
        Vector3f Facing = new Vector3f();
        for (int Face = 0; Face < 6; Face++) {
            int[] Rect = Rects[Face];
            float Left = Rect[0];
            float Right = Rect[0] + Rect[2];
            float Top = Rect[1];
            float Bottom = Rect[1] + Rect[3];
            float[] Us = {Right, Left, Left, Right};
            float[] Vs = {Top, Top, Bottom, Bottom};
            Last.transformNormal(Normals[Face][0], Normals[Face][1], Normals[Face][2], Facing);
            for (int Corner = 0; Corner < 4; Corner++) {
                float[] Spot = Points[Corners[Face][Corner]];
                Matrix.transformPosition(Spot[0], Spot[1], Spot[2], Point);
                Out.vertex(Point.x(), Point.y(), Point.z(), Us[Corner], Vs[Corner], Facing.x(), Facing.y(), Facing.z());
            }
        }
        Pose.popPose();
    }
}
