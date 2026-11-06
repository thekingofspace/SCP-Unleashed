package net.rai.scp_unleashed.oil;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class OilEvents {
    private OilEvents() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        for (OilMachineBlock Machine : List.of(OilRegistry.OilExtractor.get(), OilRegistry.DistillationTower.get(), OilRegistry.ChemicalProcessor.get(),
                OilRegistry.CombustionGenerator.get(), OilRegistry.OilRefinery.get())) {
            Event.registerBlock(Capabilities.FluidHandler.BLOCK, (World, Pos, State, Tile, Side) -> Machine.fluidCapability(World, Pos, State, Side), Machine);
        }
        Event.registerBlock(Capabilities.ItemHandler.BLOCK, (World, Pos, State, Tile, Side) -> OilRefineryBlock.itemHandlerAt(World, Pos, State, Side),
                OilRegistry.OilRefinery.get());
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            Event.accept(OilRegistry.OilExtractorItem.get());
            Event.accept(OilRegistry.DistillationTowerItem.get());
            Event.accept(OilRegistry.ChemicalProcessorItem.get());
            Event.accept(OilRegistry.CombustionGeneratorItem.get());
            Event.accept(OilRegistry.OilRefineryItem.get());
        } else if (Event.getTabKey() == ModCreativeTabs.Resources.getKey()) {
            Event.accept(OilRegistry.Plastic.get());
            for (OilLiquid Liquid : OilRegistry.Liquids) {
                Event.accept(Liquid.getBucket().get());
            }
        }
    }
}
