package net.rai.scp_unleashed.matrix;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class MatrixEvents {
    private static final String[] PlaceAfter = {"instability_reactor"};

    private MatrixEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void buildCreativeTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() != ModCreativeTabs.Machines.getKey()) {
            return;
        }
        ItemStack Entry = new ItemStack(MatrixRegistry.InstabilityMatrixItem.get());
        ItemStack After = neighbour(Event);
        if (After != null) {
            Event.insertAfter(After, Entry, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
            return;
        }
        Event.accept(Entry);
    }

    @Nullable
    private static ItemStack neighbour(BuildCreativeModeTabContentsEvent Event) {
        for (String Name : PlaceAfter) {
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
