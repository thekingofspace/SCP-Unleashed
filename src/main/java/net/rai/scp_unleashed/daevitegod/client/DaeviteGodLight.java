package net.rai.scp_unleashed.daevitegod.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.joml.Matrix4f;

public final class DaeviteGodLight {
    public static final ResourceLocation Pillar = DaeviteGodGeoModel.location("textures/entity/daevite_god/pillar.png");
    public static final ResourceLocation Orb = DaeviteGodGeoModel.location("textures/entity/daevite_god/orb.png");
    public static final ResourceLocation Ring = DaeviteGodGeoModel.location("textures/entity/daevite_god/sigil_ring.png");
    public static final ResourceLocation LightningGlyph = DaeviteGodGeoModel.location("textures/entity/daevite_god/sigil_lightning.png");
    public static final ResourceLocation SpikeGlyph = DaeviteGodGeoModel.location("textures/entity/daevite_god/sigil_spikes.png");
    public static final ResourceLocation SpiritGlyph = DaeviteGodGeoModel.location("textures/entity/daevite_god/sigil_spirits.png");
    public static final int BoltSections = 8;
    public static final float BoltSection = 4.0f;

    private DaeviteGodLight() {
    }

    public static RenderType glow(ResourceLocation Texture) {
        return ShaderCompat.useFallback() ? RenderType.eyes(Texture) : RenderType.entityTranslucentEmissive(Texture);
    }

    public static boolean additive() {
        return ShaderCompat.useFallback();
    }

    public static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, float X, float Y, float Z, float U, float V, float Red, float Green, float Blue, float Alpha,
                              float NormalX, float NormalY, float NormalZ) {
        float Scale = additive() ? Alpha : 1.0f;
        Consumer.addVertex(Last, X, Y, Z)
                .setColor(Red * Scale, Green * Scale, Blue * Scale, additive() ? 1.0f : Alpha)
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(Last, NormalX, NormalY, NormalZ);
    }

    public static void flat(MultiBufferSource Buffers, PoseStack Pose, ResourceLocation Texture, float Radius, float Lift, float Angle, float Red, float Green, float Blue,
                            float Alpha) {
        if (Alpha <= 0.003f || Radius <= 0.0f) {
            return;
        }
        VertexConsumer Consumer = Buffers.getBuffer(glow(Texture));
        Pose.pushPose();
        Pose.translate(0.0f, Lift, 0.0f);
        Pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(Angle));
        PoseStack.Pose Last = Pose.last();
        vertex(Consumer, Last, -Radius, 0.0f, -Radius, 0.0f, 0.0f, Red, Green, Blue, Alpha, 0.0f, 1.0f, 0.0f);
        vertex(Consumer, Last, -Radius, 0.0f, Radius, 0.0f, 1.0f, Red, Green, Blue, Alpha, 0.0f, 1.0f, 0.0f);
        vertex(Consumer, Last, Radius, 0.0f, Radius, 1.0f, 1.0f, Red, Green, Blue, Alpha, 0.0f, 1.0f, 0.0f);
        vertex(Consumer, Last, Radius, 0.0f, -Radius, 1.0f, 0.0f, Red, Green, Blue, Alpha, 0.0f, 1.0f, 0.0f);
        vertex(Consumer, Last, Radius, 0.0f, -Radius, 1.0f, 0.0f, Red, Green, Blue, Alpha, 0.0f, -1.0f, 0.0f);
        vertex(Consumer, Last, Radius, 0.0f, Radius, 1.0f, 1.0f, Red, Green, Blue, Alpha, 0.0f, -1.0f, 0.0f);
        vertex(Consumer, Last, -Radius, 0.0f, Radius, 0.0f, 1.0f, Red, Green, Blue, Alpha, 0.0f, -1.0f, 0.0f);
        vertex(Consumer, Last, -Radius, 0.0f, -Radius, 0.0f, 0.0f, Red, Green, Blue, Alpha, 0.0f, -1.0f, 0.0f);
        Pose.popPose();
    }

    public static void beam(MultiBufferSource Buffers, PoseStack Pose, float Radius, float Bottom, float Top, float Spin, float Scroll, float Alpha) {
        if (Alpha <= 0.003f) {
            return;
        }
        VertexConsumer Consumer = Buffers.getBuffer(glow(Pillar));
        Pose.pushPose();
        Pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(Spin));
        PoseStack.Pose Last = Pose.last();
        float V0 = Scroll;
        float V1 = Scroll + (Top - Bottom) / 4.0f;
        for (int Side = 0; Side < 4; Side++) {
            float Angle = Side * Mth.HALF_PI;
            float Ax = Mth.cos(Angle) * Radius;
            float Az = Mth.sin(Angle) * Radius;
            float Bx = Mth.cos(Angle + Mth.HALF_PI) * Radius;
            float Bz = Mth.sin(Angle + Mth.HALF_PI) * Radius;
            float Nx = Mth.cos(Angle + Mth.HALF_PI * 0.5f);
            float Nz = Mth.sin(Angle + Mth.HALF_PI * 0.5f);
            vertex(Consumer, Last, Ax, Bottom, Az, 0.0f, V1, 0.95f, 0.22f, 0.16f, Alpha, Nx, 0.0f, Nz);
            vertex(Consumer, Last, Ax, Top, Az, 0.0f, V0, 0.95f, 0.22f, 0.16f, Alpha * 0.2f, Nx, 0.0f, Nz);
            vertex(Consumer, Last, Bx, Top, Bz, 1.0f, V0, 0.95f, 0.22f, 0.16f, Alpha * 0.2f, Nx, 0.0f, Nz);
            vertex(Consumer, Last, Bx, Bottom, Bz, 1.0f, V1, 0.95f, 0.22f, 0.16f, Alpha, Nx, 0.0f, Nz);
            vertex(Consumer, Last, Bx, Bottom, Bz, 1.0f, V1, 0.95f, 0.22f, 0.16f, Alpha, -Nx, 0.0f, -Nz);
            vertex(Consumer, Last, Bx, Top, Bz, 1.0f, V0, 0.95f, 0.22f, 0.16f, Alpha * 0.2f, -Nx, 0.0f, -Nz);
            vertex(Consumer, Last, Ax, Top, Az, 0.0f, V0, 0.95f, 0.22f, 0.16f, Alpha * 0.2f, -Nx, 0.0f, -Nz);
            vertex(Consumer, Last, Ax, Bottom, Az, 0.0f, V1, 0.95f, 0.22f, 0.16f, Alpha, -Nx, 0.0f, -Nz);
        }
        Pose.popPose();
    }

    public static void orb(MultiBufferSource Buffers, PoseStack Pose, float Size, float Spin, float Alpha) {
        if (Alpha <= 0.003f || Size <= 0.0f) {
            return;
        }
        VertexConsumer Consumer = Buffers.getBuffer(glow(Orb));
        for (int Plane = 0; Plane < 3; Plane++) {
            Pose.pushPose();
            Pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(Spin + Plane * 60.0f));
            if (Plane == 2) {
                Pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0f));
            }
            PoseStack.Pose Last = Pose.last();
            vertex(Consumer, Last, -Size, -Size, 0.0f, 0.0f, 1.0f, 1.0f, 0.5f, 0.38f, Alpha, 0.0f, 0.0f, 1.0f);
            vertex(Consumer, Last, Size, -Size, 0.0f, 1.0f, 1.0f, 1.0f, 0.5f, 0.38f, Alpha, 0.0f, 0.0f, 1.0f);
            vertex(Consumer, Last, Size, Size, 0.0f, 1.0f, 0.0f, 1.0f, 0.5f, 0.38f, Alpha, 0.0f, 0.0f, 1.0f);
            vertex(Consumer, Last, -Size, Size, 0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 0.38f, Alpha, 0.0f, 0.0f, 1.0f);
            vertex(Consumer, Last, -Size, Size, 0.0f, 0.0f, 0.0f, 1.0f, 0.5f, 0.38f, Alpha, 0.0f, 0.0f, -1.0f);
            vertex(Consumer, Last, Size, Size, 0.0f, 1.0f, 0.0f, 1.0f, 0.5f, 0.38f, Alpha, 0.0f, 0.0f, -1.0f);
            vertex(Consumer, Last, Size, -Size, 0.0f, 1.0f, 1.0f, 1.0f, 0.5f, 0.38f, Alpha, 0.0f, 0.0f, -1.0f);
            vertex(Consumer, Last, -Size, -Size, 0.0f, 0.0f, 1.0f, 1.0f, 0.5f, 0.38f, Alpha, 0.0f, 0.0f, -1.0f);
            Pose.popPose();
        }
    }

    public static void bolt(MultiBufferSource Buffers, PoseStack Pose, long Seed, float Alpha) {
        if (Alpha <= 0.01f) {
            return;
        }
        float[] OffsetX = new float[BoltSections];
        float[] OffsetZ = new float[BoltSections];
        float SumX = 0.0f;
        float SumZ = 0.0f;
        RandomSource Random = RandomSource.create(Seed);
        for (int Index = BoltSections - 1; Index >= 0; Index--) {
            OffsetX[Index] = SumX;
            OffsetZ[Index] = SumZ;
            SumX += (Random.nextInt(11) - 5) * 0.25f;
            SumZ += (Random.nextInt(11) - 5) * 0.25f;
        }
        VertexConsumer Consumer = Buffers.getBuffer(RenderType.lightning());
        Matrix4f Matrix = Pose.last().pose();
        for (int Layer = 0; Layer < 4; Layer++) {
            RandomSource Branch = RandomSource.create(Seed);
            float Red = Layer == 0 ? 1.0f : 0.92f;
            float Green = Layer == 0 ? 0.62f : 0.14f;
            float Blue = Layer == 0 ? 0.48f : 0.1f;
            for (int Section = BoltSections - 1; Section >= 0; Section--) {
                float FromX = OffsetX[Section] - SumX;
                float FromZ = OffsetZ[Section] - SumZ;
                float ToX = FromX + (Branch.nextInt(11) - 5) * 0.25f;
                float ToZ = FromZ + (Branch.nextInt(11) - 5) * 0.25f;
                float Inner = (0.03f + Layer * 0.06f) * (Section * 0.1f + 1.0f);
                float Outer = (0.03f + Layer * 0.06f) * ((Section - 1.0f) * 0.1f + 1.0f);
                float A = Alpha * (Layer == 0 ? 0.9f : 0.32f);
                boltQuad(Matrix, Consumer, ToX, ToZ, Section, FromX, FromZ, Red, Green, Blue, A, Inner, Outer, false, false, true, false);
                boltQuad(Matrix, Consumer, ToX, ToZ, Section, FromX, FromZ, Red, Green, Blue, A, Inner, Outer, true, false, true, true);
                boltQuad(Matrix, Consumer, ToX, ToZ, Section, FromX, FromZ, Red, Green, Blue, A, Inner, Outer, true, true, false, true);
                boltQuad(Matrix, Consumer, ToX, ToZ, Section, FromX, FromZ, Red, Green, Blue, A, Inner, Outer, false, true, false, false);
            }
        }
    }

    private static void boltQuad(Matrix4f Matrix, VertexConsumer Consumer, float X1, float Z1, int Section, float X2, float Z2, float Red, float Green, float Blue, float Alpha,
                                 float Inner, float Outer, boolean LeftX, boolean LeftZ, boolean RightX, boolean RightZ) {
        Consumer.addVertex(Matrix, X1 + (LeftX ? Outer : -Outer), Section * BoltSection, Z1 + (LeftZ ? Outer : -Outer)).setColor(Red, Green, Blue, Alpha);
        Consumer.addVertex(Matrix, X2 + (LeftX ? Inner : -Inner), (Section + 1) * BoltSection, Z2 + (LeftZ ? Inner : -Inner)).setColor(Red, Green, Blue, Alpha);
        Consumer.addVertex(Matrix, X2 + (RightX ? Inner : -Inner), (Section + 1) * BoltSection, Z2 + (RightZ ? Inner : -Inner)).setColor(Red, Green, Blue, Alpha);
        Consumer.addVertex(Matrix, X1 + (RightX ? Outer : -Outer), Section * BoltSection, Z1 + (RightZ ? Outer : -Outer)).setColor(Red, Green, Blue, Alpha);
    }
}
