package net.rai.scp_unleashed.scp5001.mekhane.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp5001.mekhane.MekhaneProtocol;
import net.rai.scp_unleashed.scp5001.mekhane.Scp5001MekhaneRegistry;
import net.rai.scp_unleashed.shadercompat.client.ShaderCompat;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class MekhaneClientEvents {
    public static final float SkyTop = 420.0f;
    public static final float FogShare = 0.35f;
    public static final float WhiteRed = 1.0f;
    public static final float WhiteGreen = 0.9f;
    public static final float WhiteBlue = 0.66f;

    private MekhaneClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp5001MekhaneRegistry.MekhaneType.get(), MekhaneRenderer::new);
        Event.registerBlockEntityRenderer(Scp5001MekhaneRegistry.EmitterEntity.get(), MekhaneEmitterRenderer::new);
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, MekhaneOverlay.LayerId, MekhaneOverlay::render);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        MekhaneClientState.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        MekhaneClientState.reset();
    }

    @SubscribeEvent
    public static void shake(ViewportEvent.ComputeCameraAngles Event) {
        float Amount = MekhaneClientState.shake();
        if (Amount < 0.01f) {
            return;
        }
        double Time = MekhaneClientState.ticks() + Event.getPartialTick();
        float Yaw = (float) (Math.sin(Time * 1.37) * Math.cos(Time * 0.61) + Math.sin(Time * 2.9) * 0.4) * Amount * 1.2f;
        float Pitch = (float) (Math.sin(Time * 1.71 + 1.3) * Math.cos(Time * 0.47) + Math.sin(Time * 3.3 + 0.2) * 0.4) * Amount * 1.0f;
        float Roll = (float) (Math.sin(Time * 1.13 + 0.4) + Math.sin(Time * 2.3 + 2.1) * 0.5) * Amount * 1.4f;
        Event.setYaw(Event.getYaw() + Yaw);
        Event.setPitch(Event.getPitch() + Pitch);
        Event.setRoll(Event.getRoll() + Roll);
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.ComputeFogColor Event) {
        float Level = MekhaneClientState.tint((float) Event.getPartialTick()) * FogShare;
        if (Level <= 0.001f) {
            return;
        }
        Event.setRed(Mth.lerp(Level, Event.getRed(), WhiteRed));
        Event.setGreen(Mth.lerp(Level, Event.getGreen(), WhiteGreen));
        Event.setBlue(Mth.lerp(Level, Event.getBlue(), WhiteBlue));
    }

    @SubscribeEvent
    public static void renderLevel(RenderLevelStageEvent Event) {
        if (Event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES || ShaderCompat.renderingShadowPass()) {
            return;
        }
        Minecraft Client = Minecraft.getInstance();
        if (Client.level == null || MekhaneClientState.stage() == MekhaneProtocol.Stage.Idle && MekhaneClientState.shocks().isEmpty()) {
            return;
        }
        float Partial = Event.getPartialTick().getGameTimeDeltaPartialTick(false);
        Camera View = Event.getCamera();
        Vec3 Eye = View.getPosition();
        PoseStack Pose = Event.getPoseStack();
        MultiBufferSource.BufferSource Buffers = Client.renderBuffers().bufferSource();
        float Time = MekhaneClientState.ticks() + Partial;
        float Beam = MekhaneClientState.beam(Partial);
        if (Beam > 0.003f) {
            Vec3 Base = Vec3.atCenterOf(MekhaneClientState.emitter()).subtract(Eye);
            float Height = SkyTop - (float) (Base.y + Eye.y);
            float Pulse = 1.0f + 0.08f * Mth.sin(Time * 0.35f);
            MekhaneLight.column(Buffers, Pose, Base, Height, 2.2f * Pulse * Beam, Time * 0.02f, -Time * 0.05f, 0.95f * Beam);
            MekhaneLight.column(Buffers, Pose, Base, Height, 4.6f * Pulse * Beam, -Time * 0.012f, -Time * 0.03f, 0.35f * Beam);
            MekhaneLight.column(Buffers, Pose, Base, Height, 8.0f * Beam, Time * 0.006f, -Time * 0.015f, 0.12f * Beam);
        }
        for (MekhaneClientState.Shock Each : MekhaneClientState.shocks()) {
            float Age = (MekhaneClientState.ticks() - Each.Born() + Partial) / MekhaneClientState.ShockLife;
            if (Age < 0.0f || Age > 1.0f) {
                continue;
            }
            Vec3 Centre = Each.At().subtract(Eye);
            float Radius = MekhaneClientState.ShockReach * Each.Power() * (0.15f + 0.85f * (float) Math.sqrt(Age));
            float Alpha = (1.0f - Age) * Math.min(1.0f, Each.Power());
            MekhaneLight.disc(Buffers, Pose, MekhaneLight.ShockRing, Centre, Centre.scale(-1.0), Radius, Age * 1.5f, 1.0f, 0.9f, 0.65f, Alpha);
            MekhaneLight.disc(Buffers, Pose, MekhaneLight.ShockRing, Centre, new Vec3(0.0, 1.0, 0.0), Radius * 1.3f, -Age, 1.0f, 0.85f, 0.55f, Alpha * 0.6f);
        }
        Buffers.endBatch();
    }
}
