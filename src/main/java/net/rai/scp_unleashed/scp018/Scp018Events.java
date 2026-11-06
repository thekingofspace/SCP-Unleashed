package net.rai.scp_unleashed.scp018;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp018.client.Scp018ClientHooks;
import net.rai.scp_unleashed.scp018.network.BallPathPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp018Events {
    private Scp018Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(BallPathPayload.PayloadType, BallPathPayload.PayloadCodec, (Payload, Context) -> Scp018ClientHooks.path(Payload));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp018Registry.BallItem.get());
        }
    }
}
