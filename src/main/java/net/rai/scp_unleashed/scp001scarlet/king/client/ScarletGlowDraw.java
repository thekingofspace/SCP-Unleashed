package net.rai.scp_unleashed.scp001scarlet.king.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class ScarletGlowDraw {
    private ScarletGlowDraw() {
    }

    public static void quad(VertexConsumer Consumer, Matrix4f Pose, Vec3 A, Vec3 B, Vec3 C, Vec3 D, float Red, float Green, float Blue, float Alpha) {
        Consumer.addVertex(Pose, (float) A.x, (float) A.y, (float) A.z).setColor(Red, Green, Blue, Alpha);
        Consumer.addVertex(Pose, (float) B.x, (float) B.y, (float) B.z).setColor(Red, Green, Blue, Alpha);
        Consumer.addVertex(Pose, (float) C.x, (float) C.y, (float) C.z).setColor(Red, Green, Blue, Alpha);
        Consumer.addVertex(Pose, (float) D.x, (float) D.y, (float) D.z).setColor(Red, Green, Blue, Alpha);
        Consumer.addVertex(Pose, (float) D.x, (float) D.y, (float) D.z).setColor(Red, Green, Blue, Alpha);
        Consumer.addVertex(Pose, (float) C.x, (float) C.y, (float) C.z).setColor(Red, Green, Blue, Alpha);
        Consumer.addVertex(Pose, (float) B.x, (float) B.y, (float) B.z).setColor(Red, Green, Blue, Alpha);
        Consumer.addVertex(Pose, (float) A.x, (float) A.y, (float) A.z).setColor(Red, Green, Blue, Alpha);
    }

    public static void ribbon(VertexConsumer Consumer, Matrix4f Pose, Vec3 From, Vec3 To, Vec3 Eye, float Width, float Red, float Green, float Blue, float Alpha) {
        Vec3 Along = To.subtract(From);
        if (Along.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 Middle = From.add(To).scale(0.5);
        Vec3 Side = Along.cross(Eye.subtract(Middle));
        if (Side.lengthSqr() < 1.0E-8) {
            Side = Along.cross(new Vec3(0.0, 1.0, 0.0));
            if (Side.lengthSqr() < 1.0E-8) {
                Side = new Vec3(1.0, 0.0, 0.0);
            }
        }
        Side = Side.normalize().scale(Width / 2.0);
        quad(Consumer, Pose, From.subtract(Side), From.add(Side), To.add(Side), To.subtract(Side), Red, Green, Blue, Alpha);
    }

    public static void face(VertexConsumer Consumer, Matrix4f Pose, Vec3 A, Vec3 B, Vec3 C, Vec3 D, float Red, float Green, float Blue, float Alpha) {
        Consumer.addVertex(Pose, (float) A.x, (float) A.y, (float) A.z).setColor(Red, Green, Blue, Alpha);
        Consumer.addVertex(Pose, (float) B.x, (float) B.y, (float) B.z).setColor(Red, Green, Blue, Alpha);
        Consumer.addVertex(Pose, (float) C.x, (float) C.y, (float) C.z).setColor(Red, Green, Blue, Alpha);
        Consumer.addVertex(Pose, (float) D.x, (float) D.y, (float) D.z).setColor(Red, Green, Blue, Alpha);
    }

    public static Vec3[] frame(Vec3 Along) {
        Vec3 Way = Along.lengthSqr() < 1.0E-8 ? new Vec3(0.0, 1.0, 0.0) : Along.normalize();
        Vec3 Side = Way.cross(new Vec3(0.0, 1.0, 0.0));
        if (Side.lengthSqr() < 1.0E-6) {
            Side = Way.cross(new Vec3(1.0, 0.0, 0.0));
        }
        Side = Side.normalize();
        Vec3 Lift = Side.cross(Way).normalize();
        return new Vec3[]{Side, Lift};
    }

    public static void prism(VertexConsumer Consumer, Matrix4f Pose, Vec3 From, Vec3 To, Vec3 Side, Vec3 Lift, double Half, float Red, float Green, float Blue, float Alpha) {
        Vec3 A = Side.scale(Half).add(Lift.scale(Half));
        Vec3 B = Side.scale(-Half).add(Lift.scale(Half));
        Vec3 C = Side.scale(-Half).add(Lift.scale(-Half));
        Vec3 D = Side.scale(Half).add(Lift.scale(-Half));
        Vec3[] Corners = {A, B, C, D};
        for (int Index = 0; Index < 4; Index++) {
            Vec3 First = Corners[Index];
            Vec3 Second = Corners[(Index + 1) % 4];
            face(Consumer, Pose, From.add(First), To.add(First), To.add(Second), From.add(Second), Red, Green, Blue, Alpha);
        }
    }

    public static void groundRing(VertexConsumer Consumer, Matrix4f Pose, Vec3 Centre, double Radius, double Thickness, int Segments, float Red, float Green, float Blue,
                                  float Alpha) {
        for (int Index = 0; Index < Segments; Index++) {
            double A0 = Index * Math.PI * 2.0 / Segments;
            double A1 = (Index + 1) * Math.PI * 2.0 / Segments;
            Vec3 InnerA = Centre.add(Math.cos(A0) * (Radius - Thickness), 0.0, Math.sin(A0) * (Radius - Thickness));
            Vec3 OuterA = Centre.add(Math.cos(A0) * Radius, 0.0, Math.sin(A0) * Radius);
            Vec3 InnerB = Centre.add(Math.cos(A1) * (Radius - Thickness), 0.0, Math.sin(A1) * (Radius - Thickness));
            Vec3 OuterB = Centre.add(Math.cos(A1) * Radius, 0.0, Math.sin(A1) * Radius);
            quad(Consumer, Pose, InnerA, OuterA, OuterB, InnerB, Red, Green, Blue, Alpha);
        }
    }

    public static void wall(VertexConsumer Consumer, Matrix4f Pose, Vec3 Centre, double Radius, double Low, double High, int Segments, float Red, float Green, float Blue,
                            float Alpha) {
        for (int Index = 0; Index < Segments; Index++) {
            double A0 = Index * Math.PI * 2.0 / Segments;
            double A1 = (Index + 1) * Math.PI * 2.0 / Segments;
            Vec3 Bottom0 = Centre.add(Math.cos(A0) * Radius, Low, Math.sin(A0) * Radius);
            Vec3 Bottom1 = Centre.add(Math.cos(A1) * Radius, Low, Math.sin(A1) * Radius);
            quad(Consumer, Pose, Bottom0, Bottom1, Bottom1.add(0.0, High - Low, 0.0), Bottom0.add(0.0, High - Low, 0.0), Red, Green, Blue, Alpha);
        }
    }
}
