package net.rai.scp_unleashed.scp428;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;

public final class Scp428Quarry {
    public static final double HeadShare = 0.93;

    private Scp428Quarry() {
    }

    public static boolean isQuarry(LivingEntity Candidate) {
        if (Candidate instanceof Scp428Core || Candidate instanceof Scp428Instance || !Candidate.isAlive() || Candidate.isRemoved() || InfectionImmunity.isImmune(Candidate)) {
            return false;
        }
        if (!(Candidate instanceof Player) && !ControlBodies.isPersonLike(Candidate)) {
            return false;
        }
        if (Candidate instanceof DClassLike Body && (Body.isSpecimen() || Body.isPuppet())) {
            return false;
        }
        return !Scp268.isNeglected(Candidate) && AnomalyRealm.allows(Candidate.level());
    }

    public static List<LivingEntity> around(ServerLevel World, Vec3 Centre, double Range) {
        List<LivingEntity> Found = World.getEntitiesOfClass(LivingEntity.class, new AABB(Centre, Centre).inflate(Range, Range * 0.6, Range),
                Candidate -> isQuarry(Candidate) && Candidate.position().distanceToSqr(Centre) <= Range * Range);
        Found.sort(Comparator.comparingDouble(Candidate -> Candidate.position().distanceToSqr(Centre)));
        return Found;
    }

    public static boolean sees(Mob Looker, LivingEntity Target) {
        Vec3 From = Looker.getEyePosition();
        Vec3 To = Target.getEyePosition();
        return Looker.level().clip(new ClipContext(From, To, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, Looker)).getType() == HitResult.Type.MISS;
    }

    @Nullable
    public static LivingEntity pick(ServerLevel World, Mob Looker, double Range, double Through) {
        LivingEntity Heard = null;
        for (LivingEntity Candidate : around(World, Looker.position(), Range)) {
            if (sees(Looker, Candidate)) {
                return Candidate;
            }
            if (Heard == null && Candidate.distanceTo(Looker) <= Through) {
                Heard = Candidate;
            }
        }
        return Heard;
    }

    public static Vec3 head(LivingEntity Target) {
        return new Vec3(Target.getX(), Target.getY() + Target.getBbHeight() * HeadShare, Target.getZ());
    }
}
