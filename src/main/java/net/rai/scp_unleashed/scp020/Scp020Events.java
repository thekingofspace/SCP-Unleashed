package net.rai.scp_unleashed.scp020;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp020.client.Scp020ClientState;
import net.rai.scp_unleashed.scp020.client.Scp020SporeSight;
import net.rai.scp_unleashed.scp020.network.Scp020SporesPayload;
import net.rai.scp_unleashed.scp020.network.Scp020StatePayload;

import java.util.List;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp020Events {
    private Scp020Events() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp020StatePayload.PayloadType, Scp020StatePayload.PayloadCodec, (Payload, Context) -> Scp020ClientState.apply(Payload));
        Registrar.playToClient(Scp020SporesPayload.PayloadType, Scp020SporesPayload.PayloadCodec, (Payload, Context) -> Context.enqueueWork(() -> Scp020SporeSight.apply(Payload)));
    }

    @SubscribeEvent
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpItems.getKey()) {
            Event.accept(Scp020Registry.Goggles.get());
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        Scp020Spores.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void levelTick(LevelTickEvent.Post Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            Scp020Migration.tick(World);
            Scp020Mold.tick(World);
        }
    }

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load Event) {
        if (Event.getLevel() instanceof ServerLevel World && Event.getChunk() instanceof LevelChunk Chunk) {
            Scp020Infection.chunkLoaded(World, Chunk);
            Scp020Migration.chunkLoaded(World, Chunk);
        }
    }

    @SubscribeEvent
    public static void chunkUnloaded(ChunkEvent.Unload Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            Scp020Infection.chunkUnloaded(World, Event.getChunk().getPos());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void blockBroken(BlockEvent.BreakEvent Event) {
        if (!Event.isCanceled() && Event.getLevel() instanceof ServerLevel World) {
            Scp020Mold.broken(World, Event.getPos());
        }
    }

    @SubscribeEvent
    public static void blockChanged(BlockEvent.NeighborNotifyEvent Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            Scp020Infection.prune(World, Event.getPos());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void exploded(ExplosionEvent.Detonate Event) {
        if (Event.getLevel() instanceof ServerLevel World) {
            for (BlockPos Pos : Event.getAffectedBlocks()) {
                Scp020Mold.broken(World, Pos);
            }
        }
    }

    @SubscribeEvent
    public static void serverStopped(ServerStoppedEvent Event) {
        Scp020Infection.clearAll();
        Scp020Migration.clearAll();
        Scp020Spores.clearAll();
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User)) {
            return;
        }
        Scp020Affliction.tick(User);
        if (Math.floorMod(User.serverLevel().getGameTime() + User.getId(), Scp020Mold.BreatheTicks) == 0) {
            Scp020Mold.breathe(User);
        }
    }

    @SubscribeEvent
    public static void entityTick(EntityTickEvent.Post Event) {
        Entity Ticked = Event.getEntity();
        if (!Ticked.level().isClientSide) {
            visit(Ticked);
        }
    }

    private static void visit(Entity Ticked) {
        if (Ticked instanceof LivingEntity Subject && !(Ticked instanceof Player) && Scp020Affliction.isAfflicted(Subject)) {
            Scp020Affliction.tick(Subject);
        }
        if (Ticked.isVehicle()) {
            for (Entity Rider : List.copyOf(Ticked.getPassengers())) {
                visit(Rider);
            }
        }
    }

    @SubscribeEvent
    public static void loggedIn(PlayerEvent.PlayerLoggedInEvent Event) {
        resend(Event.getEntity());
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        Scp020Spores.forget(Event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        resend(Event.getEntity());
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        resend(Event.getEntity());
    }

    private static void resend(Player Target) {
        if (Target instanceof ServerPlayer User) {
            Scp020Affliction.sync(User);
            Scp020Spores.forget(User.getUUID());
        }
    }
}
