package net.rai.scp_unleashed.scp860;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.rai.scp_unleashed.Scp_unleashed;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp860Rules {
    private Scp860Rules() {
    }

    public static boolean bound(Entity Target) {
        if (!Scp860Forest.bound(Target)) {
            return false;
        }
        return !(Target instanceof Player User) || !Scp860Forest.exempt(User);
    }

    public static boolean sealedDoor(Level World, BlockPos Pos, BlockState State) {
        if (!Scp860DoorBlock.isDoor(State)) {
            return false;
        }
        return ((Scp860DoorBlock) State.getBlock()).far() || Scp860DoorBlock.sealed(World, Pos, State);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breaking(BlockEvent.BreakEvent Event) {
        if (Event.getPlayer() != null && bound(Event.getPlayer())) {
            Event.setCanceled(true);
            return;
        }
        if (Event.getLevel() instanceof Level World && sealedDoor(World, Event.getPos(), Event.getState())) {
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
        if (Event.getEntity() != null && bound(Event.getEntity())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void fluidMade(BlockEvent.FluidPlaceBlockEvent Event) {
        if (Event.getLevel() instanceof Level World && Scp860Forest.strict(World)) {
            Event.setNewState(Event.getOriginalState());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void portal(BlockEvent.PortalSpawnEvent Event) {
        if (Event.getLevel() instanceof Level World && Scp860Forest.strict(World)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void explosion(ExplosionEvent.Start Event) {
        if (Scp860Forest.strict(Event.getLevel())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void detonate(ExplosionEvent.Detonate Event) {
        Level World = Event.getLevel();
        Event.getAffectedBlocks().removeIf(Pos -> sealedDoor(World, Pos, World.getBlockState(Pos)));
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
        if (!(Event.getLevel().getLevel() instanceof Level World) || !Scp860Forest.covers(World, Event.getEntity().blockPosition())) {
            return;
        }
        MobSpawnType Kind = Event.getSpawnType();
        if (Kind == MobSpawnType.COMMAND || Kind == MobSpawnType.SPAWN_EGG) {
            return;
        }
        if (Event.getEntity() instanceof Scp860Watcher && Kind == MobSpawnType.EVENT) {
            return;
        }
        if (Scp860Forest.strict(World)) {
            Event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void dying(LivingDeathEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && Scp860Forest.record(User).Active) {
            Scp860Trip.died(User);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void drops(LivingDropsEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && Scp860Forest.record(User).Active) {
            Event.getDrops().clear();
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void experience(LivingExperienceDropEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && Scp860Forest.record(User).Active) {
            Event.setCanceled(true);
        }
    }
}
