package net.rai.scp_unleashed.scp4666.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.Input;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import net.rai.scp_unleashed.scp4666.Scp4666World;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp4666ClientEvents {
    public static final float FogNear = 26.0f;
    public static final float FogEdge = 3.5f;
    public static final float FogStartShare = 0.04f;
    public static final float ShakeDegrees = 3.5f;

    private Scp4666ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Event.enqueueWork(Scp4666Display::register);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp4666Registry.YuleMan.get(), Scp4666Renderer::new);
    }

    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent Event) {
        Event.register(Scp4666Registry.location("scp4666_winter"), new Scp4666WinterEffects());
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent Event) {
        Event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, Scp4666Overlay.LayerId, Scp4666Overlay::render);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Scp4666ClientState.tick(Minecraft.getInstance());
        Scp4666Atmosphere.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void frame(RenderFrameEvent.Pre Event) {
        Minecraft Client = Minecraft.getInstance();
        if (Client.level != null && Scp4666ClientState.inside(Client)) {
            Scp4666ClientState.hold(Client.level, Event.getPartialTick().getGameTimeDeltaPartialTick(false));
        }
    }

    public static float fogReach(double Distance) {
        double Share = Mth.clamp((Distance - Scp4666World.FogStart) / (Scp4666World.FogEdge - Scp4666World.FogStart), 0.0, 1.0);
        return (float) Mth.lerp(Math.pow(Share, 1.3), FogNear, FogEdge);
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog Event) {
        Minecraft Client = Minecraft.getInstance();
        if (!Scp4666ClientState.inside(Client) || Client.player == null || Client.gameRenderer.getMainCamera().getFluidInCamera() != FogType.NONE
                || Client.player.hasEffect(MobEffects.BLINDNESS) || Client.player.hasEffect(MobEffects.DARKNESS)) {
            return;
        }
        float Far = Math.min(Event.getFarPlaneDistance(), fogReach(Scp4666ClientState.distanceFromCentre(Client.player.position())));
        Event.setNearPlaneDistance(Far * FogStartShare);
        Event.setFarPlaneDistance(Far);
        Event.setCanceled(true);
    }

    @SubscribeEvent
    public static void cameraAngles(ViewportEvent.ComputeCameraAngles Event) {
        if (!Scp4666ClientState.inside(Minecraft.getInstance())) {
            return;
        }
        float Partial = (float) Event.getPartialTick();
        float Shake = Scp4666ClientState.shake(Partial) * ShakeDegrees * Minecraft.getInstance().options.screenEffectScale().get().floatValue();
        if (Shake > 0.002f) {
            float Time = Scp4666ClientState.ticks() + Partial;
            Event.setPitch(Event.getPitch() + Shake * Mth.sin(Time * 3.1f));
            Event.setRoll(Event.getRoll() + Shake * 0.8f * Mth.sin(Time * 2.3f + 0.7f));
        }
    }

    @SubscribeEvent
    public static void movement(MovementInputUpdateEvent Event) {
        if (!Scp4666ClientState.held() || !Scp4666ClientState.inside(Minecraft.getInstance())) {
            return;
        }
        Input Keys = Event.getInput();
        Keys.forwardImpulse = 0.0f;
        Keys.leftImpulse = 0.0f;
        Keys.up = false;
        Keys.down = false;
        Keys.left = false;
        Keys.right = false;
        Keys.jumping = false;
        Keys.shiftKeyDown = false;
    }

    @SubscribeEvent
    public static void interact(InputEvent.InteractionKeyMappingTriggered Event) {
        Minecraft Client = Minecraft.getInstance();
        if (!Event.isUseItem() || Client.level == null || Client.screen != null || !(Client.hitResult instanceof BlockHitResult Hit) || Hit.getType() != HitResult.Type.BLOCK) {
            return;
        }
        if (!Client.level.getBlockState(Hit.getBlockPos()).is(Scp4666Registry.ChoreList.get())) {
            return;
        }
        Client.setScreen(new Scp4666ChoreListScreen());
        Event.setSwingHand(false);
        Event.setCanceled(true);
    }

    @SubscribeEvent
    public static void selectMusic(SelectMusicEvent Event) {
        if (Scp4666ClientState.inside(Minecraft.getInstance())) {
            Event.setMusic(null);
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp4666ClientState.reset();
        Scp4666ClientState.forget();
        Scp4666Atmosphere.stop();
    }
}
