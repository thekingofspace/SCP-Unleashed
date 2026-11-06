package net.rai.scp_unleashed.scp469;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;

public final class Scp469Prey {
    public static final double CloserShare = 0.6;

    private Scp469Prey() {
    }

    public static boolean isPerson(LivingEntity Candidate) {
        return Candidate instanceof Player || ControlBodies.isPersonLike(Candidate);
    }

    public static boolean prey(Scp469Angel Hunter, LivingEntity Candidate) {
        if (Candidate == Hunter || !Candidate.isAlive() || Candidate.isRemoved() || Candidate instanceof Scp469Angel || Candidate.level() != Hunter.level()
                || !Targeting.isTargetable(Candidate)) {
            return false;
        }
        if (Candidate instanceof Player User) {
            return !User.isSpectator() && !User.isCreative() && !Scp268.isNeglected(User) && AnomalyRealm.allows(Candidate.level());
        }
        return ControlBodies.isPersonLike(Candidate) && !Candidate.isInvulnerable() && AnomalyRealm.allows(Candidate.level());
    }

    @Nullable
    public static LivingEntity nearest(ServerLevel World, Scp469Angel Hunter, double Range) {
        LivingEntity Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (LivingEntity Candidate : World.getEntitiesOfClass(LivingEntity.class, Hunter.getBoundingBox().inflate(Range), Candidate -> prey(Hunter, Candidate))) {
            double Distance = Hunter.distanceToSqr(Candidate);
            if (Distance > Range * Range || Distance >= BestDistance || !Hunter.getSensing().hasLineOfSight(Candidate)) {
                continue;
            }
            Best = Candidate;
            BestDistance = Distance;
        }
        return Best;
    }

    public static boolean clearlyCloser(Scp469Angel Hunter, LivingEntity Candidate, LivingEntity Current) {
        return Hunter.distanceTo(Candidate) < Hunter.distanceTo(Current) * CloserShare;
    }
}
