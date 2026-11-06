package net.rai.scp_unleashed.marksman;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.marksman.ability.ToolAbilityPayload;
import net.rai.scp_unleashed.marksman.client.MarksmanClientHooks;
import net.rai.scp_unleashed.marksman.network.CoinRicochetPayload;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class MarksmanEvents {
    private MarksmanEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToServer(ToolAbilityPayload.PayloadType, ToolAbilityPayload.PayloadCodec, ToolAbilityPayload::handle);
        Registrar.playToClient(CoinRicochetPayload.PayloadType, CoinRicochetPayload.PayloadCodec, (Payload, Context) -> MarksmanClientHooks.onRicochet(Payload));
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Weapons.getKey()) {
            Event.accept(MarksmanRevolverItem.loaded());
        }
    }

    @SubscribeEvent
    public static void forgetTosser(PlayerEvent.PlayerLoggedOutEvent Event) {
        CoinToss.forget(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void forgetTossers(ServerStoppedEvent Event) {
        CoinToss.forgetAll();
    }
}
