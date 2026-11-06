package net.rai.scp_unleashed.refinery;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class RefineryEvents {
    private static final String[] PlaceAfter = {"centrifuge", "item_press", "electric_furnace"};
    private static final String[] PlaceBefore = {"advanced_assembler"};

    private RefineryEvents() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        Event.registerBlock(Capabilities.ItemHandler.BLOCK, (World, Pos, State, Tile, Side) -> OrePurifierBlock.itemHandlerAt(World, Pos, State, Side),
                RefineryRegistry.OrePurifier.get());
        Event.registerBlock(Capabilities.FluidHandler.BLOCK, (World, Pos, State, Tile, Side) -> OrePurifierBlock.fluidHandlerAt(World, Pos, State, Side),
                RefineryRegistry.OrePurifier.get());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void buildCreativeTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Resources.getKey()) {
            for (DeferredItem<? extends Item> Entry : RefineryRegistry.resourceOrder()) {
                Event.accept(Entry.get());
            }
            return;
        }
        if (Event.getTabKey() != ModCreativeTabs.Machines.getKey()) {
            return;
        }
        ItemStack Entry = new ItemStack(RefineryRegistry.OrePurifierItem.get());
        ItemStack After = neighbour(Event, PlaceAfter);
        if (After != null) {
            Event.insertAfter(After, Entry, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            return;
        }
        ItemStack Before = neighbour(Event, PlaceBefore);
        if (Before != null) {
            Event.insertBefore(Before, Entry, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            return;
        }
        Event.accept(Entry);
    }

    @Nullable
    private static ItemStack neighbour(BuildCreativeModeTabContentsEvent Event, String[] Names) {
        for (String Name : Names) {
            ResourceLocation Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name);
            if (!BuiltInRegistries.ITEM.containsKey(Id)) {
                continue;
            }
            ItemStack Wanted = new ItemStack(BuiltInRegistries.ITEM.get(Id));
            if (Event.getParentEntries().contains(Wanted) && Event.getSearchEntries().contains(Wanted)) {
                return Wanted;
            }
        }
        return null;
    }
}
