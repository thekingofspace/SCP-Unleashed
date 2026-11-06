package net.rai.scp_unleashed.intro;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.ICancellableEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class IntroRules {
    private IntroRules() {
    }

    public static boolean exempt(Player User) {
        if (User instanceof ServerPlayer Server && IntroDirector.session(Server) != null) {
            return false;
        }
        return IntroWorld.standingIn() || User.isSpectator() || User.isCreative() && User.hasPermissions(2);
    }

    public static boolean bound(Entity Target) {
        return IntroWorld.inside(Target) && !(Target instanceof Player User && exempt(User));
    }

    private static void refuse(ICancellableEvent Event, Entity Target) {
        if (Target != null && bound(Target)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breaking(BlockEvent.BreakEvent Event) {
        refuse(Event, Event.getPlayer());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breakSpeed(PlayerEvent.BreakSpeed Event) {
        if (bound(Event.getEntity())) {
            Event.setNewSpeed(0.0f);
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void placing(BlockEvent.EntityPlaceEvent Event) {
        if (Event.getLevel() instanceof Level World && IntroWorld.inside(World)) {
            Entity Placer = Event.getEntity();
            if (Placer instanceof Player User ? !exempt(User) : !IntroWorld.standingIn()) {
                Event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void fluids(BlockEvent.FluidPlaceBlockEvent Event) {
        if (Event.getLevel() instanceof Level World && IntroWorld.strict(World)) {
            Event.setNewState(Event.getOriginalState());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useBlock(PlayerInteractEvent.RightClickBlock Event) {
        refuse(Event, Event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useItem(PlayerInteractEvent.RightClickItem Event) {
        refuse(Event, Event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hitBlock(PlayerInteractEvent.LeftClickBlock Event) {
        refuse(Event, Event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void touchEntity(PlayerInteractEvent.EntityInteract Event) {
        refuse(Event, Event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void touchEntityAt(PlayerInteractEvent.EntityInteractSpecific Event) {
        refuse(Event, Event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attacking(AttackEntityEvent Event) {
        refuse(Event, Event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void tossing(ItemTossEvent Event) {
        Player User = Event.getPlayer();
        if (User == null || !bound(User)) {
            return;
        }
        ItemEntity Dropped = Event.getEntity();
        ItemStack Stack = Dropped.getItem().copy();
        Event.setCanceled(true);
        if (!Stack.isEmpty() && !User.getInventory().add(Stack)) {
            User.drop(Stack, false);
        }
        User.inventoryMenu.broadcastChanges();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void damage(LivingIncomingDamageEvent Event) {
        Entity Target = Event.getEntity();
        if (IntroWorld.inside(Target) && (Target instanceof Player User && !(IntroWorld.standingIn() && exempt(User)) || Target instanceof IntroScientist)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void spawning(FinalizeSpawnEvent Event) {
        if (Event.getLevel().getLevel() instanceof Level World && IntroWorld.strict(World) && Event.getSpawnType() != MobSpawnType.COMMAND) {
            Event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void joining(EntityJoinLevelEvent Event) {
        if (!Event.getLevel().isClientSide() && IntroWorld.strict(Event.getLevel()) && Event.getEntity() instanceof Enemy && !Event.loadedFromDisk()) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void portal(BlockEvent.PortalSpawnEvent Event) {
        if (Event.getLevel() instanceof Level World && IntroWorld.strict(World)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void explosion(ExplosionEvent.Start Event) {
        if (IntroWorld.strict(Event.getLevel())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void pearl(EntityTeleportEvent.EnderPearl Event) {
        refuse(Event, Event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void chorus(EntityTeleportEvent.ChorusFruit Event) {
        refuse(Event, Event.getEntity());
    }
}
