package net.rai.scp_unleashed.scp902.client;

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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp902.Scp902Registry;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public final class Scp902Sky {
    public static final ResourceLocation SunTexture = Scp902Registry.location("textures/environment/scp902_sun.png");
    public static final ResourceLocation GlowTexture = Scp902Registry.location("textures/environment/scp902_sun_glow.png");
    public static final ResourceLocation ShaftTexture = Scp902Registry.location("textures/environment/scp902_sun_shafts.png");
    public static final float SkyDistance = 100.0f;
    public static final float SunYaw = 180.0f;
    public static final float SunElevation = 8.5f;
    public static final float SunRadius = 10.0f;
    public static final float GlowRadius = 28.0f;
    public static final float ShaftRadius = 40.0f;
    public static final float ShaftSpin = 0.0009f;
    public static final float ShaftAlpha = 0.12f;
    public static final float GlowAlpha = 0.42f;
    public static final float FlashWidth = 18.0f;
    public static final float FlashHeight = 7.0f;
    public static final float FlashElevation = 4.0f;
    public static final int Columns = 48;
    public static final int Rows = 24;
    public static final Vec3 Zenith = new Vec3(0.29, 0.30, 0.32);
    public static final Vec3 Overcast = new Vec3(0.44, 0.43, 0.42);
    public static final Vec3 Horizon = new Vec3(0.60, 0.56, 0.51);
    public static final Vec3 Ember = new Vec3(0.86, 0.62, 0.38);
    public static final Vec3 Ground = new Vec3(0.47, 0.44, 0.40);
    @Nullable
    private static VertexBuffer Dome;

    private Scp902Sky() {
    }

    public static Vector3f sunDirection() {
        float Yaw = SunYaw * Mth.DEG_TO_RAD;
        float Up = SunElevation * Mth.DEG_TO_RAD;
        return new Vector3f(-Mth.sin(Yaw) * Mth.cos(Up), Mth.sin(Up), Mth.cos(Yaw) * Mth.cos(Up));
    }

    public static Vec3 colour(Vector3f Direction) {
        Vector3f Sun = sunDirection();
        float Height = Direction.y;
        Vec3 Base;
        if (Height < 0.0f) {
            Base = Horizon.lerp(Ground, Mth.clamp(-Height * 6.0f, 0.0f, 1.0f));
        } else {
            float Lift = (float) Math.pow(Mth.clamp(Height, 0.0f, 1.0f), 0.55);
            Base = Lift < 0.35f ? Horizon.lerp(Overcast, Lift / 0.35f) : Overcast.lerp(Zenith, (Lift - 0.35f) / 0.65f);
        }
        float Facing = Math.max(0.0f, Direction.dot(Sun));
        float Warm = (float) (Math.pow(Facing, 5.0) * 0.55 + Math.pow(Facing, 24.0) * 0.35);
        float Band = Height < 0.0f ? 0.4f : 1.0f - Mth.clamp(Height * 2.2f, 0.0f, 0.7f);
        return Base.lerp(Ember, Mth.clamp(Warm * Band, 0.0f, 0.85f));
    }

    public static void render(ClientLevel World, float PartialTick, Matrix4f ModelView, Matrix4f Projection) {
        if (Dome == null) {
            Dome = new VertexBuffer(VertexBuffer.Usage.STATIC);
            Dome.bind();
            Dome.upload(buildDome());
            VertexBuffer.unbind();
        }
        FogRenderer.setupNoFog();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        PoseStack Pose = new PoseStack();
        Pose.mulPose(ModelView);
        Dome.bind();
        Dome.drawWithShader(Pose.last().pose(), Projection, GameRenderer.getPositionColorShader());
        VertexBuffer.unbind();
        Matrix4f Sky = Pose.last().pose();
        float Time = World.getGameTime() + PartialTick;
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, ShaftTexture);
        RenderSystem.setShaderColor(1.0f, 0.86f, 0.62f, ShaftAlpha * (0.85f + 0.15f * Mth.sin(Time * 0.013f)));
        billboard(Sky, SunYaw, SunElevation, ShaftRadius, ShaftRadius, Time * ShaftSpin);
        RenderSystem.setShaderTexture(0, GlowTexture);
        RenderSystem.setShaderColor(1.0f, 0.74f, 0.46f, GlowAlpha);
        billboard(Sky, SunYaw, SunElevation, GlowRadius, GlowRadius, 0.0f);
        float Flash = Scp902Ambience.flash(PartialTick);
        if (Flash > 0.01f) {
            RenderSystem.setShaderColor(1.0f, 0.62f, 0.32f, Flash * 0.55f);
            billboard(Sky, Scp902Ambience.flashBearing(), FlashElevation, FlashWidth, FlashHeight, 0.0f);
        }
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, SunTexture);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        billboard(Sky, SunYaw, SunElevation, SunRadius, SunRadius, 0.0f);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
    }

    private static void billboard(Matrix4f Matrix, float Yaw, float Elevation, float HalfWidth, float HalfHeight, float Spin) {
        float YawRad = Yaw * Mth.DEG_TO_RAD;
        float Up = Elevation * Mth.DEG_TO_RAD;
        Vector3f Forward = new Vector3f(-Mth.sin(YawRad) * Mth.cos(Up), Mth.sin(Up), Mth.cos(YawRad) * Mth.cos(Up));
        Vector3f Right = new Vector3f(-Mth.cos(YawRad), 0.0f, -Mth.sin(YawRad));
        Vector3f Top = new Vector3f(Right).cross(Forward).normalize();
        if (Top.y < 0.0f) {
            Top.negate();
        }
        if (Spin != 0.0f) {
            Vector3f Turned = new Vector3f(Right).mul(Mth.cos(Spin)).add(new Vector3f(Top).mul(Mth.sin(Spin)));
            Top = new Vector3f(Top).mul(Mth.cos(Spin)).sub(new Vector3f(Right).mul(Mth.sin(Spin)));
            Right = Turned;
        }
        float Width = SkyDistance * (float) Math.tan(HalfWidth * Mth.DEG_TO_RAD);
        float Height = SkyDistance * (float) Math.tan(HalfHeight * Mth.DEG_TO_RAD);
        Vector3f Centre = new Vector3f(Forward).mul(SkyDistance);
        Vector3f A = new Vector3f(Centre).sub(new Vector3f(Right).mul(Width)).add(new Vector3f(Top).mul(Height));
        Vector3f B = new Vector3f(Centre).add(new Vector3f(Right).mul(Width)).add(new Vector3f(Top).mul(Height));
        Vector3f C = new Vector3f(Centre).add(new Vector3f(Right).mul(Width)).sub(new Vector3f(Top).mul(Height));
        Vector3f D = new Vector3f(Centre).sub(new Vector3f(Right).mul(Width)).sub(new Vector3f(Top).mul(Height));
        BufferBuilder Builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Builder.addVertex(Matrix, A.x, A.y, A.z).setUv(0.0f, 0.0f);
        Builder.addVertex(Matrix, D.x, D.y, D.z).setUv(0.0f, 1.0f);
        Builder.addVertex(Matrix, C.x, C.y, C.z).setUv(1.0f, 1.0f);
        Builder.addVertex(Matrix, B.x, B.y, B.z).setUv(1.0f, 0.0f);
        BufferUploader.drawWithShader(Builder.buildOrThrow());
    }

    private static Vector3f point(int Column, int Row) {
        float Yaw = Column / (float) Columns * Mth.TWO_PI;
        float Pitch = (Row / (float) Rows - 0.5f) * Mth.PI;
        return new Vector3f(Mth.cos(Pitch) * Mth.cos(Yaw), Mth.sin(Pitch), Mth.cos(Pitch) * Mth.sin(Yaw));
    }

    private static MeshData buildDome() {
        BufferBuilder Builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int Row = 0; Row < Rows; Row++) {
            for (int Column = 0; Column < Columns; Column++) {
                vertex(Builder, point(Column, Row));
                vertex(Builder, point(Column, Row + 1));
                vertex(Builder, point(Column + 1, Row + 1));
                vertex(Builder, point(Column + 1, Row));
            }
        }
        return Builder.buildOrThrow();
    }

    private static void vertex(BufferBuilder Builder, Vector3f Direction) {
        Vec3 Shade = colour(Direction);
        Builder.addVertex(Direction.x * SkyDistance, Direction.y * SkyDistance, Direction.z * SkyDistance).setColor((float) Shade.x, (float) Shade.y, (float) Shade.z, 1.0f);
    }

    public static void reset() {
        if (Dome != null) {
            Dome.close();
            Dome = null;
        }
    }
}
