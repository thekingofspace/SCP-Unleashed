package net.rai.scp_unleashed.sulfur;

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
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.sulfur.tank.IndustrialTankBlock;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class SulfurEvents {
    private static final String[] TankAfter = {"gas_compressor", "industrial_cooler"};

    private SulfurEvents() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        Event.registerBlock(Capabilities.FluidHandler.BLOCK, (World, Pos, State, Tile, Side) -> IndustrialTankBlock.fluidHandlerAt(World, Pos, State, Side),
                SulfurRegistry.Tank.get());
        Event.registerBlock(Capabilities.ItemHandler.BLOCK, (World, Pos, State, Tile, Side) -> IndustrialTankBlock.itemHandlerAt(World, Pos, State, Side),
                SulfurRegistry.Tank.get());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void buildCreativeTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Resources.getKey()) {
            Event.accept(SulfurRegistry.OreItem.get());
            Event.accept(SulfurRegistry.DeepslateOreItem.get());
            Event.accept(SulfurRegistry.Sulfur.get());
            Event.accept(SulfurRegistry.SulfurBlockItem.get());
            Event.accept(SulfurRegistry.AcidBucket.get());
            return;
        }
        if (Event.getTabKey() != ModCreativeTabs.Machines.getKey()) {
            return;
        }
        ItemStack Entry = new ItemStack(SulfurRegistry.TankItem.get());
        ItemStack After = neighbour(Event);
        if (After != null) {
            Event.insertAfter(After, Entry, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        } else {
            Event.accept(Entry);
        }
    }

    @Nullable
    private static ItemStack neighbour(BuildCreativeModeTabContentsEvent Event) {
        for (String Name : TankAfter) {
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
