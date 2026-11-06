package net.rai.scp_unleashed.scp106;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.control.ControlManager;
import net.rai.scp_unleashed.control.ControlSessions;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Scp106Targeting {
    public static final int RankNone = 0;
    public static final int RankVillager = 1;
    public static final int RankPerson = 2;
    public static final int ReachChecks = 8;
    public static final double Stickiness = 1.2;

    private record Scored(LivingEntity Body, int Rank, double Distance) {
    }

    private Scp106Targeting() {
    }

    public static int rank(Level World, @Nullable LivingEntity Candidate) {
        if (Candidate == null || !Candidate.isAlive() || Candidate.isRemoved() || Candidate.isDeadOrDying() || Candidate.isSpectator() || Candidate.isInvulnerable()
                || Candidate instanceof Scp106OldMan || Candidate.level() != World || !Targeting.isTargetable(Candidate)) {
            return RankNone;
        }
        if (Candidate instanceof Player User) {
            if (User.isCreative() || World.getDifficulty() == Difficulty.PEACEFUL || Scp268.isNeglected(User)) {
                return RankNone;
            }
            return User instanceof ServerPlayer Online && ControlManager.hasPendingDeath(Online) ? RankNone : RankPerson;
        }
        if (World.getServer() != null && Scp106Pocket.inPocket(Candidate) && Scp106Pocket.isCaptive(World.getServer(), Candidate.getUUID())) {
            return RankNone;
        }
        if (ControlBodies.isPersonLike(Candidate)) {
            if (ControlSessions.isBoundToSession(Candidate)) {
                return RankNone;
            }
            if (Candidate instanceof DClassLike Body && (Body.isSpecimen() || Body.isPuppet())) {
                return RankNone;
            }
            return RankPerson;
        }
        return Candidate instanceof Villager ? RankVillager : RankNone;
    }

    public static boolean valid(Level World, @Nullable LivingEntity Candidate) {
        return rank(World, Candidate) != RankNone;
    }

    public static boolean reachable(Level World, Vec3 From, LivingEntity Candidate) {
        return !Scp106Phasing.lineHeld(World, From, Candidate.getEyePosition());
    }

    @Nullable
    public static LivingEntity choose(ServerLevel World, Vec3 From, double Range, double Keep, double MinRange, @Nullable LivingEntity Current, double Stick,
                                      boolean CheckReach, boolean PersonsOnly) {
        double Reach = Math.max(Range, Keep);
        List<Scored> Found = new ArrayList<>();
        Scored Held = null;
        for (LivingEntity Candidate : World.getEntitiesOfClass(LivingEntity.class, AABB.ofSize(From, Reach * 2.0, Reach * 2.0, Reach * 2.0), Living -> valid(World, Living))) {
            int Rank = rank(World, Candidate);
            if (PersonsOnly && Rank != RankPerson) {
                continue;
            }
            double Distance = Candidate.distanceToSqr(From);
            if (Distance < MinRange * MinRange) {
                continue;
            }
            Scored Entry = new Scored(Candidate, Rank, Distance);
            if (Candidate == Current && Distance <= Keep * Keep) {
                Held = Entry;
            }
            if (Distance <= Range * Range || Entry == Held) {
                Found.add(Entry);
            }
        }
        if (Found.isEmpty()) {
            return null;
        }
        Found.sort(Comparator.comparingInt((Scored Entry) -> -Entry.Rank()).thenComparingDouble(Scored::Distance));
        Scored Pick = Found.get(0);
        boolean PickReach = true;
        if (CheckReach) {
            PickReach = false;
            int Checks = 0;
            for (Scored Entry : Found) {
                if (Checks++ >= ReachChecks) {
                    break;
                }
                if (reachable(World, From, Entry.Body())) {
                    Pick = Entry;
                    PickReach = true;
                    break;
                }
            }
        }
        if (Held != null && Held != Pick) {
            boolean HeldReach = !CheckReach || reachable(World, From, Held.Body());
            boolean SameKind = Held.Rank() == Pick.Rank() && HeldReach == PickReach;
            boolean Better = HeldReach && !PickReach && Held.Rank() >= Pick.Rank();
            if (Better || SameKind && Held.Distance() <= Pick.Distance() * Stick * Stick) {
                return Held.Body();
            }
        }
        return Pick.Body();
    }
}
