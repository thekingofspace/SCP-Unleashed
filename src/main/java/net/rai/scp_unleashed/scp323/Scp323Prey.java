package net.rai.scp_unleashed.scp323;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;

public final class Scp323Prey {
    public static final double ClearlyCloser = 3.0;
    public static final double CloserShare = 0.75;

    private Scp323Prey() {
    }

    public static boolean isPerson(LivingEntity Candidate) {
        return Candidate instanceof Player || ControlBodies.isPersonLike(Candidate) || Candidate instanceof AbstractVillager;
    }

    public static boolean prey(Scp323Beast Hunter, LivingEntity Candidate) {
        if (Candidate == Hunter || !Candidate.isAlive() || Candidate.isRemoved() || Candidate instanceof Scp323Beast || Candidate.level() != Hunter.level() || !Targeting.isTargetable(Candidate)) {
            return false;
        }
        if (!isPerson(Candidate) || Scp268.isNeglected(Candidate)) {
            return false;
        }
        if (Candidate instanceof Player User) {
            if (User.isCreative() || User.isSpectator() || Scp323Watch.isWatching(User) || User.getUUID().equals(Hunter.hostPlayer())) {
                return false;
            }
        }
        return !Candidate.getUUID().equals(Hunter.hostBody());
    }

    public static boolean noticed(Scp323Beast Hunter, LivingEntity Candidate, double Range, double Sense) {
        double Distance = Hunter.distanceTo(Candidate);
        return Distance <= Range && (Distance <= Sense || Hunter.hasLineOfSight(Candidate));
    }

    @Nullable
    public static LivingEntity closest(ServerLevel World, Scp323Beast Hunter, double Range, double Sense) {
        LivingEntity Best = null;
        double BestDistance = Double.MAX_VALUE;
        for (LivingEntity Candidate : World.getEntitiesOfClass(LivingEntity.class, Hunter.getBoundingBox().inflate(Range), Candidate -> prey(Hunter, Candidate))) {
            double Distance = Hunter.distanceTo(Candidate);
            if (Distance < BestDistance && noticed(Hunter, Candidate, Range, Sense)) {
                BestDistance = Distance;
                Best = Candidate;
            }
        }
        return Best;
    }

    public static boolean clearlyCloser(Scp323Beast Hunter, LivingEntity Current, LivingEntity Other) {
        double Held = Hunter.distanceTo(Current);
        double Offered = Hunter.distanceTo(Other);
        return Offered < Held - ClearlyCloser || Offered < Held * CloserShare;
    }
}
