package net.rai.scp_unleashed.scp323;

import net.minecraft.Util;
import net.minecraft.network.protocol.game.ClientboundSetCarriedItemPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.scp323.network.Scp323WatchPayload;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Scp323Watch {
    public static final int CameraDelay = 3;
    public static final int CameraRetry = 40;
    private static final Map<UUID, Integer> Slots = new HashMap<>();
    private static final Map<UUID, Integer> LastCamera = new HashMap<>();

    private Scp323Watch() {
    }

    public static boolean isWatching(@Nullable Entity Target) {
        if (!(Target instanceof Player User)) {
            return false;
        }
        UUID Id = User.getExistingDataOrNull(Scp323Registry.Watch.get());
        return Id != null && !Util.NIL_UUID.equals(Id);
    }

    @Nullable
    public static UUID watched(Player User) {
        UUID Id = User.getExistingDataOrNull(Scp323Registry.Watch.get());
        return Id == null || Util.NIL_UUID.equals(Id) ? null : Id;
    }

    public static void start(ServerPlayer User, Scp323Beast Beast) {
        if (User.containerMenu != User.inventoryMenu) {
            User.closeContainer();
        }
        if (User.isUsingItem()) {
            User.stopUsingItem();
        }
        User.stopRiding();
        if (User.isSleeping()) {
            User.stopSleepInBed(true, true);
        }
        User.setData(Scp323Registry.Watch.get(), Beast.getUUID());
        Slots.put(User.getUUID(), User.getInventory().selected);
        LastCamera.remove(User.getUUID());
        User.setInvisible(true);
        User.setDeltaMovement(Vec3.ZERO);
        Scp323Sync.watch(User, Beast.getId());
    }

    public static void stop(ServerPlayer User) {
        boolean Had = User.hasData(Scp323Registry.Watch.get());
        User.removeData(Scp323Registry.Watch.get());
        Slots.remove(User.getUUID());
        LastCamera.remove(User.getUUID());
        if (User.getCamera() != User) {
            User.setCamera(User);
        }
        User.setInvisible(User.hasEffect(MobEffects.INVISIBILITY));
        if (Had) {
            User.fallDistance = 0.0f;
            User.setDeltaMovement(Vec3.ZERO);
            if (User.level() instanceof ServerLevel World && User.connection != null && !User.hasDisconnected()) {
                User.teleportTo(World, User.getX(), User.getY(), User.getZ(), User.getYRot(), User.getXRot());
            }
        }
        Scp323Sync.watch(User, Scp323WatchPayload.Nobody);
    }

    public static void tick(ServerPlayer User) {
        UUID Id = watched(User);
        if (Id == null) {
            return;
        }
        Scp323Bond Bond = Scp323Bond.of(User);
        Scp323Beast Beast = Scp323Hosts.beast(User.server, Id);
        if (!Bond.transformed() || !Id.equals(Bond.Creature()) || Beast == null || !Beast.isAlive() || !User.getUUID().equals(Beast.hostPlayer())
                || ControlSessions.isControlling(User) && !ControlSessions.session(User).BodyId().equals(Beast.hostBody())) {
            if (Beast == null && Bond.transformed()) {
                return;
            }
            stop(User);
            return;
        }
        if (!(Beast.level() instanceof ServerLevel BeastWorld)) {
            return;
        }
        if (User.level() != BeastWorld) {
            User.teleportTo(BeastWorld, Beast.getX(), Beast.getY(), Beast.getZ(), Beast.getYRot(), Beast.getXRot());
            LastCamera.remove(User.getUUID());
            return;
        }
        if (User.containerMenu != User.inventoryMenu) {
            User.closeContainer();
        }
        if (User.isUsingItem()) {
            User.stopUsingItem();
        }
        if (User.isPassenger()) {
            User.stopRiding();
        }
        if (User.isSleeping()) {
            User.stopSleepInBed(true, true);
        }
        User.setInvisible(true);
        User.setShiftKeyDown(false);
        User.setSprinting(false);
        User.fallDistance = 0.0f;
        User.clearFire();
        User.setAirSupply(User.getMaxAirSupply());
        Integer Slot = Slots.get(User.getUUID());
        Inventory Pockets = User.getInventory();
        if (Slot == null) {
            Slots.put(User.getUUID(), Pockets.selected);
        } else if (Pockets.selected != Slot) {
            Pockets.selected = Slot;
            if (User.connection != null) {
                User.connection.send(new ClientboundSetCarriedItemPacket(Slot));
            }
        }
        User.absMoveTo(Beast.getX(), Beast.getY(), Beast.getZ(), Beast.getYHeadRot(), Beast.getXRot());
        User.setDeltaMovement(Vec3.ZERO);
        BeastWorld.getChunkSource().move(User);
        if (Beast.tickCount >= CameraDelay) {
            Integer Last = LastCamera.get(User.getUUID());
            if (User.getCamera() != Beast && (Last == null || User.tickCount - Last >= CameraRetry || User.tickCount < Last)) {
                LastCamera.put(User.getUUID(), User.tickCount);
                User.setCamera(Beast);
                Scp323Sync.watch(User, Beast.getId());
            }
        }
    }

    public static void forget(UUID Id) {
        Slots.remove(Id);
        LastCamera.remove(Id);
    }

    public static void clear() {
        Slots.clear();
        LastCamera.clear();
    }
}
