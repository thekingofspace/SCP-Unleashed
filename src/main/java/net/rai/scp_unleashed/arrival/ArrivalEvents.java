package net.rai.scp_unleashed.arrival;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.arrival.client.ArrivalClientHooks;
import net.rai.scp_unleashed.arrival.network.ArrivalCardPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class ArrivalEvents {
    private ArrivalEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        Event.registrar("1").playToClient(ArrivalCardPayload.PayloadType, ArrivalCardPayload.PayloadCodec, (Payload, Context) -> ArrivalClientHooks.card(Payload));
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent Event) {
        ArrivalCommands.register(Event.getDispatcher());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            ArrivalCards.changed(User, Event.getFrom(), Event.getTo());
        }
    }
}
