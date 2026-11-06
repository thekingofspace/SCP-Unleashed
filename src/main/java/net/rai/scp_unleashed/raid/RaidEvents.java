package net.rai.scp_unleashed.raid;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.faction.FactionType;
import net.rai.scp_unleashed.faction.FactionWorld;
import net.rai.scp_unleashed.faction.Factions;
import net.rai.scp_unleashed.raid.base.BaseSignals;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class RaidEvents {
    private RaidEvents() {
    }

    private static RaidMark markOf(Entity Body) {
        return Body.hasData(RaidRegistry.Mark) ? Body.getData(RaidRegistry.Mark) : RaidMark.None;
    }

    @Nullable
    private static Raid liveRaid(ServerLevel World, RaidMark Mark) {
        Raid Found = RaidWorld.get(World.getServer()).raid(Mark.Raid());
        return Found != null && Found.live() ? Found : null;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void joined(EntityJoinLevelEvent Event) {
        if (!(Event.getLevel() instanceof ServerLevel World) || !(Event.getEntity() instanceof Mob Raider)) {
            return;
        }
        RaidMark Mark = markOf(Raider);
        if (!Mark.marked()) {
            return;
        }
        Raid Found = liveRaid(World, Mark);
        if (Found == null || !Found.raiders().contains(Raider.getUUID())) {
            FactionType Owner = Factions.factionOf(Raider);
            if (Owner != null) {
                RaidLedger Ledger = RaidWorld.get(World.getServer()).ledger(Owner.id(), FactionWorld.state(World.getServer(), Owner).era());
                for (ItemStack Stack : RaidLoot.takeAll(Raider)) {
                    Ledger.stow(Stack);
                }
            }
            Event.setCanceled(true);
            return;
        }
        RaidContext Context = RaidDirector.context(World.getServer(), Found, World.getServer().overworld().getGameTime());
        if (Context != null) {
            Context.attach(Raider);
        }
    }

    @SubscribeEvent
    public static void left(EntityLeaveLevelEvent Event) {
        if (!(Event.getLevel() instanceof ServerLevel World) || !(Event.getEntity() instanceof Mob Raider)) {
            return;
        }
        RaidMark Mark = markOf(Raider);
        if (!Mark.marked()) {
            return;
        }
        Entity.RemovalReason Reason = Raider.getRemovalReason();
        if (Reason != Entity.RemovalReason.DISCARDED && Reason != Entity.RemovalReason.CHANGED_DIMENSION) {
            return;
        }
        Raid Found = liveRaid(World, Mark);
        if (Found != null) {
            Found.release(Raider.getUUID(), false);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void died(LivingDeathEvent Event) {
        LivingEntity Body = Event.getEntity();
        if (!(Body.level() instanceof ServerLevel World) || !(Body instanceof Mob Raider)) {
            return;
        }
        RaidMark Mark = markOf(Raider);
        if (!Mark.marked()) {
            return;
        }
        RaidLoot.drop(Raider);
        Raid Found = liveRaid(World, Mark);
        if (Found == null || !Found.release(Raider.getUUID(), true)) {
            return;
        }
        RaidContext Context = RaidDirector.context(World.getServer(), Found, World.getServer().overworld().getGameTime());
        if (Context != null) {
            Context.Style().raiderLost(Context, Raider, Event.getSource(), Found.isCaster(Raider.getUUID()));
        }
    }

    @SubscribeEvent
    public static void despawn(MobDespawnEvent Event) {
        Mob Raider = Event.getEntity();
        if (Raider.level() instanceof ServerLevel World && markOf(Raider).marked() && liveRaid(World, markOf(Raider)) != null) {
            Event.setResult(MobDespawnEvent.Result.DENY);
        }
    }

    @SubscribeEvent
    public static void placed(BlockEvent.EntityPlaceEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && Event.getLevel() instanceof ServerLevel World) {
            BaseSignals.placed(User, World, Event.getPos(), Event.getPlacedBlock());
        }
    }

    @SubscribeEvent
    public static void woke(PlayerWakeUpEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            User.getSleepingPos().ifPresent(Bed -> BaseSignals.slept(User, Bed));
        }
    }

    @SubscribeEvent
    public static void spawnSet(PlayerSetSpawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && Event.getNewSpawn() != null && !Event.isForced()) {
            BaseSignals.spawnSet(User, Event.getSpawnLevel(), Event.getNewSpawn());
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        BaseSignals.left(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        BaseSignals.forget();
    }
}
