package net.rai.scp_unleashed.scp049;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp049Events {
    private Scp049Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp049Registry.Doctor.get(), Scp049Doctor.createAttributes().build());
        Event.put(Scp049Registry.Cured.get(), Scp049Cured.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp049Registry.DoctorEgg.get());
            Event.accept(Scp049Registry.CuredEgg.get());
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp049Registry.TrayItem.get());
        }
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent Event) {
        if (Event.getLevel().isClientSide || !(Event.getEntity() instanceof LivingEntity Body)) {
            return;
        }
        AttributeInstance Speed = Body.getAttribute(Attributes.MOVEMENT_SPEED);
        if (Scp049Hold.isHeld(Body) || Speed != null && Speed.hasModifier(Scp049Hold.SpeedLock)) {
            Scp049Hold.release(Body);
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Scp049Hold.release(Event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void useItem(PlayerInteractEvent.RightClickItem Event) {
        if (Scp049Hold.isHeld(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void useBlock(PlayerInteractEvent.RightClickBlock Event) {
        if (Scp049Hold.isHeld(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void useEntity(PlayerInteractEvent.EntityInteract Event) {
        if (Scp049Hold.isHeld(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void startUsing(LivingEntityUseItemEvent.Start Event) {
        if (Scp049Hold.isHeld(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void attack(AttackEntityEvent Event) {
        if (Scp049Hold.isHeld(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void breakSpeed(PlayerEvent.BreakSpeed Event) {
        if (Scp049Hold.isHeld(Event.getEntity())) {
            Event.setNewSpeed(0.0f);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void heal(LivingHealEvent Event) {
        if (Scp049Hold.isHeld(Event.getEntity()) && !(Event.getEntity() instanceof Villager)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void mount(EntityMountEvent Event) {
        Entity Rider = Event.getEntityMounting();
        if (Event.isMounting() && Scp049Hold.isHeld(Rider)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent Event) {
        Scp049Locator.clear();
    }
}
