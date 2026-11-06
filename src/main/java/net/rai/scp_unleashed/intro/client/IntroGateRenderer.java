package net.rai.scp_unleashed.intro.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.intro.IntroRegistry;
import net.rai.scp_unleashed.intro.IntroWorld;
import net.rai.scp_unleashed.intro.block.IntroGateCoreBlockEntity;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

public class IntroGateRenderer implements BlockEntityRenderer<IntroGateCoreBlockEntity> {
    public static final ResourceLocation FieldTexture = IntroRegistry.location("textures/block/intro/gate_field.png");
    public static final RenderType Field = RenderType.entityTranslucentEmissive(FieldTexture);
    public static final float[][] Layers = {
            {0.42f, 0.70f, 1.00f, 0.66f, 0.012f, 6.5f},
            {0.62f, 0.86f, 1.00f, 0.44f, -0.019f, 7.5f},
            {0.90f, 0.97f, 1.00f, 0.28f, 0.027f, 9.0f}
    };

    public IntroGateRenderer(BlockEntityRendererProvider.Context Context) {
    }

    @Override
    public void render(IntroGateCoreBlockEntity Gate, float PartialTick, PoseStack Pose, MultiBufferSource Buffers, int PackedLight, int PackedOverlay) {
        Level World = Gate.getLevel();
        if (World == null || ShaderCompat.renderingShadowPass()) {
            return;
        }
        AABB Local = IntroWorld.GateField;
        float X0 = (float) (Local.minX - IntroWorld.GateCore.getX());
        float X1 = (float) (Local.maxX - IntroWorld.GateCore.getX());
        float Y0 = (float) (Local.minY - IntroWorld.GateCore.getY());
        float Y1 = (float) (Local.maxY - IntroWorld.GateCore.getY());
        float Z = (float) ((Local.minZ + Local.maxZ) * 0.5 - IntroWorld.GateCore.getZ());
        float Clock = (World.getGameTime() % 24000L) + PartialTick;
        Pose.pushPose();
        VertexConsumer Consumer = Buffers.getBuffer(Field);
        PoseStack.Pose Last = Pose.last();
        float CentreX = (X0 + X1) * 0.5f;
        float CentreY = (Y0 + Y1) * 0.5f;
        for (int Index = 0; Index < Layers.length; Index++) {
            float[] Layer = Layers[Index];
            float Angle = Clock * Layer[4];
            float Scale = Layer[5];
            float Pulse = 0.85f + 0.15f * Mth.sin(Clock * 0.09f + Index * 2.1f);
            float Alpha = Layer[3] * Pulse;
            float Depth = Z + (Index - 1) * 0.06f;
            float Drift = Mth.sin(Clock * 0.013f + Index * 1.7f) * 0.02f;
            quad(Consumer, Last, X0, Y0, X1, Y1, Depth, CentreX, CentreY, Angle, Scale, Drift, Layer, Alpha);
        }
        Pose.popPose();
    }

    private static void quad(VertexConsumer Consumer, PoseStack.Pose Last, float X0, float Y0, float X1, float Y1, float Z, float CentreX, float CentreY, float Angle,
                             float Scale, float Drift, float[] Colour, float Alpha) {
        vertex(Consumer, Last, X0, Y0, Z, CentreX, CentreY, Angle, Scale, Drift, Colour, Alpha);
        vertex(Consumer, Last, X1, Y0, Z, CentreX, CentreY, Angle, Scale, Drift, Colour, Alpha);
        vertex(Consumer, Last, X1, Y1, Z, CentreX, CentreY, Angle, Scale, Drift, Colour, Alpha);
        vertex(Consumer, Last, X0, Y1, Z, CentreX, CentreY, Angle, Scale, Drift, Colour, Alpha);
    }

    private static void vertex(VertexConsumer Consumer, PoseStack.Pose Last, float X, float Y, float Z, float CentreX, float CentreY, float Angle, float Scale,
                               float Drift, float[] Colour, float Alpha) {
        float Dx = (X - CentreX) / Scale;
        float Dy = (Y - CentreY) / Scale;
        float Cos = Mth.cos(Angle);
        float Sin = Mth.sin(Angle);
        float U = 0.5f + Dx * Cos - Dy * Sin;
        float V = 0.5f + Dx * Sin + Dy * Cos + Drift;
        Consumer.addVertex(Last, X, Y, Z)
                .setColor(Colour[0], Colour[1], Colour[2], Alpha)
                .setUv(U, V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(Last, 0.0f, 0.0f, 1.0f);
    }

    @Override
    public boolean shouldRenderOffScreen(IntroGateCoreBlockEntity Gate) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    @Override
    public AABB getRenderBoundingBox(IntroGateCoreBlockEntity Gate) {
        return Gate.renderBounds();
    }
}
