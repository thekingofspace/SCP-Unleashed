package net.rai.scp_unleashed.daevitegod.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.daevitegod.DaeviteGod;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;
import org.joml.Matrix4f;

public final class DaeviteGodSky {
    public static final double NearRange = 32.0;
    public static final double FarRange = 96.0;
    public static final float Approach = 0.04f;
    public static final float FogRed = 0.11f;
    public static final float FogGreen = 0.015f;
    public static final float FogBlue = 0.025f;
    public static final float DomeRed = 0.09f;
    public static final float DomeGreen = 0.0f;
    public static final float DomeBlue = 0.015f;
    public static final float DomeSize = 40.0f;
    public static final float DomeShare = 0.78f;
    public static final float FogShare = 0.7f;
    private static float Darkness;
    private static float DarknessO;
    private static float Wanted;

    private DaeviteGodSky() {
    }

    public static void tick(Minecraft Client) {
        DarknessO = Darkness;
        Player Viewer = Client.player;
        if (Client.level == null || Viewer == null) {
            Darkness = 0.0f;
            DarknessO = 0.0f;
            return;
        }
        if (Client.level.getGameTime() % 5 == 0) {
            Wanted = 0.0f;
            AABB Reach = Viewer.getBoundingBox().inflate(FarRange);
            for (DaeviteGod God : Client.level.getEntitiesOfClass(DaeviteGod.class, Reach, Found -> true)) {
                double Distance = Viewer.distanceTo(God);
                float Falloff = (float) Mth.clamp(1.0 - (Distance - NearRange) / (FarRange - NearRange), 0.0, 1.0);
                Wanted = Math.max(Wanted, God.skyDarkness(0.0f) * Falloff);
            }
        }
        Darkness += Mth.clamp(Wanted - Darkness, -Approach, Approach);
    }

    public static float darkness(float Partial) {
        return Mth.lerp(Partial, DarknessO, Darkness);
    }

    public static void fog(ViewportEvent.ComputeFogColor Event) {
        float Level = darkness((float) Event.getPartialTick()) * FogShare;
        if (Level <= 0.001f) {
            return;
        }
        Event.setRed(Mth.lerp(Level, Event.getRed(), FogRed));
        Event.setGreen(Mth.lerp(Level, Event.getGreen(), FogGreen));
        Event.setBlue(Mth.lerp(Level, Event.getBlue(), FogBlue));
    }

    public static void dome(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY || ShaderCompat.shaderPackInUse()) {
            return;
        }
        float Level = darkness(Event.getPartialTick().getGameTimeDeltaPartialTick(false)) * DomeShare;
        if (Level <= 0.002f) {
            return;
        }
        PoseStack Pose = new PoseStack();
        Pose.mulPose(Event.getModelViewMatrix());
        Matrix4f Matrix = Pose.last().pose();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);
        RenderSystem.disableDepthTest();
        RenderSystem.disableCull();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder Builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        float S = DomeSize;
        float[][] Faces = {
                {-S, S, -S, S, S, -S, S, S, S, -S, S, S},
                {-S, -S, -S, -S, -S, S, S, -S, S, S, -S, -S},
                {-S, -S, -S, S, -S, -S, S, S, -S, -S, S, -S},
                {-S, -S, S, -S, S, S, S, S, S, S, -S, S},
                {-S, -S, -S, -S, S, -S, -S, S, S, -S, -S, S},
                {S, -S, -S, S, -S, S, S, S, S, S, S, -S},
        };
        for (int Index = 0; Index < Faces.length; Index++) {
            float[] Face = Faces[Index];
            float Shade = Index == 1 ? 0.4f : 1.0f;
            for (int Corner = 0; Corner < 4; Corner++) {
                float Y = Face[Corner * 3 + 1];
                float Alpha = Level * (Y > 0.0f ? 1.0f : 0.55f) * Shade;
                Builder.addVertex(Matrix, Face[Corner * 3], Y, Face[Corner * 3 + 2]).setColor(DomeRed, DomeGreen, DomeBlue, Alpha);
            }
        }
        BufferUploader.drawWithShader(Builder.buildOrThrow());
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }
}
