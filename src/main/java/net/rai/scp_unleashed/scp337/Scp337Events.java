package net.rai.scp_unleashed.scp337;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp337Events {
    private Scp337Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp337Registry.Mass.get(), Scp337Mass.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp337Registry.MassEgg.get());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void died(LivingDeathEvent Event) {
        LivingEntity Victim = Event.getEntity();
        if (Victim.level().isClientSide || !(Event.getSource().getEntity() instanceof Scp337Mass Mass) || Mass == Victim) {
            return;
        }
        if (Victim instanceof Player || ControlBodies.isPersonLike(Victim)) {
            Mass.recordTaken(Victim);
        }
    }
}
