package net.rai.scp_unleashed.processing;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class ProcessingEvents {
    private static final Set<ResourceLocation> LaterMachines = Set.of(
            ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "centrifuge"),
            ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "advanced_assembler"));

    private ProcessingEvents() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        Event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ProcessingRegistry.ElectricFurnaceEntity.get(), (Machine, Side) -> Machine.getItemHandler(Side));
        Event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ProcessingRegistry.ItemPressEntity.get(), (Machine, Side) -> Machine.getItemHandler(Side));
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() != ModCreativeTabs.Machines.getKey()) {
            return;
        }
        ItemStack Anchor = firstLaterMachine(Event);
        for (ItemStack Entry : List.of(new ItemStack(ProcessingRegistry.ElectricFurnaceItem.get()), new ItemStack(ProcessingRegistry.ItemPressItem.get()))) {
            if (Anchor == null) {
                Event.accept(Entry);
            } else {
                Event.insertBefore(Anchor, Entry, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            }
        }
    }

    @Nullable
    private static ItemStack firstLaterMachine(BuildCreativeModeTabContentsEvent Event) {
        for (ItemStack Entry : Event.getParentEntries()) {
            if (LaterMachines.contains(BuiltInRegistries.ITEM.getKey(Entry.getItem())) && Event.getSearchEntries().contains(Entry)) {
                return Entry;
            }
        }
        return null;
    }
}
