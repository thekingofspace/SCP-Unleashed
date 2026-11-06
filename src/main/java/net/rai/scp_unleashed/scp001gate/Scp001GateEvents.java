package net.rai.scp_unleashed.scp001gate;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.item.BlockItem;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp001gate.client.Scp001GateClientHooks;
import net.rai.scp_unleashed.scp001gate.network.Scp001GateStrikePayload;
import net.rai.scp_unleashed.scp001gate.network.Scp001GateWarnPayload;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp001GateEvents {
    private Scp001GateEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        PayloadRegistrar Registrar = Event.registrar("1");
        Registrar.playToClient(Scp001GateWarnPayload.PayloadType, Scp001GateWarnPayload.PayloadCodec, (Payload, Context) -> Scp001GateClientHooks.warn(Payload));
        Registrar.playToClient(Scp001GateStrikePayload.PayloadType, Scp001GateStrikePayload.PayloadCodec, (Payload, Context) -> Scp001GateClientHooks.strike(Payload));
    }

    @SubscribeEvent
    public static void chunkLoaded(ChunkEvent.Load Event) {
        if (Event.getLevel() instanceof ServerLevel World && World.dimension() == Level.OVERWORLD) {
            Scp001GateEmitter.chunkLoaded(World, Event.getChunk().getPos());
        }
    }

    @SubscribeEvent
    public static void serverTick(ServerTickEvent.Post Event) {
        Scp001GateEmitter.tick(Event.getServer());
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent Event) {
        Scp001GateEmitter.clear();
        Scp001GateZones.reset();
    }

    @SubscribeEvent
    public static void joined(EntityJoinLevelEvent Event) {
        if (!Event.getLevel().isClientSide()) {
            Scp001GateAvoid.install(Event.getEntity());
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void spawning(FinalizeSpawnEvent Event) {
        MobSpawnType Type = Event.getSpawnType();
        if (Scp001GateGarden.barred(Type) && Scp001GateGarden.covers(Event.getLevel().getLevel(), Event.getX(), Event.getZ())) {
            Event.setSpawnCancelled(true);
            return;
        }
        if (Type != MobSpawnType.NATURAL && Type != MobSpawnType.CHUNK_GENERATION && Type != MobSpawnType.PATROL && Type != MobSpawnType.EVENT) {
            return;
        }
        if (!(Event.getLevel().getLevel() instanceof ServerLevel World)) {
            return;
        }
        if (Scp001GateZones.killing(World, new Vec3(Event.getX(), Event.getY(), Event.getZ()))) {
            Event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent
    public static void entityTicked(EntityTickEvent.Post Event) {
        Entity Body = Event.getEntity();
        if (Body.level().isClientSide() || !Scp001GateZones.live(Body.level())) {
            return;
        }
        Scp001GateRepel.hold(Body);
    }

    @SubscribeEvent
    public static void teleporting(EntityTeleportEvent Event) {
        if (Event.getEntity().level().isClientSide() || !Scp001GateAvoid.avoids(Event.getEntity())) {
            return;
        }
        if (Scp001GateAvoid.blocked(Event.getEntity().level(), new Vec3(Event.getTargetX(), Event.getTargetY(), Event.getTargetZ()))) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void playerTick(PlayerTickEvent.Post Event) {
        if (!(Event.getEntity() instanceof ServerPlayer User) || !User.isSpectator() || User.tickCount % 2 != 0) {
            return;
        }
        ServerLevel World = User.serverLevel();
        BlockPos Feet = User.blockPosition();
        for (BlockPos At : new BlockPos[]{Feet, Feet.above()}) {
            if (World.getBlockState(At).is(Scp001GateRegistry.Portal.get())) {
                Scp001GateGarden.passThrough(User, World, At);
                return;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            for (DeferredItem<BlockItem> Entry : Scp001GateRegistry.TabBlocks) {
                Event.accept(Entry.get());
            }
        }
    }
}
