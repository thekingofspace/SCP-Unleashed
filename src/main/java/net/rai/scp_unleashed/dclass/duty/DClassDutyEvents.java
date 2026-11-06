package net.rai.scp_unleashed.dclass.duty;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.dclass.duty.client.DClassDutyClient;
import net.rai.scp_unleashed.dclass.duty.network.DClassBlinkPayload;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class DClassDutyEvents {
    private DClassDutyEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(DClassBlinkPayload.PayloadType, DClassBlinkPayload.PayloadCodec, (Payload, Context) -> DClassDutyClient.blink(Payload));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Facility.getKey()) {
            Event.accept(DClassDutyRegistry.Manager.get());
        }
    }

    @SubscribeEvent
    public static void tick(EntityTickEvent.Post Event) {
        if (Event.getEntity() instanceof DClassLike Body && !Body.level().isClientSide) {
            DClassGaze.tick(Body);
        }
    }
}
