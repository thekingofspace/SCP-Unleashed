package net.rai.scp_unleashed.scp469;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp469Events {
    private Scp469Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp469Registry.Angel.get(), Scp469Angel.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp469Registry.Feather.get());
            Event.accept(Scp469Registry.AngelEgg.get());
        }
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            Scp469Reform.get(World).tick(World);
        }
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent Event) {
        if (Event.getLevel().isClientSide || !(Event.getEntity() instanceof LivingEntity Body)) {
            return;
        }
        if (Scp469Embrace.isHeld(Body) || Scp469Embrace.locked(Body)) {
            Scp469Embrace.release(Body);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Scp469Embrace.release(Event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void useItem(PlayerInteractEvent.RightClickItem Event) {
        if (Scp469Embrace.isHeld(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void useBlock(PlayerInteractEvent.RightClickBlock Event) {
        if (Scp469Embrace.isHeld(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void useEntity(PlayerInteractEvent.EntityInteract Event) {
        if (Scp469Embrace.isHeld(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void startUsing(LivingEntityUseItemEvent.Start Event) {
        if (Scp469Embrace.isHeld(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed Event) {
        if (Scp469Embrace.isHeld(Event.getEntity())) {
            Event.setNewSpeed(0.0f);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void mount(EntityMountEvent Event) {
        Entity Rider = Event.getEntityMounting();
        if (Event.isMounting() && Scp469Embrace.isHeld(Rider)) {
            Event.setCanceled(true);
        }
    }
}
