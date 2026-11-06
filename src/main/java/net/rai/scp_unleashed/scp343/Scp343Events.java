package net.rai.scp_unleashed.scp343;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp343Events {
    private Scp343Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp343Registry.God.get(), Scp343God.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp343Registry.SpawnEgg.get());
        }
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp343Registry.ThroneItem.get());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent Event) {
        Scp343Judgement.murdered(Event.getEntity(), Event.getSource());
    }

    @SubscribeEvent
    public static void changeTarget(LivingChangeTargetEvent Event) {
        if (Event.getNewAboutToBeSetTarget() instanceof Scp343God) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        Scp343Away.tick(Event.getServer());
        Scp343Judgement.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp343Judgement.loggedOut(User);
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Scp343Judgement.clear();
    }
}
