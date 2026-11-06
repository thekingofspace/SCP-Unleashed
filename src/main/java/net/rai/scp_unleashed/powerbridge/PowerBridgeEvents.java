package net.rai.scp_unleashed.powerbridge;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class PowerBridgeEvents {
    private PowerBridgeEvents() {
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            Event.accept(PowerBridgeRegistry.ReceiverItem.get());
            Event.accept(PowerBridgeRegistry.EmitterItem.get());
        }
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        Event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, PowerBridgeRegistry.ReceiverEntity.get(), PowerBridgeBlockEntity::energyFor);
        Event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, PowerBridgeRegistry.EmitterEntity.get(), PowerBridgeBlockEntity::energyFor);
    }
}
