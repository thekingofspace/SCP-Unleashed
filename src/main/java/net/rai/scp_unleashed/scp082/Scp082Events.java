package net.rai.scp_unleashed.scp082;

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
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp082Events {
    private Scp082Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp082Registry.Fernand.get(), Scp082Fernand.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp082Registry.SpawnEgg.get());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void mount(EntityMountEvent Event) {
        Entity Rider = Event.getEntityMounting();
        if (!(Rider instanceof LivingEntity Body) || !Scp082Hold.isHeld(Body) || Body.isRemoved() || Body.isDeadOrDying()) {
            return;
        }
        Entity Mount = Event.getEntityBeingMounted();
        if (Event.isDismounting()) {
            if (Mount instanceof Scp082Fernand Holder && Holder.isAlive() && !Holder.isRemoved() && Scp082Hold.heldBy(Body, Holder)) {
                Event.setCanceled(true);
            }
            return;
        }
        if (!(Mount instanceof Scp082Fernand)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent Event) {
        if (Event.getLevel().isClientSide || !(Event.getEntity() instanceof LivingEntity Body) || !Scp082Hold.isHeld(Body)) {
            return;
        }
        if (!(Body.getVehicle() instanceof Scp082Fernand)) {
            Scp082Hold.release(Body);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Scp082Hold.isHeld(Event.getEntity())) {
            Scp082Hold.release(Event.getEntity());
            Event.getEntity().stopRiding();
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void useItem(PlayerInteractEvent.RightClickItem Event) {
        if (Scp082Hold.isHeld(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void useBlock(PlayerInteractEvent.RightClickBlock Event) {
        if (Scp082Hold.isHeld(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void useEntity(PlayerInteractEvent.EntityInteract Event) {
        if (Scp082Hold.isHeld(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void startUsing(LivingEntityUseItemEvent.Start Event) {
        if (Scp082Hold.isHeld(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed Event) {
        if (Scp082Hold.isHeld(Event.getEntity())) {
            Event.setNewSpeed(0.0f);
        }
    }
}
