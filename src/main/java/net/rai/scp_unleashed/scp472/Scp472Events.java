package net.rai.scp_unleashed.scp472;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp472.client.Scp472ClientHooks;
import net.rai.scp_unleashed.scp472.network.Scp472MindPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp472Events {
    private Scp472Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp472MindPayload.PayloadType, Scp472MindPayload.PayloadCodec, (Payload, Context) -> Scp472ClientHooks.mind(Payload));
    }

    @SubscribeEvent
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp472Registry.BloodstoneItem.get());
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        resend(Event.getEntity());
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        resend(Event.getEntity());
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        resend(Event.getEntity());
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp472Sync.forget(User);
        }
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent Event) {
        Scp472Sync.forgetAll();
        Scp472Hunger.forgetAll();
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Scp472Commands.register(Event.getDispatcher());
    }

    private static void resend(Player Target) {
        if (Target instanceof ServerPlayer User) {
            Scp472Sync.mind(User, true);
        }
    }
}
