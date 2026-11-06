package net.rai.scp_unleashed.scp352;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp352Events {
    private Scp352Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp352Registry.Crone.get(), Scp352Crone.createAttributes().build());
    }

    @SubscribeEvent
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp352Registry.SpawnEgg.get());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onDeath(LivingDeathEvent Event) {
        LivingEntity Fallen = Event.getEntity();
        if (!(Fallen.level() instanceof ServerLevel) || !Scp352Prey.isPerson(Fallen)) {
            return;
        }
        Scp352Crone Hunter = hunter(Event.getSource().getEntity(), Fallen);
        if (Hunter != null && Hunter.isAlive()) {
            Hunter.claimKill(Fallen);
        }
    }

    @Nullable
    private static Scp352Crone hunter(@Nullable Entity Killer, LivingEntity Fallen) {
        if (Killer instanceof Scp352Crone Creature) {
            return Creature;
        }
        return Fallen.getKillCredit() instanceof Scp352Crone Creature ? Creature : null;
    }
}
