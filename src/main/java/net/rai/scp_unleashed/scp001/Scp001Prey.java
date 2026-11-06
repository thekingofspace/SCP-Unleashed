package net.rai.scp_unleashed.scp001;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;

public final class Scp001Prey {
    public static final int PlayerRank = 0;
    public static final int PersonRank = 1;
    public static final int CreatureRank = 2;
    public static final double CloserShare = 0.6;

    private Scp001Prey() {
    }

    public static boolean prey(Scp001Prototype Hunter, LivingEntity Candidate) {
        if (Candidate == Hunter || !Candidate.isAlive() || Candidate instanceof Scp001Prototype || Candidate instanceof ArmorStand || Candidate.isInvulnerable() || !Targeting.isTargetable(Candidate)) {
            return false;
        }
        if (Candidate instanceof Player User && (User.isSpectator() || User.isCreative())) {
            return false;
        }
        return !Scp268.isNeglected(Candidate) && Candidate.level() == Hunter.level() && AnomalyRealm.allows(Candidate.level());
    }

    public static int rank(LivingEntity Candidate) {
        if (Candidate instanceof Player) {
            return PlayerRank;
        }
        return ControlBodies.isPersonLike(Candidate) ? PersonRank : CreatureRank;
    }

    public static boolean noticed(Scp001Prototype Hunter, LivingEntity Candidate, double Range, double Sense) {
        double Distance = Hunter.distanceTo(Candidate);
        return Distance <= Range && (Distance <= Sense || Hunter.hasLineOfSight(Candidate));
    }

    @Nullable
    public static LivingEntity best(ServerLevel World, Scp001Prototype Hunter, double Range, double Sense) {
        LivingEntity Best = null;
        int BestRank = Integer.MAX_VALUE;
        double BestDistance = Double.MAX_VALUE;
        for (LivingEntity Candidate : World.getEntitiesOfClass(LivingEntity.class, Hunter.getBoundingBox().inflate(Range), Candidate -> prey(Hunter, Candidate))) {
            if (!noticed(Hunter, Candidate, Range, Sense)) {
                continue;
            }
            int Rank = rank(Candidate);
            double Distance = Hunter.distanceTo(Candidate);
            if (Rank < BestRank || Rank == BestRank && Distance < BestDistance) {
                Best = Candidate;
                BestRank = Rank;
                BestDistance = Distance;
            }
        }
        return Best;
    }

    public static boolean better(Scp001Prototype Hunter, LivingEntity Candidate, LivingEntity Current) {
        int CandidateRank = rank(Candidate);
        int CurrentRank = rank(Current);
        if (CandidateRank != CurrentRank) {
            return CandidateRank < CurrentRank;
        }
        return Hunter.distanceTo(Candidate) < Hunter.distanceTo(Current) * CloserShare;
    }

    @Nullable
    public static LivingEntity nearest(ServerLevel World, Scp001Prototype Hunter, double Reach) {
        LivingEntity Best = null;
        int BestRank = Integer.MAX_VALUE;
        double BestDistance = Double.MAX_VALUE;
        for (LivingEntity Candidate : World.getEntitiesOfClass(LivingEntity.class, Hunter.getBoundingBox().inflate(Reach), Candidate -> prey(Hunter, Candidate))) {
            double Distance = Hunter.distanceTo(Candidate);
            if (Distance > Reach || !Hunter.hasLineOfSight(Candidate)) {
                continue;
            }
            int Rank = rank(Candidate);
            if (Rank < BestRank || Rank == BestRank && Distance < BestDistance) {
                Best = Candidate;
                BestRank = Rank;
                BestDistance = Distance;
            }
        }
        return Best;
    }
}
