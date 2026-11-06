package net.rai.scp_unleashed.scp179.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.rai.scp_unleashed.scp179.SatelliteTerminalBlockEntity;
import net.rai.scp_unleashed.scp179.Scp179Omen;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Scp179View implements SkyView {
    public static final ResourceLocation SunTexture = SkyScene.location("sun.png");
    public static final ResourceLocation HaloTexture = SkyScene.location("sun_halo.png");
    public static final float Fov = 50.0f;
    public static final float[] LightA = {0.2f, 1.0f, 1.0f};
    public static final float[] LightB = {-0.2f, 1.0f, 0.0f};
    public static final float[] CameraEye = {3.8f, 0.9f, -11.0f};
    public static final float[] CameraTarget = {-6.8f, 0.2f, 9.0f};
    public static final float[] FigurePosition = {0.0f, 0.0f, 0.0f};
    public static final float FigureYaw = 35.0f;
    public static final float FigureScale = 1.0f;
    public static final float[] SunCentre = {-35.0f, 7.0f, 44.5f};
    public static final float SunRadius = 9.5f;
    public static final float SunVoxel = 2.0f;
    public static final int SunFrames = 6;
    public static final int SunFrameTicks = 5;
    public static final int SunSeed = 1791;
    public static final float[] SunShades = {1.0f, 0.93f, 0.84f, 0.74f};
    public static final float[] SunRims = {-0.55f, -0.78f, -0.92f};
    public static final float HaloSize = 27.0f;
    public static final int HaloFrames = 4;
    public static final int HaloFrameTicks = 7;
    public static final int StepTicks = 8;
    public static final int WordY = 7;
    public static final int WordScale = 2;
    public static final int WordCycle = 32;
    public static final int WordShown = 24;
    public static final int WordPad = 4;
    public static final int WordBack = 0xC0100818;
    public static final float ShimmerLow = 0.72f;
    public static final float ShimmerSpeed = 0.07f;

    private static final VoxelSphere Sun = new VoxelSphere(SunRadius);
    private static final Scp179Figure Figure = new Scp179Figure();
    private static Scp179FigureRenderer Renderer;

    public static Vector3f eye(float Ticks) {
        return new Vector3f(CameraEye[0], CameraEye[1], CameraEye[2]);
    }

    public static Vector3f target() {
        return new Vector3f(CameraTarget[0], CameraTarget[1], CameraTarget[2]);
    }

    public static float shimmer(float Ticks) {
        return ShimmerLow + (1.0f - ShimmerLow) * (0.5f + 0.5f * (float) Math.sin(Ticks * ShimmerSpeed));
    }

    public static boolean wordShown(Scp179Omen Omen, float Elapsed) {
        return Omen.isActive() && Elapsed >= StepTicks * 2 && Math.floorMod((int) Math.floor(Elapsed), WordCycle) < WordShown;
    }

    public static float step(float Value, float[] Levels, float[] Bounds) {
        for (int Index = 0; Index < Bounds.length; Index++) {
            if (Value > Bounds[Index]) {
                return Levels[Index];
            }
        }
        return Levels[Levels.length - 1];
    }

    public static float[] sunFace(int X, int Y, int Z, int Side, Vector3f Eye, float Ticks) {
        int Frame = Math.floorMod(SkyScene.pick(SunSeed, X * 1031 + Y * 97 + Z, Side, SunFrames) + (int) Math.floor(Ticks / SunFrameTicks), SunFrames);
        Vector3f Toward = new Vector3f(Eye.x - SunCentre[0], Eye.y - SunCentre[1], Eye.z - SunCentre[2]).normalize();
        float Along = X * Toward.x + Y * Toward.y + Z * Toward.z;
        float Px = X - Toward.x * Along;
        float Py = Y - Toward.y * Along;
        float Pz = Z - Toward.z * Along;
        float Rim = (float) Math.sqrt(Px * Px + Py * Py + Pz * Pz) / SunRadius;
        float Shade = step(-Rim, SunShades, SunRims);
        return new float[]{0.0f, Frame / (float) SunFrames, 1.0f, (Frame + 1) / (float) SunFrames, Shade};
    }

    private static Scp179FigureRenderer renderer() {
        if (Renderer == null) {
            Renderer = new Scp179FigureRenderer();
        }
        return Renderer;
    }

    @Override
    public void render(GuiGraphics Graphics, int X, int Y, int Width, int Height, SatelliteTerminalBlockEntity Terminal, float Ticks) {
        Minecraft Game = Minecraft.getInstance();
        float Partial = Game.getTimer().getGameTimeDeltaPartialTick(false);
        long Now = Game.level == null ? 0L : Game.level.getGameTime();
        Scp179Omen Omen = Terminal.getOmen();
        float Elapsed = Terminal.getOmenSince() <= 0L ? Float.MAX_VALUE : Now - Terminal.getOmenSince() + Partial;
        Vector3f Eye = eye(Ticks);
        Matrix4f View = SkyScene.view(Eye, target());
        SkyScene.begin(Graphics, X, Y, Width, Height, Fov);
        SkyScene.sky(View);
        SkyScene.stars(View, Eye, FigurePosition, Ticks);
        BufferBuilder SunFaces = SkyScene.quads();
        Sun.emit(SunFaces, View, SunCentre[0], SunCentre[1], SunCentre[2], SunVoxel, (Fx, Fy, Fz, Side) -> sunFace(Fx, Fy, Fz, Side, Eye, Ticks));
        SkyScene.draw(SunFaces, SunTexture, false, true);
        BufferBuilder Halo = SkyScene.quads();
        int Frame = Math.floorMod((int) Math.floor(Ticks / HaloFrameTicks), HaloFrames);
        SkyScene.billboard(Halo, View, SunCentre[0], SunCentre[1], SunCentre[2], HaloSize, Frame / (float) HaloFrames, 0.0f, (Frame + 1) / (float) HaloFrames, 1.0f, 1.0f, 1.0f);
        SkyScene.draw(Halo, HaloTexture, true, false);
        figure(View, Omen, Ticks, Partial);
        SkyScene.end();
        if (wordShown(Omen, Elapsed)) {
            word(Graphics, Game.font, Omen, X, Y, Width);
        }
    }

    private static void figure(Matrix4f View, Scp179Omen Omen, float Ticks, float Partial) {
        Figure.update(Omen, Ticks, shimmer(Ticks));
        PoseStack Pose = new PoseStack();
        Pose.last().pose().set(View);
        Pose.last().normal().set(new Matrix3f(View));
        Pose.translate(FigurePosition[0], FigurePosition[1], FigurePosition[2]);
        Pose.mulPose(Axis.YP.rotationDegrees(FigureYaw));
        Pose.scale(FigureScale, FigureScale, FigureScale);
        RenderSystem.setShaderLights(new Vector3f(LightA[0], LightA[1], LightA[2]).normalize(), new Vector3f(LightB[0], LightB[1], LightB[2]).normalize());
        MultiBufferSource.BufferSource Buffers = Minecraft.getInstance().renderBuffers().bufferSource();
        renderer().render(Pose, Figure, Buffers, null, null, LightTexture.FULL_BRIGHT, Partial);
        Buffers.endBatch();
    }

    public static void word(GuiGraphics Graphics, Font Text, Scp179Omen Omen, int X, int Y, int Width) {
        Component Word = Omen.word();
        int Wide = Text.width(Word) * WordScale;
        int Left = X + (Width - Wide) / 2;
        int Top = Y + WordY;
        Graphics.fill(Left - WordPad, Top - WordPad, Left + Wide + WordPad - 1, Top + Text.lineHeight * WordScale + WordPad - 2, WordBack);
        Graphics.pose().pushPose();
        Graphics.pose().translate(Left, Top, 0.0f);
        Graphics.pose().scale(WordScale, WordScale, 1.0f);
        Graphics.drawString(Text, Word, 0, 0, Omen.getColour(), true);
        Graphics.pose().popPose();
    }
}
