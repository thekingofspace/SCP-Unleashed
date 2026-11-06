package net.rai.scp_unleashed.scp163;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp163.client.Scp163ClientHooks;
import net.rai.scp_unleashed.scp163.network.Scp163ImpactPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp163Events {
    private Scp163Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp163Registry.Castaway.get(), Scp163Castaway.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp163ImpactPayload.PayloadType, Scp163ImpactPayload.PayloadCodec, (Payload, Context) -> Scp163ClientHooks.impact(Payload));
    }

    @SubscribeEvent
    public static void tickCrashes(ServerTickEvent.Post Event) {
        Scp163Crashes.get(Event.getServer()).tick(Event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp163Registry.Suit.get());
            Event.accept(Scp163Registry.CastawayEgg.get());
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp163Registry.ApparatusItem.get());
            Event.accept(Scp163Registry.ConsoleItem.get());
            Event.accept(Scp163Registry.SignallerItem.get());
            Event.accept(Scp163Registry.HullItem.get());
            Event.accept(Scp163Registry.HullSeamItem.get());
            Event.accept(Scp163Registry.HullLightItem.get());
            Event.accept(Scp163Registry.DeckItem.get());
            Event.accept(Scp163Registry.HatchItem.get());
        }
    }
}
