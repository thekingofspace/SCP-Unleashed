package net.rai.scp_unleashed.z0003.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class Z0003FieldLayers {
    public static final int Shell = 0;
    public static final int BrightBand = 1;
    public static final int DarkBand = 2;
    public static final int Core = 3;
    public static final int Halo = 4;
    public static final int Glow = 0;
    public static final int Body = 1;
    public static final float SpanScale = 128.0f;
    public static final float AgeScale = 16.0f;
    public static final float BrightShare = 0.74f;
    public static final float DarkShare = 0.56f;
    public static final float CoreShare = 0.2f;
    public static final float MinCore = 0.35f;
    public static final float Breathe = 0.015f;
    public static final float HaloScale = 1.6f;
    public static final float HaloBloom = 1.6f;
    public static final float HaloWindow = 0.8f;
    public static final float HaloCap = 4.0f;
    public static final float HaloNear = 1.6f;
    public static final float HaloFar = 2.6f;
    public static final float MaxSpan = 250.0f;
    public static final float FineReach = 6.0f;
    public static final Mesh Fine = new Mesh(24, 48);
    public static final Mesh Coarse = new Mesh(16, 32);
    public static final Mesh Small = new Mesh(12, 24);
    public static final float PlasmaRepeat = 3.0f;
    public static final float[] DeepTint = {0.06f, 0.42f, 0.14f};
    public static final float[] GlowTint = {0.36f, 1.0f, 0.46f};

    private Z0003FieldLayers() {
    }

    public record Look(float Reach, float Fade, float Grow, float Flash, float Front, float Age) {
        public float bright() {
            return Reach * (BrightShare + Breathe * Mth.sin(Age * 0.05f));
        }

        public float dark() {
            return Reach * (DarkShare + Breathe * Mth.sin(Age * 0.05f + 2.1f));
        }

        public float core() {
            return Math.max(MinCore, Reach * CoreShare);
        }

        public int ageCode() {
            return Mth.clamp(Math.round(Age * AgeScale), 0, Short.MAX_VALUE);
        }

        public int[] packed() {
            return new int[]{byteOf(Fade), byteOf(Grow), byteOf(Flash), byteOf(Front)};
        }
    }

    public static final class Mesh {
        public final int Rings;
        public final int Segments;
        public final float[] Polar;
        public final float[] Azimuth;
        public final float[] X;
        public final float[] Y;
        public final float[] Z;

        public Mesh(int RingCount, int SegmentCount) {
            Rings = RingCount;
            Segments = SegmentCount;
            int Count = (Rings + 1) * (Segments + 1);
            Polar = new float[Count];
            Azimuth = new float[Count];
            X = new float[Count];
            Y = new float[Count];
            Z = new float[Count];
            for (int Ring = 0; Ring <= Rings; Ring++) {
                float Down = Mth.clamp(Mth.PI * Ring / Rings, 1.0E-4f, Mth.PI - 1.0E-4f);
                for (int Slice = 0; Slice <= Segments; Slice++) {
                    float Around = Mth.TWO_PI * Slice / Segments;
                    int Index = Ring * (Segments + 1) + Slice;
                    Polar[Index] = Down;
                    Azimuth[Index] = Around;
                    X[Index] = (float) (Math.sin(Down) * Math.cos(Around));
                    Y[Index] = (float) Math.cos(Down);
                    Z[Index] = (float) (Math.sin(Down) * Math.sin(Around));
                }
            }
        }

        public int at(int Ring, int Slice) {
            return Ring * (Segments + 1) + Slice;
        }
    }

    public static Mesh meshFor(float Radius) {
        return Radius >= FineReach ? Fine : Radius >= 1.5f ? Coarse : Small;
    }

    public static void render(PoseStack Pose, MultiBufferSource Buffers, Look Shown, Vec3 Eye, Quaternionf Turn) {
        if (Z0003RenderTypes.fieldShaded()) {
            shaded(Pose, Buffers, Shown, Eye, Turn);
        } else {
            fallback(Pose, Buffers, Shown, Eye, Turn);
        }
    }

    private static void shaded(PoseStack Pose, MultiBufferSource Buffers, Look Shown, Vec3 Eye, Quaternionf Turn) {
        PoseStack.Pose Last = Pose.last();
        double Distance = Eye.length();
        float Reach = Shown.Reach();
        float Bright = Shown.bright();
        float Dark = Shown.dark();
        float CoreRadius = Shown.core();
        int[] Packed = Shown.packed();
        int AgeCode = Shown.ageCode();
        Mesh Outer = meshFor(Reach);
        boolean Bands = Shown.Grow() > 0.2f && Reach > 1.2f;
        if (Distance <= Reach) {
            sphere(Buffers.getBuffer(Z0003RenderTypes.FieldBody), Last, Outer, Reach, false, Packed, Shell, Body, AgeCode);
        }
        sphere(Buffers.getBuffer(Z0003RenderTypes.FieldGlow), Last, Outer, Reach, false, Packed, Shell, Glow, AgeCode);
        if (Bands) {
            sphere(Buffers.getBuffer(Z0003RenderTypes.FieldGlow), Last, Outer, Bright, false, Packed, BrightBand, Glow, AgeCode);
            sphere(Buffers.getBuffer(Z0003RenderTypes.FieldBody), Last, Outer, Dark, false, Packed, DarkBand, Body, AgeCode);
        }
        boolean OutsideCore = Distance > CoreRadius;
        sphere(Buffers.getBuffer(Z0003RenderTypes.FieldBody), Last, Small, CoreRadius, OutsideCore, Packed, Core, Body, AgeCode);
        sphere(Buffers.getBuffer(Z0003RenderTypes.FieldGlow), Last, Small, CoreRadius, OutsideCore, Packed, Core, Glow, AgeCode);
        if (Bands && Distance > Dark) {
            sphere(Buffers.getBuffer(Z0003RenderTypes.FieldBody), Last, Outer, Dark, true, Packed, DarkBand, Body, AgeCode);
        }
        if (Bands && Distance > Bright) {
            sphere(Buffers.getBuffer(Z0003RenderTypes.FieldGlow), Last, Outer, Bright, true, Packed, BrightBand, Glow, AgeCode);
        }
        if (Distance > Reach) {
            sphere(Buffers.getBuffer(Z0003RenderTypes.FieldBody), Last, Outer, Reach, true, Packed, Shell, Body, AgeCode);
            sphere(Buffers.getBuffer(Z0003RenderTypes.FieldGlow), Last, Outer, Reach, true, Packed, Shell, Glow, AgeCode);
            halo(Buffers.getBuffer(Z0003RenderTypes.FieldGlow), Last, Shown, Turn, (float) Distance, AgeCode);
        }
    }

    private static void sphere(VertexConsumer Consumer, PoseStack.Pose Last, Mesh Shape, float Radius, boolean Outward, int[] Packed, int Kind, int Pass,
                               int AgeCode) {
        int SpanCode = Mth.clamp(Math.round(Radius * SpanScale), 0, Short.MAX_VALUE);
        for (int Ring = 0; Ring < Shape.Rings; Ring++) {
            for (int Slice = 0; Slice < Shape.Segments; Slice++) {
                int A = Shape.at(Ring, Slice);
                int B = Shape.at(Ring, Slice + 1);
                int C = Shape.at(Ring + 1, Slice + 1);
                int D = Shape.at(Ring + 1, Slice);
                if (Outward) {
                    shaderVertex(Consumer, Last, Shape, A, Radius, Packed, Kind, Pass, SpanCode, AgeCode);
                    shaderVertex(Consumer, Last, Shape, B, Radius, Packed, Kind, Pass, SpanCode, AgeCode);
                    shaderVertex(Consumer, Last, Shape, C, Radius, Packed, Kind, Pass, SpanCode, AgeCode);
                    shaderVertex(Consumer, Last, Shape, D, Radius, Packed, Kind, Pass, SpanCode, AgeCode);
                } else {
                    shaderVertex(Consumer, Last, Shape, D, Radius, Packed, Kind, Pass, SpanCode, AgeCode);
                    shaderVertex(Consumer, Last, Shape, C, Radius, Packed, Kind, Pass, SpanCode, AgeCode);
                    shaderVertex(Consumer, Last, Shape, B, Radius, Packed, Kind, Pass, SpanCode, AgeCode);
                    shaderVertex(Consumer, Last, Shape, A, Radius, Packed, Kind, Pass, SpanCode, AgeCode);
                }
            }
        }
    }

    private static void shaderVertex(VertexConsumer Consumer, PoseStack.Pose Last, Mesh Shape, int Index, float Radius, int[] Packed, int Kind, int Pass,
                                     int SpanCode, int AgeCode) {
        float Nx = Shape.X[Index];
        float Ny = Shape.Y[Index];
        float Nz = Shape.Z[Index];
        Consumer.addVertex(Last, Nx * Radius, Ny * Radius, Nz * Radius).setColor(Packed[0], Packed[1], Packed[2], Packed[3])
                .setUv(Shape.Polar[Index], Shape.Azimuth[Index]).setUv1(Kind, Pass).setUv2(SpanCode, AgeCode).setNormal(Last, Nx, Ny, Nz);
    }

    public static float haloPresence(float Reach, float Distance) {
        float Share = Mth.clamp((Distance - HaloNear * Reach) / ((HaloFar - HaloNear) * Reach), 0.0f, 1.0f);
        return Share * Share * (3.0f - 2.0f * Share);
    }

    public static float haloHalf(float Reach, float Distance) {
        float Cone = HaloBloom * Reach;
        float Size = Distance > Cone * 1.02f ? Cone * Distance / Mth.sqrt(Distance * Distance - Cone * Cone) : HaloCap * Reach;
        return Math.min(Math.min(Size / HaloWindow, HaloCap * Reach), MaxSpan / Mth.SQRT_OF_TWO);
    }

    private static void halo(VertexConsumer Consumer, PoseStack.Pose Last, Look Shown, Quaternionf Turn, float Distance, int AgeCode) {
        float Reach = Shown.Reach();
        float Presence = haloPresence(Reach, Distance);
        if (Presence <= 0.003f) {
            return;
        }
        float Half = haloHalf(Reach, Distance);
        int[] Packed = {byteOf(Shown.Fade() * Presence), byteOf(2.0f * Reach / Half), byteOf(Shown.Flash()), 0};
        Vector3f Right = Turn.transform(new Vector3f(1.0f, 0.0f, 0.0f));
        Vector3f Up = Turn.transform(new Vector3f(0.0f, 1.0f, 0.0f));
        float[][] Corners = {{-1.0f, -1.0f}, {1.0f, -1.0f}, {1.0f, 1.0f}, {-1.0f, 1.0f}};
        for (float[] Corner : Corners) {
            float X = (Right.x * Corner[0] + Up.x * Corner[1]) * Half;
            float Y = (Right.y * Corner[0] + Up.y * Corner[1]) * Half;
            float Z = (Right.z * Corner[0] + Up.z * Corner[1]) * Half;
            float Span = Mth.sqrt(X * X + Y * Y + Z * Z);
            float Polar = (float) Math.acos(Mth.clamp(Y / Span, -1.0f, 1.0f));
            float Azimuth = (float) Math.atan2(Z, X);
            int SpanCode = Mth.clamp(Math.round(Span * SpanScale), 0, Short.MAX_VALUE);
            float Exact = SpanCode / SpanScale;
            float Sine = Mth.sin(Polar);
            Consumer.addVertex(Last, Sine * Mth.cos(Azimuth) * Exact, Mth.cos(Polar) * Exact, Sine * Mth.sin(Azimuth) * Exact)
                    .setColor(Packed[0], Packed[1], Packed[2], Packed[3]).setUv(Polar, Azimuth).setUv1(Halo, Glow).setUv2(SpanCode, AgeCode)
                    .setNormal(Last, 0.0f, 1.0f, 0.0f);
        }
    }

    private static void fallback(PoseStack Pose, MultiBufferSource Buffers, Look Shown, Vec3 Eye, Quaternionf Turn) {
        PoseStack.Pose Last = Pose.last();
        Vector3f Viewer = new Vector3f((float) Eye.x, (float) Eye.y, (float) Eye.z);
        double Distance = Eye.length();
        float Reach = Shown.Reach();
        float Fade = Shown.Fade();
        float Inner = Mth.clamp((Shown.Grow() - 0.25f) / 0.65f, 0.0f, 1.0f);
        float Scroll = Shown.Age() * 0.004f;
        Mesh Outer = meshFor(Reach);
        RenderType Glowing = Z0003RenderTypes.fallbackGlow(Z0003RenderTypes.PlasmaTexture);
        RenderType Shaded = Z0003RenderTypes.fallbackBody(Z0003RenderTypes.PlasmaTexture);
        if (Distance <= Reach) {
            textured(Buffers.getBuffer(Shaded), Last, Outer, Reach, Viewer, false, DeepTint, Scroll, Rim -> (0.3f + 0.2f * Rim * Rim) * Fade);
        }
        float BackGlow = Distance <= Reach ? 1.0f : 0.6f;
        textured(Buffers.getBuffer(Glowing), Last, Outer, Reach, Viewer, false, GlowTint, Scroll, Rim -> (0.08f + 0.5f * Rim * Rim * Rim) * Fade * BackGlow);
        if (Inner > 0.0f) {
            textured(Buffers.getBuffer(Glowing), Last, Outer, Shown.bright(), Viewer, false, GlowTint, -Scroll, Rim -> (float) Math.pow(Rim, 2.5) * 0.5f * Fade * Inner);
        }
        float CoreRadius = Shown.core();
        float Dense = Mth.clamp((Shown.Grow() - 0.1f) / 0.75f, 0.0f, 1.0f) * Fade;
        textured(Buffers.getBuffer(Shaded), Last, Small, CoreRadius, Viewer, Distance > CoreRadius, DeepTint, Scroll, Rim -> (0.55f + 0.3f * (1.0f - Rim)) * Dense);
        if (Inner > 0.0f && Distance > Shown.dark()) {
            textured(Buffers.getBuffer(Shaded), Last, Outer, Shown.dark(), Viewer, true, DeepTint, Scroll, Rim -> (Rim * Rim * 0.4f + 0.03f) * Fade * Inner);
        }
        if (Inner > 0.0f && Distance > Shown.bright()) {
            textured(Buffers.getBuffer(Glowing), Last, Outer, Shown.bright(), Viewer, true, GlowTint, -Scroll, Rim -> (float) Math.pow(Rim, 2.5) * 0.75f * Fade * Inner);
        }
        if (Distance > Reach) {
            float Front = Shown.Front();
            float Flash = Shown.Flash();
            textured(Buffers.getBuffer(Shaded), Last, Outer, Reach, Viewer, true, DeepTint, Scroll, Rim -> (0.34f + 0.22f * Rim * Rim + 0.1f * Front) * Fade);
            textured(Buffers.getBuffer(Glowing), Last, Outer, Reach, Viewer, true, GlowTint, Scroll,
                    Rim -> Mth.clamp((0.1f + 0.75f * Rim * Rim * Rim + 0.2f * Front + 0.5f * Flash) * Fade, 0.0f, 1.0f));
            fallbackHalo(Buffers.getBuffer(Z0003RenderTypes.fallbackGlow(Z0003RenderTypes.HaloTexture)), Last, Reach, Turn, Fade * haloPresence(Reach, (float) Distance));
        }
    }

    private interface Strength {
        float at(float Rim);
    }

    private static void textured(VertexConsumer Consumer, PoseStack.Pose Last, Mesh Shape, float Radius, Vector3f Viewer, boolean Outward, float[] Tint, float Scroll,
                                 Strength Alpha) {
        if (Radius <= 0.01f) {
            return;
        }
        for (int Ring = 0; Ring < Shape.Rings; Ring++) {
            for (int Slice = 0; Slice < Shape.Segments; Slice++) {
                int A = Shape.at(Ring, Slice);
                int B = Shape.at(Ring, Slice + 1);
                int C = Shape.at(Ring + 1, Slice + 1);
                int D = Shape.at(Ring + 1, Slice);
                float Mx = (Shape.X[A] + Shape.X[C]) * 0.5f;
                float My = (Shape.Y[A] + Shape.Y[C]) * 0.5f;
                float Mz = (Shape.Z[A] + Shape.Z[C]) * 0.5f;
                float Toward = Mx * (Viewer.x - Mx * Radius) + My * (Viewer.y - My * Radius) + Mz * (Viewer.z - Mz * Radius);
                if (Toward > 0.0f != Outward) {
                    continue;
                }
                int[] Order = Outward ? new int[]{A, B, C, D} : new int[]{D, C, B, A};
                for (int Index : Order) {
                    texturedVertex(Consumer, Last, Shape, Index, Radius, Viewer, Tint, Scroll, Alpha);
                }
            }
        }
    }

    private static void texturedVertex(VertexConsumer Consumer, PoseStack.Pose Last, Mesh Shape, int Index, float Radius, Vector3f Viewer, float[] Tint, float Scroll,
                                       Strength Alpha) {
        float Nx = Shape.X[Index];
        float Ny = Shape.Y[Index];
        float Nz = Shape.Z[Index];
        float X = Nx * Radius;
        float Y = Ny * Radius;
        float Z = Nz * Radius;
        float Dx = Viewer.x - X;
        float Dy = Viewer.y - Y;
        float Dz = Viewer.z - Z;
        float Length = Mth.sqrt(Dx * Dx + Dy * Dy + Dz * Dz);
        float Facing = Length < 1.0E-4f ? 1.0f : Math.abs(Nx * Dx + Ny * Dy + Nz * Dz) / Length;
        float Value = Mth.clamp(Alpha.at(1.0f - Facing), 0.0f, 1.0f);
        float U = Shape.Azimuth[Index] / Mth.TWO_PI * PlasmaRepeat * 2.0f;
        float V = Shape.Polar[Index] / Mth.PI * PlasmaRepeat + Scroll;
        Consumer.addVertex(Last, X, Y, Z).setColor(Tint[0], Tint[1], Tint[2], Value).setUv(U, V).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(Last, Nx, Ny, Nz);
    }

    private static void fallbackHalo(VertexConsumer Consumer, PoseStack.Pose Last, float Reach, Quaternionf Turn, float Fade) {
        if (Fade <= 0.003f) {
            return;
        }
        Vector3f Right = Turn.transform(new Vector3f(1.0f, 0.0f, 0.0f));
        Vector3f Up = Turn.transform(new Vector3f(0.0f, 1.0f, 0.0f));
        float Size = Reach * HaloScale;
        float[][] Corners = {{-1.0f, -1.0f, 0.0f, 1.0f}, {1.0f, -1.0f, 1.0f, 1.0f}, {1.0f, 1.0f, 1.0f, 0.0f}, {-1.0f, 1.0f, 0.0f, 0.0f}};
        for (float[] Corner : Corners) {
            Consumer.addVertex(Last, (Right.x * Corner[0] + Up.x * Corner[1]) * Size, (Right.y * Corner[0] + Up.y * Corner[1]) * Size,
                            (Right.z * Corner[0] + Up.z * Corner[1]) * Size).setColor(GlowTint[0], GlowTint[1], GlowTint[2], Mth.clamp(Fade * 0.85f, 0.0f, 1.0f))
                    .setUv(Corner[2], Corner[3]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(LightTexture.FULL_BRIGHT).setNormal(Last, 0.0f, 1.0f, 0.0f);
        }
    }

    private static int byteOf(float Value) {
        return Mth.clamp(Math.round(Value * 255.0f), 0, 255);
    }
}
