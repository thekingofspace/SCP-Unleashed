package net.rai.scp_unleashed.scp4666;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.rai.scp_unleashed.Scp_unleashed;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp4666Rules {
    public static final long WarnGap = 50L;
    private static final Map<UUID, Long> Warned = new ConcurrentHashMap<>();

    private Scp4666Rules() {
    }

    public static boolean bound(Entity Target) {
        if (!Scp4666World.inside(Target)) {
            return false;
        }
        if (!(Target instanceof Player User)) {
            return !Scp4666World.standingIn();
        }
        return !Scp4666World.exempt(User) && Scp4666World.participant(User);
    }

    public static void refuse(Player User, String Rule) {
        if (!(User instanceof ServerPlayer Visitor)) {
            return;
        }
        long Now = Visitor.serverLevel().getGameTime();
        Long Last = Warned.get(Visitor.getUUID());
        if (Last != null && Now >= Last && Now - Last < WarnGap) {
            return;
        }
        Warned.put(Visitor.getUUID(), Now);
        Visitor.displayClientMessage(Component.translatable("message.scp_unleashed.scp4666.rule." + Rule).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void breaking(BlockEvent.BreakEvent Event) {
        Player User = Event.getPlayer();
        if (User != null && bound(User)) {
            Event.setCanceled(true);
            refuse(User, "break");
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
        if (!(Event.getLevel() instanceof Level World) || !Scp4666World.inside(World)) {
            return;
        }
        Entity Placer = Event.getEntity();
        if (Placer instanceof Player User && Scp4666World.exempt(User)) {
            return;
        }
        if (Placer == null) {
            return;
        }
        if (Scp4666World.standingIn() && !bound(Placer)) {
            return;
        }
        Event.setCanceled(true);
        if (Placer instanceof Player User) {
            refuse(User, "build");
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void fluidMade(BlockEvent.FluidPlaceBlockEvent Event) {
        if (Event.getLevel() instanceof Level World && Scp4666World.covers(World, Event.getPos())) {
            Event.setNewState(Event.getOriginalState());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void portal(BlockEvent.PortalSpawnEvent Event) {
        if (Event.getLevel() instanceof Level World && Scp4666World.covers(World, Event.getPos())) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void explosion(ExplosionEvent.Start Event) {
        if (Scp4666World.covers(Event.getLevel(), BlockPos.containing(Event.getExplosion().center()))) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void pearl(EntityTeleportEvent.EnderPearl Event) {
        if (bound(Event.getEntity())) {
            Event.setCanceled(true);
            if (Event.getEntity() instanceof Player User) {
                refuse(User, "escape");
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void chorus(EntityTeleportEvent.ChorusFruit Event) {
        if (bound(Event.getEntity())) {
            Event.setCanceled(true);
            if (Event.getEntity() instanceof Player User) {
                refuse(User, "escape");
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void spawning(FinalizeSpawnEvent Event) {
        if (!(Event.getLevel().getLevel() instanceof Level World) || !Scp4666World.covers(World, Event.getEntity().blockPosition())) {
            return;
        }
        MobSpawnType Kind = Event.getSpawnType();
        if (Kind != MobSpawnType.COMMAND && Kind != MobSpawnType.MOB_SUMMONED && Kind != MobSpawnType.EVENT && Kind != MobSpawnType.SPAWN_EGG) {
            Event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent
    public static void forget(ServerStoppedEvent Event) {
        Warned.clear();
    }
}
