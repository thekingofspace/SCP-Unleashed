package net.rai.scp_unleashed.scp294;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp294.client.Scp294ClientHooks;
import net.rai.scp_unleashed.scp294.network.Scp294OrderPayload;
import net.rai.scp_unleashed.scp294.network.Scp294RadiancePayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp294Events {
    private Scp294Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToServer(Scp294OrderPayload.PayloadType, Scp294OrderPayload.PayloadCodec, (Payload, Context) -> {
            if (Context.player() instanceof ServerPlayer User) {
                received(User, Payload.Text());
            }
        });
        Registrar.playToClient(Scp294RadiancePayload.PayloadType, Scp294RadiancePayload.PayloadCodec, (Payload, Context) -> Scp294ClientHooks.radiance(Payload));
    }

    public static Scp294MachineBlockEntity.Outcome received(ServerPlayer User, String Text) {
        if (!(User.containerMenu instanceof Scp294Menu Menu) || !Menu.stillValid(User)) {
            return null;
        }
        Scp294MachineBlockEntity Machine = Menu.machine(User);
        return Machine == null ? null : Machine.order(User, Text);
    }

    @SubscribeEvent
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp294Registry.Cup.get());
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp294Registry.MachineItem.get());
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Scp294Commands.register(Event.getDispatcher());
    }

    @SubscribeEvent
    public static void started(ServerStartedEvent Event) {
        Scp294Resolver.forget();
        Scp294Config.load(Event.getServer());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Scp294Config.clear();
        Scp294Resolver.forget();
        Scp294Ascension.clear();
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        Scp294Ascension.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Scp294Ascension.forget(Event.getEntity().getUUID());
    }
}
