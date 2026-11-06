package net.rai.scp_unleashed.scp444;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.senses.Senses;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Scp444Spread {
    public static final double Radius = 5.0;
    public static final int Cooldown = 40;
    public static final int BurstGlyphs = 14;
    public static final double BurstSpeed = 0.11;
    public static final double BurstSpreadDegrees = 75.0;
    public static final double BurstLift = 0.05;
    public static final double BurstLiftRange = 0.2;
    public static final double BurstPush = 0.8;
    public static final double BurstPushRange = 0.6;
    public static final int SettleGlyphs = 5;
    public static final double SettleSpeed = 0.05;
    public static final float SettleVolume = 0.35f;
    public static final float SettlePitch = 0.72f;
    private static final Map<UUID, Long> LastBurst = new HashMap<>();

    private Scp444Spread() {
    }

    public static boolean ready(ServerPlayer Speaker) {
        long Now = Speaker.serverLevel().getGameTime();
        Long Previous = LastBurst.get(Speaker.getUUID());
        return Previous == null || Now < Previous || Now - Previous >= Cooldown;
    }

    public static List<LivingEntity> speak(ServerPlayer Speaker) {
        ServerLevel World = Speaker.serverLevel();
        LastBurst.put(Speaker.getUUID(), World.getGameTime());
        Vec3 Origin = origin(Speaker);
        burst(World, Speaker, Origin);
        if (!Scp444Registry.spreads(World.getGameRules())) {
            return List.of();
        }
        List<LivingEntity> Caught = new ArrayList<>();
        for (LivingEntity Listener : listeners(World, Speaker, Origin)) {
            if (canCatch(Speaker, Listener, Origin) && Scp444Affliction.afflict(Listener)) {
                settle(World, Listener);
                if (Listener instanceof ServerPlayer Heard) {
                    Scp444Sync.update(Heard);
                }
                Caught.add(Listener);
            }
        }
        return Caught;
    }

    public static List<LivingEntity> listeners(ServerLevel World, LivingEntity Speaker, Vec3 Origin) {
        List<LivingEntity> Found = new ArrayList<>(World.players());
        AABB Reach = AABB.ofSize(Origin, Radius * 2.0, Radius * 2.0, Radius * 2.0);
        Found.addAll(World.getEntitiesOfClass(LivingEntity.class, Reach, Candidate -> ControlBodies.isPersonLike(Candidate) && !(Candidate instanceof Player)));
        Found.remove(Speaker);
        return Found;
    }

    public static boolean isImmune(LivingEntity Listener) {
        if (!Listener.isAlive() || Listener.isRemoved()) {
            return true;
        }
        if (Listener instanceof Player User && (User.isSpectator() || User.isCreative())) {
            return true;
        }
        return Scp444Affliction.isShielded(Listener) || Senses.isDeafened(Listener) || InfectionImmunity.isImmune(Listener);
    }

    public static boolean canCatch(LivingEntity Speaker, LivingEntity Listener, Vec3 Origin) {
        if (Listener == Speaker || Listener.level() != Speaker.level() || Scp444Affliction.isAfflicted(Listener) || isImmune(Listener)) {
            return false;
        }
        if (Listener.getEyePosition().distanceToSqr(Origin) > Radius * Radius) {
            return false;
        }
        return reaches(Speaker.level(), Origin, Listener, Speaker);
    }

    public static Vec3 origin(LivingEntity Speaker) {
        Vec3 Eye = Speaker.getEyePosition();
        Vec3 Mouth = Scp444Affliction.mouth(Speaker);
        return clear(Speaker.level(), Eye, Mouth, Speaker) ? Mouth : Eye;
    }

    public static boolean reaches(Level World, Vec3 From, LivingEntity Listener, Entity Source) {
        return clear(World, From, Listener.getEyePosition(), Source) || clear(World, From, Listener.getBoundingBox().getCenter(), Source);
    }

    public static boolean clear(Level World, Vec3 From, Vec3 To, Entity Source) {
        return World.clip(new ClipContext(From, To, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Source)).getType() == HitResult.Type.MISS;
    }

    private static void burst(ServerLevel World, ServerPlayer Speaker, Vec3 Origin) {
        float BaseYaw = Speaker.getYRot();
        for (int Index = 0; Index < BurstGlyphs; Index++) {
            double Yaw = Math.toRadians(BaseYaw + (Speaker.getRandom().nextDouble() * 2.0 - 1.0) * BurstSpreadDegrees);
            double Lift = BurstLift + Speaker.getRandom().nextDouble() * BurstLiftRange;
            double Push = BurstPush + Speaker.getRandom().nextDouble() * BurstPushRange;
            double Dx = -Mth.sin((float) Yaw) * Push;
            double Dz = Mth.cos((float) Yaw) * Push;
            World.sendParticles(Scp444Registry.Glyph.get(), Origin.x, Origin.y, Origin.z, 0, Dx, Lift, Dz, BurstSpeed);
        }
    }

    private static void settle(ServerLevel World, LivingEntity Listener) {
        Vec3 Head = Listener.getEyePosition();
        Scp444Affliction.play(World, Head, Scp444Registry.HushLayers, SettleVolume, SettlePitch);
        for (int Index = 0; Index < SettleGlyphs; Index++) {
            double Angle = Index * Mth.TWO_PI / SettleGlyphs + Listener.getRandom().nextDouble();
            double X = Head.x + Math.cos(Angle) * 0.35;
            double Z = Head.z + Math.sin(Angle) * 0.35;
            World.sendParticles(Scp444Registry.Glyph.get(), X, Head.y + 0.45, Z, 0, (Head.x - X) * 0.4, -1.0, (Head.z - Z) * 0.4, SettleSpeed);
        }
    }

    public static void forget(UUID Speaker) {
        LastBurst.remove(Speaker);
    }

    public static void clear() {
        LastBurst.clear();
    }
}
