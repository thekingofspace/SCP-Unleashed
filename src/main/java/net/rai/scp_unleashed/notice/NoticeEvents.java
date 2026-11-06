package net.rai.scp_unleashed.notice;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.notice.client.NoticeClientHooks;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class NoticeEvents {
    private NoticeEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        Event.registrar("1").playToClient(NoticePayload.PayloadType, NoticePayload.PayloadCodec, (Payload, Context) -> NoticeClientHooks.receive(Payload.Shown()));
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        NoticeCommands.register(Event.getDispatcher());
    }
}
