package net.rai.scp_unleashed.fission;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.gas.GasRegistry;
import net.rai.scp_unleashed.gas.VaporizerBlock;
import net.rai.scp_unleashed.plumbing.DrainBlock;
import net.rai.scp_unleashed.plumbing.PlumbingRegistry;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.registry.ModItems;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class FissionEvents {
    private FissionEvents() {
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        Event.registerBlock(Capabilities.FluidHandler.BLOCK, (World, Pos, State, Machine, Side) -> FissionReactorBlock.fluidHandlerAt(World, Pos, State, Side),
                FissionRegistry.Reactor.get());
        Event.registerBlock(Capabilities.FluidHandler.BLOCK, (World, Pos, State, Machine, Side) -> HeatExchangerBlock.capabilityAt(World, Pos, Side),
                FissionRegistry.HeatExchanger.get());
        Event.registerBlock(Capabilities.FluidHandler.BLOCK, (World, Pos, State, Machine, Side) -> SonicVibratorBlock.capabilityAt(World, Pos, State, Side),
                FissionRegistry.SonicVibrator.get());
        Event.registerBlock(Capabilities.FluidHandler.BLOCK, (World, Pos, State, Machine, Side) -> GasDissolverBlock.capabilityAt(World, Pos, State, Side),
                FissionRegistry.GasDissolver.get());
        Event.registerBlock(Capabilities.FluidHandler.BLOCK, (World, Pos, State, Machine, Side) -> VaporizerBlock.capabilityAt(World, Pos, State, Side),
                GasRegistry.Vaporizer.get());
        Event.registerBlock(Capabilities.FluidHandler.BLOCK, (World, Pos, State, Machine, Side) -> DrainBlock.capabilityAt(World, Pos, Side),
                PlumbingRegistry.Drain.get());
    }

    private static void placeAfter(BuildCreativeModeTabContentsEvent Event, ItemLike Anchor, ItemLike Entry) {
        ItemStack After = new ItemStack(Anchor);
        ItemStack Stack = new ItemStack(Entry);
        if (Event.getParentEntries().contains(After) && Event.getSearchEntries().contains(After)) {
            Event.insertAfter(After, Stack, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        } else {
            Event.accept(Stack);
        }
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            Event.accept(FissionRegistry.HeatExchangerItem.get());
            Event.accept(FissionRegistry.SonicVibratorItem.get());
            Event.accept(FissionRegistry.GasDissolverItem.get());
            Event.accept(GasRegistry.VaporizerItem.get());
            placeAfter(Event, ModItems.Tank.get(), GasRegistry.GasTankItem.get());
            placeAfter(Event, GasRegistry.GasTankItem.get(), PlumbingRegistry.DrainItem.get());
        } else if (Event.getTabKey() == ModCreativeTabs.Resources.getKey()) {
            Event.accept(FissionRegistry.WasteWaterBucket.get());
        }
    }
}
