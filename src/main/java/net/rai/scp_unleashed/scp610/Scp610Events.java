package net.rai.scp_unleashed.scp610;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.AnomalyCure;
import net.rai.scp_unleashed.faction.FactionControl;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp610.client.Scp610ClientHooks;
import net.rai.scp_unleashed.scp610.network.Scp610DialoguePayload;
import net.rai.scp_unleashed.scp610.network.Scp610ReplyPayload;
import net.rai.scp_unleashed.scp610.network.Scp610StatePayload;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp610Events {
    private Scp610Events() {
    }

    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent Event) {
        for (Scp610Form Form : Scp610Form.values()) {
            Event.put(Scp610Registry.typeOf(Form), Scp610Creature.attributes(Form).build());
        }
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp610StatePayload.PayloadType, Scp610StatePayload.PayloadCodec, (Payload, Context) -> Scp610ClientHooks.state(Payload));
        Registrar.playToClient(Scp610DialoguePayload.PayloadType, Scp610DialoguePayload.PayloadCodec, (Payload, Context) -> Scp610ClientHooks.dialogue(Payload));
        Registrar.playToServer(Scp610ReplyPayload.PayloadType, Scp610ReplyPayload.PayloadCodec, Scp610ReplyPayload::handle);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            for (DeferredItem<DeferredSpawnEggItem> Egg : Scp610Registry.eggs()) {
                Event.accept(Egg.get());
            }
        } else if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(Scp610Registry.FleshItem.get());
            Event.accept(Scp610Registry.VeinedFleshItem.get());
            Event.accept(Scp610Registry.WoundItem.get());
            Event.accept(Scp610Registry.PustuleItem.get());
            Event.accept(Scp610Registry.SinewItem.get());
            Event.accept(Scp610Registry.TendrilsItem.get());
        } else if (Event.getTabKey() == ModCreativeTabs.Machines.getKey()) {
            Event.accept(Scp610Registry.CommunicatorItem.get());
        }
    }

    @SubscribeEvent
    public static void tickInfected(EntityTickEvent.Post Event) {
        Entity Ticked = Event.getEntity();
        if (!Ticked.level().isClientSide) {
            visit(Ticked);
        }
    }

    private static void visit(Entity Ticked) {
        if (Ticked instanceof LivingEntity Subject && Scp610Infection.isInfected(Subject)) {
            Scp610Infection.tick(Subject);
        }
        if (Ticked.isVehicle()) {
            for (Entity Rider : List.copyOf(Ticked.getPassengers())) {
                visit(Rider);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDeath(LivingDeathEvent Event) {
        LivingEntity Fallen = Event.getEntity();
        if (Event.isCanceled() || !(Fallen.level() instanceof ServerLevel World)) {
            return;
        }
        if (Fallen instanceof ServerPlayer User && !(User instanceof FakePlayer) && Scp610Infection.diedOfIt(User, Event.getSource())) {
            Scp610Creature Risen = Scp610Spawning.raise(World, Scp610Form.Apostle, User.position(), User.getYRot(), true);
            if (Risen != null) {
                Risen.setCustomName(null);
                FactionControl.stamp(Scp610Faction.Instance, Scp610Hives.state(World.getServer()), Risen);
            }
        }
        if (Event.getSource().getEntity() instanceof Scp610Creature Killer) {
            Scp610Biomass.killed(World, Fallen, Killer);
        }
        if (Scp610Infection.isInfected(Fallen)) {
            Scp610Infection.clearEffects(Fallen);
            Scp610Infection.forget(Fallen);
        }
    }

    @SubscribeEvent
    public static void cure(AnomalyCure Event) {
        Event.markCured(Scp610Infection.treat(Event.target()));
    }

    @SubscribeEvent
    public static void startTracking(PlayerEvent.StartTracking Event) {
        if (Event.getTarget() instanceof ServerPlayer Target && Event.getEntity() instanceof ServerPlayer Watcher && Scp610Infection.isInfected(Target)) {
            Scp610Sync.sendTo(Watcher, Target);
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        Scp610Sync.resend(Event.getEntity());
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        Scp610Sync.resend(Event.getEntity());
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        Scp610Sync.resend(Event.getEntity());
    }

    @SubscribeEvent
    public static void broken(BlockEvent.BreakEvent Event) {
        Player User = Event.getPlayer();
        BlockState State = Event.getState();
        if (!(Event.getLevel() instanceof ServerLevel World) || User == null || User.isCreative() || !Scp610Growth.isFlesh(State)) {
            return;
        }
        if (Scp610Hives.ownerOf(World, Event.getPos()) != null) {
            Scp610Pact.fleshBroken(World.getServer(), Scp610Hives.state(World.getServer()));
        }
    }

    @SubscribeEvent
    public static void saving(LevelEvent.Save Event) {
        if (Event.getLevel() instanceof ServerLevel World && World == World.getServer().overworld()) {
            Scp610Hives.flush(World.getServer());
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Scp610Merging.clear();
    }
}
