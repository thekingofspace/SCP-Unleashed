package net.rai.scp_unleashed.scp1762;

import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp1762Events {
    private Scp1762Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        for (Scp1762Kind Kind : Scp1762Kind.values()) {
            Event.put(Scp1762Registry.dragon(Kind), Scp1762Dragon.createAttributes().build());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp1762Registry.BoxItem.get());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void breaking(BlockEvent.BreakEvent Event) {
        BlockState State = Event.getState();
        if (State.getBlock() instanceof Scp1762BoxBlock && Scp1762BoxBlock.guarded(Event.getLevel(), Event.getPos())) {
            Event.setCanceled(true);
        }
    }
}
