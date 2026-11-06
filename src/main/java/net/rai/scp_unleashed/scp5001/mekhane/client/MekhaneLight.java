package net.rai.scp_unleashed.scp5001.mekhane.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp5001.mekhane.Scp5001MekhaneRegistry;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

public final class MekhaneLight {
    public static final ResourceLocation Beam = Scp5001MekhaneRegistry.location("textures/entity/mekhane/beam.png");
    public static final ResourceLocation Major = Scp5001MekhaneRegistry.location("textures/entity/mekhane/sigil_major.png");
    public static final ResourceLocation Minor = Scp5001MekhaneRegistry.location("textures/entity/mekhane/sigil_minor.png");
    public static final ResourceLocation ShockRing = Scp5001MekhaneRegistry.location("textures/entity/mekhane/shock_ring.png");
    public static final ResourceLocation PortalCore = Scp5001MekhaneRegistry.location("textures/entity/mekhane/portal_core.png");
    public static final float Red = 1.0f;
    public static final float Green = 0.88f;
    public static final float Blue = 0.6f;

    private MekhaneLight() {
    }

    public static RenderType glow(ResourceLocation Texture) {
        return ShaderCompat.useFallback() ? RenderType.eyes(Texture) : RenderType.entityTranslucentEmissive(Texture);
    }

    public static boolean additive() {
        return ShaderCompat.useFallback();
    }

    public static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, float X, float Y, float Z, float U, float V, float R, float G, float B, float Alpha, float Nx, float Ny, float Nz) {
        float Scale = additive() ? Alpha : 1.0f;
        Consumer.addVertex(Last, X, Y, Z).setColor(R * Scale, G * Scale, B * Scale, additive() ? 1.0f : Alpha).setUv(U, V).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT).setNormal(Last, Nx, Ny, Nz);
    }

    public static void quad(VertexConsumer Consumer, PoseStack.Pose Last, Vec3 A, Vec3 B, Vec3 C, Vec3 D, float U0, float V0, float U1, float V1, float R, float G, float Bl,
                            float AlphaNear, float AlphaFar) {
        Vec3 Normal = B.subtract(A).cross(D.subtract(A)).normalize();
        float Nx = (float) Normal.x;
        float Ny = (float) Normal.y;
        float Nz = (float) Normal.z;
        vertex(Consumer, Last, (float) A.x, (float) A.y, (float) A.z, U0, V0, R, G, Bl, AlphaNear, Nx, Ny, Nz);
        vertex(Consumer, Last, (float) B.x, (float) B.y, (float) B.z, U0, V1, R, G, Bl, AlphaFar, Nx, Ny, Nz);
        vertex(Consumer, Last, (float) C.x, (float) C.y, (float) C.z, U1, V1, R, G, Bl, AlphaFar, Nx, Ny, Nz);
        vertex(Consumer, Last, (float) D.x, (float) D.y, (float) D.z, U1, V0, R, G, Bl, AlphaNear, Nx, Ny, Nz);
        vertex(Consumer, Last, (float) D.x, (float) D.y, (float) D.z, U1, V0, R, G, Bl, AlphaNear, -Nx, -Ny, -Nz);
        vertex(Consumer, Last, (float) C.x, (float) C.y, (float) C.z, U1, V1, R, G, Bl, AlphaFar, -Nx, -Ny, -Nz);
        vertex(Consumer, Last, (float) B.x, (float) B.y, (float) B.z, U0, V1, R, G, Bl, AlphaFar, -Nx, -Ny, -Nz);
        vertex(Consumer, Last, (float) A.x, (float) A.y, (float) A.z, U0, V0, R, G, Bl, AlphaNear, -Nx, -Ny, -Nz);
    }

    public static void ray(MultiBufferSource Buffers, PoseStack Pose, Vec3 From, Vec3 To, float Radius, float Spin, float Scroll, float Alpha, float FarAlpha) {
        if (Alpha <= 0.003f || Radius <= 0.0f) {
            return;
        }
        Vec3 Axis = To.subtract(From);
        double Length = Axis.length();
        if (Length < 0.01) {
            return;
        }
        Vec3 Along = Axis.scale(1.0 / Length);
        Vec3 Helper = Math.abs(Along.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 First = Along.cross(Helper).normalize();
        Vec3 Second = Along.cross(First).normalize();
        VertexConsumer Consumer = Buffers.getBuffer(glow(Beam));
        PoseStack.Pose Last = Pose.last();
        float V1 = Scroll + (float) (Length / 6.0);
        for (int Side = 0; Side < 4; Side++) {
            double Angle = Spin + Side * Math.PI / 2.0;
            Vec3 A = First.scale(Math.cos(Angle) * Radius).add(Second.scale(Math.sin(Angle) * Radius));
            Vec3 B = First.scale(Math.cos(Angle + Math.PI / 2.0) * Radius).add(Second.scale(Math.sin(Angle + Math.PI / 2.0) * Radius));
            quad(Consumer, Last, From.add(A), To.add(A), To.add(B), From.add(B), 0.0f, Scroll, 1.0f, V1, Red, Green, Blue, Alpha, FarAlpha);
        }
    }

    public static void disc(MultiBufferSource Buffers, PoseStack Pose, ResourceLocation Texture, Vec3 Centre, Vec3 Normal, float Radius, float Spin, float R, float G, float B, float Alpha) {
        if (Alpha <= 0.003f || Radius <= 0.0f) {
            return;
        }
        Vec3 Facing = Normal.normalize();
        Vec3 Helper = Math.abs(Facing.y) > 0.9 ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 First = Facing.cross(Helper).normalize();
        Vec3 Second = Facing.cross(First).normalize();
        float Cos = Mth.cos(Spin);
        float Sin = Mth.sin(Spin);
        Vec3 U = First.scale(Cos).add(Second.scale(Sin)).scale(Radius);
        Vec3 V = Second.scale(Cos).subtract(First.scale(Sin)).scale(Radius);
        VertexConsumer Consumer = Buffers.getBuffer(glow(Texture));
        quad(Consumer, Pose.last(), Centre.subtract(U).subtract(V), Centre.subtract(U).add(V), Centre.add(U).add(V), Centre.add(U).subtract(V), 0.0f, 0.0f, 1.0f, 1.0f, R, G, B,
                Alpha, Alpha);
    }

    public static void column(MultiBufferSource Buffers, PoseStack Pose, Vec3 Base, float Height, float Radius, float Spin, float Scroll, float Alpha) {
        ray(Buffers, Pose, Base, Base.add(0.0, Height, 0.0), Radius, Spin, Scroll, Alpha, Alpha * 0.35f);
    }
}
