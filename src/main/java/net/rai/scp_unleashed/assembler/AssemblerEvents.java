package net.rai.scp_unleashed.assembler;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class AssemblerEvents {
    private AssemblerEvents() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        Event.registerBlock(Capabilities.ItemHandler.BLOCK, (World, Pos, State, Entity, Side) -> AdvancedAssemblerBlock.itemHandlerAt(World, Pos, State, Side),
                AssemblerRegistry.AdvancedAssembler.get());
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            Event.accept(AssemblerRegistry.AdvancedAssemblerItem.get());
        }
    }
}
