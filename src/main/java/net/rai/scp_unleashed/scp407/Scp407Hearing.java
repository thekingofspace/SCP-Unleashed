package net.rai.scp_unleashed.scp407;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.labware.LabExposure;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class Scp407Hearing {
    public static final double Range = 16.0;
    public static final int EffectTicks = 60;
    public static final int RegenerationLevel = 0;
    public static final int SlownessLevel = 1;
    public static final long HearingGrace = 20L;

    private Scp407Hearing() {
    }

    public static boolean counts(Entity Target) {
        if (!(Target instanceof LivingEntity Living) || !Living.isAlive() || InfectionImmunity.isImmune(Living)) {
            return false;
        }
        if (Target instanceof Player User) {
            return !User.isCreative() && !User.isSpectator();
        }
        return ControlBodies.isPersonLike(Target);
    }

    public static UUID keyOf(LivingEntity Target) {
        if (Target instanceof ServerPlayer User && ControlSessions.isControlling(User)) {
            return ControlSessions.session(User).BodyId();
        }
        return Target.getUUID();
    }

    @Nullable
    public static LivingEntity resolve(MinecraftServer Server, UUID Key) {
        for (ServerLevel World : Server.getAllLevels()) {
            for (ServerPlayer User : World.players()) {
                boolean Controlling = ControlSessions.isControlling(User);
                if (Controlling && Key.equals(ControlSessions.session(User).BodyId())) {
                    return User;
                }
                if (!Controlling && Key.equals(User.getUUID())) {
                    return User;
                }
            }
        }
        for (ServerLevel World : Server.getAllLevels()) {
            Entity Found = World.getEntity(Key);
            if (Found instanceof LivingEntity Living && !(Found instanceof Player)) {
                return Living;
            }
        }
        return null;
    }

    @Nullable
    public static LivingEntity resolveIn(ServerLevel World, UUID Key) {
        LivingEntity Found = resolve(World.getServer(), Key);
        return Found != null && Found.level() == World ? Found : null;
    }

    public static boolean ignored(ServerLevel World, BlockPos Speaker, BlockPos Pos) {
        return Pos.equals(Speaker) || World.getBlockState(Pos).getBlock() instanceof Scp407GrowthBlock;
    }

    public static boolean hears(ServerLevel World, Vec3 Mouth, BlockPos Speaker, LivingEntity Target) {
        if (!counts(Target) || Senses.isDeafened(Target) || !Targeting.canWitness(Target)) {
            return false;
        }
        Vec3 Eyes = Target.getEyePosition();
        if (Eyes.distanceToSqr(Mouth) > Range * Range) {
            return false;
        }
        return LabExposure.clearPath(World, Mouth, Eyes, Pos -> ignored(World, Speaker, Pos))
                || LabExposure.clearPath(World, Mouth, Target.getBoundingBox().getCenter(), Pos -> ignored(World, Speaker, Pos));
    }

    public static List<LivingEntity> listeners(ServerLevel World, Vec3 Mouth, BlockPos Speaker) {
        AABB Reach = AABB.ofSize(Mouth, Range * 2.0 + 2.0, Range * 2.0 + 2.0, Range * 2.0 + 2.0);
        return World.getEntitiesOfClass(LivingEntity.class, Reach, Target -> hears(World, Mouth, Speaker, Target));
    }

    public static void soothe(LivingEntity Target) {
        Target.addEffect(new MobEffectInstance(MobEffects.REGENERATION, EffectTicks, RegenerationLevel, true, false, true));
        Target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, EffectTicks, SlownessLevel, true, false, true));
    }

    public static void scan(ServerLevel World, Scp407Session Session, Vec3 Mouth, BlockPos Speaker, long Position) {
        long Now = World.getGameTime();
        for (LivingEntity Target : listeners(World, Mouth, Speaker)) {
            UUID Key = keyOf(Target);
            Scp407Session.Listener Entry = Session.Listeners.get(Key);
            if (Entry == null) {
                Entry = new Scp407Session.Listener(Key, Target.getName().getString(), Target instanceof Player, Position);
                Session.Listeners.put(Key, Entry);
            }
            Entry.LastHeard = Now;
            soothe(Target);
        }
    }

    public static boolean hearingNow(Scp407Session.Listener Entry, long Now) {
        return Now - Entry.LastHeard <= HearingGrace;
    }
}
