package net.rai.scp_unleashed.scp330.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.rai.scp_unleashed.scp330.Scp330Registry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class Scp330Bubble {
    public static final ResourceLocation Texture = Scp330Registry.location("textures/entity/scp330/barrier.png");
    public static final int Across = 10;
    public static final int Tall = 18;
    public static final float PadWidth = 0.7f;
    public static final float PadHeight = 0.5f;
    public static final int Red = 150;
    public static final int Green = 208;
    public static final int Blue = 255;
    public static final float BaseAlpha = 0.08f;
    public static final float Shimmer = 0.22f;
    public static final float Twinkle = 0.3f;
    public static final float Speed = 0.025f;
    public static final float TileSize = 0.25f;
    private static final int[][] Steps = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};

    private record Face(float[] Corners, float NormalX, float NormalY, float NormalZ, float Height, float Seed, float TileU, float TileV) {
    }

    @Nullable
    private static List<Face> Faces;

    private Scp330Bubble() {
    }

    private static boolean inside(int X, int Y, int Z) {
        if (X < 0 || Y < 0 || Z < 0 || X >= Across || Y >= Tall || Z >= Across) {
            return false;
        }
        float Cx = (X + 0.5f) / Across * 2.0f - 1.0f;
        float Cy = (Y + 0.5f) / Tall * 2.0f - 1.0f;
        float Cz = (Z + 0.5f) / Across * 2.0f - 1.0f;
        return Cx * Cx + Cy * Cy + Cz * Cz <= 1.02f;
    }

    private static float hash(int X, int Y, int Z, int Side) {
        int Mixed = X * 73856093 ^ Y * 19349663 ^ Z * 83492791 ^ Side * 2654435;
        Mixed ^= Mixed >>> 13;
        Mixed *= 0x5bd1e995;
        Mixed ^= Mixed >>> 15;
        return (Mixed & 0xFFFF) / 65535.0f;
    }

    private static List<Face> faces() {
        if (Faces != null) {
            return Faces;
        }
        List<Face> Built = new ArrayList<>();
        for (int X = 0; X < Across; X++) {
            for (int Y = 0; Y < Tall; Y++) {
                for (int Z = 0; Z < Across; Z++) {
                    if (!inside(X, Y, Z)) {
                        continue;
                    }
                    for (int Side = 0; Side < Steps.length; Side++) {
                        int[] Step = Steps[Side];
                        if (inside(X + Step[0], Y + Step[1], Z + Step[2])) {
                            continue;
                        }
                        Built.add(face(X, Y, Z, Side, Step));
                    }
                }
            }
        }
        Faces = Built;
        return Built;
    }

    private static Face face(int X, int Y, int Z, int Side, int[] Step) {
        float X0 = (float) X / Across - 0.5f;
        float X1 = (float) (X + 1) / Across - 0.5f;
        float Y0 = (float) Y / Tall;
        float Y1 = (float) (Y + 1) / Tall;
        float Z0 = (float) Z / Across - 0.5f;
        float Z1 = (float) (Z + 1) / Across - 0.5f;
        float[] Corners = switch (Side) {
            case 0 -> new float[]{X1, Y0, Z0, X1, Y1, Z0, X1, Y1, Z1, X1, Y0, Z1};
            case 1 -> new float[]{X0, Y0, Z1, X0, Y1, Z1, X0, Y1, Z0, X0, Y0, Z0};
            case 2 -> new float[]{X0, Y1, Z0, X0, Y1, Z1, X1, Y1, Z1, X1, Y1, Z0};
            case 3 -> new float[]{X0, Y0, Z1, X0, Y0, Z0, X1, Y0, Z0, X1, Y0, Z1};
            case 4 -> new float[]{X1, Y0, Z1, X1, Y1, Z1, X0, Y1, Z1, X0, Y0, Z1};
            default -> new float[]{X0, Y0, Z0, X0, Y1, Z0, X1, Y1, Z0, X1, Y0, Z0};
        };
        float Seed = hash(X, Y, Z, Side);
        int Tile = (int) (hash(Z, X, Y, Side + 7) * 16.0f) & 15;
        return new Face(Corners, Step[0], Step[1], Step[2], (Y + 0.5f) / Tall, Seed, (Tile & 3) * TileSize, (Tile >> 2) * TileSize);
    }

    public static void render(PoseStack Pose, MultiBufferSource Buffers, LivingEntity Shown, float Partial) {
        float Width = Shown.getBbWidth() + PadWidth;
        float Height = Shown.getBbHeight() + PadHeight;
        float Time = Shown.tickCount + Partial;
        VertexConsumer Buffer = Buffers.getBuffer(RenderType.entityTranslucent(Texture));
        Pose.pushPose();
        Pose.translate(0.0f, -PadHeight * 0.5f, 0.0f);
        Pose.scale(Width, Height, Width);
        PoseStack.Pose Last = Pose.last();
        for (Face Each : faces()) {
            float Band = 0.5f + 0.5f * Mth.sin((Each.Height() - Time * Speed) * Mth.TWO_PI);
            float Spark = Mth.sin(Time * 0.15f + Each.Seed() * Mth.TWO_PI);
            float Alpha = BaseAlpha + Shimmer * Band * Band * Band * Band + Twinkle * Math.max(0.0f, Spark * Spark * Spark * Spark * Spark * Spark * Spark * Spark) * Each.Seed();
            int Opacity = Mth.clamp((int) (Alpha * 255.0f), 0, 255);
            float[] Corners = Each.Corners();
            float U0 = Each.TileU();
            float V0 = Each.TileV();
            float[] Us = {U0, U0, U0 + TileSize, U0 + TileSize};
            float[] Vs = {V0 + TileSize, V0, V0, V0 + TileSize};
            for (int Corner = 0; Corner < 4; Corner++) {
                Buffer.addVertex(Last, Corners[Corner * 3], Corners[Corner * 3 + 1], Corners[Corner * 3 + 2])
                        .setColor(Red, Green, Blue, Opacity)
                        .setUv(Us[Corner], Vs[Corner])
                        .setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(LightTexture.FULL_BRIGHT)
                        .setNormal(Last, Each.NormalX(), Each.NormalY(), Each.NormalZ());
            }
        }
        Pose.popPose();
    }
}
