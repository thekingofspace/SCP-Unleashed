package net.rai.scp_unleashed.scp337;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Scp337Quarry {
    public static final double BodyShare = 0.55;

    private Scp337Quarry() {
    }

    public static boolean isQuarry(Scp337Mass Mass, LivingEntity Candidate) {
        if (Candidate == Mass || Candidate instanceof Scp337Mass || !Candidate.isAlive() || Candidate.isRemoved() || Candidate.isInvulnerable() || Candidate.level() != Mass.level() || !Targeting.isTargetable(Candidate)) {
            return false;
        }
        if (Candidate instanceof Player User) {
            if (User.isSpectator() || User.isCreative()) {
                return false;
            }
        } else if (!ControlBodies.isPersonLike(Candidate)) {
            return false;
        }
        return !Scp268.isNeglected(Candidate) && AnomalyRealm.allows(Candidate.level());
    }

    public static double reach(Scp337Mass Mass, LivingEntity Candidate) {
        double Dx = Candidate.getX() - Mass.getX();
        double Dz = Candidate.getZ() - Mass.getZ();
        return Math.sqrt(Dx * Dx + Dz * Dz);
    }

    public static List<LivingEntity> around(ServerLevel World, Scp337Mass Mass, double Range) {
        List<LivingEntity> Found = new ArrayList<>(World.getEntitiesOfClass(LivingEntity.class, Mass.getBoundingBox().inflate(Range, Range * 0.5, Range),
                Candidate -> isQuarry(Mass, Candidate) && reach(Mass, Candidate) <= Range));
        Found.sort(Comparator.comparingDouble(Candidate -> Candidate.distanceToSqr(Mass)));
        return Found;
    }

    @Nullable
    public static LivingEntity nearest(ServerLevel World, Scp337Mass Mass, double Range) {
        List<LivingEntity> Found = around(World, Mass, Range);
        return Found.isEmpty() ? null : Found.get(0);
    }

    public static Vec3 bodyPoint(LivingEntity Target) {
        return new Vec3(Target.getX(), Target.getY() + Target.getBbHeight() * BodyShare, Target.getZ());
    }

    public static boolean clear(Level World, Vec3 From, Vec3 To, Scp337Mass Mass) {
        return World.clip(new ClipContext(From, To, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Mass)).getType() == HitResult.Type.MISS;
    }

    @Nullable
    public static Vec3 openPoint(Scp337Mass Mass, LivingEntity Target, long Seed) {
        Vec3 Body = bodyPoint(Target);
        Vec3 Eye = Target.getEyePosition();
        Vec3 Feet = new Vec3(Target.getX(), Target.getY() + Math.min(0.3, Target.getBbHeight() * 0.2), Target.getZ());
        for (Vec3 Aim : new Vec3[]{Body, Eye, Feet}) {
            Vec3 From = Scp337Lash.origin(Mass.position(), Aim, Seed);
            if (From.distanceTo(Aim) <= Scp337Mass.LashRange + Scp337Mass.LashSlack && clear(Mass.level(), From, Aim, Mass)) {
                return Aim;
            }
        }
        return null;
    }
}
