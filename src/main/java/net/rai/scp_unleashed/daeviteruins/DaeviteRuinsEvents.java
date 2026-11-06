package net.rai.scp_unleashed.daeviteruins;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.daeviteruins.plan.RuinPages;
import net.rai.scp_unleashed.daeviteruins.world.DaeviteRuin;
import net.rai.scp_unleashed.daeviteruins.world.DaeviteRuinsPlanner;
import net.rai.scp_unleashed.daeviteruins.world.DaeviteRuinsRuntime;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp140.api.Scp140PageEvent;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class DaeviteRuinsEvents {
    private DaeviteRuinsEvents() {
    }

    @SubscribeEvent
    public static void pageFilled(Scp140PageEvent Event) {
        int Page = Event.getPage();
        if (Event.isAwakening() || Page < RuinPages.First || Page > RuinPages.Last || !RuinPages.known(Page)) {
            return;
        }
        ServerLevel From = Event.getLevel();
        ServerLevel World = From.getServer().overworld();
        BlockPos Origin = DaeviteRuinsPlanner.overworldOrigin(From, Event.getOrigin());
        DaeviteRuin Ruin = DaeviteRuinsPlanner.plan(World, Page, Origin);
        Event.setSite(Level.OVERWORLD, Ruin.where());
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (Event.getLevel() instanceof ServerLevel World && World.dimension() == Level.OVERWORLD) {
            DaeviteRuinsRuntime.tick(World);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            for (DeferredItem<? extends Item> Entry : DaeviteRuinsRegistry.CarriedItems) {
                Event.accept(Entry.get());
            }
        }
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            for (DeferredItem<? extends Item> Entry : DaeviteRuinsRegistry.DecorItems) {
                Event.accept(Entry.get());
            }
        }
    }
}
