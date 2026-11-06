package net.rai.scp_unleashed.guards;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.security.TurretCategory;
import net.rai.scp_unleashed.security.TurretRules;
import net.rai.scp_unleashed.security.TurretTargeting;
import org.jetbrains.annotations.Nullable;

public final class GuardThreats {
    public static final double EngageRange = 20.0;
    public static final double CloseRange = 12.0;
    public static final double LeashRange = 24.0;
    public static final int SelfDefenceTicks = 200;
    public static final float WeakHealth = 20.0f;

    private GuardThreats() {
    }

    public static boolean isProtected(PatrolRoute Route, TurretRules Rules, LivingEntity Candidate) {
        if (Candidate instanceof Player User && (Route.isOwner(User.getUUID()) || Rules.allowsName(User.getGameProfile().getName()))) {
            return true;
        }
        if (GuardDuty.isGuard(Candidate)) {
            return true;
        }
        if (Boolean.FALSE.equals(Rules.typeVerdict(Candidate))) {
            return true;
        }
        if (Candidate instanceof OwnableEntity Pet && Pet.getOwnerUUID() != null) {
            if (Route.isOwner(Pet.getOwnerUUID())) {
                return true;
            }
            return Pet.getOwner() instanceof Player Keeper && Rules.allowsName(Keeper.getGameProfile().getName());
        }
        return false;
    }

    public static boolean isThreat(DClass Guard, PatrolRoute Route, TurretRules Rules, LivingEntity Candidate) {
        if (Candidate == Guard || !TurretTargeting.eligible(Candidate) || isProtected(Route, Rules, Candidate)) {
            return false;
        }
        if (Candidate == Guard.getLastHurtByMob() && Guard.tickCount - Guard.getLastHurtByMobTimestamp() < SelfDefenceTicks) {
            return true;
        }
        Boolean Listed = Rules.verdict(Candidate);
        if (Listed != null) {
            return Listed;
        }
        TurretCategory Category = TurretTargeting.categoryOf(Candidate);
        return Category != null && Rules.has(Category);
    }

    public static boolean isWeak(LivingEntity Candidate) {
        return Candidate.getMaxHealth() <= WeakHealth && TurretTargeting.categoryOf(Candidate) != TurretCategory.Scp;
    }

    public static double routeDistanceSqr(PatrolRoute Route, Vec3 At) {
        double Best = Double.MAX_VALUE;
        for (PatrolPoint Point : Route.Points()) {
            Best = Math.min(Best, At.distanceToSqr(Vec3.atBottomCenterOf(Point.Pos())));
        }
        return Best;
    }

    public static boolean nearRoute(PatrolRoute Route, LivingEntity Guard, LivingEntity Candidate) {
        return Guard.distanceToSqr(Candidate) <= CloseRange * CloseRange || routeDistanceSqr(Route, Candidate.position()) <= LeashRange * LeashRange;
    }

    @Nullable
    public static LivingEntity find(DClass Guard, PatrolRoute Route, TurretRules Rules) {
        AABB Area = Guard.getBoundingBox().inflate(EngageRange);
        LivingEntity Best = null;
        double BestDistance = EngageRange * EngageRange;
        for (LivingEntity Candidate : Guard.level().getEntitiesOfClass(LivingEntity.class, Area, Found -> Found != Guard && Found.isAlive())) {
            double Distance = Guard.distanceToSqr(Candidate);
            if (Distance > BestDistance || !isThreat(Guard, Route, Rules, Candidate) || !nearRoute(Route, Guard, Candidate) || !Guard.getSensing().hasLineOfSight(Candidate)) {
                continue;
            }
            Best = Candidate;
            BestDistance = Distance;
        }
        return Best;
    }
}
