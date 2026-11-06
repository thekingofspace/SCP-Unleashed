package net.rai.scp_unleashed.space.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.rai.scp_unleashed.space.SpaceRegistry;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class MoonSky {
    public static final ResourceLocation SunTexture = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    public static final ResourceLocation EarthTexture = SpaceRegistry.location("textures/environment/space_earth.png");
    public static final int StarCount = 3200;
    public static final long StarSeed = 7365871L;
    public static final float SkyDistance = 100.0F;
    public static final float SunSize = 26.0F;
    public static final float EarthSize = 21.0F;
    public static final float EarthYaw = -24.0F;
    public static final float EarthElevation = 42.0F;
    public static final float EarthTilt = 18.0F;
    @Nullable
    private static VertexBuffer Stars;

    private MoonSky() {
    }

    public static void render(ClientLevel World, float PartialTick, Matrix4f ModelView, Matrix4f Projection) {
        if (Stars == null) {
            Stars = new VertexBuffer(VertexBuffer.Usage.STATIC);
            Stars.bind();
            Stars.upload(buildStars());
            VertexBuffer.unbind();
        }
        FogRenderer.setupNoFog();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        PoseStack Pose = new PoseStack();
        Pose.mulPose(ModelView);
        Pose.pushPose();
        Pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        Pose.mulPose(Axis.XP.rotationDegrees(World.getTimeOfDay(PartialTick) * 360.0F));
        Stars.bind();
        Stars.drawWithShader(Pose.last().pose(), Projection, GameRenderer.getPositionColorShader());
        VertexBuffer.unbind();
        Matrix4f Celestial = Pose.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, SunTexture);
        quad(Celestial, SunSize, false);
        Pose.popPose();
        RenderSystem.defaultBlendFunc();
        Pose.pushPose();
        Pose.mulPose(Axis.YP.rotationDegrees(EarthYaw));
        Pose.mulPose(Axis.XP.rotationDegrees(90.0F - EarthElevation));
        Pose.mulPose(Axis.YP.rotationDegrees(EarthTilt));
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, EarthTexture);
        quad(Pose.last().pose(), EarthSize, true);
        Pose.popPose();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
    }

    private static void quad(Matrix4f Matrix, float Size, boolean Mirrored) {
        float Left = Mirrored ? 1.0F : 0.0F;
        float Right = Mirrored ? 0.0F : 1.0F;
        BufferBuilder Builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Builder.addVertex(Matrix, -Size, SkyDistance, -Size).setUv(Left, 0.0F);
        Builder.addVertex(Matrix, Size, SkyDistance, -Size).setUv(Right, 0.0F);
        Builder.addVertex(Matrix, Size, SkyDistance, Size).setUv(Right, 1.0F);
        Builder.addVertex(Matrix, -Size, SkyDistance, Size).setUv(Left, 1.0F);
        BufferUploader.drawWithShader(Builder.buildOrThrow());
    }

    private static MeshData buildStars() {
        RandomSource Random = RandomSource.create(StarSeed);
        BufferBuilder Builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int Placed = 0;
        while (Placed < StarCount) {
            float X = Random.nextFloat() * 2.0F - 1.0F;
            float Y = Random.nextFloat() * 2.0F - 1.0F;
            float Z = Random.nextFloat() * 2.0F - 1.0F;
            float Length = Mth.lengthSquared(X, Y, Z);
            if (Length <= 0.01F || Length >= 1.0F) {
                continue;
            }
            Placed++;
            float Roll = Random.nextFloat();
            float Size = Roll < 0.92F ? 0.09F + Random.nextFloat() * 0.1F : 0.2F + Random.nextFloat() * 0.14F;
            float Light = Roll < 0.92F ? 0.35F + Random.nextFloat() * 0.45F : 0.85F + Random.nextFloat() * 0.15F;
            float Hue = Random.nextFloat();
            float Red = Hue < 0.18F ? Light * 0.78F : Hue > 0.86F ? Light : Light * 0.94F;
            float Green = Hue < 0.18F ? Light * 0.86F : Hue > 0.86F ? Light * 0.88F : Light * 0.96F;
            float Blue = Hue < 0.18F ? Light : Hue > 0.86F ? Light * 0.7F : Light;
            Vector3f Centre = new Vector3f(X, Y, Z).normalize(SkyDistance);
            Quaternionf Turn = new Quaternionf().rotateTo(new Vector3f(0.0F, 0.0F, -1.0F), Centre).rotateZ(Random.nextFloat() * Mth.TWO_PI);
            Builder.addVertex(new Vector3f(Centre).add(new Vector3f(Size, -Size, 0.0F).rotate(Turn))).setColor(Red, Green, Blue, 1.0F);
            Builder.addVertex(new Vector3f(Centre).add(new Vector3f(Size, Size, 0.0F).rotate(Turn))).setColor(Red, Green, Blue, 1.0F);
            Builder.addVertex(new Vector3f(Centre).add(new Vector3f(-Size, Size, 0.0F).rotate(Turn))).setColor(Red, Green, Blue, 1.0F);
            Builder.addVertex(new Vector3f(Centre).add(new Vector3f(-Size, -Size, 0.0F).rotate(Turn))).setColor(Red, Green, Blue, 1.0F);
        }
        return Builder.buildOrThrow();
    }
}
