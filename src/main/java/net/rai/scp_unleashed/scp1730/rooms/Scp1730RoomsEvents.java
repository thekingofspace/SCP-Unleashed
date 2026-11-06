package net.rai.scp_unleashed.scp1730.rooms;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.PlayLevelSoundEvent;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.registry.ModCreativeTabs;
import net.rai.scp_unleashed.scp1730.Scp1730State;
import net.rai.scp_unleashed.scp1730.Scp1730World;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = Scp_unleashed.ModId)
public final class Scp1730RoomsEvents {
    public static final int SyncEvery = 10;
    private static final Map<UUID, Long> Sent = new ConcurrentHashMap<>();

    private Scp1730RoomsEvents() {
    }

    @SubscribeEvent
    public static void registerPayloads(RegisterPayloadHandlersEvent Event) {
        Event.registrar("1").playToClient(Scp1730RoomsPayload.PayloadType, Scp1730RoomsPayload.PayloadCodec,
                (Payload, Context) -> Scp1730SpecialRooms.acceptClient(Payload.dimension(), Payload.Rooms()));
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void buildTabs(BuildCreativeModeTabContentsEvent Event) {
        if (Event.getTabKey() == ModCreativeTabs.ScpBlocks.getKey()) {
            Event.accept(new ItemStack(Scp1730RoomsRegistry.TelevisionItem.get()));
            Event.accept(new ItemStack(Scp1730RoomsRegistry.AcousticFoamItem.get()));
            Event.accept(new ItemStack(Scp1730RoomsRegistry.MicStandItem.get()));
            Event.accept(new ItemStack(Scp1730RoomsRegistry.SpeakerCabinetItem.get()));
            Event.accept(new ItemStack(Scp1730RoomsRegistry.StudioMonitorItem.get()));
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post Event) {
        MinecraftServer Server = Event.getServer();
        ServerLevel Facility = Scp1730World.level(Server);
        for (ServerLevel World : Server.getAllLevels()) {
            if (World == Facility || !Scp1730SpecialRooms.of(World).isEmpty()) {
                Scp1730RoomRules.tick(World);
            }
        }
        if (Server.getTickCount() % SyncEvery != 0) {
            return;
        }
        for (ServerPlayer User : Server.getPlayerList().getPlayers()) {
            sync(User, false);
        }
    }

    public static long signature(ServerPlayer User) {
        Level World = User.level();
        long Base = World.dimension().location().hashCode() * 31L + Scp1730SpecialRooms.pinVersion();
        if (World instanceof ServerLevel Server && Scp1730World.inside(Server)) {
            Base = Base * 31L + Scp1730State.get(Server.getServer()).run() + (Scp1730World.standingIn() ? 7L : 0L);
        }
        return Base;
    }

    public static void sync(ServerPlayer User, boolean Force) {
        long Signature = signature(User);
        Long Before = Sent.get(User.getUUID());
        if (!Force && Before != null && Before == Signature) {
            return;
        }
        Sent.put(User.getUUID(), Signature);
        List<Scp1730SpecialRoom> Rooms = Scp1730SpecialRooms.of(User.level());
        PacketDistributor.sendToPlayer(User, new Scp1730RoomsPayload(User.level().dimension().location().toString(), Rooms));
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void soundAtPosition(PlayLevelSoundEvent.AtPosition Event) {
        Vec3 At = Event.getPosition();
        if (Scp1730SpecialRooms.silentAt(Event.getLevel(), At.x, At.y, At.z)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void soundAtEntity(PlayLevelSoundEvent.AtEntity Event) {
        Entity Source = Event.getEntity();
        if (Source != null && Scp1730SpecialRooms.silenced(Source)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void gameEvent(VanillaGameEvent Event) {
        Vec3 At = Event.getEventPosition();
        if (Scp1730SpecialRooms.silentAt(Event.getLevel(), At.x, At.y, At.z)) {
            Event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void fall(LivingFallEvent Event) {
        if (Event.getEntity().level().isClientSide) {
            return;
        }
        if (Scp1730RoomRules.cushioned(Event.getEntity())) {
            Event.setDistance(0.0f);
            Event.setDamageMultiplier(0.0f);
            Scp1730RoomRules.landed(Event.getEntity());
        }
    }

    @SubscribeEvent
    public static void loggedOut(PlayerEvent.PlayerLoggedOutEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp1730RoomRules.forget(User);
            Sent.remove(User.getUUID());
        }
    }

    @SubscribeEvent
    public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp1730RoomRules.forget(User);
            sync(User, true);
        }
    }

    @SubscribeEvent
    public static void respawned(PlayerEvent.PlayerRespawnEvent Event) {
        if (Event.getEntity() instanceof ServerPlayer User) {
            Scp1730RoomRules.forget(User);
            sync(User, true);
        }
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent Event) {
        Scp1730RoomRules.reset();
        Scp1730SpecialRooms.clear();
        Sent.clear();
    }
}
