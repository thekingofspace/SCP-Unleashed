package net.rai.scp_unleashed.stranger;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.stranger.client.StrangerClientHooks;
import net.rai.scp_unleashed.stranger.network.StrangerDialoguePayload;
import net.rai.scp_unleashed.stranger.network.StrangerReplyPayload;
import net.rai.scp_unleashed.stranger.network.StrangerVanishPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class StrangerEvents {
    private StrangerEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(StrangerDialoguePayload.PayloadType, StrangerDialoguePayload.PayloadCodec, (Payload, Context) -> StrangerClientHooks.openDialogue(Payload));
        Registrar.playToClient(StrangerVanishPayload.PayloadType, StrangerVanishPayload.PayloadCodec, (Payload, Context) -> StrangerClientHooks.vanish(Payload));
        Registrar.playToServer(StrangerReplyPayload.PayloadType, StrangerReplyPayload.PayloadCodec, (Payload, Context) -> {
            if (Context.player() instanceof ServerPlayer Reader && Reader.level().getEntity(Payload.EntityId()) instanceof StrangerEntity Figure) {
                Figure.reply(Reader, Payload.Finished());
            }
        });
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(StrangerRegistry.Figure.get(), StrangerEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void tickEncounters(ServerTickEvent.Post Event) {
        StrangerEncounters.get(Event.getServer()).tick(Event.getServer());
    }

    @SubscribeEvent
    public static void readerLeft(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer Reader) {
            StrangerEncounters.get(Reader.server).readerLoggedOut(Reader.server, Reader.getUUID());
        }
    }

    @SubscribeEvent
    public static void readerJoined(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer Reader) {
            StrangerEncounters.get(Reader.server).readerJoined(Reader);
        }
    }

    @SubscribeEvent
    public static void readerTravelled(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer Reader) {
            StrangerEncounters.get(Reader.server).readerTravelled(Reader.server, Reader.getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void readerDied(LivingDeathEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer Reader) {
            StrangerEncounters.get(Reader.server).readerDied(Reader.server, Reader.getUUID());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(StrangerRegistry.FigureEgg.get());
        }
    }
}
