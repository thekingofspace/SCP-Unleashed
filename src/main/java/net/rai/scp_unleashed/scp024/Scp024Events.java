package net.rai.scp_unleashed.scp024;

import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingExperienceDropEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp024.client.Scp024ClientHooks;
import net.rai.scp_unleashed.scp024.network.Scp024BarPayload;
import net.rai.scp_unleashed.scp024.network.Scp024BurstPayload;
import net.rai.scp_unleashed.scp024.network.Scp024ClearPayload;
import net.rai.scp_unleashed.scp024.network.Scp024CuePayload;
import net.rai.scp_unleashed.scp024.network.Scp024DialoguePayload;
import net.rai.scp_unleashed.scp024.network.Scp024QuipPayload;
import net.rai.scp_unleashed.scp024.network.Scp024ReplyPayload;
import org.slf4j.Logger;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp024Events {
    public static final Logger Log = LogUtils.getLogger();
    private static final Map<UUID, Placement> Pending = new ConcurrentHashMap<>();

    private Scp024Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp024DialoguePayload.PayloadType, Scp024DialoguePayload.PayloadCodec, (Payload, Context) -> Scp024ClientHooks.dialogue(Payload));
        Registrar.playToClient(Scp024QuipPayload.PayloadType, Scp024QuipPayload.PayloadCodec, (Payload, Context) -> Scp024ClientHooks.quip(Payload));
        Registrar.playToClient(Scp024CuePayload.PayloadType, Scp024CuePayload.PayloadCodec, (Payload, Context) -> Scp024ClientHooks.cue(Payload));
        Registrar.playToClient(Scp024BarPayload.PayloadType, Scp024BarPayload.PayloadCodec, (Payload, Context) -> Scp024ClientHooks.bar(Payload));
        Registrar.playToClient(Scp024ClearPayload.PayloadType, Scp024ClearPayload.PayloadCodec, (Payload, Context) -> Scp024ClientHooks.clear(Payload));
        Registrar.playToClient(Scp024BurstPayload.PayloadType, Scp024BurstPayload.PayloadCodec, (Payload, Context) -> Scp024ClientHooks.burst(Payload));
        Registrar.playToServer(Scp024ReplyPayload.PayloadType, Scp024ReplyPayload.PayloadCodec, (Payload, Context) -> {
            if (Context.player() instanceof ServerPlayer User) {
                Scp024Runs.reply(User, Payload.Token());
            }
        });
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        Event.put(Scp024Registry.Host.get(), Scp024Host.createAttributes().build());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp024Registry.DollHairs.get());
            Event.accept(Scp024Registry.Ribbon.get());
        }
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp024Registry.StageDoorItem.get());
            Event.accept(Scp024Registry.ExitDoorItem.get());
            for (DeferredItem<? extends Item> Registered : Scp024Registry.BlockItems) {
                Event.accept(Registered.get());
            }
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        Scp024Runs.tick(Event.getServer());
        Scp024Relocation.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void serverStopping(ServerStoppingEvent Event) {
        Scp024Runs.stopping(Event.getServer());
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp024Runs.loggedOut(User);
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp024Runs.settle(User);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp024Runs.settle(User);
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && Event.getFrom() == Scp024Stage.Key && Event.getTo() != Scp024Stage.Key) {
            Scp024Runs.leftStage(User);
        }
    }

    @SubscribeEvent
    public static void serverStarted(ServerStartedEvent Event) {
        Scp024Runs.started(Event.getServer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void died(LivingDeathEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User && (Scp024Stage.inStage(User) || Scp024Runs.runOf(User) != null)) {
            Placement Spot = Scp024Stage.inStage(User) ? lostAndFound(User) : null;
            if (Spot != null) {
                Pending.put(User.getUUID(), Spot);
            }
            Scp024Runs.died(User);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void drops(LivingDropsEvent Event) {
        Entity Dead = Event.getEntity();
        if (!Scp024Stage.inStage(Dead)) {
            return;
        }
        if (Scp024Stage.isOwned(Dead)) {
            Event.setCanceled(true);
            return;
        }
        Event.getDrops().removeIf(Drop -> Scp024Session.isProp(Drop.getItem()));
        Placement Spot;
        if (Dead instanceof ServerPlayer User) {
            Spot = Pending.containsKey(User.getUUID()) ? Pending.get(User.getUUID()) : lostAndFound(User);
        } else {
            Scp024Relocation.Spot Where = Scp024Relocation.outside(Dead.getServer());
            Spot = new Placement(Where.World(), Where.Pos());
        }
        if (Spot == null) {
            return;
        }
        for (ItemEntity Drop : Event.getDrops()) {
            ItemEntity Moved = new ItemEntity(Spot.World, Spot.Pos.x, Spot.Pos.y + 0.3, Spot.Pos.z, Drop.getItem().copy());
            Moved.setDefaultPickUpDelay();
            Spot.World.addFreshEntity(Moved);
        }
        Event.getDrops().clear();
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void experience(LivingExperienceDropEvent Event) {
        Entity Dead = Event.getEntity();
        if (!Scp024Stage.inStage(Dead)) {
            return;
        }
        if (Scp024Stage.isOwned(Dead)) {
            Event.setCanceled(true);
            return;
        }
        if (Dead instanceof ServerPlayer User && Event.getDroppedExperience() > 0) {
            Placement Spot = Pending.remove(User.getUUID());
            if (Spot != null) {
                ExperienceOrb.award(Spot.World, Spot.Pos, Event.getDroppedExperience());
                Event.setCanceled(true);
            }
        }
    }

    private record Placement(ServerLevel World, Vec3 Pos) {
    }

    private static Placement lostAndFound(ServerPlayer User) {
        Scp024Runs.Outside Spot = Scp024Runs.outside(User);
        return new Placement(Spot.World(), Spot.Pos());
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent Event) {
        Level World = Event.getLevel();
        if (World.isClientSide || !Scp024Stage.inStage(World)) {
            return;
        }
        Entity Target = Event.getEntity();
        if (Scp024Stage.isOwned(Target)) {
            UUID Owner = Scp024Stage.owner(Target);
            if (!Scp024Runs.isLive(Owner)) {
                Event.setCanceled(true);
            }
            return;
        }
        if (Target instanceof AbstractArrow Arrow && Arrow.getOwner() instanceof ServerPlayer) {
            Arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
        }
        if (Scp024Relocation.belongsOutside(Target)) {
            Scp024Relocation.queue(Target);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void targeting(LivingChangeTargetEvent Event) {
        LivingEntity Hunter = Event.getEntity();
        if (Hunter.level().isClientSide || !Scp024Stage.isOwned(Hunter) || !Scp024Stage.inStage(Hunter)
                || !(Event.getNewAboutToBeSetTarget() instanceof Player Target)) {
            return;
        }
        if (!Target.getUUID().equals(Scp024Runs.contestantOf(Scp024Stage.owner(Hunter)))) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void impact(ProjectileImpactEvent Event) {
        Projectile Shot = Event.getProjectile();
        if (Shot.level().isClientSide || !Scp024Stage.inStage(Shot)) {
            return;
        }
        HitResult Hit = Event.getRayTraceResult();
        if (Hit instanceof BlockHitResult Block && Hit.getType() == HitResult.Type.BLOCK && Shot.level().getServer() != null) {
            BlockPos Pos = Block.getBlockPos();
            Scp024Runs.projectileHit(Shot.level().getServer(), Shot, Pos);
        }
    }
}
