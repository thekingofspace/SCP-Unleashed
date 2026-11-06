package net.rai.scp_unleashed.scp244.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.ParticleStatus;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp244.Scp244VaseBlockEntity;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class Scp244FogRenderer {
    public static final ResourceLocation PuffTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/scp244/fog_puff.png");
    public static final ResourceLocation SheetTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/entity/scp244/fog_sheet.png");
    public static final int Period = 72000;
    public static final float PuffSize = 1.7f;
    public static final float SizeJitter = 0.25f;
    public static final float PlaceJitter = 0.2f;
    public static final float PuffAlpha = 0.36f;
    public static final float MaxAlpha = 0.6f;
    public static final float NearFadeStart = 1.2f;
    public static final float NearFadeEnd = 3.2f;
    public static final float Drift = 0.12f;
    public static final float Bob = 0.07f;
    public static final float NearLod = 12.0f;
    public static final float FarLod = 24.0f;
    public static final float MidScale = 1.26f;
    public static final float MidAlpha = 1.27f;
    public static final float FarScale = 2.0f;
    public static final float FarAlpha = 2.0f;
    public static final float Red = 0.86f;
    public static final float Green = 0.89f;
    public static final float Blue = 0.93f;
    public static final float[] SheetHeights = {0.06f, 0.3f};
    public static final float[] SheetAlpha = {0.55f, 0.35f};
    public static final float[] SheetTiles = {4.0f, 6.0f};
    public static final float[] ScrollU = {0.003f, -0.0025f};
    public static final float[] ScrollV = {0.0015f, 0.003f};
    private static final int[][] CornerSteps = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
    private static final float[][] Offsets = {{-1.0f, -1.0f}, {-1.0f, 1.0f}, {1.0f, 1.0f}, {1.0f, -1.0f}};

    private Scp244FogRenderer() {
    }

    public static void render(Scp244VaseBlockEntity Vase, float Partial, PoseStack Pose, MultiBufferSource Buffers) {
        Scp244FogEffect Fog = Scp244FogEffect.peek(Vase);
        Level World = Vase.getLevel();
        if (Fog == null || World == null || !Fog.showing() || ShaderCompat.renderingShadowPass()) {
            return;
        }
        float Time = (World.getGameTime() % Period) + Partial;
        ParticleStatus Status = Minecraft.getInstance().options.particles().get();
        sheets(Fog, Vase.getBlockPos(), Partial, Time, Pose, Buffers, Status == ParticleStatus.MINIMAL ? 1 : 2);
        puffs(Fog, Vase.getBlockPos(), Partial, Time, Pose, Buffers, Status);
    }

    private static void sheets(Scp244FogEffect Fog, BlockPos Origin, float Partial, float Time, PoseStack Pose, MultiBufferSource Buffers, int Layers) {
        int[] Floors = Fog.floors();
        if (Floors.length == 0) {
            return;
        }
        VertexConsumer Buffer = Buffers.getBuffer(RenderType.entityTranslucent(SheetTexture));
        PoseStack.Pose Last = Pose.last();
        float[] Shown = new float[4];
        for (int Entry = 0; Entry < Floors.length; Entry++) {
            int Slot = Floors[Entry];
            float Peak = 0.0f;
            for (int Corner = 0; Corner < 4; Corner++) {
                Shown[Corner] = Fog.corner(Entry, Corner, Partial);
                Peak = Math.max(Peak, Shown[Corner]);
            }
            if (Peak < Scp244FogEffect.Visible) {
                continue;
            }
            long Key = Fog.key(Slot);
            int X = BlockPos.getX(Key);
            int Y = BlockPos.getY(Key);
            int Z = BlockPos.getZ(Key);
            float Rx = X - Origin.getX();
            float Ry = Y - Origin.getY();
            float Rz = Z - Origin.getZ();
            int PackedLight = Fog.light(Slot);
            for (int Layer = 0; Layer < Layers; Layer++) {
                for (int Corner = 0; Corner < 4; Corner++) {
                    int Dx = CornerSteps[Corner][0];
                    int Dz = CornerSteps[Corner][1];
                    float U = (X + Dx) / SheetTiles[Layer] + Time * ScrollU[Layer];
                    float V = (Z + Dz) / SheetTiles[Layer] + Time * ScrollV[Layer];
                    float Alpha = Math.min(MaxAlpha, Shown[Corner] * SheetAlpha[Layer]);
                    vertex(Buffer, Last, Rx + Dx, Ry + SheetHeights[Layer], Rz + Dz, U, V, 0.94f + Layer * 0.06f, Alpha, PackedLight);
                }
            }
        }
    }

    private static void puffs(Scp244FogEffect Fog, BlockPos Origin, float Partial, float Time, PoseStack Pose, MultiBufferSource Buffers, ParticleStatus Status) {
        Camera View = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vec3 Eye = View.getPosition();
        Quaternionf Turn = View.rotation();
        Vector3f[] Spokes = new Vector3f[4];
        for (int Corner = 0; Corner < 4; Corner++) {
            Spokes[Corner] = new Vector3f(Offsets[Corner][0], Offsets[Corner][1], 0.0f).rotate(Turn);
        }
        float Near = Status == ParticleStatus.ALL ? NearLod : Status == ParticleStatus.DECREASED ? NearLod * 0.5f : 0.0f;
        float Far = Status == ParticleStatus.MINIMAL ? NearLod : FarLod;
        VertexConsumer Buffer = Buffers.getBuffer(RenderType.entityTranslucent(PuffTexture));
        PoseStack.Pose Last = Pose.last();
        long OriginKey = Origin.asLong();
        for (int Slot = 0; Slot < Fog.count(); Slot++) {
            long Key = Fog.key(Slot);
            if (Key == OriginKey) {
                continue;
            }
            float Density = Fog.alpha(Slot, Partial);
            if (Density < Scp244FogEffect.Visible) {
                continue;
            }
            int X = BlockPos.getX(Key);
            int Y = BlockPos.getY(Key);
            int Z = BlockPos.getZ(Key);
            double Dx = X + 0.5 - Eye.x;
            double Dy = Y + 0.5 - Eye.y;
            double Dz = Z + 0.5 - Eye.z;
            float Away = (float) Math.sqrt(Dx * Dx + Dy * Dy + Dz * Dz);
            float Scale = 1.0f;
            float Boost = 1.0f;
            if (Away > Far) {
                if (((X | Y | Z) & 1) != 0) {
                    continue;
                }
                Scale = FarScale;
                Boost = FarAlpha;
            } else if (Away > Near) {
                if (((X ^ Y ^ Z) & 1) != 0) {
                    continue;
                }
                Scale = MidScale;
                Boost = MidAlpha;
            }
            float Fade = smooth((Away - NearFadeStart) / (NearFadeEnd - NearFadeStart));
            float Alpha = Math.min(MaxAlpha, Density * PuffAlpha * Boost * Fade);
            if (Alpha < 0.004f) {
                continue;
            }
            int Seed = hash(X, Y, Z);
            float First = ((Seed & 0xFF) / 255.0f);
            float Second = (((Seed >> 8) & 0xFF) / 255.0f);
            float Third = (((Seed >> 16) & 0xFF) / 255.0f);
            float Half = (PuffSize + (First - 0.5f) * 2.0f * SizeJitter) * Scale * 0.5f;
            float Cx = X - Origin.getX() + 0.5f + (Second - 0.5f) * 2.0f * PlaceJitter + Drift * Mth.sin(Time * 0.021f + First * Mth.TWO_PI);
            float Cy = Y - Origin.getY() + 0.5f + (Third - 0.5f) * 2.0f * PlaceJitter + Bob * Mth.sin(Time * 0.013f + Third * Mth.TWO_PI);
            float Cz = Z - Origin.getZ() + 0.5f + (First - 0.5f) * 2.0f * PlaceJitter + Drift * Mth.cos(Time * 0.017f + Second * Mth.TWO_PI);
            int Variant = (Seed >>> 24) & 3;
            float U0 = (Variant & 1) * 0.5f;
            float V0 = (Variant >> 1) * 0.5f;
            boolean Flip = ((Seed >>> 26) & 1) != 0;
            float Shade = 0.92f + Third * 0.08f;
            int PackedLight = Fog.light(Slot);
            for (int Corner = 0; Corner < 4; Corner++) {
                Vector3f Spoke = Spokes[Corner];
                float U = U0 + ((Corner >= 2) != Flip ? 0.5f : 0.0f);
                float V = V0 + (Corner == 1 || Corner == 2 ? 0.0f : 0.5f);
                vertex(Buffer, Last, Cx + Spoke.x * Half, Cy + Spoke.y * Half, Cz + Spoke.z * Half, U, V, Shade, Alpha, PackedLight);
            }
        }
    }

    private static int hash(int X, int Y, int Z) {
        int Value = X * 73856093 ^ Y * 19349663 ^ Z * 83492791;
        Value ^= Value >>> 16;
        Value *= 0x45D9F3B;
        Value ^= Value >>> 16;
        Value *= 0x45D9F3B;
        Value ^= Value >>> 16;
        return Value;
    }

    private static float smooth(float Value) {
        float Clamped = Mth.clamp(Value, 0.0f, 1.0f);
        return Clamped * Clamped * (3.0f - 2.0f * Clamped);
    }

    private static void vertex(VertexConsumer Buffer, PoseStack.Pose Last, float X, float Y, float Z, float U, float V, float Shade, float Alpha, int PackedLight) {
        Buffer.addVertex(Last, X, Y, Z)
                .setColor(Red * Shade, Green * Shade, Blue * Shade, Mth.clamp(Alpha, 0.0f, 1.0f))
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(PackedLight)
                .setNormal(Last, 0.0f, 1.0f, 0.0f);
    }
}
