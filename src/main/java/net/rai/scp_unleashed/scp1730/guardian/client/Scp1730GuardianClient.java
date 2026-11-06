package net.rai.scp_unleashed.scp1730.guardian.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp1730.client.Scp1730EntityRenderer;
import net.rai.scp_unleashed.scp1730.guardian.Scp1730GuardianRegistry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp1730GuardianClient {
    private Scp1730GuardianClient() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp1730GuardianRegistry.ContainedType.get(), Context -> new Scp1730EntityRenderer<>(Context, "scp1730_2", null, 1.65f, 2.4f, true));
    }
}
