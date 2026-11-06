package net.rai.scp_unleashed.item;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.network.WireCutterPayload;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.registry.ModItems;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class WireCutterEvents {
    private WireCutterEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        Event.registrar("1").playToServer(WireCutterPayload.PayloadType, WireCutterPayload.PayloadCodec, WireCutterPayload::handle);
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            Event.accept(ModItems.WireCutter.get());
        }
    }
}
