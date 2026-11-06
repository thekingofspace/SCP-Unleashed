package net.rai.scp_unleashed.mtf;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

import java.util.UUID;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class MtfEvents {
    private MtfEvents() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(MtfRegistry.OperativeType.get(), MtfOperative.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent Event) {
        MtfCommands.register(Event.getDispatcher());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.Facility.getKey()) {
            Event.accept(MtfRegistry.SignalRadio.get());
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post Event) {
        MtfMissions.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent Event) {
        MtfMissions.clear();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void holdFire(LivingIncomingDamageEvent Event) {
        Entity Source = Event.getSource().getEntity();
        LivingEntity Victim = Event.getEntity();
        if (Source == null || Source == Victim) {
            return;
        }
        if (Source instanceof MtfOperative Shooter) {
            UUID Caller = Shooter.callerId();
            if (Caller != null && MtfFriends.isFriend(Caller, Victim)) {
                Event.setCanceled(true);
            }
            return;
        }
        if (Victim instanceof MtfOperative Member && Member.callerId() != null && Member.callerId().equals(Source.getUUID())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void noteFight(LivingDamageEvent.Post Event) {
        Entity Source = Event.getSource().getEntity();
        LivingEntity Victim = Event.getEntity();
        if (Victim.level().isClientSide || Source == Victim) {
            return;
        }
        if (Source instanceof ServerPlayer Caller && MtfMissions.busy(Caller.getUUID())) {
            MtfMissions.noteHit(Caller, Victim);
        }
        if (Victim instanceof ServerPlayer Caller && Source instanceof LivingEntity Attacker && MtfMissions.busy(Caller.getUUID())) {
            MtfMissions.noteAttack(Caller, Attacker);
        }
    }
}
