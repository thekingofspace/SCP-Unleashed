package net.rai.scp_unleashed.control;

import com.mojang.logging.LogUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.control.network.ControlEventPayload;
import net.rai.scp_unleashed.control.network.ControlLookPayload;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class ControlManager {
    public static final double Reach = 6.0;
    public static final double DeathLineRange = 48.0;
    private static final Logger Log = LogUtils.getLogger();
    private static final Map<UUID, DamageSource> PendingDeaths = new HashMap<>();
    private static final Set<UUID> PendingReleases = new HashSet<>();

    private ControlManager() {
    }

    public static boolean hasPendingDeath(ServerPlayer User) {
        return PendingDeaths.containsKey(User.getUUID());
    }

    public static void requestRelease(ServerPlayer User) {
        PendingReleases.add(User.getUUID());
    }

    public static boolean takeRelease(ServerPlayer User) {
        return PendingReleases.remove(User.getUUID());
    }

    public static void markDeath(ServerPlayer User, DamageSource Source) {
        PendingDeaths.putIfAbsent(User.getUUID(), Source);
    }

    public static boolean possess(ServerPlayer User, LivingEntity Target) {
        if (!(Target instanceof Controllable Body)) {
            return false;
        }
        if (User.isSpectator() || !User.isAlive() || User.isSleeping() || hasPendingDeath(User)) {
            refuse(User, "message.scp_unleashed.control.unavailable");
            return false;
        }
        if (Target.level() != User.level() || Target.distanceToSqr(User) > Reach * Reach || !Body.canBeControlledBy(User)) {
            refuse(User, "message.scp_unleashed.control.out_of_reach");
            return false;
        }
        ServerLevel World = User.serverLevel();
        ControlSession Current = ControlSessions.session(User);
        Vec3 From = User.position();
        if (Current.active()) {
            detach(User, Current);
        } else {
            Current = leaveHome(User);
        }
        Body.beforeControl(User);
        Target.stopRiding();
        Target.ejectPassengers();
        UUID BodyId = Target.getUUID();
        ControlSessions.beginTransfer(BodyId);
        try {
            CompoundTag BodyTag = Body.saveBody(World.registryAccess());
            CompoundTag Identity = Body.saveIdentity();
            Component Name = Body.controlName();
            Identity.putString("ControlName", Name.getString());
            ResourceLocation Type = EntityType.getKey(Target.getType());
            Vec3 At = Target.position();
            float Yaw = Target.getYRot();
            float HeadYaw = Target.getYHeadRot();
            float Pitch = Target.getXRot();
            Body.loadBody(BodyState.fresh(1.0f), World.registryAccess());
            Target.discard();
            ControlRoster.get(World.getServer()).possess(BodyId, User.getUUID(), User.getGameProfile().getName());
            User.setData(ControlRegistry.Session.get(), Current.withBody(Type, BodyId, Identity));
            BodyState.apply(User, BodyTag);
            User.teleportTo(World, At.x, At.y, At.z, Yaw, Pitch);
            User.setYHeadRot(HeadYaw);
            User.setYBodyRot(Yaw);
            User.setDeltaMovement(Vec3.ZERO);
            User.fallDistance = 0.0f;
            ControlResync.afflictions(User);
            broadcastLook(User);
            event(User, ControlEventPayload.Linked);
            play(World, At, ControlRegistry.LinkLayers, 1.0f);
            World.sendParticles(ParticleTypes.REVERSE_PORTAL, From.x, From.y + 1.0, From.z, 24, 0.3, 0.6, 0.3, 0.02);
            World.sendParticles(ParticleTypes.ELECTRIC_SPARK, At.x, At.y + 1.0, At.z, 18, 0.3, 0.6, 0.3, 0.05);
            User.displayClientMessage(Component.translatable("message.scp_unleashed.control.linked", Name).withStyle(ChatFormatting.AQUA), true);
        } finally {
            ControlSessions.endTransfer(BodyId);
        }
        return true;
    }

    private static ControlSession leaveHome(ServerPlayer User) {
        ServerLevel World = User.serverLevel();
        User.closeContainer();
        User.stopRiding();
        CompoundTag Home = BodyState.capture(User);
        User.getInventory().clearContent();
        VacantBody Seat = VacantBody.create(User);
        Optional<UUID> SeatId = Optional.empty();
        if (World.addFreshEntity(Seat)) {
            SeatId = Optional.of(Seat.getUUID());
            ControlRoster.get(World.getServer()).setVacant(User.getUUID(), Seat.getUUID());
        }
        return new ControlSession(Home, World.dimension().location(), User.getX(), User.getY(), User.getZ(), User.getYRot(), User.getXRot(), SeatId,
                ControlSession.NoBody, Util.NIL_UUID, new CompoundTag(), World.getGameTime());
    }

    private static void detach(ServerPlayer User, ControlSession Session) {
        User.closeContainer();
        Entity Vehicle = User.getVehicle();
        User.stopRiding();
        CompoundTag BodyTag = BodyState.capture(User);
        User.getInventory().clearContent();
        BodyAttachments.clear(User);
        LivingEntity Body = writeBack(User.serverLevel(), Session, BodyTag, User.position(), User.getYRot(), User.getYHeadRot(), User.getXRot(), false);
        if (Body != null && Vehicle != null && Vehicle.isAlive() && Vehicle.level() == Body.level()) {
            Body.startRiding(Vehicle, true);
        }
        play(User.serverLevel(), User.position(), ControlRegistry.UnlinkLayers, 1.0f);
    }

    @Nullable
    private static LivingEntity writeBack(ServerLevel World, ControlSession Session, CompoundTag BodyTag, Vec3 At, float Yaw, float HeadYaw, float Pitch, boolean Dying) {
        ControlRoster Roster = ControlRoster.get(World.getServer());
        Optional<EntityType<?>> Type = BuiltInRegistries.ENTITY_TYPE.getOptional(Session.BodyType());
        Entity Made = Type.map(Found -> Found.create(World)).orElse(null);
        if (!(Made instanceof Controllable Body) || !(Made instanceof LivingEntity Living)) {
            Log.warn("Control body type {} could not be recreated; dropping its items", Session.BodyType());
            BodyState.dropItems(World, At, BodyTag);
            Roster.dead(Session.BodyId());
            return null;
        }
        Body.loadIdentity(Session.Identity());
        Body.loadBody(BodyTag, World.registryAccess());
        Living.setUUID(Session.BodyId());
        Body.setControlIncarnation(Dying ? Roster.incarnation(Session.BodyId()) : Roster.free(Session.BodyId()));
        Living.moveTo(At.x, At.y, At.z, Yaw, Pitch);
        Living.setYHeadRot(HeadYaw);
        Living.setYBodyRot(Yaw);
        ControlSessions.beginTransfer(Session.BodyId());
        boolean Added;
        try {
            Added = World.addFreshEntity(Living);
            if (!Added) {
                UUID Spare = UUID.randomUUID();
                Living.setUUID(Spare);
                ControlSessions.beginTransfer(Spare);
                try {
                    Added = World.addFreshEntity(Living);
                } finally {
                    ControlSessions.endTransfer(Spare);
                }
            }
        } finally {
            ControlSessions.endTransfer(Session.BodyId());
        }
        if (!Added) {
            Log.warn("Control body {} could not be placed back into the world; dropping its items", Session.BodyId());
            BodyState.dropItems(World, At, BodyTag);
            Roster.dead(Session.BodyId());
            return null;
        }
        return Living;
    }

    public static void release(ServerPlayer User) {
        ControlSession Session = ControlSessions.session(User);
        if (!Session.active()) {
            return;
        }
        if (hasPendingDeath(User)) {
            processDeath(User);
            return;
        }
        detach(User, Session);
        goHome(User, Session);
        event(User, ControlEventPayload.Released);
        User.displayClientMessage(Component.translatable("message.scp_unleashed.control.released").withStyle(ChatFormatting.GRAY), true);
    }

    public static void processDeath(ServerPlayer User) {
        processDeath(User, Body -> {
        });
    }

    @Nullable
    public static LivingEntity processDeath(ServerPlayer User, Consumer<LivingEntity> Restored) {
        DamageSource Source = PendingDeaths.remove(User.getUUID());
        ControlSession Session = ControlSessions.session(User);
        if (!Session.active()) {
            return null;
        }
        if (Source == null) {
            Source = User.damageSources().generic();
        }
        ServerLevel World = User.serverLevel();
        User.closeContainer();
        User.stopRiding();
        Vec3 At = User.position();
        CompoundTag BodyTag = BodyState.capture(User);
        User.getInventory().clearContent();
        BodyAttachments.clear(User);
        User.removeAllEffects();
        User.clearFire();
        LivingEntity Corpse = writeBack(World, Session, BodyTag, At, User.getYRot(), User.getYHeadRot(), User.getXRot(), true);
        ControlRoster Roster = ControlRoster.get(World.getServer());
        Component Line;
        if (Corpse != null) {
            Line = Source.getLocalizedDeathMessage(Corpse);
            Restored.accept(Corpse);
            ControlSessions.beginTransfer(Corpse.getUUID());
            try {
                Corpse.setHealth(0.0f);
                Corpse.die(Source);
            } finally {
                ControlSessions.endTransfer(Corpse.getUUID());
            }
            if (Corpse.isDeadOrDying() || Corpse.isRemoved()) {
                Roster.dead(Session.BodyId());
            } else {
                Roster.free(Session.BodyId());
            }
        } else {
            Line = Component.translatable("message.scp_unleashed.control.body_lost");
        }
        World.sendParticles(ParticleTypes.SMOKE, At.x, At.y + 1.0, At.z, 20, 0.3, 0.6, 0.3, 0.02);
        play(World, At, ControlRegistry.SeveredLayers, 1.0f);
        Component Shown = Line.copy().withStyle(ChatFormatting.GRAY);
        for (ServerPlayer Nearby : World.players()) {
            if (Nearby != User && Nearby.distanceToSqr(At) <= DeathLineRange * DeathLineRange) {
                Nearby.sendSystemMessage(Shown);
            }
        }
        goHome(User, Session);
        User.sendSystemMessage(Shown);
        event(User, ControlEventPayload.Severed);
        User.displayClientMessage(Component.translatable("message.scp_unleashed.control.severed").withStyle(ChatFormatting.RED), true);
        return Corpse;
    }

    public static void abandon(ServerPlayer User) {
        PendingDeaths.remove(User.getUUID());
        ControlSession Session = ControlSessions.session(User);
        if (!Session.active()) {
            return;
        }
        ControlRoster.get(User.getServer()).dead(Session.BodyId());
        User.getInventory().clearContent();
        BodyAttachments.clear(User);
        goHome(User, Session);
        event(User, ControlEventPayload.Severed);
    }

    private static void goHome(ServerPlayer User, ControlSession Session) {
        MinecraftServer Server = User.getServer();
        ControlRoster Roster = ControlRoster.get(Server);
        ServerLevel Home = Server.getLevel(Session.homeLevel());
        Vec3 Target = new Vec3(Session.HomeX(), Session.HomeY(), Session.HomeZ());
        float Yaw = Session.HomeYaw();
        float Pitch = Session.HomePitch();
        VacantBody Seat = Session.Vacant().map(Id -> findSeat(Server, Id)).orElse(null);
        if (Seat != null && Seat.level() instanceof ServerLevel SeatLevel) {
            Home = SeatLevel;
            Target = Seat.position();
            Yaw = Seat.getYRot();
            Seat.discard();
        }
        if (Home == null) {
            Home = Server.overworld();
            Target = Vec3.atBottomCenterOf(Home.getSharedSpawnPos());
        }
        Roster.clearVacant(User.getUUID());
        Home.getChunkAt(BlockPos.containing(Target));
        BodyState.apply(User, Session.Home());
        User.removeData(ControlRegistry.Session.get());
        User.stopRiding();
        User.teleportTo(Home, Target.x, Target.y, Target.z, Yaw, Pitch);
        User.setDeltaMovement(Vec3.ZERO);
        User.fallDistance = 0.0f;
        User.setYHeadRot(Yaw);
        ControlResync.afflictions(User);
        broadcastLook(User);
        play(Home, Target, ControlRegistry.UnlinkLayers, 0.85f);
    }

    @Nullable
    private static VacantBody findSeat(MinecraftServer Server, UUID Id) {
        for (ServerLevel World : Server.getAllLevels()) {
            if (World.getEntity(Id) instanceof VacantBody Seat && !Seat.isRemoved()) {
                return Seat;
            }
        }
        return null;
    }

    public static void releaseAll(MinecraftServer Server) {
        for (ServerPlayer User : List.copyOf(Server.getPlayerList().getPlayers())) {
            if (hasPendingDeath(User)) {
                processDeath(User);
            } else {
                release(User);
            }
        }
        PendingDeaths.clear();
        PendingReleases.clear();
    }

    public static void broadcastLook(ServerPlayer User) {
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(User, lookOf(User));
    }

    public static ControlLookPayload lookOf(ServerPlayer User) {
        ControlSession Session = ControlSessions.session(User);
        if (!Session.active()) {
            return new ControlLookPayload(User.getUUID(), false, ControlSession.NoBody, new CompoundTag());
        }
        return new ControlLookPayload(User.getUUID(), true, Session.BodyType(), Session.Identity());
    }

    private static void event(ServerPlayer User, byte Kind) {
        if (User.connection != null) {
            PacketDistributor.sendToPlayer(User, new ControlEventPayload(Kind));
        }
    }

    private static void refuse(ServerPlayer User, String Key) {
        User.displayClientMessage(Component.translatable(Key).withStyle(ChatFormatting.RED), true);
        User.playNotifySound(ControlRegistry.Refuse.get(), SoundSource.PLAYERS, 0.6f, 1.0f);
    }

    public static void play(ServerLevel World, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Pitch) {
        float Varied = Pitch * (0.95f + World.getRandom().nextFloat() * 0.1f);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, At.x, At.y + 1.0, At.z, Layer.get(), SoundSource.PLAYERS, 0.9f, Varied);
        }
    }
}
