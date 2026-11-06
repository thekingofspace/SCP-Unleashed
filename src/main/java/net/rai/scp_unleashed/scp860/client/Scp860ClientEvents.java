package net.rai.scp_unleashed.scp860.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.material.FogType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterDimensionSpecialEffectsEvent;
import net.neoforged.neoforge.client.event.SelectMusicEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp860.Scp860Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp860ClientEvents {
    public static final float FogNear = 0.0f;
    public static final float FogFar = 18.0f;

    private Scp860ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp860Registry.Watcher.get(), Scp860WatcherRenderer::new);
        Event.registerBlockEntityRenderer(Scp860Registry.DoorEntity.get(), Scp860DoorRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        Event.register(Scp860DoorRenderer.LeafModel);
    }

    @SubscribeEvent
    public static void registerEffects(RegisterDimensionSpecialEffectsEvent Event) {
        Event.register(Scp860Registry.location("scp860_forest"), new Scp860ForestEffects());
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Scp860Ambience.tick(Minecraft.getInstance());
    }

    @SubscribeEvent
    public static void fogColour(ViewportEvent.ComputeFogColor Event) {
        Minecraft Client = Minecraft.getInstance();
        if (!Scp860Ambience.inside(Client) || Event.getCamera().getFluidInCamera() != FogType.NONE) {
            return;
        }
        Event.setRed((float) Scp860ForestEffects.Haze.x);
        Event.setGreen((float) Scp860ForestEffects.Haze.y);
        Event.setBlue((float) Scp860ForestEffects.Haze.z);
    }

    @SubscribeEvent
    public static void fog(ViewportEvent.RenderFog Event) {
        Minecraft Client = Minecraft.getInstance();
        if (!Scp860Ambience.inside(Client) || Event.getCamera().getFluidInCamera() != FogType.NONE || Client.player.hasEffect(MobEffects.BLINDNESS)
                || Client.player.hasEffect(MobEffects.DARKNESS)) {
            return;
        }
        Event.setNearPlaneDistance(FogNear);
        Event.setFarPlaneDistance(Math.min(Event.getFarPlaneDistance(), FogFar));
        Event.setCanceled(true);
    }

    @SubscribeEvent
    public static void selectMusic(SelectMusicEvent Event) {
        if (Scp860Ambience.inside(Minecraft.getInstance())) {
            Event.setMusic(null);
        }
    }

    @SubscribeEvent
    public static void loggingOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp860Ambience.stop(Minecraft.getInstance());
    }
}
