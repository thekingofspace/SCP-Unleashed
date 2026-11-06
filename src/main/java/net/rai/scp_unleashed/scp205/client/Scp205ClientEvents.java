package net.rai.scp_unleashed.scp205.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp205.Scp205Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp205ClientEvents {
    private Scp205ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp205Registry.Devil.get(), Scp205DevilRenderer::new);
        Event.registerBlockEntityRenderer(Scp205Registry.LampEntity.get(), Scp205LampRenderer::new);
    }
}
