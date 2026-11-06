package net.rai.scp_unleashed.nuclear;

import net.minecraft.world.item.Item;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.block.CoalGeneratorBlock;
import net.rai.scp_unleashed.block.entity.CoalGeneratorBlockEntity;
import net.rai.scp_unleashed.registry.ModBlocks;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class NuclearEvents {
    private NuclearEvents() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        Event.registerBlock(Capabilities.ItemHandler.BLOCK, (World, Pos, State, Tile, Side) -> {
            if (!(State.getBlock() instanceof CoalGeneratorBlock Generator)) {
                return null;
            }
            return World.getBlockEntity(Generator.devicePos(State, Pos)) instanceof CoalGeneratorBlockEntity Machine ? Machine.getAutomation() : null;
        }, ModBlocks.CoalGenerator.get());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() != ModCreativeTabs.Resources.getKey()) {
            return;
        }
        for (DeferredItem<? extends Item> Entry : NuclearRegistry.creativeOrder()) {
            Event.accept(Entry.get());
        }
    }
}
