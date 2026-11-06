package net.rai.scp_unleashed.scp001day.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp001day.sun.Scp001DaySun;
import net.rai.scp_unleashed.scp001day.sun.SunVector;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Scp001DayEffects extends DimensionSpecialEffects {
    public static final ResourceLocation SunTexture = ResourceLocation.withDefaultNamespace("textures/environment/sun.png");
    public static final ResourceLocation MoonTexture = ResourceLocation.withDefaultNamespace("textures/environment/moon_phases.png");
    public static final float ShadeDim = 0.55f;
    public static final float SunSize = 34.0f;
    public static final float MoonSize = 20.0f;
    public static final Vector3f Bleach = new Vector3f(0.94f, 0.91f, 0.84f);

    public Scp001DayEffects() {
        super(Float.NaN, true, SkyType.NORMAL, false, false);
    }

    @Override
    public Vec3 getBrightnessDependentFogColor(Vec3 FogColour, float Brightness) {
        Vec3 Washed = FogColour.multiply(Brightness * 0.94 + 0.06, Brightness * 0.94 + 0.06, Brightness * 0.91 + 0.09);
        return Washed.lerp(new Vec3(Bleach.x, Bleach.y, Bleach.z), 0.35 * Brightness);
    }

    @Override
    public boolean isFoggyAt(int X, int Y) {
        return false;
    }

    @Override
    public void adjustLightmapColors(ClientLevel World, float PartialTicks, float SkyDarken, float BlockFlicker, float SkyLight, int BlockLevel, int SkyLevel, Vector3f Colours) {
        if (ShaderCompat.useFallback()) {
            return;
        }
        float Block = LightTexture.getBrightness(World.dimensionType(), BlockLevel) * BlockFlicker;
        float BlockGreen = Block * ((Block * 0.6f + 0.4f) * 0.6f + 0.4f);
        float BlockBlue = Block * (Block * Block * 0.6f + 0.4f);
        float Sky = LightTexture.getBrightness(World.dimensionType(), SkyLevel) * SkyLight * ShadeDim;
        float Red = Sky * 0.98f;
        float Green = Sky * 0.97f;
        float Blue = Sky;
        Colours.set(1.0f - (1.0f - Mth.clamp(Block, 0.0f, 1.0f)) * (1.0f - Red),
                1.0f - (1.0f - Mth.clamp(BlockGreen, 0.0f, 1.0f)) * (1.0f - Green),
                1.0f - (1.0f - Mth.clamp(BlockBlue, 0.0f, 1.0f)) * (1.0f - Blue));
    }

    @Override
    public boolean renderSky(ClientLevel World, int Ticks, float PartialTick, Matrix4f ModelView, Camera View, Matrix4f Projection, boolean Foggy, Runnable SetupFog) {
        if (ShaderCompat.useFallback()) {
            return false;
        }
        SetupFog.run();
        Vec3 Colour = World.getSkyColor(View.getPosition(), PartialTick);
        PoseStack Pose = new PoseStack();
        Pose.mulPose(ModelView);
        FogRenderer.levelFogColor();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionShader);
        RenderSystem.setShaderColor((float) Colour.x, (float) Colour.y, (float) Colour.z, 1.0f);
        BufferBuilder Disc = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION);
        Matrix4f Last = Pose.last().pose();
        Disc.addVertex(Last, 0.0f, 16.0f, 0.0f);
        for (int Step = 0; Step <= 16; Step++) {
            float Angle = Step * Mth.TWO_PI / 16.0f;
            Disc.addVertex(Last, 512.0f * Mth.cos(Angle), 16.0f, -512.0f * Mth.sin(Angle));
        }
        BufferUploader.drawWithShader(Disc.buildOrThrow());
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE,
                com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE, com.mojang.blaze3d.platform.GlStateManager.DestFactor.ZERO);
        SunVector Sun = Scp001DaySun.direction(World.getDayTime());
        float Rain = 1.0f - World.getRainLevel(PartialTick);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, Rain);
        body(Pose, Sun.X(), Sun.Y(), Sun.Z(), SunTexture, SunSize, 0.0f, 0.0f, 1.0f, 1.0f);
        int Phase = World.getMoonPhase();
        float U = (Phase % 4) / 4.0f;
        float V = (Phase / 4 % 2) / 2.0f;
        body(Pose, -Sun.X(), -Sun.Y(), -Sun.Z(), MoonTexture, MoonSize, U, V, U + 0.25f, V + 0.5f);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(true);
        return true;
    }

    private static void body(PoseStack Pose, double X, double Y, double Z, ResourceLocation Texture, float Size, float U0, float V0, float U1, float V1) {
        Pose.pushPose();
        Pose.mulPose(new Quaternionf().rotationTo(0.0f, 1.0f, 0.0f, (float) X, (float) Y, (float) Z));
        Matrix4f Last = Pose.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, Texture);
        BufferBuilder Quad = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Quad.addVertex(Last, -Size, 100.0f, -Size).setUv(U0, V0);
        Quad.addVertex(Last, Size, 100.0f, -Size).setUv(U1, V0);
        Quad.addVertex(Last, Size, 100.0f, Size).setUv(U1, V1);
        Quad.addVertex(Last, -Size, 100.0f, Size).setUv(U0, V1);
        BufferUploader.drawWithShader(Quad.buildOrThrow());
        Pose.popPose();
    }

    @Override
    public boolean renderClouds(ClientLevel World, int Ticks, float PartialTick, PoseStack Pose, double CameraX, double CameraY, double CameraZ, Matrix4f ModelView, Matrix4f Projection) {
        return true;
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel World, int Ticks, float PartialTick, LightTexture Light, double CameraX, double CameraY, double CameraZ) {
        return true;
    }

    @Override
    public boolean tickRain(ClientLevel World, int Ticks, Camera View) {
        return true;
    }
}
