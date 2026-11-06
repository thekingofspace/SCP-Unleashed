package net.rai.scp_unleashed.scp179.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.Scp_unleashed;
import org.joml.Matrix4f;
import org.joml.Matrix4fStack;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

public final class SkyScene {
    public static final ResourceLocation SkyTexture = location("sky.png");
    public static final ResourceLocation StarTexture = location("star.png");
    public static final float NearPlane = 0.05f;
    public static final float FarPlane = 400.0f;
    public static final float SkyRadius = 120.0f;
    public static final int SkyFaces = 6;
    public static final int StarKinds = 4;
    public static final int StarFrames = 2;
    public static final int StarCount = 72;
    public static final int StarSeed = 1797;
    public static final float StarNear = 24.0f;
    public static final float StarFar = 70.0f;
    public static final float[] StarSizes = {0.1f, 0.13f, 0.16f};
    public static final float StarScale = 30.0f;
    public static final int TwinkleTicks = 11;
    public static final int TwinkleCycle = 11;
    public static final float[] Up = {0.0f, 1.0f, 0.0f};

    private SkyScene() {
    }

    public static ResourceLocation location(String Name) {
        return ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "textures/gui/satellite_terminal/" + Name);
    }

    public static int mix(int Value) {
        int Mixed = Value;
        Mixed ^= Mixed >>> 16;
        Mixed *= 0x7FEB352D;
        Mixed ^= Mixed >>> 15;
        Mixed *= 0x846CA68B;
        Mixed ^= Mixed >>> 16;
        return Mixed;
    }

    public static int pick(int Seed, int Index, int Salt, int Range) {
        return Integer.remainderUnsigned(mix(Seed * 31 + Index * 977 + Salt * 7919), Range);
    }

    public static float unit(int Seed, int Index, int Salt) {
        return pick(Seed, Index, Salt, 10000) / 10000.0f;
    }

    public static Matrix4f view(Vector3f Eye, Vector3f Target) {
        return new Matrix4f().lookAt(Eye, Target, new Vector3f(Up[0], Up[1], Up[2]));
    }

    public static void begin(GuiGraphics Graphics, int X, int Y, int Width, int Height, float Fov) {
        Graphics.flush();
        Window Screen = Minecraft.getInstance().getWindow();
        double Scale = Screen.getGuiScale();
        int Left = (int) Math.round(X * Scale);
        int Wide = (int) Math.round(Width * Scale);
        int Tall = (int) Math.round(Height * Scale);
        int Bottom = Screen.getHeight() - (int) Math.round(Y * Scale) - Tall;
        RenderSystem.viewport(Left, Bottom, Wide, Tall);
        RenderSystem.backupProjectionMatrix();
        RenderSystem.setProjectionMatrix(new Matrix4f().perspective((float) Math.toRadians(Fov), Wide / (float) Math.max(1, Tall), NearPlane, FarPlane),
                VertexSorting.DISTANCE_TO_ORIGIN);
        Matrix4fStack ModelView = RenderSystem.getModelViewStack();
        ModelView.pushMatrix();
        ModelView.identity();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.disableCull();
    }

    public static void end() {
        Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
        Window Screen = Minecraft.getInstance().getWindow();
        Matrix4fStack ModelView = RenderSystem.getModelViewStack();
        ModelView.popMatrix();
        RenderSystem.applyModelViewMatrix();
        RenderSystem.restoreProjectionMatrix();
        RenderSystem.viewport(0, 0, Screen.getWidth(), Screen.getHeight());
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        Lighting.setupFor3DItems();
    }

    public static BufferBuilder quads() {
        return Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
    }

    public static void draw(BufferBuilder Builder, ResourceLocation Texture, boolean Translucent, boolean WriteDepth) {
        MeshData Mesh = Builder.build();
        if (Mesh == null) {
            return;
        }
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, Texture);
        if (Translucent) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
        } else {
            RenderSystem.disableBlend();
        }
        RenderSystem.depthMask(WriteDepth);
        BufferUploader.drawWithShader(Mesh);
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    public static void vertex(BufferBuilder Builder, Matrix4f Pose, float X, float Y, float Z, float U, float V, float Shade, float Alpha) {
        Builder.addVertex(Pose, X, Y, Z).setUv(U, V).setColor(Shade, Shade, Shade, Alpha);
    }

    public static void sky(Matrix4f View) {
        Matrix4f Turn = new Matrix4f(View).setTranslation(0.0f, 0.0f, 0.0f);
        BufferBuilder Builder = quads();
        float R = SkyRadius;
        float[][][] Faces = {
                {{-R, R, -R}, {R, R, -R}, {R, -R, -R}, {-R, -R, -R}},
                {{R, R, R}, {-R, R, R}, {-R, -R, R}, {R, -R, R}},
                {{-R, R, R}, {-R, R, -R}, {-R, -R, -R}, {-R, -R, R}},
                {{R, R, -R}, {R, R, R}, {R, -R, R}, {R, -R, -R}},
                {{-R, R, R}, {R, R, R}, {R, R, -R}, {-R, R, -R}},
                {{-R, -R, -R}, {R, -R, -R}, {R, -R, R}, {-R, -R, R}}
        };
        for (int Face = 0; Face < SkyFaces; Face++) {
            float U0 = Face / (float) SkyFaces;
            float U1 = (Face + 1) / (float) SkyFaces;
            float[][] C = Faces[Face];
            vertex(Builder, Turn, C[0][0], C[0][1], C[0][2], U0, 0.0f, 1.0f, 1.0f);
            vertex(Builder, Turn, C[3][0], C[3][1], C[3][2], U0, 1.0f, 1.0f, 1.0f);
            vertex(Builder, Turn, C[2][0], C[2][1], C[2][2], U1, 1.0f, 1.0f, 1.0f);
            vertex(Builder, Turn, C[1][0], C[1][1], C[1][2], U1, 0.0f, 1.0f, 1.0f);
        }
        draw(Builder, SkyTexture, false, false);
    }

    public static void billboard(BufferBuilder Builder, Matrix4f View, float Cx, float Cy, float Cz, float Half, float U0, float V0, float U1, float V1, float Shade, float Alpha) {
        float Rx = View.m00();
        float Ry = View.m10();
        float Rz = View.m20();
        float Ux = View.m01();
        float Uy = View.m11();
        float Uz = View.m21();
        vertex(Builder, View, Cx - (Rx - Ux) * Half, Cy - (Ry - Uy) * Half, Cz - (Rz - Uz) * Half, U0, V0, Shade, Alpha);
        vertex(Builder, View, Cx - (Rx + Ux) * Half, Cy - (Ry + Uy) * Half, Cz - (Rz + Uz) * Half, U0, V1, Shade, Alpha);
        vertex(Builder, View, Cx + (Rx - Ux) * Half, Cy + (Ry - Uy) * Half, Cz + (Rz - Uz) * Half, U1, V1, Shade, Alpha);
        vertex(Builder, View, Cx + (Rx + Ux) * Half, Cy + (Ry + Uy) * Half, Cz + (Rz + Uz) * Half, U1, V0, Shade, Alpha);
    }

    public static float[] starAt(int Index, float[] Centre) {
        float Theta = unit(StarSeed, Index, 1) * (float) (Math.PI * 2.0);
        float Lift = unit(StarSeed, Index, 2) * 1.6f - 0.8f;
        float Reach = StarNear + unit(StarSeed, Index, 3) * (StarFar - StarNear);
        float Flat = (float) Math.sqrt(Math.max(0.0f, 1.0f - Lift * Lift));
        return new float[]{Centre[0] + (float) Math.cos(Theta) * Flat * Reach, Centre[1] + Lift * Reach, Centre[2] + (float) Math.sin(Theta) * Flat * Reach};
    }

    public static void stars(Matrix4f View, Vector3f Eye, float[] Centre, float Ticks) {
        BufferBuilder Builder = quads();
        int Twinkle = (int) Math.floor(Ticks / TwinkleTicks);
        float Span = 1.0f / (StarKinds * StarFrames);
        for (int Index = 0; Index < StarCount; Index++) {
            float[] At = starAt(Index, Centre);
            int Kind = pick(StarSeed, Index, 4, StarKinds);
            int Frame = Math.floorMod(Twinkle + pick(StarSeed, Index, 5, TwinkleCycle), TwinkleCycle) == 0 ? 1 : 0;
            float Reach = (float) Math.sqrt((At[0] - Eye.x) * (At[0] - Eye.x) + (At[1] - Eye.y) * (At[1] - Eye.y) + (At[2] - Eye.z) * (At[2] - Eye.z));
            float Size = StarSizes[pick(StarSeed, Index, 6, StarSizes.length)] * Reach / StarScale;
            float U0 = (Kind * StarFrames + Frame) * Span;
            billboard(Builder, View, At[0], At[1], At[2], Size, U0, 0.0f, U0 + Span, 1.0f, 1.0f, 1.0f);
        }
        draw(Builder, StarTexture, true, false);
    }
}
