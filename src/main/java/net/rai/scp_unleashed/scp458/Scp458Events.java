package net.rai.scp_unleashed.scp458;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp458Events {
    private Scp458Events() {
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Scp458Commands.register(Event.getDispatcher());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Foods.getKey()) {
            for (Scp458Pizza Each : Scp458Pizza.values()) {
                ItemStack Made = Scp458Box.slice(Each);
                Event.accept(Made);
            }
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp458Registry.BoxItem.get());
        }
    }

    @SubscribeEvent
    public static void rightClickBlock(PlayerInteractEvent.RightClickBlock Event) {
        Player User = Event.getEntity();
        if (User.isSecondaryUseActive() && Scp458BoxBlock.isOpen(Event.getLevel().getBlockState(Event.getPos()))) {
            Event.setUseBlock(TriState.TRUE);
        }
    }
}
