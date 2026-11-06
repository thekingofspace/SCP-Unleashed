package net.rai.scp_unleashed.scp058;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Cow;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.archive.investigation.Investigations;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp058Events {
    private Scp058Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp058Registry.Heart.get(), Scp058Heart.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp058Registry.HeartEgg.get());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent Event) {
        LivingEntity Fallen = Event.getEntity();
        if (Event.isCanceled() || !(Fallen instanceof Cow) || !(Fallen.level() instanceof ServerLevel World)
                || !(Event.getSource().getEntity() instanceof ServerPlayer Killer)) {
            return;
        }
        Investigations Data = Investigations.get(World.getServer());
        Data.find(Killer.getUUID(), Scp058Registry.DocumentId).ifPresent(Case -> {
            if (Scp058CowCondition.isMarked(Case)) {
                return;
            }
            Scp058CowCondition.mark(Case, Fallen);
            Case.consumeDirty();
            Data.setDirty();
            Scp058Emergence.omen(World, Fallen);
        });
    }
}
