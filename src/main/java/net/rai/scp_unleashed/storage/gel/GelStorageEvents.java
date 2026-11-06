package net.rai.scp_unleashed.storage.gel;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.storage.StorageUpgradeItem;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class GelStorageEvents {
    private GelStorageEvents() {
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Facility.getKey()) {
            Event.accept(GelStorageRegistry.GelContainerItem.get());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void refuseUpgrade(PlayerInteractEvent.RightClickBlock Event) {
        if (!(Event.getItemStack().getItem() instanceof StorageUpgradeItem)) {
            return;
        }
        BlockState State = Event.getLevel().getBlockState(Event.getPos());
        if (State.getBlock() instanceof GelStorageContainerBlock) {
            if (!Event.getLevel().isClientSide) {
                Event.getEntity().displayClientMessage(Component.translatable("message.scp_unleashed.storage_gel.no_upgrades"), true);
            }
            Event.setCanceled(true);
            Event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent Event) {
        Event.registerBlock(Capabilities.FluidHandler.BLOCK, (World, Pos, State, Tile, Side) -> GelStorageContainerBlock.capabilityAt(World, Pos, State, Side),
                GelStorageRegistry.GelContainer.get());
    }
}
