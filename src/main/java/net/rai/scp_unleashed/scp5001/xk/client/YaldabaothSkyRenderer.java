package net.rai.scp_unleashed.scp5001.xk.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class YaldabaothSkyRenderer {
    public static final float FarShare = 0.85f;
    public static final double CullRadius = 12.0;
    private static final Map<YaldabaothPart.Kind, YaldabaothPartRenderer> Renderers = new EnumMap<>(YaldabaothPart.Kind.class);

    private YaldabaothSkyRenderer() {
    }

    private static YaldabaothPartRenderer renderer(YaldabaothPart.Kind Kind) {
        return Renderers.computeIfAbsent(Kind, YaldabaothPartRenderer::new);
    }

    public static void stage(RenderLevelStageEvent Event) {
        boolean Shaders = ShaderCompat.shaderPackInUse();
        RenderLevelStageEvent.Stage Wanted = Shaders ? RenderLevelStageEvent.Stage.AFTER_ENTITIES : RenderLevelStageEvent.Stage.AFTER_SKY;
        if (Event.getStage() != Wanted || !YaldabaothClient.present()) {
            return;
        }
        Minecraft Client = Minecraft.getInstance();
        ClientLevel World = Client.level;
        if (World == null || World.dimension() != Level.OVERWORLD) {
            return;
        }
        float Partial = Event.getPartialTick().getGameTimeDeltaPartialTick(false);
        List<YaldabaothClient.Placement> Parts = YaldabaothClient.placements(Partial);
        if (Parts.isEmpty()) {
            return;
        }
        Camera View = Event.getCamera();
        Vec3 Eye = View.getPosition();
        Frustum Cull = Event.getFrustum();
        float Far = Client.gameRenderer.getDepthFar() * FarShare;
        PoseStack Stack = new PoseStack();
        Stack.mulPose(Event.getModelViewMatrix());
        MultiBufferSource.BufferSource Buffers = Client.renderBuffers().bufferSource();
        float FogStart = RenderSystem.getShaderFogStart();
        float FogEnd = RenderSystem.getShaderFogEnd();
        FogShape Shape = RenderSystem.getShaderFogShape();
        float Render = Client.gameRenderer.getRenderDistance();
        RenderSystem.setShaderFogStart(Math.max(96.0f, Render * 1.2f));
        RenderSystem.setShaderFogEnd(Math.max(400.0f, Far * 1.6f));
        RenderSystem.setShaderFogShape(FogShape.SPHERE);
        Lighting.setupLevel();
        int Light = LightTexture.pack(3, 15);
        long Clock = YaldabaothClient.clock();
        float Jaw = YaldabaothClient.jaw(Partial);
        boolean Shut = YaldabaothClient.shut();
        for (YaldabaothClient.Placement Part : Parts) {
            if (Part.Scale() <= 0.001f) {
                continue;
            }
            Vec3 Offset = Part.Position().subtract(Eye);
            double Distance = Offset.length();
            double Pull = Distance > Far ? Far / Distance : 1.0;
            if (Pull >= 1.0 && Cull != null && !Cull.isVisible(new AABB(Part.Position(), Part.Position()).inflate(CullRadius * Part.Scale() / 4.0))) {
                continue;
            }
            Vec3 Placed = Offset.scale(Pull);
            Stack.pushPose();
            Stack.translate(Placed.x, Placed.y, Placed.z);
            Stack.mulPose(Axis.YP.rotation(Part.Yaw()));
            Stack.mulPose(Axis.XP.rotation(Part.Pitch()));
            Stack.mulPose(Axis.ZP.rotation(Part.Roll()));
            float Size = (float) (Part.Scale() * Pull);
            Stack.scale(Size, Size, Size * Part.Stretch());
            Vec3 Local = Eye.subtract(Part.Position());
            double LookYaw = Math.atan2(Local.x, Local.z) - Part.Yaw();
            LookYaw = Mth.wrapDegrees(LookYaw * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD;
            double Flat = Math.sqrt(Local.x * Local.x + Local.z * Local.z);
            double LookPitch = -Math.atan2(Local.y, Flat) - Part.Pitch();
            float Flicker = 0.78f + 0.22f * (float) Math.sin((Clock + Partial) * 0.11f + Part.Index() * 0.9f);
            int Glow = 0xFF000000 | (int) (Flicker * 255.0f) << 16 | (int) (Flicker * 255.0f) << 8 | (int) (Flicker * 255.0f);
            renderer(Part.Kind()).draw(Stack, Buffers, Light, new YaldabaothPartRenderer.Pose(Jaw, (float) (LookYaw * 0.35), (float) (LookPitch * 0.35), Part.Index(),
                    Clock, Partial, Shut, Glow));
            Stack.popPose();
        }
        Buffers.endBatch();
        RenderSystem.setShaderFogStart(FogStart);
        RenderSystem.setShaderFogEnd(FogEnd);
        RenderSystem.setShaderFogShape(Shape);
    }
}
