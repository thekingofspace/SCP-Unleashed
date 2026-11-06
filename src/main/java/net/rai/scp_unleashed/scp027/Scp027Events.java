package net.rai.scp_unleashed.scp027;

import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Silverfish;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp027Events {
    private Scp027Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp027Registry.Subject.get(), Scp027Entity.createAttributes().build());
        Event.put(Scp027Registry.Rat.get(), Scp027Rat.createAttributes().build());
        Event.put(Scp027Registry.Silverfish.get(), Silverfish.createAttributes().build());
        Event.put(Scp027Registry.Endermite.get(), Endermite.createAttributes().build());
        Event.put(Scp027Registry.Bat.get(), Bat.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp027Registry.SpawnEgg.get());
        }
    }
}
