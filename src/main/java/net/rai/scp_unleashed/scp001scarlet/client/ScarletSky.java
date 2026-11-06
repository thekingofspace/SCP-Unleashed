package net.rai.scp_unleashed.scp001scarlet.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.scp001scarlet.ScarletClientConfig;
import org.joml.Matrix4f;

public final class ScarletSky {
    public static final float[] Sky = {0.46f, 0.04f, 0.06f};
    public static final float[] Fog = {0.42f, 0.07f, 0.07f};
    public static final float[] Cloud = {0.36f, 0.05f, 0.06f};
    public static final float ElsewhereShare = 0.4f;
    public static final int Streaks = 7;
    public static final float StreakDistance = 90.0f;

    private ScarletSky() {
    }

    public static float strength(ClientLevel World, float Partial) {
        if (!ScarletClientConfig.sky()) {
            return 0.0f;
        }
        float Raw = ScarletClient.sky(Partial);
        if (Raw <= 0.0001f) {
            return 0.0f;
        }
        return World.dimension() == Level.OVERWORLD ? Raw : Raw * ElsewhereShare;
    }

    private static Vec3 blend(float[] Key, Vec3 Vanilla, float Progress, float Mix) {
        if (Progress <= 0.0001f) {
            return Vanilla;
        }
        double Light = Mth.clamp(Math.max(Vanilla.x, Math.max(Vanilla.y, Vanilla.z)) * 1.4, 0.18, 1.0);
        double Share = Mix * Progress;
        return new Vec3(Mth.lerp(Share, Vanilla.x, Key[0] * Light), Mth.lerp(Share, Vanilla.y, Key[1] * Light), Mth.lerp(Share, Vanilla.z, Key[2] * Light));
    }

    public static Vec3 sky(Vec3 Vanilla, ClientLevel World, float Partial) {
        return blend(Sky, Vanilla, strength(World, Partial), 0.92f);
    }

    public static Vec3 cloud(Vec3 Vanilla, ClientLevel World, float Partial) {
        return blend(Cloud, Vanilla, strength(World, Partial), 0.85f);
    }

    public static float darken(float Vanilla, ClientLevel World, float Partial) {
        return Vanilla * (1.0f - 0.3f * strength(World, Partial));
    }

    public static void fog(ViewportEvent.ComputeFogColor Event) {
        Minecraft Client = Minecraft.getInstance();
        ClientLevel World = Client.level;
        if (World == null || Event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        float Progress = strength(World, (float) Event.getPartialTick());
        if (Progress <= 0.0001f) {
            return;
        }
        Vec3 Made = blend(Fog, new Vec3(Event.getRed(), Event.getGreen(), Event.getBlue()), Progress, 0.85f);
        Event.setRed((float) Made.x);
        Event.setGreen((float) Made.y);
        Event.setBlue((float) Made.z);
    }

    public static void streaks(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) {
            return;
        }
        Minecraft Client = Minecraft.getInstance();
        ClientLevel World = Client.level;
        if (World == null || World.dimension() != Level.OVERWORLD) {
            return;
        }
        float Partial = Event.getPartialTick().getGameTimeDeltaPartialTick(false);
        float Progress = strength(World, Partial);
        if (Progress <= 0.05f) {
            return;
        }
        Matrix4f Pose = new Matrix4f(Event.getModelViewMatrix());
        float Time = (ScarletClient.clock() + Partial) * 0.0006f;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder Builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        for (int Index = 0; Index < Streaks; Index++) {
            float Angle = Index * Mth.TWO_PI / Streaks + Time * (1.0f + Index * 0.13f);
            float Lift = 18.0f + 26.0f * ((Index * 37) % 11) / 11.0f;
            float Length = 70.0f + 20.0f * (Index % 3);
            float Thick = 2.0f + (Index % 2) * 2.5f;
            float Cos = Mth.cos(Angle);
            float Sin = Mth.sin(Angle);
            float Cx = Cos * StreakDistance;
            float Cz = Sin * StreakDistance;
            float Tx = -Sin * Length * 0.5f;
            float Tz = Cos * Length * 0.5f;
            int Alpha = (int) (Progress * 150.0f);
            Builder.addVertex(Pose, Cx - Tx, Lift - Thick, Cz - Tz).setColor(12, 2, 4, 0);
            Builder.addVertex(Pose, Cx - Tx, Lift + Thick, Cz - Tz).setColor(12, 2, 4, 0);
            Builder.addVertex(Pose, Cx, Lift + Thick * 1.4f, Cz).setColor(10, 1, 3, Alpha);
            Builder.addVertex(Pose, Cx, Lift - Thick * 0.6f, Cz).setColor(10, 1, 3, Alpha);
            Builder.addVertex(Pose, Cx, Lift - Thick * 0.6f, Cz).setColor(10, 1, 3, Alpha);
            Builder.addVertex(Pose, Cx, Lift + Thick * 1.4f, Cz).setColor(10, 1, 3, Alpha);
            Builder.addVertex(Pose, Cx + Tx, Lift + Thick, Cz + Tz).setColor(12, 2, 4, 0);
            Builder.addVertex(Pose, Cx + Tx, Lift - Thick, Cz + Tz).setColor(12, 2, 4, 0);
        }
        BufferUploader.drawWithShader(Builder.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }
}
