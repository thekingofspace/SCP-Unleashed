package net.rai.scp_unleashed.scp439;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

import java.util.ArrayList;
import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp439Events {
    public static final int ReconcileTicks = 20;
    private static final List<Entity> Vacate = new ArrayList<>();
    private static final List<LivingEntity> Orphans = new ArrayList<>();

    private Scp439Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp439Registry.Queen.get(), Scp439Queen.createAttributes().build());
        Event.put(Scp439Registry.Worker.get(), Scp439Worker.createAttributes().build());
        Event.put(Scp439Registry.Brute.get(), Scp439Brute.createAttributes().build());
        Event.put(Scp439Registry.Hive.get(), Scp439Hive.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            for (DeferredItem<DeferredSpawnEggItem> Egg : Scp439Registry.Eggs) {
                Event.accept(Egg.get());
            }
        }
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        Scp439Commands.register(Event.getDispatcher());
    }

    @SubscribeEvent
    public static void entityTick(EntityTickEvent.Post Event) {
        if (!(Event.getEntity() instanceof LivingEntity Living) || Living.level().isClientSide) {
            return;
        }
        if (Scp439Infection.stateOf(Living) != null) {
            Scp439Infection.tick(Living);
        } else if (Living instanceof Player && Living.tickCount % ReconcileTicks == 0 && Scp439Infection.hasModifiers(Living)) {
            Scp439Infection.clear(Living);
        }
    }

    @SubscribeEvent
    public static void died(LivingDeathEvent Event) {
        LivingEntity Dead = Event.getEntity();
        if (!(Dead.level() instanceof ServerLevel) || Scp439Infection.stateOf(Dead) == null) {
            return;
        }
        if (Scp439Infection.becomeHive(Dead) != null && !(Dead instanceof Player)) {
            synchronized (Vacate) {
                Vacate.add(Dead);
            }
        }
    }

    @SubscribeEvent
    public static void left(EntityLeaveLevelEvent Event) {
        if (!(Event.getLevel() instanceof ServerLevel World) || !(Event.getEntity() instanceof LivingEntity Living)) {
            return;
        }
        Entity.RemovalReason Reason = Living.getRemovalReason();
        if (Reason == null) {
            return;
        }
        if (!Living.getType().canSerialize() && (Reason == Entity.RemovalReason.DISCARDED || Reason == Entity.RemovalReason.UNLOADED_TO_CHUNK)) {
            Scp439Colonies.get(World).removeHost(Living.getUUID());
        }
        if (Reason.shouldDestroy() && Scp439Infection.stateOf(Living) != null) {
            synchronized (Orphans) {
                Orphans.add(Living);
            }
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        synchronized (Orphans) {
            Orphans.clear();
        }
        synchronized (Vacate) {
            Vacate.clear();
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        List<LivingEntity> Lost;
        synchronized (Orphans) {
            Lost = List.copyOf(Orphans);
            Orphans.clear();
        }
        for (LivingEntity Host : Lost) {
            Scp439Infection.abandon(Host);
        }
        List<Entity> Due;
        synchronized (Vacate) {
            if (Vacate.isEmpty()) {
                return;
            }
            Due = List.copyOf(Vacate);
            Vacate.clear();
        }
        for (Entity Body : Due) {
            if (!Body.isRemoved()) {
                Body.discard();
            }
        }
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        LivingEntity Target = Event.target();
        if (Scp439Infection.stateOf(Target) != null) {
            Event.markCured(Scp439Infection.cure(Target, Event.source() instanceof LivingEntity Source ? Source : null));
        }
    }
}
