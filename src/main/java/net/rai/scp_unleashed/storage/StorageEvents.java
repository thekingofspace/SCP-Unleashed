package net.rai.scp_unleashed.storage;

import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class StorageEvents {
    private StorageEvents() {
    }

    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock Event) {
        if (!Event.getEntity().isSecondaryUseActive()) {
            return;
        }
        BlockState State = Event.getLevel().getBlockState(Event.getPos());
        if (State.getBlock() instanceof StorageContainerBlock && Event.getHitVec().getDirection() == State.getValue(StorageContainerBlock.Facing)) {
            Event.setUseBlock(TriState.TRUE);
        }
    }

    @SubscribeEvent
    public static void buildContents(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Facility.getKey()) {
            Event.accept(StorageRegistry.SmallContainerItem.get());
            Event.accept(StorageRegistry.TallContainerItem.get());
        } else if (Event.getTabKey() == ModCreativeTabs.Upgrades.getKey()) {
            Event.accept(StorageRegistry.HeatedUpgrade.get());
            Event.accept(StorageRegistry.ColdUpgrade.get());
        }
    }
}
