package net.rai.scp_unleashed.intro;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.intro.client.IntroClientHooks;
import net.rai.scp_unleashed.intro.network.IntroActionPayload;
import net.rai.scp_unleashed.intro.network.IntroAwaitPayload;
import net.rai.scp_unleashed.intro.network.IntroCuePayload;
import net.rai.scp_unleashed.intro.network.IntroReadyPayload;
import net.rai.scp_unleashed.intro.network.IntroScenePayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class IntroEvents {
    private IntroEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(IntroAwaitPayload.PayloadType, IntroAwaitPayload.PayloadCodec, (Payload, Context) -> IntroClientHooks.await(Payload));
        Registrar.playToClient(IntroScenePayload.PayloadType, IntroScenePayload.PayloadCodec, (Payload, Context) -> IntroClientHooks.scene(Payload));
        Registrar.playToClient(IntroCuePayload.PayloadType, IntroCuePayload.PayloadCodec, (Payload, Context) -> IntroClientHooks.cue(Payload));
        Registrar.playToServer(IntroReadyPayload.PayloadType, IntroReadyPayload.PayloadCodec, (Payload, Context) -> {
            if (Context.player() instanceof ServerPlayer User) {
                IntroDirector.ready(User, Payload.Token(), Payload.Skip());
            }
        });
        Registrar.playToServer(IntroActionPayload.PayloadType, IntroActionPayload.PayloadCodec, (Payload, Context) -> {
            if (Context.player() instanceof ServerPlayer User) {
                IntroDirector.action(User, Payload.Token(), Payload.Action());
            }
        });
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(IntroRegistry.Scientist.get(), IntroScientist.createAttributes().build());
        Event.put(IntroRegistry.Staff.get(), IntroStaff.createAttributes().build());
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent Event) {
        IntroCommands.register(Event.getDispatcher());
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post Event) {
        IntroDirector.tick(Event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            IntroDirector.loggedIn(User);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            IntroDirector.loggedOut(User);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && !Event.isEndConquered()) {
            IntroDirector.respawned(User);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        IntroDirector.stopped(Event.getServer());
    }
}
