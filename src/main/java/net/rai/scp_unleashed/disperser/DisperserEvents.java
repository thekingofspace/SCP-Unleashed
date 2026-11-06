package net.rai.scp_unleashed.disperser;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.labware.LabOrganism;
import net.rai.scp_unleashed.labware.LabOrganisms;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class DisperserEvents {
    private DisperserEvents() {
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent Event) {
        Event.enqueueWork(() -> DispenserBlock.registerProjectileBehavior(DisperserRegistry.OrganicGrenade.get()));
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        Event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DisperserRegistry.DisperserEntity.get(), (Machine, Side) -> Machine.automation());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            Event.accept(new ItemStack(DisperserRegistry.DisperserItem.get()));
            return;
        }
        if (Event.getTabKey() != ModCreativeTabs.Weapons.getKey()) {
            return;
        }
        Event.accept(new ItemStack(DisperserRegistry.OrganicGrenade.get()));
        for (LabOrganism Organism : LabOrganisms.all()) {
            Event.accept(OrganicGrenadeItem.loaded(Organism.getId()));
        }
    }
}
