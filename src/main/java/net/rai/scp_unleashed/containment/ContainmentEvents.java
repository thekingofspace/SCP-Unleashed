package net.rai.scp_unleashed.containment;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public class ContainmentEvents {
    @SubscribeEvent
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Facility.getKey()) {
            Event.accept(ContainmentRegistry.ReinforcedWallItem.get());
            Event.accept(ContainmentRegistry.HazardWallItem.get());
            Event.accept(ContainmentRegistry.FloorDrainItem.get());
            Event.accept(ContainmentRegistry.ReinforcedGlassItem.get());
            Event.accept(ContainmentRegistry.ReinforcedPaneItem.get());
            Event.accept(ContainmentRegistry.OneWayGlassItem.get());
            Event.accept(ContainmentRegistry.SmartGlassItem.get());
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            SmartGlassGroups.tickLevel(World);
        }
    }

    @SubscribeEvent
    public static void levelUnload(LevelEvent.Unload Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            SmartGlassGroups.forget(World);
        }
    }
}
