package net.rai.scp_unleashed.scp105;

import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public final class Scp105Goals {
    public static final double PlayerRange = 12.0;
    public static final double AnimalRange = 10.0;
    public static final double CloseRange = 2.5;
    public static final float PlayerPreference = 0.7f;
    public static final float SceneryChance = 0.35f;
    public static final double SceneryReach = 8.0;
    public static final int SnapGapMin = 600;
    public static final int SnapGapMax = 1400;
    public static final int SnapRetry = 100;
    public static final float TurnSpeed = 30.0f;

    private Scp105Goals() {
    }

    public static class Wander extends RandomStrollGoal {
        private final Scp105Iris Iris;

        public Wander(Scp105Iris Owner, double Speed, int Interval) {
            super(Owner, Speed, Interval);
            Iris = Owner;
        }

        @Override
        public boolean canUse() {
            return !Iris.isBusy() && !Iris.isAttending() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !Iris.isBusy() && !Iris.isAttending() && super.canContinueToUse();
        }
    }

    public static class Attend extends Goal {
        private final Scp105Iris Iris;

        public Attend(Scp105Iris Owner) {
            Iris = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !Iris.isInWater() && Iris.isAttending();
        }

        @Override
        public void start() {
            Iris.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Player Listener = Iris.listener();
            if (Listener != null) {
                Iris.getLookControl().setLookAt(Listener, TurnSpeed, TurnSpeed);
            }
            if (Iris.isGiving()) {
                Iris.setYBodyRot(Iris.getYHeadRot());
            }
        }
    }

    public static class Snap extends Goal {
        private final Scp105Iris Iris;
        @Nullable
        private LivingEntity Subject;
        @Nullable
        private Vec3 Scenery;
        private int Tick;

        public Snap(Scp105Iris Owner) {
            Iris = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (Iris.tickCount < Iris.snapReady() || Iris.act() != Scp105Iris.ActNone || Iris.isAttending() || !Iris.onGround() || Iris.isInWater() || Iris.isPassenger()) {
                return false;
            }
            Subject = pickSubject();
            Scenery = null;
            if (Subject == null && Iris.getRandom().nextFloat() < SceneryChance) {
                Vec3 Facing = Vec3.directionFromRotation(0.0f, Iris.getYRot() + (Iris.getRandom().nextFloat() - 0.5f) * 160.0f);
                Scenery = Iris.getEyePosition().add(Facing.scale(SceneryReach));
            }
            if (Subject == null && Scenery == null) {
                Iris.deferSnap(SnapRetry);
                return false;
            }
            return true;
        }

        @Nullable
        private LivingEntity pickSubject() {
            Player Nearest = Iris.level().getNearestPlayer(Iris, PlayerRange);
            boolean Person = Nearest != null && !Nearest.isSpectator() && Targeting.isTargetable(Nearest) && !Scp268.isNeglected(Nearest) && Nearest.distanceToSqr(Iris) > CloseRange * CloseRange && Iris.getSensing().hasLineOfSight(Nearest);
            if (Person && Iris.getRandom().nextFloat() < PlayerPreference) {
                return Nearest;
            }
            List<Animal> Animals = Iris.level().getEntitiesOfClass(Animal.class, Iris.getBoundingBox().inflate(AnimalRange, 4.0, AnimalRange),
                    Found -> Found.isAlive() && Found.distanceToSqr(Iris) > CloseRange * CloseRange && Iris.getSensing().hasLineOfSight(Found));
            if (!Animals.isEmpty()) {
                return Animals.get(Iris.getRandom().nextInt(Animals.size()));
            }
            return Person ? Nearest : null;
        }

        @Override
        public void start() {
            Tick = 0;
            Iris.getNavigation().stop();
            Iris.startAct(Scp105Iris.ActSnap, Scp105Iris.SnapTicks);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return Iris.isSnapping() && Tick < Scp105Iris.SnapTicks && (Subject == null || Subject.isAlive());
        }

        @Override
        public void tick() {
            Tick++;
            if (Subject != null) {
                Iris.getLookControl().setLookAt(Subject, TurnSpeed, TurnSpeed);
            } else if (Scenery != null) {
                Iris.getLookControl().setLookAt(Scenery.x, Scenery.y, Scenery.z, TurnSpeed, TurnSpeed);
            }
            Iris.setYBodyRot(Iris.getYHeadRot());
            if (Tick == Scp105Iris.SnapClick) {
                Iris.click();
            }
        }

        @Override
        public void stop() {
            if (Iris.isSnapping()) {
                Iris.stopAct();
            }
            Subject = null;
            Scenery = null;
            Iris.deferSnap(SnapGapMin + Iris.getRandom().nextInt(SnapGapMax - SnapGapMin + 1));
        }
    }

    public static class Flee extends AvoidEntityGoal<LivingEntity> {
        private final Scp105Iris Iris;

        public Flee(Scp105Iris Owner, float Range, double Walk, double Run) {
            super(Owner, LivingEntity.class, Scp105Iris::isThreat, Range, Walk, Run, EntitySelector.NO_CREATIVE_OR_SPECTATOR::test);
            Iris = Owner;
        }

        @Override
        public void start() {
            super.start();
            Iris.forget();
            if (Iris.act() != Scp105Iris.ActNone) {
                Iris.stopAct();
            }
        }
    }
}
