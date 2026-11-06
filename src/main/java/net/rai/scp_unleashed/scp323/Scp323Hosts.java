package net.rai.scp_unleashed.scp323;

import com.mojang.logging.LogUtils;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlSessions;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class Scp323Hosts {
    private static final Logger Log = LogUtils.getLogger();
    public static final int KillQuota = 2;

    private Scp323Hosts() {
    }

    public static boolean canHost(LivingEntity Target) {
        if (!Target.isAlive() || Target.isRemoved() || !(Target.level() instanceof ServerLevel) || Scp323Bond.isBound(Target) || Target instanceof Scp323Beast || InfectionImmunity.isImmune(Target)) {
            return false;
        }
        if (Target instanceof ServerPlayer User) {
            return !User.isCreative() && !User.isSpectator() && !ControlManager.hasPendingDeath(User) && !Scp323Watch.isWatching(User);
        }
        return Target instanceof Mob && ControlBodies.isPersonLike(Target);
    }

    public static boolean canChange(LivingEntity Host) {
        if (!Host.isAlive() || Host.isRemoved() || !(Host.level() instanceof ServerLevel) || Host.isDeadOrDying() || !Targeting.isTargetable(Host)) {
            return false;
        }
        if (Host instanceof ServerPlayer User) {
            return !User.isSpectator() && !User.isCreative() && !ControlManager.hasPendingDeath(User);
        }
        return Host instanceof Mob;
    }

    public static boolean fuse(LivingEntity Host) {
        if (!canHost(Host)) {
            return false;
        }
        ServerLevel World = (ServerLevel) Host.level();
        Vec3 Head = Host.getEyePosition();
        play(World, Head, Scp323Registry.FuseLayers, 1.0f, 0.9f);
        World.sendParticles(ParticleTypes.WHITE_ASH, Head.x, Head.y, Head.z, 24, 0.3, 0.3, 0.3, 0.02);
        Scp323Bond.set(Host, Scp323Bond.fresh());
        return transform(Host, true) != null;
    }

    @Nullable
    public static Scp323Beast transform(LivingEntity Host, boolean Fresh) {
        if (!(Host.level() instanceof ServerLevel World) || !canChange(Host)) {
            return null;
        }
        Scp323Bond Bond = Scp323Bond.of(Host);
        if (!Bond.horned()) {
            return null;
        }
        Scp323Beast Beast = Scp323Registry.Beast.get().create(World);
        if (Beast == null) {
            return null;
        }
        if (Host.isSleeping()) {
            if (Host instanceof Player User) {
                User.stopSleepInBed(true, true);
            } else {
                Host.stopSleeping();
            }
        }
        Host.stopRiding();
        Host.ejectPassengers();
        Vec3 At = Host.position();
        float Yaw = Host.getYRot();
        Beast.moveTo(At.x, At.y, At.z, Yaw, 0.0f);
        Beast.setYHeadRot(Host.getYHeadRot());
        Beast.setYBodyRot(Yaw);
        Beast.finalizeSpawn(World, World.getCurrentDifficultyAt(Beast.blockPosition()), MobSpawnType.CONVERSION, null);
        long Time = Scp323Night.time(World);
        boolean Night = Scp323Night.isNight(Time);
        long NightId = Night ? Scp323Night.nightId(Time) : Bond.LastNight();
        boolean First = !Bond.FirstDone();
        Beast.prepare(First, Bond.Kills(), Night, NightId);
        if (Host instanceof ServerPlayer User) {
            UUID Body = ControlSessions.isControlling(User) ? ControlSessions.session(User).BodyId() : User.getUUID();
            Beast.bindPlayer(User, Body);
            if (!World.addFreshEntity(Beast)) {
                return null;
            }
            Scp323Bond.set(User, Bond.transform(Beast.getUUID(), NightId));
            Scp323Watch.start(User, Beast);
        } else {
            Scp323Bond.set(Host, Bond.transform(Beast.getUUID(), NightId));
            CompoundTag Stored = save(Host);
            if (Stored == null) {
                Scp323Bond.set(Host, Bond);
                return null;
            }
            Beast.bindBody(Host.getUUID(), Stored, Host.getName().getString());
            if (!World.addFreshEntity(Beast)) {
                Scp323Bond.set(Host, Bond);
                return null;
            }
            Host.discard();
        }
        Beast.beginMorph();
        Vec3 Middle = At.add(0.0, 1.0, 0.0);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, Middle.x, Middle.y, Middle.z, 26, 0.35, 0.7, 0.35, 0.02);
        World.sendParticles(ParticleTypes.WHITE_ASH, Middle.x, Middle.y + 0.6, Middle.z, 40, 0.5, 0.9, 0.5, 0.03);
        play(World, Middle, Scp323Registry.MorphLayers, 1.0f, Fresh ? 0.9f : 1.0f);
        return Beast;
    }

    @Nullable
    public static CompoundTag save(LivingEntity Host) {
        CompoundTag Saved = new CompoundTag();
        try {
            Host.saveWithoutId(Saved);
        } catch (RuntimeException Problem) {
            Log.warn("SCP-323 could not save its host {}", Host, Problem);
            return null;
        }
        Saved.putString("id", EntityType.getKey(Host.getType()).toString());
        Saved.remove("Passengers");
        Saved.remove("leash");
        return Saved;
    }

    @Nullable
    public static LivingEntity restore(ServerLevel World, CompoundTag Stored, Vec3 At, float Yaw, Scp323Bond Bond) {
        CompoundTag Copy = Stored.copy();
        Optional<Entity> Made = EntityType.create(Copy, World);
        if (Made.isEmpty() || !(Made.get() instanceof LivingEntity Body)) {
            Log.warn("SCP-323 could not rebuild its host from {}", Stored.getString("id"));
            return null;
        }
        Body.moveTo(At.x, At.y, At.z, Yaw, 0.0f);
        Body.setYHeadRot(Yaw);
        Body.setYBodyRot(Yaw);
        Body.setDeltaMovement(Vec3.ZERO);
        Body.fallDistance = 0.0f;
        if (Bond.Stage() > 0) {
            Body.setData(Scp323Registry.Bond.get(), Bond);
        } else {
            Body.removeData(Scp323Registry.Bond.get());
        }
        if (!World.addFreshEntity(Body)) {
            Log.warn("SCP-323 host {} could not rejoin the world", Body.getUUID());
            return null;
        }
        Scp323Sync.bond(Body);
        return Body;
    }

    public static void dropSkull(ServerLevel World, Vec3 At, float Yaw, int Quiet) {
        Scp323Skull Placed = Scp323Skull.place(World, At.add(0.0, 0.1, 0.0), Yaw, true);
        if (Placed != null) {
            Placed.hush(Quiet);
        }
    }

    @Nullable
    public static ServerPlayer player(MinecraftServer Server, @Nullable UUID Id) {
        if (Id == null) {
            return null;
        }
        ServerPlayer Found = Server.getPlayerList().getPlayer(Id);
        if (Found != null) {
            return Found;
        }
        for (ServerLevel World : Server.getAllLevels()) {
            for (ServerPlayer Candidate : World.players()) {
                if (Candidate.getUUID().equals(Id)) {
                    return Candidate;
                }
            }
        }
        return null;
    }

    @Nullable
    public static Scp323Beast beast(MinecraftServer Server, @Nullable UUID Id) {
        if (Id == null) {
            return null;
        }
        for (ServerLevel World : Server.getAllLevels()) {
            if (World.getEntity(Id) instanceof Scp323Beast Found && !Found.isRemoved()) {
                return Found;
            }
        }
        return null;
    }

    public static void hostDied(LivingEntity Host, DamageSource Source) {
        if (!(Host.level() instanceof ServerLevel World)) {
            return;
        }
        Scp323Bond Bond = Scp323Bond.of(Host);
        if (Bond.Stage() <= 0) {
            return;
        }
        Scp323Bond.clear(Host);
        Vec3 At = Host.position();
        float Yaw = Host.getYRot();
        if (Bond.transformed()) {
            Scp323Beast Beast = beast(World.getServer(), Bond.Creature());
            if (Beast != null) {
                Beast.release();
                At = Beast.position();
                if (Beast.level() instanceof ServerLevel BeastWorld) {
                    World = BeastWorld;
                    BeastWorld.sendParticles(ParticleTypes.LARGE_SMOKE, At.x, At.y + 1.2, At.z, 30, 0.4, 0.8, 0.4, 0.02);
                }
                Beast.discard();
            }
        }
        if (Host instanceof ServerPlayer User) {
            Scp323Watch.stop(User);
        }
        dropSkull(World, At, Yaw, Scp323Skull.DeathQuiet);
    }

    public static void beastDied(Scp323Beast Beast, DamageSource Source) {
        if (!(Beast.level() instanceof ServerLevel World) || !Beast.release()) {
            return;
        }
        Vec3 At = Beast.position();
        float Yaw = Beast.getYRot();
        DamageSource Severed = Beast.damageSources().source(Scp323Registry.Severed, Beast);
        ServerPlayer User = player(World.getServer(), Beast.hostPlayer());
        if (User != null && Scp323Bond.of(User).transformed() && Beast.getUUID().equals(Scp323Bond.of(User).Creature())) {
            Scp323Bond.clear(User);
            Scp323Watch.stop(User);
            if (User.level() != World || User.distanceToSqr(At) > 4.0) {
                User.teleportTo(World, At.x, At.y, At.z, Yaw, User.getXRot());
            }
            if (ControlSessions.isControlling(User)) {
                ControlManager.markDeath(User, Severed);
                LivingEntity Corpse = ControlManager.processDeath(User, Body -> Body.removeData(Scp323Registry.Bond.get()));
                Vec3 Fell = Corpse != null ? Corpse.position() : At;
                dropSkull(World, Fell, Yaw, Scp323Skull.DeathQuiet);
                return;
            }
            dropSkull(World, At, Yaw, Scp323Skull.DeathQuiet);
            User.getCombatTracker().recordDamage(Severed, User.getHealth());
            User.setHealth(0.0f);
            User.die(Severed);
            return;
        }
        CompoundTag Stored = Beast.storedBody();
        if (Stored != null) {
            LivingEntity Body = restore(World, Stored, At, Yaw, Scp323Bond.None);
            dropSkull(World, At, Yaw, Scp323Skull.DeathQuiet);
            if (Body != null) {
                Body.getCombatTracker().recordDamage(Severed, Body.getHealth());
                Body.setHealth(0.0f);
                Body.die(Severed);
            }
            return;
        }
        dropSkull(World, At, Yaw, Scp323Skull.DeathQuiet);
    }

    public static boolean cure(LivingEntity Target) {
        if (!(Target.level() instanceof ServerLevel World) || Target instanceof Scp323Beast) {
            return false;
        }
        Scp323Bond Bond = Scp323Bond.of(Target);
        if (!Bond.horned()) {
            return false;
        }
        Scp323Bond.clear(Target);
        Vec3 Feet = Target.position();
        World.sendParticles(ParticleTypes.WHITE_ASH, Feet.x, Feet.y + Target.getBbHeight(), Feet.z, 30, 0.3, 0.2, 0.3, 0.02);
        World.playSound(null, Feet.x, Feet.y + 1.0, Feet.z, Scp323Registry.Eject.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        dropSkull(World, Feet, Target.getYRot(), Scp323Skull.CureQuiet);
        return true;
    }

    public static void retracted(ServerLevel World, Vec3 At) {
        World.sendParticles(ParticleTypes.LARGE_SMOKE, At.x, At.y + 1.0, At.z, 22, 0.35, 0.6, 0.35, 0.02);
        World.sendParticles(ParticleTypes.WHITE_ASH, At.x, At.y + 1.5, At.z, 30, 0.4, 0.6, 0.4, 0.03);
    }

    public static boolean wearsSkull(LivingEntity Target) {
        return Scp323SkullItem.is(Target.getItemBySlot(EquipmentSlot.HEAD));
    }

    public static void checkHead(LivingEntity Target) {
        if (!wearsSkull(Target) || !Scp323Night.isNight(Target.level()) || !canHost(Target)) {
            return;
        }
        Target.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        fuse(Target);
    }

    public static void play(ServerLevel World, Vec3 At, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        float Varied = Pitch * (0.95f + World.getRandom().nextFloat() * 0.1f);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, At.x, At.y, At.z, Layer.get(), SoundSource.HOSTILE, Volume, Varied);
        }
    }
}
