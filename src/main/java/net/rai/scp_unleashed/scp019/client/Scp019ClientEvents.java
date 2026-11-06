package net.rai.scp_unleashed.scp019.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.scp019.Scp019Brute;
import net.rai.scp_unleashed.scp019.Scp019Drone;
import net.rai.scp_unleashed.scp019.Scp019Registry;

@EventBusSubscriber(modid = Scp_unleashed.ModId, value = Dist.CLIENT)
public final class Scp019ClientEvents {
    private Scp019ClientEvents() {
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers Event) {
        Event.registerEntityRenderer(Scp019Registry.Drone.get(), Context -> new Scp019CreatureRenderer<>(Context, new Scp019GeoModel<Scp019Drone>("drone", 25.0f, 50.0f), 0.25f));
        Event.registerEntityRenderer(Scp019Registry.Brute.get(), Context -> new Scp019CreatureRenderer<>(Context, new Scp019GeoModel<Scp019Brute>("brute", 25.0f, 45.0f), 0.6f));
        Event.registerBlockEntityRenderer(Scp019Registry.PotEntity.get(), Scp019PotRenderer::new);
    }

    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional Event) {
        Event.register(Scp019PotRenderer.LidModel);
    }
}
