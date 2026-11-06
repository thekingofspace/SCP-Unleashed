package net.rai.scp_unleashed.nethub;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class NetHubEvents {
    private NetHubEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        Event.registrar("1").playToServer(NetHubPortPayload.PayloadType, NetHubPortPayload.PayloadCodec, NetHubPortPayload::handle);
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            Event.accept(NetHubRegistry.NetworkHubItem.get());
        }
    }

    @SubscribeEvent
    public static void serverStarting(ServerStartingEvent Event) {
        NetHubs.forgetAll();
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        NetHubs.forgetAll();
    }

    @SubscribeEvent
    public static void levelUnload(LevelEvent.Unload Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            NetHubs.forgetLevel(World);
        }
    }
}
