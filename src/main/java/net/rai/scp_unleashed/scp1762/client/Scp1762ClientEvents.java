package net.rai.scp_unleashed.scp1762.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp1762.Scp1762Kind;
import net.rai.scp_unleashed.scp1762.Scp1762Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp1762ClientEvents {
    private Scp1762ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerBlockEntityRenderer(Scp1762Registry.BoxEntity.get(), Scp1762BoxRenderer::new);
        for (Scp1762Kind Kind : Scp1762Kind.values()) {
            Event.registerEntityRenderer(Scp1762Registry.dragon(Kind), Context -> new Scp1762DragonRenderer(Context, Kind));
        }
    }
}
