package net.rai.scp_unleashed.rifttech;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.upgrade.UpgradeType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class RiftTechEvents {
    private RiftTechEvents() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        Event.registerBlock(Capabilities.ItemHandler.BLOCK, (World, Pos, State, Tile, Side) -> RiftCollectorBlock.itemHandlerAt(World, Pos, State),
                RiftTechRegistry.RiftCollector.get());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void buildCreativeTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            place(Event, List.of(new ItemStack(RiftTechRegistry.InstabilityPylonItem.get()), new ItemStack(RiftTechRegistry.RiftCollectorItem.get())), "instability_matrix");
        } else if (Event.getTabKey() == ModCreativeTabs.Upgrades.getKey()) {
            place(Event, List.of(new ItemStack(UpgradeType.Range.getItem())), "speed_upgrade");
        } else if (Event.getTabKey() == ModCreativeTabs.Anomalies.getKey()) {
            place(Event, List.of(new ItemStack(RiftTechRegistry.ApocalypticFragment.get())), "unstable_fragment_" + FragmentRarity.Abyssal.getSerializedName());
        }
    }

    private static void place(BuildCreativeModeTabContentsEvent Event, List<ItemStack> Entries, String After) {
        ItemStack Anchor = neighbour(Event, After);
        for (ItemStack Entry : Entries) {
            if (Anchor != null) {
                Event.insertAfter(Anchor, Entry, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
                Anchor = Entry;
            } else {
                Event.accept(Entry);
            }
        }
    }

    @Nullable
    private static ItemStack neighbour(BuildCreativeModeTabContentsEvent Event, String Name) {
        ResourceLocation Id = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, Name);
        if (!BuiltInRegistries.ITEM.containsKey(Id)) {
            return null;
        }
        ItemStack Wanted = new ItemStack(BuiltInRegistries.ITEM.get(Id));
        return Event.getParentEntries().contains(Wanted) && Event.getSearchEntries().contains(Wanted) ? Wanted : null;
    }
}
