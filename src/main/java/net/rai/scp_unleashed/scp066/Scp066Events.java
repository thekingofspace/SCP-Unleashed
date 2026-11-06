package net.rai.scp_unleashed.scp066;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp066.client.Scp066ClientHooks;
import net.rai.scp_unleashed.scp066.network.Scp066ChordPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp066Events {
    private Scp066Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp066Registry.Yarn.get(), Scp066Entity.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp066ChordPayload.PayloadType, Scp066ChordPayload.PayloadCodec, (Payload, Context) -> Scp066ClientHooks.chord(Payload));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp066Registry.BallItem.get());
            Event.accept(Scp066Registry.SpawnEgg.get());
        } else if (Event.getTabKey() == ModCreativeTabs.Foods.getKey()) {
            Event.accept(Scp066Registry.Candy.get());
            Event.accept(Scp066Registry.Cupcake.get());
        }
    }
}
