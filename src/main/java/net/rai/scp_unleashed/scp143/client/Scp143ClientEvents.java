package net.rai.scp_unleashed.scp143.client;

import net.minecraft.client.renderer.Sheets;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp143.Scp143Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp143ClientEvents {
    private Scp143ClientEvents() {
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent Event) {
        Event.enqueueWork(() -> Sheets.addWoodType(Scp143Registry.Bladewood));
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp143Registry.BoatType.get(), Context -> new Scp143BoatRenderer(Context, false));
        Event.registerEntityRenderer(Scp143Registry.ChestBoatType.get(), Context -> new Scp143BoatRenderer(Context, true));
    }

    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent Event) {
        Event.registerSpriteSet(Scp143Registry.Petal.get(), Sprites -> {
            Scp143ClientState.sprites(Sprites);
            return new Scp143PetalParticle.Provider(Sprites);
        });
        Event.registerSpriteSet(Scp143Registry.Drip.get(), Scp143DripParticle.Provider::new);
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut Event) {
        Scp143ClientState.clear();
    }
}
