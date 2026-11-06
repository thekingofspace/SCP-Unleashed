package net.rai.scp_unleashed.steam;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class SteamEvents {
    private SteamEvents() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        Event.registerBlock(Capabilities.FluidHandler.BLOCK, (World, Pos, State, Machine, Side) -> WaterBoilerBlock.capabilityAt(World, Pos, State, Side),
                SteamRegistry.WaterBoiler.get());
        Event.registerBlock(Capabilities.ItemHandler.BLOCK, (World, Pos, State, Machine, Side) -> WaterBoilerBlock.itemHandlerAt(World, Pos, State),
                SteamRegistry.WaterBoiler.get());
        Event.registerBlock(Capabilities.FluidHandler.BLOCK, (World, Pos, State, Machine, Side) -> CoolingTowerBlock.capabilityAt(World, Pos, State, Side),
                SteamRegistry.CoolingTower.get());
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            Event.accept(SteamRegistry.WaterBoilerItem.get());
            Event.accept(SteamRegistry.SteamTurbineItem.get());
            Event.accept(SteamRegistry.IndustrialTurbineItem.get());
            Event.accept(SteamRegistry.CoolingTowerItem.get());
        }
    }
}
