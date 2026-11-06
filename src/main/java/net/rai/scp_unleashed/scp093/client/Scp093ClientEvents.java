package net.rai.scp_unleashed.scp093.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp093.Scp093Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp093ClientEvents {
    public static final float FogReach = 168.0f;
    public static final float FogEndShare = 0.82f;
    public static final float FogStartShare = 0.06f;
    public static final float VisionFogShare = 0.45f;
    public static final float RollAmount = 3.5f;
    public static final float FovPulse = 0.05f;

    private Scp093ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(Scp093Display::register);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        for (ModelResourceLocation Model : Scp093Display.models()) {
            Event.register(Model);
        }
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp093Registry.Torso.get(), Scp093TorsoRenderer::new);
    }

    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent Event) {
        Event.register(Scp093Registry.location("scp093_wastes"), new Scp093WastesEffects());
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Scp093Overlay.LayerId, Scp093Overlay::render);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Scp093ClientState.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void renderStage(RenderLevelStageEvent Event) {
        if (Event.getStage() == RenderLevelStageEvent.Stage.AFTER_SKY) {
            Scp093SunGlare.render(Event);
        }
    }

    @SubscribeEvent
    public static void fogColour(ViewportEvent.ComputeFogColor Event) {
        if (!Scp093ClientState.inWastes(Minecraft.getInstance())) {
            return;
        }
        Event.setRed((float) Scp093WastesEffects.Haze.x);
        Event.setGreen((float) Scp093WastesEffects.Haze.y);
        Event.setBlue((float) Scp093WastesEffects.Haze.z);
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog Event) {
        Minecraft Client = Minecraft.getInstance();
        if (!Scp093ClientState.inWastes(Client) || Client.gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE || Client.player == null
                || Client.player.hasEffect(MobEffects.BLINDNESS) || Client.player.hasEffect(MobEffects.DARKNESS)) {
            return;
        }
        float Far = Math.min(Event.getFarPlaneDistance(), FogReach) * FogEndShare;
        float Vision = Scp093ClientState.intensity((float) Event.getPartialTick());
        Far *= 1.0f - VisionFogShare * Vision;
        Event.setNearPlaneDistance(Far * FogStartShare);
        Event.setFarPlaneDistance(Far);
        Event.setCanceled(true);
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles Event) {
        float Partial = (float) Event.getPartialTick();
        float Time = Scp093ClientState.ticks() + Partial;
        float Strength = Scp093ClientState.intensity(Partial);
        if (Strength > 0.01f) {
            Event.setRoll(Event.getRoll() + Mth.sin(Time * 0.07f) * RollAmount * Strength);
            Event.setYaw(Event.getYaw() + Mth.sin(Time * 0.043f) * 1.2f * Strength);
        }
        float Shake = Scp093ClientState.rumble(Partial) * Scp093ClientState.ShakeDegrees * Minecraft.getInstance().options.screenEffectScale().get().floatValue();
        if (Shake > 0.002f) {
            Event.setPitch(Event.getPitch() + Shake * Mth.sin(Time * 2.9f));
            Event.setYaw(Event.getYaw() + Shake * 0.5f * Mth.sin(Time * 2.3f + 1.3f));
            Event.setRoll(Event.getRoll() + Shake * 0.7f * Mth.sin(Time * 3.7f + 0.4f));
        }
    }

    @SubscribeEvent
    public static void fieldOfView(ViewportEvent.ComputeFov Event) {
        float Strength = Scp093ClientState.intensity((float) Event.getPartialTick());
        if (Strength <= 0.01f) {
            return;
        }
        float Time = Scp093ClientState.ticks() + (float) Event.getPartialTick();
        Event.setFOV(Event.getFOV() * (1.0 + FovPulse * Strength * Mth.sin(Time * 0.052f)));
    }

    @SubscribeEvent
    public static void selectMusic(SelectMusicEvent Event) {
        if (Scp093ClientState.inWastes(Minecraft.getInstance())) {
            Event.setMusic(null);
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp093ClientState.reset();
    }
}
