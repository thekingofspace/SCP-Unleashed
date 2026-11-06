package net.rai.scp_unleashed.computer;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.computer.network.ComputerActions;
import net.rai.scp_unleashed.computer.network.ComputerNetworking;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class ComputerEvents {
    private ComputerEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        ComputerNetworking.register(Event.registrar("1"));
    }

    @SubscribeEvent
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            Event.accept(ComputerRegistry.ComputerItem.get());
            Event.accept(ComputerRegistry.ChatServerItem.get());
            Event.accept(ComputerRegistry.MessageDatabaseItem.get());
            Event.accept(ComputerRegistry.UploadPortItem.get());
            Event.accept(ComputerRegistry.FloppyDisc.get());
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        ComputerActions.forget(Event.getEntity().getUUID());
    }
}
