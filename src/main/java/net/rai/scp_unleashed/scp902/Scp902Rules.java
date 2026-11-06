package net.rai.scp_unleashed.scp902;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp902Rules {
    private Scp902Rules() {
    }

    public static boolean bound(Entity Target) {
        if (!Scp902Wastes.inside(Target)) {
            return false;
        }
        if (!(Target instanceof Player User)) {
            return !Scp902Wastes.standingIn();
        }
        if (User.isSpectator()) {
            return false;
        }
        return onTrip(User) || !Scp902Wastes.standingIn() && !Scp902Wastes.exempt(User);
    }

    public static boolean onTrip(Player User) {
        return User instanceof ServerPlayer Visitor ? Scp902State.get(Visitor.server).trip(Visitor.getUUID()) != null : Scp902Wastes.inside(User);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breaking(BlockEvent.BreakEvent Event) {
        if (Event.getPlayer() != null && bound(Event.getPlayer())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breakSpeed(PlayerEvent.BreakSpeed Event) {
        if (bound(Event.getEntity())) {
            Event.setNewSpeed(0.0f);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void placing(BlockEvent.EntityPlaceEvent Event) {
        if (Event.getLevel() instanceof Level World && Scp902Wastes.inside(World) && Event.getEntity() != null && bound(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void fluidMade(BlockEvent.FluidPlaceBlockEvent Event) {
        if (Event.getLevel() instanceof Level World && Scp902Wastes.strict(World)) {
            Event.setNewState(Event.getOriginalState());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void portal(BlockEvent.PortalSpawnEvent Event) {
        if (Event.getLevel() instanceof Level World && Scp902Wastes.strict(World)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void explosion(ExplosionEvent.Start Event) {
        if (Scp902Wastes.strict(Event.getLevel())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useBlock(PlayerInteractEvent.RightClickBlock Event) {
        if (bound(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void useItem(PlayerInteractEvent.RightClickItem Event) {
        if (bound(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void hitBlock(PlayerInteractEvent.LeftClickBlock Event) {
        if (bound(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void touchEntity(PlayerInteractEvent.EntityInteract Event) {
        if (bound(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void touchEntityAt(PlayerInteractEvent.EntityInteractSpecific Event) {
        if (bound(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void attack(AttackEntityEvent Event) {
        if (bound(Event.getEntity()) || Event.getTarget() instanceof Scp902Copy) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void toss(ItemTossEvent Event) {
        Player User = Event.getPlayer();
        if (User == null || !bound(User)) {
            return;
        }
        ItemStack Kept = Event.getEntity().getItem().copy();
        Event.setCanceled(true);
        Event.getEntity().discard();
        if (!Kept.isEmpty() && !User.getInventory().add(Kept)) {
            User.getInventory().placeItemBackInInventory(Kept);
        }
        User.inventoryMenu.broadcastChanges();
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void pearl(EntityTeleportEvent.EnderPearl Event) {
        if (bound(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void chorus(EntityTeleportEvent.ChorusFruit Event) {
        if (bound(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void spawning(FinalizeSpawnEvent Event) {
        if (!(Event.getLevel().getLevel() instanceof Level World) || !Scp902Wastes.inside(World)) {
            return;
        }
        if (Scp902Wastes.standingIn() && !Scp902Wastes.nearSlots(Event.getEntity().blockPosition())) {
            return;
        }
        MobSpawnType Kind = Event.getSpawnType();
        if (Kind != MobSpawnType.COMMAND && Kind != MobSpawnType.SPAWN_EGG) {
            Event.setSpawnCancelled(true);
        }
    }

    public static boolean endsTrip(DamageSource Source) {
        return Source.is(DamageTypes.GENERIC_KILL) || Source.is(DamageTypes.FELL_OUT_OF_WORLD) || Source.is(DamageTypes.OUTSIDE_BORDER);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void incoming(LivingIncomingDamageEvent Event) {
        if (Event.getEntity() instanceof Scp902Copy) {
            Event.setCanceled(true);
            return;
        }
        if (!(Event.getEntity() instanceof ServerPlayer User) || !Scp902Wastes.inside(User) || !onTrip(User)) {
            return;
        }
        Event.setCanceled(true);
        if (endsTrip(Event.getSource())) {
            Scp902Trip.hurry(User);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void dying(LivingDeathEvent Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User) || !onTrip(User)) {
            return;
        }
        Event.setCanceled(true);
        User.setHealth(User.getMaxHealth());
        User.clearFire();
        Scp902Trip.hurry(User);
        Scp902State.Trip Each = Scp902State.get(User.server).trip(User.getUUID());
        if (Each != null && Each.Stage == Scp902State.StageLeaving) {
            Scp902Trip.home(User, Each);
        }
    }
}
