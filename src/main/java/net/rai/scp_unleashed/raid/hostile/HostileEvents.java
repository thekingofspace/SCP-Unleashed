package net.rai.scp_unleashed.raid.hostile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class HostileEvents {
    private HostileEvents() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(HostileRegistry.AgentType.get(), HostileRaider.createAttributes().build());
        Event.put(HostileRegistry.InsurgentType.get(), HostileRaider.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        HostileCommands.register(Event.getDispatcher());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Anomalies.getKey()) {
            Event.accept(HostileRegistry.Pod.get());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent Event) {
        if (Event.getEntity() instanceof HostileRaider Raider && !Raider.level().isClientSide) {
            HostileRaidStyle.died(Raider);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void friendlyFire(LivingIncomingDamageEvent Event) {
        Entity Source = Event.getSource().getEntity();
        LivingEntity Victim = Event.getEntity();
        if (Source instanceof HostileRaider Raider && Source != Victim && Raider.ally(Victim)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void left(EntityLeaveLevelEvent Event) {
        if (!(Event.getLevel() instanceof ServerLevel) || !(Event.getEntity() instanceof HostileRaider Raider)) {
            return;
        }
        Entity.RemovalReason Reason = Raider.getRemovalReason();
        if (Reason != Entity.RemovalReason.UNLOADED_TO_CHUNK && Reason != Entity.RemovalReason.UNLOADED_WITH_PLAYER) {
            return;
        }
        Raid Current = Raider.raid();
        if (Current != null) {
            Current.release(Raider.getUUID(), false);
        }
    }
}
