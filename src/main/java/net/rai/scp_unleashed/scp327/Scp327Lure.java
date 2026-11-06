package net.rai.scp_unleashed.scp327;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.antimemetic.AntiMemetic;
import net.rai.scp_unleashed.scp148.Scp148Shield;
import net.rai.scp_unleashed.scp327.network.Scp327LurePayload;
import net.rai.scp_unleashed.scp327.network.Scp327Net;
import net.rai.scp_unleashed.senses.Senses;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class Scp327Lure {
    public static final float AdultGrow = 1.0f / 240.0f;
    public static final float TeenGrow = 1.0f / 480.0f;
    public static final float AdultCap = 1.0f;
    public static final float TeenCap = 0.35f;
    public static final float Fade = 1.0f / 50.0f;
    public static final float ShieldFade = 1.0f / 12.0f;
    public static final int Linger = 10;
    public static final float SendStep = 0.02f;
    public static final double PointStep = 0.2;
    public static final int NpcInterval = 10;
    public static final float NpcThreshold = 0.12f;
    public static final double NpcSpeed = 0.55;
    public static final double NpcPush = 0.035;
    private static final Map<UUID, Drawn> States = new HashMap<>();

    private Scp327Lure() {
    }

    public static boolean isProtected(LivingEntity Listener) {
        return Senses.isDeafened(Listener) || AntiMemetic.isProtected(Listener) || Scp148Shield.blocks(Listener);
    }

    public static void sing(LivingEntity Listener, Scp327Siren Singer, float Cap, float Grow) {
        Drawn State = States.computeIfAbsent(Listener.getUUID(), Key -> new Drawn());
        long Now = Listener.level().getGameTime();
        if (State.Singer != null && !State.Singer.equals(Singer.getUUID()) && Now - State.LastSung <= Linger) {
            if (Singer.distanceToSqr(Listener) > State.Point.distanceToSqr(Listener.position())) {
                return;
            }
        }
        if (State.Singer == null || !State.Singer.equals(Singer.getUUID())) {
            State.Strength *= 0.5f;
            State.Singer = Singer.getUUID();
        }
        State.Point = Singer.songPoint();
        State.LastSung = Now;
        State.Living = Listener;
        State.Protected = isProtected(Listener);
        if (State.Protected) {
            State.Strength = Math.max(0.0f, State.Strength - ShieldFade);
        } else {
            State.Strength = Math.min(Cap, State.Strength + Grow);
            State.Peak = Math.max(State.Peak, State.Strength);
        }
    }

    public static void disrupt(LivingEntity Listener, float Share) {
        Drawn State = States.get(Listener.getUUID());
        if (State != null) {
            State.Strength *= Math.max(0.0f, 1.0f - Share);
        }
    }

    public static void release(UUID Singer) {
        for (Drawn State : States.values()) {
            if (Singer.equals(State.Singer)) {
                State.Strength = 0.0f;
                State.LastSung = Long.MIN_VALUE / 2;
            }
        }
    }

    public static float strength(LivingEntity Listener) {
        Drawn State = States.get(Listener.getUUID());
        return State == null ? 0.0f : State.Strength;
    }

    public static float peak(LivingEntity Listener) {
        Drawn State = States.get(Listener.getUUID());
        return State == null ? 0.0f : State.Peak;
    }

    public static boolean shielded(LivingEntity Listener) {
        Drawn State = States.get(Listener.getUUID());
        return State != null && State.Protected;
    }

    @Nullable
    public static Vec3 point(LivingEntity Listener) {
        Drawn State = States.get(Listener.getUUID());
        return State == null || State.Strength <= 0.0f ? null : State.Point;
    }

    public static void tick(MinecraftServer Server) {
        Iterator<Map.Entry<UUID, Drawn>> Entries = States.entrySet().iterator();
        while (Entries.hasNext()) {
            Map.Entry<UUID, Drawn> Entry = Entries.next();
            Drawn State = Entry.getValue();
            LivingEntity Listener = State.Living;
            if (Listener == null || Listener.isRemoved() || !Listener.isAlive()) {
                if (Listener instanceof ServerPlayer User && !User.hasDisconnected()) {
                    send(User, State, false);
                }
                Entries.remove();
                continue;
            }
            long Now = Listener.level().getGameTime();
            boolean Fresh = Now - State.LastSung <= Linger;
            if (!Fresh) {
                State.Strength = Math.max(0.0f, State.Strength - Fade);
            }
            boolean Active = State.Strength > 0.0f && !Scp327Hold.isHeld(Listener) && Targeting.isTargetable(Listener);
            if (Listener instanceof ServerPlayer User) {
                send(User, State, Active);
            } else if (Listener instanceof Mob Body && Active && Listener.level() instanceof ServerLevel) {
                pullMob(Body, State, Now);
            }
            if (!Fresh && State.Strength <= 0.0f && !State.SentActive) {
                Entries.remove();
            }
        }
    }

    private static void pullMob(Mob Body, Drawn State, long Now) {
        if (State.Strength < NpcThreshold || Body.isNoAi()) {
            return;
        }
        Vec3 Toward = State.Point.subtract(Body.position());
        double Flat = Math.sqrt(Toward.x * Toward.x + Toward.z * Toward.z);
        if ((Now + Body.getId()) % NpcInterval == 0) {
            Body.getNavigation().moveTo(State.Point.x, State.Point.y, State.Point.z, NpcSpeed + NpcSpeed * State.Strength);
            Body.getLookControl().setLookAt(State.Point.x, State.Point.y, State.Point.z);
        }
        if (Flat > 0.3 && (Body.isInWater() || Body.getNavigation().isDone())) {
            double Push = NpcPush * State.Strength;
            Body.setDeltaMovement(Body.getDeltaMovement().add(Toward.x / Flat * Push, Body.isInWater() ? Math.signum(Toward.y) * Push * 0.4 : 0.0, Toward.z / Flat * Push));
        }
    }

    private static void send(ServerPlayer User, Drawn State, boolean Active) {
        boolean Changed = !State.Sent || State.SentActive != Active || Active && (Math.abs(State.SentStrength - State.Strength) > SendStep
                || State.SentPoint.distanceToSqr(State.Point) > PointStep * PointStep);
        if (!Changed) {
            return;
        }
        State.Sent = true;
        State.SentActive = Active;
        State.SentStrength = State.Strength;
        State.SentPoint = State.Point;
        Scp327Net.send(User, new Scp327LurePayload(Active, State.Point.x, State.Point.y, State.Point.z, Active ? State.Strength : 0.0f));
    }

    public static void forget(Entity Listener) {
        States.remove(Listener.getUUID());
    }

    public static void clear() {
        States.clear();
    }

    private static final class Drawn {
        @Nullable
        private UUID Singer;
        @Nullable
        private LivingEntity Living;
        private Vec3 Point = Vec3.ZERO;
        private float Strength;
        private float Peak;
        private long LastSung = Long.MIN_VALUE / 2;
        private boolean Protected;
        private boolean Sent;
        private boolean SentActive;
        private float SentStrength;
        private Vec3 SentPoint = Vec3.ZERO;
    }
}
