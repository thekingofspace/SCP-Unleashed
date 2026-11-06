package net.rai.scp_unleashed.scp079.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp079.Scp079BlockEntity;
import net.rai.scp_unleashed.scp079.Scp079Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp079ClientEvents {
    private Scp079ClientEvents() {
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent Event) {
        Scp079BlockEntity.setClientEffects(Scp079HumSound::update);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(Scp079Registry.ComputerEntity.get(), Scp079Renderer::new);
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post Event) {
        Scp079ClientHooks.tickLurch();
    }
}
