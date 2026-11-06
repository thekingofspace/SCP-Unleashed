package net.rai.scp_unleashed.scp457;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiPredicate;

public final class Scp457Goals {
    public static final int RetargetInterval = 10;
    public static final double SenseRange = 10.0;
    public static final double Hysteresis = 1.0;
    public static final double LoseRange = 48.0;
    public static final int LoseSight = 200;
    public static final int CoverRadius = 14;
    public static final int CoverRecheck = 20;
    private static final List<BiPredicate<Scp457, LivingEntity>> Quarry = new CopyOnWriteArrayList<>();

    private Scp457Goals() {
    }

    public static void addPrey(BiPredicate<Scp457, LivingEntity> Test) {
        Quarry.add(Test);
    }

    public static boolean isPrey(Scp457 Hunter, @Nullable Entity Found) {
        if (!(Found instanceof LivingEntity Body) || Body instanceof Scp457 || Body == Hunter) {
            return false;
        }
        if (!Body.isAlive() || Body.isRemoved() || Body.isDeadOrDying() || Body.isSpectator() || !Targeting.isTargetable(Body)) {
            return false;
        }
        for (BiPredicate<Scp457, LivingEntity> Each : Quarry) {
            if (Each.test(Hunter, Body)) {
                return true;
            }
        }
        if (Body instanceof Player User) {
            return !User.isCreative() && Hunter.level().getDifficulty() != Difficulty.PEACEFUL && !Scp268.isNeglected(User);
        }
        if (Body instanceof DClassLike Person && (Person.isSpecimen() || Person.isPuppet())) {
            return false;
        }
        return Body instanceof AbstractVillager || ControlBodies.isPersonLike(Body);
    }

    @Nullable
    public static LivingEntity closest(Scp457 Hunter) {
        double Range = Hunter.getAttributeValue(Attributes.FOLLOW_RANGE);
        LivingEntity Best = null;
        double BestGap = Double.MAX_VALUE;
        for (LivingEntity Found : Hunter.level().getEntitiesOfClass(LivingEntity.class, Hunter.getBoundingBox().inflate(Range, Range * 0.5, Range), Body -> isPrey(Hunter, Body))) {
            double Gap = Hunter.distanceToSqr(Found);
            if (Gap < BestGap && Gap <= Range * Range && (Gap < SenseRange * SenseRange || Hunter.getSensing().hasLineOfSight(Found))) {
                BestGap = Gap;
                Best = Found;
            }
        }
        return Best;
    }

    public static class Prey extends Goal {
        private final Scp457 Owner;
        @Nullable
        private LivingEntity Candidate;
        private int Unseen;
        private int NextScan;

        public Prey(Scp457 Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (Owner.isRising() || Owner.tickCount < NextScan) {
                return false;
            }
            NextScan = Owner.tickCount + RetargetInterval;
            Candidate = closest(Owner);
            return Candidate != null;
        }

        @Override
        public void start() {
            Unseen = 0;
            Owner.setTarget(Candidate);
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity Current = Owner.getTarget();
            if (!isPrey(Owner, Current) || Current.level() != Owner.level() || Owner.distanceToSqr(Current) > LoseRange * LoseRange) {
                return false;
            }
            if (Owner.getSensing().hasLineOfSight(Current) || Owner.distanceToSqr(Current) < SenseRange * SenseRange) {
                Unseen = 0;
            } else if (++Unseen > LoseSight) {
                return false;
            }
            return true;
        }

        @Override
        public void tick() {
            if (Owner.tickCount < NextScan) {
                return;
            }
            NextScan = Owner.tickCount + RetargetInterval;
            LivingEntity Current = Owner.getTarget();
            LivingEntity Nearer = closest(Owner);
            if (Nearer != null && Nearer != Current && (Current == null || Math.sqrt(Owner.distanceToSqr(Nearer)) + Hysteresis < Math.sqrt(Owner.distanceToSqr(Current)))) {
                Owner.setTarget(Nearer);
                Unseen = 0;
            }
        }

        @Override
        public void stop() {
            if (!isPrey(Owner, Owner.getTarget())) {
                Owner.setTarget(null);
            }
            Candidate = null;
        }
    }

    public static boolean rainBarred(Scp457 Owner, @Nullable LivingEntity Target) {
        Level World = Owner.level();
        return World.isRaining() && Target != null && Scp457Water.rainCell(World, Target.blockPosition());
    }

    public static class Hunt extends MeleeAttackGoal {
        private final Scp457 Owner;

        public Hunt(Scp457 Owner) {
            super(Owner, Scp457.ChaseSpeed, true);
            this.Owner = Owner;
        }

        @Override
        public boolean canUse() {
            return !Owner.isRising() && !rainBarred(Owner, Owner.getTarget()) && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !Owner.isRising() && !rainBarred(Owner, Owner.getTarget()) && super.canContinueToUse();
        }
    }

    public static class FleeWater extends AvoidEntityGoal<Player> {
        private final Scp457 Owner;

        public FleeWater(Scp457 Owner) {
            super(Owner, Player.class, Holder -> Scp457Water.holdsWater(Holder), Scp457.FleeRange, Scp457.FleeWalk, Scp457.FleeSprint, EntitySelector.NO_SPECTATORS::test);
            this.Owner = Owner;
        }

        @Override
        public boolean canUse() {
            return !Owner.isRising() && super.canUse();
        }
    }

    public static class Wander extends WaterAvoidingRandomStrollGoal {
        private final Scp457 Owner;

        public Wander(Scp457 Owner) {
            super(Owner, Scp457.StrollSpeed);
            this.Owner = Owner;
        }

        @Override
        public boolean canUse() {
            return !Owner.isRising() && !Scp457Water.rainedOn(Owner) && super.canUse();
        }
    }

    public static class Shelter extends Goal {
        private final Scp457 Owner;
        @Nullable
        private BlockPos Cover;
        private int Recheck;

        public Shelter(Scp457 Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (Owner.isRising() || !Owner.level().isRaining() || !Scp457Water.rainCell(Owner.level(), Owner.blockPosition())) {
                return false;
            }
            Cover = findCover(Owner);
            return Cover != null;
        }

        @Override
        public void start() {
            Recheck = CoverRecheck;
            moveToCover();
        }

        private void moveToCover() {
            if (Cover != null) {
                Owner.getNavigation().moveTo(Cover.getX() + 0.5, Cover.getY(), Cover.getZ() + 0.5, Scp457.ShelterSpeed);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return Cover != null && Owner.level().isRaining() && Scp457Water.rainCell(Owner.level(), Owner.blockPosition());
        }

        @Override
        public void tick() {
            if (--Recheck > 0 && !Owner.getNavigation().isDone()) {
                return;
            }
            Recheck = CoverRecheck;
            Cover = findCover(Owner);
            moveToCover();
        }

        @Override
        public void stop() {
            Cover = null;
            Owner.getNavigation().stop();
        }
    }

    @Nullable
    public static BlockPos findCover(Scp457 Owner) {
        Level World = Owner.level();
        BlockPos Origin = Owner.blockPosition();
        for (int Radius = 1; Radius <= CoverRadius; Radius++) {
            for (int Dx = -Radius; Dx <= Radius; Dx++) {
                for (int Dz = -Radius; Dz <= Radius; Dz++) {
                    if (Math.max(Math.abs(Dx), Math.abs(Dz)) != Radius) {
                        continue;
                    }
                    for (int Dy = 3; Dy >= -3; Dy--) {
                        BlockPos Pos = Origin.offset(Dx, Dy, Dz);
                        if (covered(World, Pos)) {
                            return Pos;
                        }
                    }
                }
            }
        }
        return null;
    }

    public static boolean covered(Level World, BlockPos Pos) {
        if (!World.isLoaded(Pos) || World.canSeeSky(Pos.above(2)) || Scp457Water.rainCell(World, Pos)) {
            return false;
        }
        BlockPos Below = Pos.below();
        if (!World.getBlockState(Below).isFaceSturdy(World, Below, Direction.UP)) {
            return false;
        }
        for (int Dy = 0; Dy < 3; Dy++) {
            BlockPos Cell = Pos.above(Dy);
            if (!World.getBlockState(Cell).getCollisionShape(World, Cell).isEmpty() || !World.getFluidState(Cell).isEmpty()) {
                return false;
            }
        }
        return World.getFluidState(Below).isEmpty();
    }
}
