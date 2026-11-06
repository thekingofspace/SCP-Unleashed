package net.rai.scp_unleashed.fission.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.client.FluidRendering;
import net.rai.scp_unleashed.fission.FissionRegistry;
import net.rai.scp_unleashed.fission.SonicVibratorBlock;
import net.rai.scp_unleashed.fission.SonicVibratorBlockEntity;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

public class SonicVibratorRenderer implements BlockEntityRenderer<SonicVibratorBlockEntity> {
    public static final ResourceLocation TendrilTexture = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/block/fission/vibrator_tendril.png");
    public static final float Pixel = 1.0f / 16.0f;
    public static final float FluidX0 = 3.3f;
    public static final float FluidX1 = 12.7f;
    public static final float FluidZ0 = 1.3f;
    public static final float FluidZ1 = 14.7f;
    public static final float FluidLow = 3.05f;
    public static final float FluidSpan = 7.8f;
    public static final float RingLimit = 4.4f;
    public static final float[][] Tendrils = {{5.5f, 5.5f}, {10.5f, 5.5f}, {5.5f, 10.5f}, {10.5f, 10.5f}};
    public static final float TendrilBase = 15.0f;
    public static final float TendrilHeight = 7.0f;
    public static final float TendrilHalf = 1.5f;
    public static final float TendrilSway = 1.4f;
    public static final float[] TendrilUv = {0.0f, 0.0f, 0.1875f, 0.4375f};
    public static final float[] SonicCyan = {0.3f, 0.9f, 1.0f};
    public static final float[] SludgeGlow = {0.45f, 0.75f, 0.2f};

    public SonicVibratorRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(SonicVibratorBlockEntity Machine, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Machine.getLevel();
        if (World == null) {
            return;
        }
        float Intensity = Machine.getRenderIntensity(PartialTick);
        float Phase = Machine.getRenderPhase(PartialTick);
        float Fill = Machine.getRenderLevel(PartialTick);
        float Surface = FluidLow + FluidSpan * Fill;
        Pose.pushPose();
        Pose.translate(0.5, 0.0, 0.5);
        Pose.mulPose(Axis.YP.rotationDegrees(-Machine.getBlockState().getValue(SonicVibratorBlock.Facing).toYRot() + 180.0f));
        Pose.translate(-0.5, 0.0, -0.5);
        if (Fill > 0.005f) {
            float Wobble = Intensity * 0.35f * Mth.sin(Phase * 2.3f) + Intensity * 0.15f * Mth.sin(Phase * 5.1f + 1.3f);
            Surface = Math.max(FluidLow + 0.2f, Surface + Wobble);
            FluidRendering.renderBox(Pose, Buffers, new FluidStack(FissionRegistry.WasteWater.get(), 1000), World, Machine.getBlockPos(),
                    FluidX0 * Pixel, FluidLow * Pixel, FluidZ0 * Pixel, FluidX1 * Pixel, Surface * Pixel, FluidZ1 * Pixel, PackedLight, true, false);
        }
        PoseStack.Pose Last = Pose.last();
        VertexConsumer Stalks = Buffers.getBuffer(RenderType.entityCutoutNoCull(TendrilTexture));
        for (int Index = 0; Index < Tendrils.length; Index++) {
            tendril(Stalks, Last, Tendrils[Index][0], Tendrils[Index][1], Index, Intensity, Phase, PackedLight);
        }
        if (Intensity <= 0.02f || ShaderCompat.renderingShadowPass()) {
            Pose.popPose();
            return;
        }
        VertexConsumer Glow = Buffers.getBuffer(FissionRenderTypes.haze(FissionFx.Texture));
        Pose.pushPose();
        Pose.scale(Pixel, Pixel, Pixel);
        PoseStack.Pose Scaled = Pose.last();
        if (Fill > 0.005f) {
            for (int Wave = 0; Wave < 2; Wave++) {
                float Age = (Phase * 0.09f + Wave * 0.5f) % 1.0f;
                float Radius = 1.0f + Age * (RingLimit - 1.0f);
                FissionFx.faceY(Glow, Scaled, 8.0f - Radius, 8.0f - Radius, 8.0f + Radius, 8.0f + Radius, Surface + 0.05f, FissionFx.Ring, SludgeGlow, 0.55f * Intensity * (1.0f - Age));
            }
        }
        for (int Index = 0; Index < Tendrils.length; Index++) {
            float[] Sway = sway(Index, Intensity, Phase);
            float TipX = Tendrils[Index][0] + Sway[0];
            float TipZ = Tendrils[Index][1] + Sway[1];
            float TipY = TendrilBase + TendrilHeight - 0.8f;
            float Size = 1.6f + 0.6f * Intensity;
            float Pulse = 0.6f + 0.4f * Mth.sin(Phase * 1.9f + Index * 1.1f);
            FissionFx.faceZ(Glow, Scaled, TipX - Size, TipY - Size, TipX + Size, TipY + Size, TipZ, FissionFx.Radial, SonicCyan, 0.9f * Intensity * Pulse);
            FissionFx.faceX(Glow, Scaled, TipX, TipY - Size, TipZ - Size, TipY + Size, TipZ + Size, FissionFx.Radial, SonicCyan, 0.9f * Intensity * Pulse);
        }
        Pose.popPose();
        Pose.popPose();
    }

    private static float[] sway(int Index, float Intensity, float Phase) {
        float Amount = 0.12f + TendrilSway * Intensity;
        return new float[]{Mth.sin(Phase * 1.7f + Index * 1.31f) * Amount, Mth.cos(Phase * 1.3f + Index * 2.17f) * Amount};
    }

    private static void tendril(VertexConsumer Consumer, PoseStack.Pose Last, float X, float Z, int Index, float Intensity, float Phase, int Light) {
        float[] Sway = sway(Index, Intensity, Phase);
        float Bottom = TendrilBase * Pixel;
        float Top = (TendrilBase + TendrilHeight) * Pixel;
        float BaseX = X * Pixel;
        float BaseZ = Z * Pixel;
        float TipX = (X + Sway[0]) * Pixel;
        float TipZ = (Z + Sway[1]) * Pixel;
        float Half = TendrilHalf * Pixel;
        stalk(Consumer, Last, BaseX - Half, BaseZ, BaseX + Half, BaseZ, TipX - Half, TipZ, TipX + Half, TipZ, Bottom, Top, Light, 0.0f, 0.0f, 1.0f);
        stalk(Consumer, Last, BaseX, BaseZ - Half, BaseX, BaseZ + Half, TipX, TipZ - Half, TipX, TipZ + Half, Bottom, Top, Light, 1.0f, 0.0f, 0.0f);
    }

    private static void stalk(VertexConsumer Consumer, PoseStack.Pose Last, float LowX0, float LowZ0, float LowX1, float LowZ1, float HighX0, float HighZ0, float HighX1, float HighZ1,
                              float Bottom, float Top, int Light, float NormalX, float NormalY, float NormalZ) {
        point(Consumer, Last, LowX0, Bottom, LowZ0, TendrilUv[0], TendrilUv[3], Light, NormalX, NormalY, NormalZ);
        point(Consumer, Last, LowX1, Bottom, LowZ1, TendrilUv[2], TendrilUv[3], Light, NormalX, NormalY, NormalZ);
        point(Consumer, Last, HighX1, Top, HighZ1, TendrilUv[2], TendrilUv[1], Light, NormalX, NormalY, NormalZ);
        point(Consumer, Last, HighX0, Top, HighZ0, TendrilUv[0], TendrilUv[1], Light, NormalX, NormalY, NormalZ);
    }

    private static void point(VertexConsumer Consumer, PoseStack.Pose Last, float X, float Y, float Z, float U, float V, int Light, float NormalX, float NormalY, float NormalZ) {
        Consumer.addVertex(Last, X, Y, Z)
                .setColor(255, 255, 255, 255)
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(Light)
                .setNormal(Last, NormalX, NormalY, NormalZ);
    }
}
