package net.rai.scp_unleashed.fission.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.rai.scp_unleashed.Scp_unleashed;

public final class FissionFx {
    public static final ResourceLocation Texture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/block/fission/fx.png");
    public static final float Third = 1.0f / 3.0f;
    public static final float[] Radial = {0.0f, 0.0f, Third, 0.5f};
    public static final float[] Band = {Third, 0.0f, 2.0f * Third, 0.5f};
    public static final float[] Ring = {2.0f * Third, 0.0f, 1.0f, 0.5f};
    public static final float[] Solid = {0.0f, 0.5f, Third, 1.0f};
    public static final float[] Plate = {Third, 0.5f, 2.0f * Third, 1.0f};
    public static final float[] Spark = {2.0f * Third, 0.5f, 1.0f, 1.0f};

    private FissionFx() {
    }

    public static void faceZ(VertexConsumer Consumer, PoseStack.Pose Last, float X0, float Y0, float X1, float Y1, float Z, float[] Uv, float[] Colour, float Strength) {
        float Red = Mth.clamp(Colour[0] * Strength, 0.0f, 1.0f);
        float Green = Mth.clamp(Colour[1] * Strength, 0.0f, 1.0f);
        float Blue = Mth.clamp(Colour[2] * Strength, 0.0f, 1.0f);
        if (Red + Green + Blue <= 0.0f) {
            return;
        }
        vertex(Consumer, Last, X0, Y0, Z, Uv[0], Uv[3], Red, Green, Blue, 0.0f, 0.0f, 1.0f);
        vertex(Consumer, Last, X1, Y0, Z, Uv[2], Uv[3], Red, Green, Blue, 0.0f, 0.0f, 1.0f);
        vertex(Consumer, Last, X1, Y1, Z, Uv[2], Uv[1], Red, Green, Blue, 0.0f, 0.0f, 1.0f);
        vertex(Consumer, Last, X0, Y1, Z, Uv[0], Uv[1], Red, Green, Blue, 0.0f, 0.0f, 1.0f);
    }

    public static void faceX(VertexConsumer Consumer, PoseStack.Pose Last, float X, float Y0, float Z0, float Y1, float Z1, float[] Uv, float[] Colour, float Strength) {
        float Red = Mth.clamp(Colour[0] * Strength, 0.0f, 1.0f);
        float Green = Mth.clamp(Colour[1] * Strength, 0.0f, 1.0f);
        float Blue = Mth.clamp(Colour[2] * Strength, 0.0f, 1.0f);
        if (Red + Green + Blue <= 0.0f) {
            return;
        }
        vertex(Consumer, Last, X, Y0, Z0, Uv[0], Uv[3], Red, Green, Blue, 1.0f, 0.0f, 0.0f);
        vertex(Consumer, Last, X, Y0, Z1, Uv[2], Uv[3], Red, Green, Blue, 1.0f, 0.0f, 0.0f);
        vertex(Consumer, Last, X, Y1, Z1, Uv[2], Uv[1], Red, Green, Blue, 1.0f, 0.0f, 0.0f);
        vertex(Consumer, Last, X, Y1, Z0, Uv[0], Uv[1], Red, Green, Blue, 1.0f, 0.0f, 0.0f);
    }

    public static void faceY(VertexConsumer Consumer, PoseStack.Pose Last, float X0, float Z0, float X1, float Z1, float Y, float[] Uv, float[] Colour, float Strength) {
        float Red = Mth.clamp(Colour[0] * Strength, 0.0f, 1.0f);
        float Green = Mth.clamp(Colour[1] * Strength, 0.0f, 1.0f);
        float Blue = Mth.clamp(Colour[2] * Strength, 0.0f, 1.0f);
        if (Red + Green + Blue <= 0.0f) {
            return;
        }
        vertex(Consumer, Last, X0, Y, Z0, Uv[0], Uv[3], Red, Green, Blue, 0.0f, 1.0f, 0.0f);
        vertex(Consumer, Last, X1, Y, Z0, Uv[2], Uv[3], Red, Green, Blue, 0.0f, 1.0f, 0.0f);
        vertex(Consumer, Last, X1, Y, Z1, Uv[2], Uv[1], Red, Green, Blue, 0.0f, 1.0f, 0.0f);
        vertex(Consumer, Last, X0, Y, Z1, Uv[0], Uv[1], Red, Green, Blue, 0.0f, 1.0f, 0.0f);
    }

    public static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, float X, float Y, float Z, float U, float V, float Red, float Green, float Blue, float NormalX, float NormalY, float NormalZ) {
        Consumer.addVertex(Last, X, Y, Z)
                .setColor(Red, Green, Blue, 1.0f)
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(Last, NormalX, NormalY, NormalZ);
    }
}
