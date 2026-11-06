package net.rai.scp_unleashed.scp343;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlSession;
import net.rai.scp_unleashed.control.ControlSessions;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Scp343Judgement {
    public static final double Reach = 24.0;
    public static final int FadeTicks = 24;
    public static final float Weight = 1.0e6f;
    public static final long Memory = 200L;
    private static final Map<UUID, Sentence> Pending = new HashMap<>();
    private static final Map<UUID, Long> Judged = new HashMap<>();

    private Scp343Judgement() {
    }

    public record Sentence(ResourceKey<Level> Where, long Due, @Nullable UUID Body) {
    }

    public static boolean isPerson(LivingEntity Victim) {
        if (Victim instanceof Player User) {
            return !User.isSpectator();
        }
        return Victim instanceof AbstractVillager || ControlBodies.isPersonLike(Victim);
    }

    @Nullable
    public static ServerPlayer killerOf(DamageSource Source, LivingEntity Victim) {
        Entity Cause = Source.getEntity();
        if (!(Cause instanceof ServerPlayer Killer) || Cause == Victim || Killer instanceof FakePlayer) {
            return null;
        }
        if (Killer.isCreative() || Killer.isSpectator() || !Killer.isAlive()) {
            return null;
        }
        return Killer;
    }

    public static boolean witnessed(ServerLevel World, Vec3 Point) {
        List<Scp343God> Found = World.getEntitiesOfClass(Scp343God.class, new AABB(Point, Point).inflate(Reach), God -> God.isAlive() && God.position().distanceToSqr(Point) <= Reach * Reach);
        return !Found.isEmpty();
    }

    public static boolean pending(UUID Killer) {
        return Pending.containsKey(Killer);
    }

    public static void murdered(LivingEntity Victim, DamageSource Source) {
        if (!(Victim.level() instanceof ServerLevel World) || !isPerson(Victim)) {
            return;
        }
        ServerPlayer Killer = killerOf(Source, Victim);
        if (Killer == null || Killer.level() != World) {
            return;
        }
        if (!witnessed(World, Victim.position()) && !witnessed(World, Killer.position())) {
            return;
        }
        long Now = World.getGameTime();
        Judged.values().removeIf(Seen -> Now - Seen > Memory);
        if (Judged.putIfAbsent(Victim.getUUID(), Now) != null) {
            return;
        }
        condemn(World, Killer);
    }

    public static void condemn(ServerLevel World, ServerPlayer Killer) {
        if (Pending.containsKey(Killer.getUUID())) {
            return;
        }
        UUID Body = null;
        if (ControlSessions.isControlling(Killer)) {
            ControlSession Session = ControlSessions.session(Killer);
            Body = Session.BodyId();
        }
        Pending.put(Killer.getUUID(), new Sentence(World.dimension(), World.getGameTime() + FadeTicks, Body));
        Scp343Speech.play(World, Killer, Scp343Registry.JudgementLayers, 1.0f, 1.0f);
        Scp343Effects.judgement(World, Killer, 24);
        Killer.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, FadeTicks + 10, 1, false, false, false));
        Killer.addEffect(new MobEffectInstance(MobEffects.GLOWING, FadeTicks + 10, 0, false, false, false));
    }

    public static void tick(MinecraftServer Server) {
        if (Pending.isEmpty()) {
            return;
        }
        long Now = Server.overworld().getGameTime();
        List<UUID> Due = new ArrayList<>();
        for (Map.Entry<UUID, Sentence> Entry : Pending.entrySet()) {
            ServerPlayer Killer = Server.getPlayerList().getPlayer(Entry.getKey());
            ServerLevel World = Server.getLevel(Entry.getValue().Where());
            if (Killer != null && World != null && Killer.level() == World && Now % 2L == 0L) {
                Scp343Effects.judgement(World, Killer, 3);
            }
            if (World == null || World.getGameTime() >= Entry.getValue().Due()) {
                Due.add(Entry.getKey());
            }
        }
        for (UUID Id : Due) {
            Sentence Found = Pending.remove(Id);
            if (Found != null) {
                carryOut(Server, Id, Found);
            }
        }
    }

    public static void carryOut(MinecraftServer Server, UUID Id, Sentence Found) {
        ServerPlayer Killer = Server.getPlayerList().getPlayer(Id);
        if (Found.Body() != null) {
            if (Killer != null && ControlSessions.isControlling(Killer) && Found.Body().equals(ControlSessions.session(Killer).BodyId())) {
                strike(Killer);
                return;
            }
            for (ServerLevel World : Server.getAllLevels()) {
                if (World.getEntity(Found.Body()) instanceof LivingEntity Body && Body.isAlive()) {
                    strike(Body);
                    return;
                }
            }
            return;
        }
        if (Killer == null || !Killer.isAlive()) {
            return;
        }
        if (ControlSessions.isControlling(Killer)) {
            ControlManager.release(Killer);
        }
        strike(Killer);
    }

    public static void strike(LivingEntity Target) {
        if (!(Target.level() instanceof ServerLevel World)) {
            return;
        }
        Scp343Effects.judgement(World, Target, 40);
        Target.hurt(World.damageSources().source(Scp343Registry.Judged), Weight);
    }

    public static void loggedOut(ServerPlayer User) {
        Sentence Found = Pending.remove(User.getUUID());
        if (Found != null) {
            carryOut(User.server, User.getUUID(), new Sentence(Found.Where(), 0L, Found.Body()));
        }
    }

    public static void clear() {
        Pending.clear();
        Judged.clear();
    }
}
