package net.rai.scp_unleashed.scp5001.event.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp5001.event.entity.Scp5001Vessel;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class Scp5001Beams {
    public static final double FindReach = 160.0;
    @Nullable
    private static Scp5001Vessel Found;
    private static long FoundAt = Long.MIN_VALUE;

    private Scp5001Beams() {
    }

    @Nullable
    public static Scp5001Vessel vessel() {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null || Client.player == null) {
            return null;
        }
        long Now = Client.level.getGameTime();
        if (Now == FoundAt && (Found == null || !Found.isRemoved())) {
            return Found;
        }
        FoundAt = Now;
        Found = null;
        double Best = FindReach * FindReach;
        AABB Area = Client.player.getBoundingBox().inflate(FindReach);
        for (Scp5001Vessel Each : Client.level.getEntitiesOfClass(Scp5001Vessel.class, Area)) {
            double Distance = Each.distanceToSqr(Client.player);
            if (Distance < Best && Each.mode() != Scp5001Vessel.ModeBurst) {
                Best = Distance;
                Found = Each;
            }
        }
        return Found;
    }

    public static Vec3 vesselAt(Scp5001Vessel Vessel, float Partial) {
        return Vessel.getPosition(Partial);
    }

    public static void aim(PoseStack Pose, Vec3 Direction) {
        Vector3f Unit = new Vector3f((float) Direction.x, (float) Direction.y, (float) Direction.z).normalize();
        Pose.mulPose(new Quaternionf().rotationTo(new Vector3f(0.0f, 0.0f, 1.0f), Unit));
    }

    public static void box(PoseStack.Pose Last, VertexConsumer Buffer, float Half, float Z0, float Z1, float V0, float V1, float TextureWidth, float TextureHeight,
                           int Light, int Colour) {
        float U0 = 0.0f;
        float U1 = (Z1 - Z0) * 16.0f / TextureWidth;
        float Va = V0 / TextureHeight;
        float Vb = V1 / TextureHeight;
        side(Last, Buffer, -Half, Half, Half, Half, Z0, Z1, U0, U1, Va, Vb, 0.0f, 1.0f, 0.0f, Light, Colour);
        side(Last, Buffer, Half, -Half, -Half, -Half, Z0, Z1, U0, U1, Va, Vb, 0.0f, -1.0f, 0.0f, Light, Colour);
        side(Last, Buffer, Half, Half, Half, -Half, Z0, Z1, U0, U1, Va, Vb, 1.0f, 0.0f, 0.0f, Light, Colour);
        side(Last, Buffer, -Half, -Half, -Half, Half, Z0, Z1, U0, U1, Va, Vb, -1.0f, 0.0f, 0.0f, Light, Colour);
        cap(Last, Buffer, Half, Z1, Va, Vb, TextureWidth, 1.0f, Light, Colour);
        cap(Last, Buffer, Half, Z0, Va, Vb, TextureWidth, -1.0f, Light, Colour);
    }

    private static void side(PoseStack.Pose Last, VertexConsumer Buffer, float Xa, float Ya, float Xb, float Yb, float Z0, float Z1, float U0, float U1, float V0, float V1,
                             float Nx, float Ny, float Nz, int Light, int Colour) {
        vertex(Last, Buffer, Xa, Ya, Z0, U0, V0, Nx, Ny, Nz, Light, Colour);
        vertex(Last, Buffer, Xb, Yb, Z0, U0, V1, Nx, Ny, Nz, Light, Colour);
        vertex(Last, Buffer, Xb, Yb, Z1, U1, V1, Nx, Ny, Nz, Light, Colour);
        vertex(Last, Buffer, Xa, Ya, Z1, U1, V0, Nx, Ny, Nz, Light, Colour);
    }

    private static void cap(PoseStack.Pose Last, VertexConsumer Buffer, float Half, float Z, float V0, float V1, float TextureWidth, float Facing, int Light, int Colour) {
        float U1 = Half * 2.0f * 16.0f / TextureWidth;
        if (Facing > 0) {
            vertex(Last, Buffer, -Half, -Half, Z, 0.0f, V0, 0.0f, 0.0f, 1.0f, Light, Colour);
            vertex(Last, Buffer, Half, -Half, Z, U1, V0, 0.0f, 0.0f, 1.0f, Light, Colour);
            vertex(Last, Buffer, Half, Half, Z, U1, V1, 0.0f, 0.0f, 1.0f, Light, Colour);
            vertex(Last, Buffer, -Half, Half, Z, 0.0f, V1, 0.0f, 0.0f, 1.0f, Light, Colour);
        } else {
            vertex(Last, Buffer, -Half, Half, Z, 0.0f, V1, 0.0f, 0.0f, -1.0f, Light, Colour);
            vertex(Last, Buffer, Half, Half, Z, U1, V1, 0.0f, 0.0f, -1.0f, Light, Colour);
            vertex(Last, Buffer, Half, -Half, Z, U1, V0, 0.0f, 0.0f, -1.0f, Light, Colour);
            vertex(Last, Buffer, -Half, -Half, Z, 0.0f, V0, 0.0f, 0.0f, -1.0f, Light, Colour);
        }
    }

    private static void vertex(PoseStack.Pose Last, VertexConsumer Buffer, float X, float Y, float Z, float U, float V, float Nx, float Ny, float Nz, int Light, int Colour) {
        Buffer.addVertex(Last, X, Y, Z).setColor(Colour).setUv(U, V).setOverlay(OverlayTexture.NO_OVERLAY).setLight(Light).setNormal(Last, Nx, Ny, Nz);
    }

    public static void ribbon(Matrix4f Matrix, VertexConsumer Buffer, Vec3 From, Vec3 To, float Width, int Red, int Green, int Blue, int Alpha) {
        Vec3 Along = To.subtract(From);
        if (Along.lengthSqr() < 1.0E-6) {
            return;
        }
        Vec3 Up = Math.abs(Along.normalize().y) > 0.95 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 SideA = Along.cross(Up).normalize().scale(Width * 0.5);
        Vec3 SideB = Along.cross(SideA).normalize().scale(Width * 0.5);
        strip(Matrix, Buffer, From, To, SideA, Red, Green, Blue, Alpha);
        strip(Matrix, Buffer, From, To, SideB, Red, Green, Blue, Alpha);
    }

    private static void strip(Matrix4f Matrix, VertexConsumer Buffer, Vec3 From, Vec3 To, Vec3 Side, int Red, int Green, int Blue, int Alpha) {
        point(Matrix, Buffer, From.subtract(Side), Red, Green, Blue, Alpha);
        point(Matrix, Buffer, To.subtract(Side), Red, Green, Blue, Alpha);
        point(Matrix, Buffer, To.add(Side), Red, Green, Blue, Alpha);
        point(Matrix, Buffer, From.add(Side), Red, Green, Blue, Alpha);
        point(Matrix, Buffer, From.add(Side), Red, Green, Blue, Alpha);
        point(Matrix, Buffer, To.add(Side), Red, Green, Blue, Alpha);
        point(Matrix, Buffer, To.subtract(Side), Red, Green, Blue, Alpha);
        point(Matrix, Buffer, From.subtract(Side), Red, Green, Blue, Alpha);
    }

    private static void point(Matrix4f Matrix, VertexConsumer Buffer, Vec3 At, int Red, int Green, int Blue, int Alpha) {
        Buffer.addVertex(Matrix, (float) At.x, (float) At.y, (float) At.z).setColor(Red, Green, Blue, Alpha);
    }

    public static void strained(Matrix4f Matrix, VertexConsumer Buffer, Vec3 From, Vec3 To, float Width, float Wobble, float Time, int Seed, int Red, int Green, int Blue,
                                int Alpha) {
        Vec3 Along = To.subtract(From);
        double Length = Along.length();
        int Pieces = Mth.clamp((int) Math.ceil(Length * 1.5), 2, 48);
        Vec3 Up = Math.abs(Along.normalize().y) > 0.95 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 SideA = Along.cross(Up).normalize();
        Vec3 SideB = Along.cross(SideA).normalize();
        Vec3 Last = From;
        for (int Index = 1; Index <= Pieces; Index++) {
            double Share = Index / (double) Pieces;
            double Envelope = Math.sin(Share * Math.PI);
            double Phase = Time * 0.9 + Index * 0.8 + Seed * 1.7;
            double OffsetA = Math.sin(Phase) * Wobble * Envelope;
            double OffsetB = Math.cos(Phase * 1.3) * Wobble * Envelope;
            Vec3 Next = From.add(Along.scale(Share)).add(SideA.scale(OffsetA)).add(SideB.scale(OffsetB));
            ribbon(Matrix, Buffer, Last, Next, Width, Red, Green, Blue, Alpha);
            Last = Next;
        }
    }

    public static void star(Matrix4f Matrix, VertexConsumer Buffer, Vec3 At, float Size, float Spin, int Red, int Green, int Blue, int Alpha) {
        for (int Index = 0; Index < 3; Index++) {
            double Angle = Spin + Index * Math.PI / 3.0;
            Vec3 Arm = new Vec3(Math.cos(Angle), Math.sin(Angle * 0.7), Math.sin(Angle)).normalize().scale(Size);
            ribbon(Matrix, Buffer, At.subtract(Arm), At.add(Arm), Size * 0.35f, Red, Green, Blue, Alpha);
        }
    }

    public static float flicker(float Time, int Seed) {
        float Noise = Mth.sin(Time * 1.7f + Seed) * 0.5f + Mth.sin(Time * 3.1f + Seed * 0.37f) * 0.3f + Mth.sin(Time * 7.3f + Seed * 2.1f) * 0.2f;
        return 0.65f + 0.35f * Noise;
    }
}
