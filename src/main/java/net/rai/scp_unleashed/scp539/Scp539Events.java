package net.rai.scp_unleashed.scp539;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp539.client.Scp539ClientHooks;
import net.rai.scp_unleashed.scp539.network.Scp539GazePayload;
import net.rai.scp_unleashed.scp539.network.Scp539WarnPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp539Events {
    private Scp539Events() {
    }

    @SubscribeEvent
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp539Registry.Stone.get());
        }
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp539GazePayload.PayloadType, Scp539GazePayload.PayloadCodec, (Payload, Context) -> Scp539ClientHooks.gaze(Payload));
        Registrar.playToClient(Scp539WarnPayload.PayloadType, Scp539WarnPayload.PayloadCodec, (Payload, Context) -> Scp539ClientHooks.warn(Payload));
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        Scp539Compulsion.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void entityTick(EntityTickEvent.Post Event) {
        if (Event.getEntity() instanceof DClass Subject && Subject.level() instanceof ServerLevel World) {
            Scp539Compulsion.hold(Subject, World);
        }
    }

    @SubscribeEvent
    public static void pickedUp(ItemEntityPickupEvent.Post Event) {
        if (Event.getOriginalStack().is(Scp539Registry.Stone.get()) && Event.getPlayer().level() instanceof ServerLevel World) {
            Scp539Compulsion.pickedUp(World, Event.getItemEntity());
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Scp539Compulsion.reset();
        Scp539Chat.reset();
        Scp539Plan.reset();
    }
}
