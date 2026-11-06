package net.rai.scp_unleashed.scp280;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp280.network.Scp280EyesPayload;
import net.rai.scp_unleashed.scp280.network.Scp280MeterPayload;

import java.util.function.Consumer;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp280Events {
    public static Consumer<Scp280EyesPayload> EyesHandler = Payload -> {
    };
    public static Consumer<Scp280MeterPayload> MeterHandler = Payload -> {
    };

    private Scp280Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp280Registry.Shade.get(), Scp280Shade.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp280Registry.ShadeEgg.get());
        }
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp280EyesPayload.PayloadType, Scp280EyesPayload.PayloadCodec, (Payload, Context) -> EyesHandler.accept(Payload));
        Registrar.playToClient(Scp280MeterPayload.PayloadType, Scp280MeterPayload.PayloadCodec, (Payload, Context) -> MeterHandler.accept(Payload));
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Scp280Commands.register(Event.getDispatcher());
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (Event.getEntity() instanceof ServerPlayer Player) {
            Scp280Haunt.tick(Player);
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer Player) {
            Scp280Haunt.joined(Player);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Scp280Haunt.forget(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer Player) {
            Scp280Haunt.send(Player, Scp280EyesPayload.clear());
            Scp280Haunt.haunt(Player.getUUID()).setCountdown(-1);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Scp280Haunt.reset();
    }
}
