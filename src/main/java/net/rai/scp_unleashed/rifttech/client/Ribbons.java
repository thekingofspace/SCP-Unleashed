package net.rai.scp_unleashed.rifttech.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public final class Ribbons {
    private static final float[][] Uvs = {{0.0f, 0.0f}, {0.0f, 1.0f}, {1.0f, 1.0f}, {1.0f, 0.0f}};
    private static final double MaxSwing = 1.1;

    private Ribbons() {
    }

    public static List<Vec3> path(RandomSource Random, Vec3 Start, Vec3 End, int Levels, double Roughness) {
        List<Vec3> Points = new ArrayList<>();
        Points.add(Start);
        Points.add(End);
        double Amount = Math.min(MaxSwing, Start.distanceTo(End) * Roughness);
        for (int Step = 0; Step < Levels; Step++) {
            List<Vec3> Next = new ArrayList<>(Points.size() * 2);
            Next.add(Points.getFirst());
            for (int Index = 0; Index + 1 < Points.size(); Index++) {
                Vec3 A = Points.get(Index);
                Vec3 B = Points.get(Index + 1);
                Vec3 Along = B.subtract(A);
                Vec3 Pick = new Vec3(Random.nextDouble() * 2.0 - 1.0, Random.nextDouble() * 2.0 - 1.0, Random.nextDouble() * 2.0 - 1.0);
                Vec3 Across = Pick.subtract(Along.scale(Pick.dot(Along) / Math.max(Along.lengthSqr(), 1.0E-9)));
                double Length = Across.length();
                Vec3 Middle = A.add(B).scale(0.5);
                if (Length > 1.0E-9) {
                    Middle = Middle.add(Across.scale(Amount * (Random.nextDouble() * 2.0 - 1.0) / Length));
                }
                Next.add(Middle);
                Next.add(B);
            }
            Points = Next;
            Amount *= 0.5;
        }
        return Points;
    }

    public static Vec3[] sides(List<Vec3> Path, Vec3 Eye) {
        int Count = Path.size();
        Vec3[] Sides = new Vec3[Count];
        for (int Index = 0; Index < Count; Index++) {
            Vec3 Along = Path.get(Math.min(Count - 1, Index + 1)).subtract(Path.get(Math.max(0, Index - 1)));
            Vec3 Side = Along.cross(Eye.subtract(Path.get(Index)));
            if (Side.lengthSqr() < 1.0E-12) {
                Sides[Index] = Index > 0 ? Sides[Index - 1] : new Vec3(1.0, 0.0, 0.0);
                continue;
            }
            Side = Side.normalize();
            if (Index > 0 && Side.dot(Sides[Index - 1]) < 0.0) {
                Side = Side.scale(-1.0);
            }
            Sides[Index] = Side;
        }
        return Sides;
    }

    public static float taper(int Index, int Count, float Tip) {
        float Along = Count <= 1 ? 0.0f : Index / (float) (Count - 1);
        return Mth.lerp(Along, 1.0f, Tip);
    }

    public static float[] widths(int Count, float HalfWidth, float Tip) {
        float[] Halves = new float[Count];
        for (int Index = 0; Index < Count; Index++) {
            Halves[Index] = HalfWidth * taper(Index, Count, Tip);
        }
        return Halves;
    }

    public static float[] widths(float[] Halves, float Factor, float Extra) {
        float[] Scaled = new float[Halves.length];
        for (int Index = 0; Index < Halves.length; Index++) {
            Scaled[Index] = Halves[Index] * Factor + Extra;
        }
        return Scaled;
    }

    public static void ribbon(VertexConsumer Consumer, PoseStack.Pose Last, List<Vec3> Path, Vec3 Eye, float HalfWidth, float Tip, float Red, float Green, float Blue,
                              float Alpha) {
        if (HalfWidth <= 0.0f) {
            return;
        }
        ribbon(Consumer, Last, Path, Eye, widths(Path.size(), HalfWidth, Tip), Red, Green, Blue, Alpha);
    }

    public static void ribbon(VertexConsumer Consumer, PoseStack.Pose Last, List<Vec3> Path, Vec3 Eye, float[] Halves, float Red, float Green, float Blue, float Alpha) {
        int Count = Math.min(Path.size(), Halves.length);
        if (Count < 2 || Alpha <= 0.005f) {
            return;
        }
        Vec3[] Sides = sides(Path, Eye);
        for (int Index = 0; Index + 1 < Count; Index++) {
            Vec3 A = Path.get(Index);
            Vec3 B = Path.get(Index + 1);
            Vec3 SideA = Sides[Index].scale(Halves[Index]);
            Vec3 SideB = Sides[Index + 1].scale(Halves[Index + 1]);
            Vec3 Normal = Eye.subtract(A);
            Normal = Normal.lengthSqr() < 1.0E-9 ? new Vec3(0.0, 1.0, 0.0) : Normal.normalize();
            Vec3[] Corners = {A.subtract(SideA), A.add(SideA), B.add(SideB), B.subtract(SideB)};
            for (int Corner = 0; Corner < 4; Corner++) {
                vertex(Consumer, Last, Corners[Corner], Uvs[Corner][0], Uvs[Corner][1], Red, Green, Blue, Alpha, Normal);
            }
        }
    }

    public static List<Vec3> offset(List<Vec3> Path, Vec3 Eye, float HalfWidth, float Tip, float Sign) {
        return offset(Path, Eye, widths(Path.size(), HalfWidth, Tip), Sign);
    }

    public static List<Vec3> offset(List<Vec3> Path, Vec3 Eye, float[] Halves, float Sign) {
        Vec3[] Sides = sides(Path, Eye);
        List<Vec3> Moved = new ArrayList<>(Path.size());
        for (int Index = 0; Index < Path.size(); Index++) {
            Moved.add(Path.get(Index).add(Sides[Index].scale(Sign * Halves[Math.min(Index, Halves.length - 1)])));
        }
        return Moved;
    }

    public static void sprite(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Centre, Vec3 Eye, float Radius, float Red, float Green, float Blue, float Alpha) {
        if (Alpha <= 0.005f) {
            return;
        }
        Vec3 Facing = Eye.subtract(Centre);
        if (Facing.lengthSqr() < 1.0E-6) {
            return;
        }
        Facing = Facing.normalize();
        Vec3 Right = Facing.cross(new Vec3(0.0, 1.0, 0.0));
        Right = Right.lengthSqr() < 1.0E-6 ? new Vec3(1.0, 0.0, 0.0) : Right.normalize();
        Vec3 Up = Right.cross(Facing).normalize();
        Right = Right.scale(Radius);
        Up = Up.scale(Radius);
        vertex(Consumer, Last, Centre.subtract(Right).subtract(Up), 0.0f, 1.0f, Red, Green, Blue, Alpha, Facing);
        vertex(Consumer, Last, Centre.add(Right).subtract(Up), 1.0f, 1.0f, Red, Green, Blue, Alpha, Facing);
        vertex(Consumer, Last, Centre.add(Right).add(Up), 1.0f, 0.0f, Red, Green, Blue, Alpha, Facing);
        vertex(Consumer, Last, Centre.subtract(Right).add(Up), 0.0f, 0.0f, Red, Green, Blue, Alpha, Facing);
    }

    public static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 Point, float U, float V, float Red, float Green, float Blue, float Alpha, Vec3 Normal) {
        Consumer.addVertex(Last, (float) Point.x, (float) Point.y, (float) Point.z)
                .setColor(Mth.clamp(Red, 0.0f, 1.0f), Mth.clamp(Green, 0.0f, 1.0f), Mth.clamp(Blue, 0.0f, 1.0f), Mth.clamp(Alpha, 0.0f, 1.0f))
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(Last, (float) Normal.x, (float) Normal.y, (float) Normal.z);
    }
}
