package net.rai.scp_unleashed.scp027;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;

public final class Scp027Goals {
    public static final double DriftRange = 24.0;
    public static final double DriftStop = 3.5;
    public static final int DriftRecheck = 20;
    public static final int MillChance = 50;
    public static final double MillRadius = 4.0;
    public static final double HeelStop = 4.0;
    public static final int ScatterReach = 16;

    private Scp027Goals() {
    }

    public static boolean isPerson(LivingEntity Target) {
        if (!Target.isAlive() || Target.isSpectator() || !Targeting.isTargetable(Target) || Target instanceof Scp027Entity || Target instanceof Scp027Vermin) {
            return false;
        }
        return Target instanceof Player || ControlBodies.isPersonLike(Target);
    }

    @Nullable
    public static LivingEntity nearestPerson(Mob Seeker, double Range) {
        AABB Area = Seeker.getBoundingBox().inflate(Range, 6.0, Range);
        return Seeker.level().getEntitiesOfClass(LivingEntity.class, Area, Scp027Goals::isPerson).stream()
                .filter(Target -> Target.distanceToSqr(Seeker) <= Range * Range)
                .min(Comparator.comparingDouble(Target -> Target.distanceToSqr(Seeker)))
                .orElse(null);
    }

    public static class Drift extends Goal {
        private final Scp027Entity Owner;
        private final double Speed;
        @Nullable
        private LivingEntity Quarry;
        private int Recheck;

        public Drift(Scp027Entity Owner, double Speed) {
            this.Owner = Owner;
            this.Speed = Speed;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (Owner.getRandom().nextInt(10) != 0) {
                return false;
            }
            Quarry = nearestPerson(Owner, DriftRange);
            return Quarry != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Quarry != null && Quarry.isAlive() && !Quarry.isSpectator() && Targeting.isTargetable(Quarry) && Quarry.distanceToSqr(Owner) <= DriftRange * DriftRange * 1.4;
        }

        @Override
        public void start() {
            Recheck = 0;
        }

        @Override
        public void stop() {
            Quarry = null;
            Owner.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Quarry == null) {
                return;
            }
            Owner.getLookControl().setLookAt(Quarry, 12.0f, 12.0f);
            if (--Recheck > 0) {
                return;
            }
            Recheck = DriftRecheck;
            LivingEntity Closer = nearestPerson(Owner, DriftRange);
            if (Closer != null) {
                Quarry = Closer;
            }
            if (Owner.distanceToSqr(Quarry) <= DriftStop * DriftStop) {
                Owner.getNavigation().stop();
            } else {
                Owner.getNavigation().moveTo(Quarry, Speed);
            }
        }
    }

    public static class Wander extends WaterAvoidingRandomStrollGoal {
        public Wander(PathfinderMob Owner, double Speed, int Interval) {
            super(Owner, Speed);
            setInterval(Interval);
        }
    }

    public static class PreyTarget extends NearestAttackableTargetGoal<LivingEntity> {
        private final Scp027Vermin Vermin;

        public PreyTarget(Mob Body, Scp027Vermin Vermin) {
            super(Body, LivingEntity.class, 5, true, false, Target -> Scp027Vermin.isPrey(Vermin, Target));
            this.Vermin = Vermin;
        }

        @Override
        public boolean canUse() {
            return !Vermin.leash().scattering() && Vermin.leash().master() != null && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !Vermin.leash().scattering() && super.canContinueToUse();
        }
    }

    public static class Heel extends Goal {
        private final PathfinderMob Body;
        private final Scp027Vermin Vermin;
        private final double Speed;

        public Heel(PathfinderMob Body, Scp027Vermin Vermin, double Speed) {
            this.Body = Body;
            this.Vermin = Vermin;
            this.Speed = Speed;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !Vermin.leash().scattering() && Body.getTarget() == null && Vermin.leash().straying(Body);
        }

        @Override
        public boolean canContinueToUse() {
            Scp027Entity Master = Vermin.leash().master();
            return Master != null && !Vermin.leash().scattering() && Body.getTarget() == null && Body.distanceToSqr(Master) > HeelStop * HeelStop;
        }

        @Override
        public void tick() {
            Scp027Entity Master = Vermin.leash().master();
            if (Master != null && (Body.getNavigation().isDone() || Body.tickCount % 20 == 0)) {
                Body.getNavigation().moveTo(Master, Speed);
            }
        }

        @Override
        public void stop() {
            Body.getNavigation().stop();
        }
    }

    public static class Mill extends Goal {
        private final PathfinderMob Body;
        private final Scp027Vermin Vermin;
        private final double Speed;

        public Mill(PathfinderMob Body, Scp027Vermin Vermin, double Speed) {
            this.Body = Body;
            this.Vermin = Vermin;
            this.Speed = Speed;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Scp027Entity Master = Vermin.leash().master();
            if (Master == null || Vermin.leash().scattering() || Body.getTarget() != null || Body.getRandom().nextInt(MillChance) != 0) {
                return false;
            }
            double Angle = Body.getRandom().nextDouble() * Math.PI * 2.0;
            double Radius = 1.5 + Body.getRandom().nextDouble() * (MillRadius - 1.5);
            return Body.getNavigation().moveTo(Master.getX() + Math.cos(Angle) * Radius, Master.getY(), Master.getZ() + Math.sin(Angle) * Radius, Speed);
        }

        @Override
        public boolean canContinueToUse() {
            return !Body.getNavigation().isDone() && Body.getTarget() == null && !Vermin.leash().scattering();
        }
    }

    public static class Flee extends Goal {
        private final PathfinderMob Body;
        private final Scp027Vermin Vermin;
        private final double Speed;

        public Flee(PathfinderMob Body, Scp027Vermin Vermin, double Speed) {
            this.Body = Body;
            this.Vermin = Vermin;
            this.Speed = Speed;
            setFlags(EnumSet.of(Flag.MOVE, Flag.TARGET, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return Vermin.leash().scattering();
        }

        @Override
        public void tick() {
            if (!Body.getNavigation().isDone()) {
                return;
            }
            Vec3 From = Vermin.leash().home() != null ? Vermin.leash().home() : Body.position();
            Vec3 Away = DefaultRandomPos.getPosAway(Body, ScatterReach, 7, From);
            if (Away != null) {
                Body.getNavigation().moveTo(Away.x, Away.y, Away.z, Speed);
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }
    }
}
