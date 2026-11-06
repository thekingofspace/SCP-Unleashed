package net.rai.scp_unleashed.scp268;

import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.antimemetic.AntiMemetic;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

public final class Scp268Neglect {
    public static final int SweepInterval = 10;
    public static final double SweepRadius = 48.0;
    public static final float DonVolume = 0.7f;
    public static final float DoffVolume = 0.8f;
    private static final List<MemoryModuleType<? extends LivingEntity>> Fixations = List.of(MemoryModuleType.ATTACK_TARGET, MemoryModuleType.NEAREST_ATTACKABLE,
            MemoryModuleType.NEAREST_VISIBLE_ATTACKABLE_PLAYER, MemoryModuleType.NEAREST_TARGETABLE_PLAYER_NOT_WEARING_GOLD, MemoryModuleType.ROAR_TARGET,
            MemoryModuleType.HURT_BY_ENTITY, MemoryModuleType.AVOID_TARGET);
    private static final List<MemoryModuleType<? extends LivingEntity>> Glances = List.of(MemoryModuleType.NEAREST_VISIBLE_PLAYER, MemoryModuleType.INTERACTION_TARGET);
    private static final Map<ServerPlayer, Boolean> LastState = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Map<ServerPlayer, Boolean> LastSight = Collections.synchronizedMap(new WeakHashMap<>());
    private static final Set<UUID> Unlisted = ConcurrentHashMap.newKeySet();

    private Scp268Neglect() {
    }

    public static void update(ServerPlayer User, boolean Sweep) {
        if (User.isRemoved()) {
            return;
        }
        boolean Now = Scp268.refresh(User);
        Boolean Was = LastState.put(User, Now);
        if (Was == null || Was != Now) {
            User.serverLevel().getChunkSource().move(User);
            if (Now) {
                clearTargets(User);
            }
            if (Was != null) {
                play(User, Now);
                User.getCooldowns().addCooldown(Scp268Registry.Cap.get(), Scp268.SwapCooldown);
            }
        }
        list(User, Now);
        boolean Sees = AntiMemetic.isProtected(User);
        Boolean Saw = LastSight.put(User, Sees);
        if (Saw != null && Saw != Sees) {
            refreshSight(User, Sees);
        }
        if (Now && Sweep && User.tickCount % SweepInterval == 0) {
            clearTargets(User);
        }
    }

    public static boolean isUnlisted(UUID Id) {
        return Unlisted.contains(Id);
    }

    public static void joined(ServerPlayer Joined) {
        if (seesThrough(Joined)) {
            return;
        }
        for (ServerPlayer Other : Joined.server.getPlayerList().getPlayers()) {
            if (Other != Joined && Unlisted.contains(Other.getUUID())) {
                Joined.connection.send(unlisted(Other));
            }
        }
    }

    public static boolean seesThrough(ServerPlayer Viewer) {
        return AntiMemetic.isProtected(Viewer);
    }

    private static void refreshSight(ServerPlayer Viewer, boolean Sees) {
        Viewer.serverLevel().getChunkSource().move(Viewer);
        for (ServerPlayer Other : Viewer.server.getPlayerList().getPlayers()) {
            if (Other != Viewer && Unlisted.contains(Other.getUUID())) {
                Viewer.connection.send(Sees ? listed(Other) : unlisted(Other));
            }
        }
    }

    public static void left(ServerPlayer User) {
        Unlisted.remove(User.getUUID());
        LastState.remove(User);
        LastSight.remove(User);
        Scp268.forget(User);
    }

    public static void reset() {
        Unlisted.clear();
        LastState.clear();
        LastSight.clear();
    }

    private static void list(ServerPlayer User, boolean Hidden) {
        if (Hidden && Unlisted.add(User.getUUID())) {
            ClientboundPlayerInfoUpdatePacket Packet = unlisted(User);
            for (ServerPlayer Other : User.server.getPlayerList().getPlayers()) {
                if (Other != User && !seesThrough(Other)) {
                    Other.connection.send(Packet);
                }
            }
        } else if (!Hidden && Unlisted.remove(User.getUUID())) {
            ClientboundPlayerInfoUpdatePacket Packet = listed(User);
            for (ServerPlayer Other : User.server.getPlayerList().getPlayers()) {
                if (Other != User) {
                    Other.connection.send(Packet);
                }
            }
        }
    }

    public static ClientboundPlayerInfoUpdatePacket listed(ServerPlayer User) {
        return new ClientboundPlayerInfoUpdatePacket(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED), List.of(User));
    }

    public static ClientboundPlayerInfoUpdatePacket unlisted(ServerPlayer User) {
        RegistryFriendlyByteBuf Buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), User.server.registryAccess(), User.connection != null ? User.connection.getConnectionType() : ConnectionType.OTHER);
        try {
            Buffer.writeEnumSet(EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED), ClientboundPlayerInfoUpdatePacket.Action.class);
            Buffer.writeVarInt(1);
            Buffer.writeUUID(User.getUUID());
            Buffer.writeBoolean(false);
            return ClientboundPlayerInfoUpdatePacket.STREAM_CODEC.decode(Buffer);
        } finally {
            Buffer.release();
        }
    }

    private static void play(ServerPlayer User, boolean Donned) {
        List<DeferredHolder<SoundEvent, SoundEvent>> Layers = Donned ? Scp268Registry.DonLayers : Scp268Registry.DoffLayers;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            float Pitch = 0.95f + User.getRandom().nextFloat() * 0.1f;
            if (Donned) {
                User.playNotifySound(Layer.get(), SoundSource.PLAYERS, DonVolume, Pitch);
            } else {
                User.level().playSound(null, User.getX(), User.getY(), User.getZ(), Layer.get(), SoundSource.PLAYERS, DoffVolume, Pitch);
            }
        }
    }

    public static void clearTargets(ServerPlayer User) {
        AABB Area = User.getBoundingBox().inflate(SweepRadius);
        for (Mob Hunter : User.serverLevel().getEntitiesOfClass(Mob.class, Area)) {
            forget(Hunter, User);
        }
    }

    public static void forget(Mob Hunter, LivingEntity Target) {
        if (Hunter.getTarget() == Target) {
            Hunter.setTarget(null);
        }
        if (Hunter.getLastHurtByMob() == Target) {
            Hunter.setLastHurtByMob(null);
        }
        if (Hunter instanceof NeutralMob Neutral && Target.getUUID().equals(Neutral.getPersistentAngerTarget())) {
            Neutral.stopBeingAngry();
        }
        if (Hunter instanceof Warden Sentinel) {
            Sentinel.clearAnger(Target);
        }
        Brain<?> Mind = Hunter.getBrain();
        erase(Mind, Fixations, Target);
        if (!(Hunter instanceof Enemy)) {
            return;
        }
        erase(Mind, Glances, Target);
        if (Mind.hasMemoryValue(MemoryModuleType.ANGRY_AT) && Target.getUUID().equals(Mind.getMemory(MemoryModuleType.ANGRY_AT).orElse(null))) {
            Mind.eraseMemory(MemoryModuleType.ANGRY_AT);
        }
        if (Mind.hasMemoryValue(MemoryModuleType.LOOK_TARGET)) {
            Optional<?> Look = Mind.getMemory(MemoryModuleType.LOOK_TARGET);
            if (Look.isPresent() && Look.get() instanceof EntityTracker Tracker && Tracker.getEntity() == Target) {
                Mind.eraseMemory(MemoryModuleType.LOOK_TARGET);
            }
        }
    }

    private static void erase(Brain<?> Mind, List<MemoryModuleType<? extends LivingEntity>> Memories, LivingEntity Target) {
        for (MemoryModuleType<? extends LivingEntity> Memory : Memories) {
            if (Mind.hasMemoryValue(Memory) && Mind.getMemory(Memory).orElse(null) == Target) {
                Mind.eraseMemory(Memory);
            }
        }
    }
}
