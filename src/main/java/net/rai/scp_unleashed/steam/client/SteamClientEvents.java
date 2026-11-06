package net.rai.scp_unleashed.steam.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.steam.CoolingTowerBlockEntity;
import net.rai.scp_unleashed.steam.IndustrialTurbineBlockEntity;
import net.rai.scp_unleashed.steam.SteamRegistry;
import net.rai.scp_unleashed.steam.SteamTurbineBlockEntity;
import net.rai.scp_unleashed.steam.WaterBoilerBlockEntity;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class SteamClientEvents {
    private SteamClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        WaterBoilerBlockEntity.setClientEffects(SteamClientEffects::boilerTick);
        SteamTurbineBlockEntity.setClientEffects(SteamClientEffects::turbineTick);
        IndustrialTurbineBlockEntity.setClientEffects(IndustrialTurbineEffects::tick);
        CoolingTowerBlockEntity.setClientEffects(SteamClientEffects::towerTick);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(SteamRegistry.SteamTurbineEntity.get(), SteamTurbineRenderer::new);
        Event.registerBlockEntityRenderer(SteamRegistry.IndustrialTurbineEntity.get(), IndustrialTurbineRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        Event.register(SteamTurbineRenderer.RotorModel);
        IndustrialTurbineRenderer.models().forEach(Event::register);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent Event) {
        Event.register(SteamRegistry.WaterBoilerMenuType.get(), WaterBoilerScreen::new);
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(SteamRegistry.SteamPlume.get(), SteamPlumeParticle.Provider::new);
    }
}
