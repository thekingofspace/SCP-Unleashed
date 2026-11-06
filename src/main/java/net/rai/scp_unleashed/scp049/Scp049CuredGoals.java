package net.rai.scp_unleashed.scp049;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class Scp049CuredGoals {
    public static final int ScanTicks = 20;

    private Scp049CuredGoals() {
    }

    public static class Rising extends Goal {
        private final Scp049Cured Owner;

        public Rising(Scp049Cured Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return Owner.isRising();
        }

        @Override
        public void start() {
            Owner.getNavigation().stop();
        }
    }

    public static class Ordered extends TargetGoal {
        private final Scp049Cured Owner;
        @Nullable
        private LivingEntity Chosen;

        public Ordered(Scp049Cured Owner) {
            super(Owner, false, false);
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (Owner.isRising()) {
                return false;
            }
            Chosen = Owner.orderTarget();
            return Chosen != null && Chosen != Owner.getTarget();
        }

        @Override
        public void start() {
            Owner.setTarget(Chosen);
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity Current = Owner.orderTarget();
            return Current != null && Current == Owner.getTarget();
        }
    }

    public static class Guard extends TargetGoal {
        private final Scp049Cured Owner;
        @Nullable
        private LivingEntity Chosen;

        public Guard(Scp049Cured Owner) {
            super(Owner, true, false);
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            BlockPos Tray = Owner.trayPos();
            if (Owner.isRising() || Owner.isMasterless() || Owner.master() == null || Tray == null || (Owner.tickCount + Owner.getId()) % ScanTicks != 0) {
                return false;
            }
            Vec3 Centre = Vec3.atCenterOf(Tray);
            boolean Spare = Scp049Persons.sparesPlayers(Owner);
            Chosen = null;
            double Best = Double.MAX_VALUE;
            for (LivingEntity Candidate : Owner.level().getEntitiesOfClass(LivingEntity.class, new AABB(Centre, Centre).inflate(Scp049Cured.GuardAlert),
                    Body -> Scp049Persons.isPerson(Body, Spare) && Scp049Persons.isNoticeable(Body) && !Scp049Hold.isHeld(Body))) {
                double Distance = Owner.distanceToSqr(Candidate);
                if (Distance < Best && Owner.nearTray(Candidate.position(), Scp049Cured.GuardAlert) && Owner.getSensing().hasLineOfSight(Candidate)) {
                    Best = Distance;
                    Chosen = Candidate;
                }
            }
            if (Chosen != null) {
                return true;
            }
            AABB Watch = new AABB(Centre, Centre).inflate(Scp049Cured.GuardAlert).minmax(Owner.getBoundingBox().inflate(Scp049Cured.GuardAlert));
            for (LivingEntity Candidate : Owner.level().getEntitiesOfClass(LivingEntity.class, Watch,
                    Body -> Scp049Persons.isZombieFoe(Body) && !Scp049Persons.isNeutralZombie(Body))) {
                double Distance = Owner.distanceToSqr(Candidate);
                boolean Close = Distance <= Scp049Cured.GuardAlert * Scp049Cured.GuardAlert || Owner.nearTray(Candidate.position(), Scp049Cured.GuardAlert);
                if (Close && Distance < Best && Owner.getSensing().hasLineOfSight(Candidate)) {
                    Best = Distance;
                    Chosen = Candidate;
                }
            }
            return Chosen != null;
        }

        @Override
        public void start() {
            Owner.setTarget(Chosen);
            super.start();
        }
    }

    public static class Masterless extends TargetGoal {
        private final Scp049Cured Owner;
        @Nullable
        private LivingEntity Chosen;

        public Masterless(Scp049Cured Owner) {
            super(Owner, true, false);
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!Owner.isMasterless() && Owner.master() != null || Owner.isRising() || (Owner.tickCount + Owner.getId()) % ScanTicks != 0) {
                return false;
            }
            boolean Spare = Scp049Persons.sparesPlayers(Owner);
            double Range = getFollowDistance();
            Chosen = null;
            double Best = Range * Range;
            for (LivingEntity Candidate : Owner.level().getEntitiesOfClass(LivingEntity.class, Owner.getBoundingBox().inflate(Range),
                    Body -> Scp049Persons.isPerson(Body, Spare) && Scp049Persons.isNoticeable(Body))) {
                double Distance = Owner.distanceToSqr(Candidate);
                if (Distance < Best && Owner.getSensing().hasLineOfSight(Candidate)) {
                    Best = Distance;
                    Chosen = Candidate;
                }
            }
            return Chosen != null;
        }

        @Override
        public void start() {
            Owner.setTarget(Chosen);
            super.start();
        }
    }
}
