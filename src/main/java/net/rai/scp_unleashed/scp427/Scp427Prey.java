package net.rai.scp_unleashed.scp427;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;

public final class Scp427Prey {
    public static final double ClearlyCloser = 3.0;
    public static final double CloserShare = 0.75;

    private Scp427Prey() {
    }

    public static boolean isPerson(LivingEntity Candidate) {
        return Candidate instanceof Player || ControlBodies.isPersonLike(Candidate) || Candidate instanceof AbstractVillager;
    }

    public static boolean prey(Scp427Flesh Hunter, LivingEntity Candidate) {
        if (Candidate == Hunter || !Candidate.isAlive() || Candidate.isRemoved() || Candidate instanceof Scp427Flesh || Candidate.level() != Hunter.level() || !Targeting.isTargetable(Candidate)) {
            return false;
        }
        if (!isPerson(Candidate) || Scp268.isNeglected(Candidate)) {
            return false;
        }
        return !(Candidate instanceof Player User) || !User.isCreative() && !User.isSpectator();
    }

    public static boolean noticed(Scp427Flesh Hunter, LivingEntity Candidate, double Range, double Sense) {
        double Distance = Hunter.distanceTo(Candidate);
        return Distance <= Range && (Distance <= Sense || Hunter.hasLineOfSight(Candidate));
    }

    @Nullable
    public static LivingEntity closest(ServerLevel World, Scp427Flesh Hunter, double Range, double Sense) {
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

    public static boolean clearlyCloser(Scp427Flesh Hunter, LivingEntity Current, LivingEntity Other) {
        double Held = Hunter.distanceTo(Current);
        double Offered = Hunter.distanceTo(Other);
        return Offered < Held - ClearlyCloser || Offered < Held * CloserShare;
    }
}
