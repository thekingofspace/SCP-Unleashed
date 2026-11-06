package net.rai.scp_unleashed.scp016;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.control.ControlBodies;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class Scp016Goals {
    private Scp016Goals() {
    }

    public static final class HoldStill extends Goal {
        private final Scp016Spawn Holder;

        public HoldStill(Scp016Spawn Holder) {
            this.Holder = Holder;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Holder.locksMovement();
        }

        @Override
        public void start() {
            Holder.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Holder.getNavigation().stop();
            Holder.setDeltaMovement(Holder.getDeltaMovement().multiply(0.0, 1.0, 0.0));
        }
    }

    public static final class Leap extends LeapAtTargetGoal {
        private final Scp016Skitter Jumper;

        public Leap(Scp016Skitter Jumper, float Lift) {
            super(Jumper, Lift);
            this.Jumper = Jumper;
        }

        @Override
        public boolean canUse() {
            return !Jumper.locksMovement() && super.canUse();
        }

        @Override
        public void start() {
            super.start();
            Jumper.leapt();
        }
    }

    public static final class Slam extends MeleeAttackGoal {
        private final Scp016Brute Striker;

        public Slam(Scp016Brute Striker, double Speed) {
            super(Striker, Speed, true);
            this.Striker = Striker;
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity Target) {
            if (!Striker.isActing() && canPerformAttack(Target)) {
                resetAttackCooldown();
                Striker.beginSlam(Target);
            }
        }
    }

    public static final class KeepDistance extends Goal {
        private final Scp016Spawn Runner;
        private final double Range;
        private final double Speed;
        @Nullable
        private LivingEntity Threat;
        @Nullable
        private Path Escape;

        public KeepDistance(Scp016Spawn Runner, double Range, double Speed) {
            this.Runner = Runner;
            this.Range = Range;
            this.Speed = Speed;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (Runner.locksMovement()) {
                return false;
            }
            Threat = Runner.getTarget();
            if (Threat == null || !Threat.isAlive() || Runner.distanceToSqr(Threat) > Range * Range) {
                return false;
            }
            Vec3 Away = DefaultRandomPos.getPosAway(Runner, 12, 6, Threat.position());
            if (Away == null || Threat.distanceToSqr(Away) < Threat.distanceToSqr(Runner)) {
                return false;
            }
            Escape = Runner.getNavigation().createPath(Away.x, Away.y, Away.z, 0);
            return Escape != null;
        }

        @Override
        public boolean canContinueToUse() {
            return !Runner.locksMovement() && !Runner.getNavigation().isDone() && Threat != null && Threat.isAlive()
                    && Runner.distanceToSqr(Threat) < (Range + 3.0) * (Range + 3.0);
        }

        @Override
        public void start() {
            Runner.getNavigation().moveTo(Escape, Speed);
        }

        @Override
        public void stop() {
            Threat = null;
            Escape = null;
        }
    }

    public static final class Challenge extends TargetGoal {
        private final Scp016Duelist Fencer;
        private final int Every;
        private final double Margin;
        private int Wait;
        @Nullable
        private LivingEntity Pick;

        public Challenge(Scp016Duelist Fencer, int Every, double Margin) {
            super(Fencer, true, false);
            this.Fencer = Fencer;
            this.Every = Every;
            this.Margin = Margin;
            setFlags(EnumSet.of(Goal.Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (Wait > 0) {
                Wait--;
                return false;
            }
            Wait = Every;
            if (Fencer.isActing()) {
                return false;
            }
            Pick = best();
            if (Pick == null) {
                return false;
            }
            LivingEntity Current = Fencer.getTarget();
            return Current == null || !Current.isAlive() || Current != Pick && Scp016Duelist.threat(Pick) > Scp016Duelist.threat(Current) + Margin;
        }

        @Nullable
        private LivingEntity best() {
            double Range = Fencer.getAttributeValue(Attributes.FOLLOW_RANGE);
            LivingEntity Found = null;
            double Score = -1.0;
            for (LivingEntity Candidate : Fencer.level().getEntitiesOfClass(LivingEntity.class, Fencer.getBoundingBox().inflate(Range, Range * 0.5, Range),
                    Each -> Each instanceof Player || ControlBodies.isPersonLike(Each))) {
                if (!Candidate.isAlive() || Candidate instanceof Player User && (User.isCreative() || User.isSpectator()) || !Scp016Spawn.isQuarry(Candidate)
                        || !Fencer.getSensing().hasLineOfSight(Candidate)
                        || Fencer.distanceTo(Candidate) > Range) {
                    continue;
                }
                double Value = Scp016Duelist.threat(Candidate) - Fencer.distanceTo(Candidate) * 0.01;
                if (Value > Score) {
                    Score = Value;
                    Found = Candidate;
                }
            }
            return Found;
        }

        @Override
        public void start() {
            mob.setTarget(Pick);
            super.start();
        }
    }

    public static final class Duel extends Goal {
        private final Scp016Duelist Fencer;
        private final double Speed;
        private int Repath;

        public Duel(Scp016Duelist Fencer, double Speed) {
            this.Fencer = Fencer;
            this.Speed = Speed;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        private boolean valid(@Nullable LivingEntity Target) {
            return Target != null && Target.isAlive() && !(Target instanceof Player User && (User.isCreative() || User.isSpectator()));
        }

        @Override
        public boolean canUse() {
            return !Fencer.isEmerging() && valid(Fencer.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Repath = 0;
            Fencer.setAggressive(true);
        }

        @Override
        public void stop() {
            Fencer.getNavigation().stop();
            Fencer.setAggressive(false);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = Fencer.getTarget();
            if (Target == null || Fencer.isActing()) {
                return;
            }
            Fencer.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            double Distance = Fencer.distanceTo(Target);
            double Reach = Fencer.comboReach(Target);
            if (Distance <= Reach && Fencer.readyToCombo() && Fencer.getSensing().hasLineOfSight(Target)) {
                Fencer.getNavigation().stop();
                Fencer.beginCombo(Target);
                return;
            }
            if (Fencer.readyToLunge() && Fencer.getRandom().nextInt(6) == 0 && Fencer.lungeWindow(Target)) {
                Fencer.getNavigation().stop();
                Fencer.beginLunge(Target);
                return;
            }
            if (Distance <= Reach * 0.75) {
                Fencer.getNavigation().stop();
                return;
            }
            if (--Repath <= 0) {
                Repath = 8 + Fencer.getRandom().nextInt(5);
                Fencer.getNavigation().moveTo(Target, Speed);
            }
        }
    }

    public static final class Forage extends NearestAttackableTargetGoal<Mob> {
        private final Scp016Glutton Eater;

        public Forage(Scp016Glutton Eater) {
            super(Eater, Mob.class, 10, true, false, Found -> Eater.hungry() && Eater.isFood(Found) && Eater.distanceTo(Found) <= Scp016Glutton.HuntRange);
            this.Eater = Eater;
        }

        @Override
        public boolean canUse() {
            return Eater.hungry() && !Eater.isEmerging() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity Current = Eater.getTarget();
            return super.canContinueToUse() && Current != null && Eater.isFood(Current);
        }
    }

    public static final class Maul extends MeleeAttackGoal {
        private final Scp016Glutton Eater;

        public Maul(Scp016Glutton Eater, double Speed) {
            super(Eater, Speed, true);
            this.Eater = Eater;
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity Target) {
            if (Eater.isActing()) {
                return;
            }
            boolean Food = Eater.hungry() && Eater.isFood(Target);
            double Reach = Eater.reach(Target, Food ? Scp016Glutton.GrabReach : Scp016Glutton.SwipeReach);
            if (isTimeToAttack() && Eater.distanceTo(Target) <= Reach && Eater.getSensing().hasLineOfSight(Target)) {
                resetAttackCooldown();
                if (Food) {
                    Eater.beginGrab(Target);
                } else {
                    Eater.beginSwipe(Target);
                }
            }
        }
    }

    public static final class Ambush extends Goal {
        private final Scp016Swimmer Lurker;
        private int Repath;

        public Ambush(Scp016Swimmer Lurker) {
            this.Lurker = Lurker;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        private boolean valid(@Nullable LivingEntity Target) {
            return Target != null && Target.isAlive() && !(Target instanceof Player User && (User.isCreative() || User.isSpectator()));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Lurker.getTarget();
            return !Lurker.isEmerging() && valid(Target) && (Lurker.isInWater() || Lurker.distanceTo(Target) <= Scp016Swimmer.ShoreRange);
        }

        @Override
        public boolean canContinueToUse() {
            return canUse() || Lurker.isActing() && valid(Lurker.getTarget());
        }

        @Override
        public void start() {
            Repath = 0;
        }

        @Override
        public void stop() {
            Lurker.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = Lurker.getTarget();
            if (Target == null || Lurker.isActing()) {
                return;
            }
            Lurker.getLookControl().setLookAt(Target, 20.0f, 20.0f);
            double Distance = Lurker.distanceTo(Target);
            if (Target.isInWater()) {
                if (Lurker.isInWater() && Distance <= Lurker.dragReach(Target) && Lurker.readyToDrag()) {
                    Lurker.getNavigation().stop();
                    Lurker.beginDrag(Target);
                    return;
                }
                if (Distance <= Lurker.biteReach(Target) && Lurker.readyToBite()) {
                    Lurker.bite(Target);
                    return;
                }
                chase(Target, 1.4);
                return;
            }
            if (Lurker.isInWater()) {
                if (Lurker.readyToBreach() && Lurker.breachWindow(Target)) {
                    Lurker.getNavigation().stop();
                    Lurker.beginBreach(Target);
                    return;
                }
                chase(Target, 1.2);
                Path Route = Lurker.getNavigation().getPath();
                if (Route != null && !Route.isDone() && !Lurker.level().getFluidState(Route.getNextNodePos()).is(FluidTags.WATER)) {
                    Lurker.getNavigation().stop();
                }
                return;
            }
            if (Distance <= Lurker.biteReach(Target) && Lurker.readyToBite()) {
                Lurker.bite(Target);
            }
        }

        private void chase(LivingEntity Target, double Speed) {
            if (--Repath <= 0) {
                Repath = 6 + Lurker.getRandom().nextInt(4);
                Lurker.getNavigation().moveTo(Target, Speed);
            }
        }
    }

    public static final class ReturnToWater extends Goal {
        private final Scp016Swimmer Lurker;
        private final int Range;
        private final double Speed;
        private int Wait;
        @Nullable
        private BlockPos Water;

        public ReturnToWater(Scp016Swimmer Lurker, int Range, double Speed) {
            this.Lurker = Lurker;
            this.Range = Range;
            this.Speed = Speed;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (Lurker.isInWater() || Lurker.isActing() || Lurker.isEmerging()) {
                return false;
            }
            if (Wait > 0) {
                Wait--;
                return false;
            }
            Wait = 20;
            Water = nearest();
            return Water != null;
        }

        @Nullable
        private BlockPos nearest() {
            BlockPos Origin = Lurker.blockPosition();
            Level World = Lurker.level();
            BlockPos Best = null;
            double Closest = Double.MAX_VALUE;
            for (BlockPos Pos : BlockPos.betweenClosed(Origin.offset(-Range, -4, -Range), Origin.offset(Range, 3, Range))) {
                if (!World.getFluidState(Pos).is(FluidTags.WATER) || !World.getBlockState(Pos.above()).getCollisionShape(World, Pos.above()).isEmpty()) {
                    continue;
                }
                double Distance = Pos.distSqr(Origin);
                if (Distance < Closest) {
                    Closest = Distance;
                    Best = Pos.immutable();
                }
            }
            return Best;
        }

        @Override
        public boolean canContinueToUse() {
            return !Lurker.isInWater() && Water != null && !Lurker.getNavigation().isDone();
        }

        @Override
        public void start() {
            if (Water != null) {
                Lurker.getNavigation().moveTo(Water.getX() + 0.5, Water.getY(), Water.getZ() + 0.5, Speed);
            }
        }

        @Override
        public void stop() {
            Water = null;
        }
    }

    public static final class Swoop extends Goal {
        public static final int Circling = 0;
        public static final int Diving = 1;
        public static final int Climbing = 2;
        public static final int ClimbTicks = 16;
        private final Scp016Flyer Hunter;
        private int Phase;
        private int Timer;
        private float Angle;
        private double Radius;
        private double Height;

        public Swoop(Scp016Flyer Hunter) {
            this.Hunter = Hunter;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !Hunter.isEmerging() && Hunter.canStrike(Hunter.getTarget());
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            RandomSource Random = Hunter.getRandom();
            circle(Random);
            Angle = Random.nextFloat() * 360.0f;
            Hunter.takeOff();
        }

        private void circle(RandomSource Random) {
            Phase = Circling;
            Timer = 40 + Random.nextInt(40);
            Radius = 4.5 + Random.nextDouble() * 2.5;
            Height = 4.5 + Random.nextDouble() * 2.5;
            Hunter.setSteering(false);
        }

        @Override
        public void stop() {
            Hunter.setSteering(false);
            if (Hunter.action() == Scp016Flyer.ActionDive) {
                Hunter.setAction(Scp016Spawn.ActionNone, 0);
            }
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = Hunter.getTarget();
            if (Target == null) {
                return;
            }
            if (Hunter.isPerched()) {
                Hunter.takeOff();
                return;
            }
            Hunter.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            if (Phase == Circling) {
                Angle += 5.0f;
                Vec3 Point = Target.position().add(Mth.cos(Angle * Mth.DEG_TO_RAD) * Radius, Height, Mth.sin(Angle * Mth.DEG_TO_RAD) * Radius);
                if (!Hunter.level().noCollision(Hunter, Hunter.getType().getDimensions().makeBoundingBox(Point))) {
                    Radius = Math.max(2.0, Radius - 0.5);
                    Height = Math.max(2.5, Height - 0.5);
                }
                Hunter.getMoveControl().setWantedPosition(Point.x, Point.y, Point.z, 1.0);
                if (--Timer <= 0 && Hunter.getY() > Target.getY() + 1.5 && Hunter.getSensing().hasLineOfSight(Target)) {
                    Phase = Diving;
                    Timer = Scp016Flyer.DiveTicks;
                    Hunter.setSteering(true);
                    Hunter.beginDive();
                }
                return;
            }
            if (Phase == Diving) {
                Vec3 Aim = Target.position().add(0.0, Target.getBbHeight() * 0.6, 0.0).subtract(Hunter.position().add(0.0, Hunter.getBbHeight() * 0.5, 0.0));
                Hunter.flight().steer(Aim.normalize().scale(Scp016Flyer.DiveSpeed), Scp016Flyer.DiveAccel);
                if (Hunter.distanceTo(Target) <= Hunter.strikeReach(Target)) {
                    Hunter.strike(Target);
                    climb();
                    return;
                }
                if (--Timer <= 0 || Hunter.horizontalCollision || Hunter.verticalCollision || Hunter.onGround()) {
                    climb();
                }
                return;
            }
            Vec3 Forward = Vec3.directionFromRotation(0.0f, Hunter.getYRot());
            Hunter.flight().steer(Forward.scale(0.45).add(0.0, 0.4, 0.0), 0.08);
            if (--Timer <= 0) {
                circle(Hunter.getRandom());
            }
        }

        private void climb() {
            Phase = Climbing;
            Timer = ClimbTicks;
            if (Hunter.action() == Scp016Flyer.ActionDive) {
                Hunter.setAction(Scp016Spawn.ActionNone, 0);
            }
        }
    }

    public static final class Perch extends Goal {
        public static final int Chance = 100;
        public static final int Tries = 24;
        public static final int Spread = 10;
        private final Scp016Flyer Bird;
        @Nullable
        private Vec3 Spot;
        private int Left;
        private boolean Landed;

        public Perch(Scp016Flyer Bird) {
            this.Bird = Bird;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            if (Bird.getTarget() != null || Bird.isPerched() || Bird.isEmerging() || Bird.getRandom().nextInt(reducedTickDelay(Chance)) != 0) {
                return false;
            }
            Spot = find();
            return Spot != null;
        }

        @Nullable
        public Vec3 find() {
            RandomSource Random = Bird.getRandom();
            Level World = Bird.level();
            Vec3 Best = null;
            for (int Try = 0; Try < Tries; Try++) {
                int X = Mth.floor(Bird.getX()) + Random.nextInt(Spread * 2 + 1) - Spread;
                int Z = Mth.floor(Bird.getZ()) + Random.nextInt(Spread * 2 + 1) - Spread;
                int Top = Mth.floor(Bird.getY()) + 6;
                for (int Y = Top; Y > Top - 14; Y--) {
                    BlockPos Pos = new BlockPos(X, Y, Z);
                    if (World.getBlockState(Pos).isFaceSturdy(World, Pos, Direction.UP) && World.getFluidState(Pos.above()).isEmpty()
                            && World.getBlockState(Pos.above()).getCollisionShape(World, Pos.above()).isEmpty()
                            && World.getBlockState(Pos.above(2)).getCollisionShape(World, Pos.above(2)).isEmpty()) {
                        Vec3 Candidate = new Vec3(X + 0.5, Y + 1.0, Z + 0.5);
                        if (Best == null || Candidate.y > Best.y) {
                            Best = Candidate;
                        }
                        break;
                    }
                    if (!World.getBlockState(Pos).getCollisionShape(World, Pos).isEmpty() || !World.getFluidState(Pos).isEmpty()) {
                        break;
                    }
                }
            }
            return Best;
        }

        @Override
        public boolean canContinueToUse() {
            return Spot != null && Bird.getTarget() == null && Left > 0 && (!Landed || Bird.isPerched());
        }

        @Override
        public void start() {
            Left = 600 + Bird.getRandom().nextInt(800);
            Landed = false;
        }

        @Override
        public void stop() {
            Spot = null;
            Landed = false;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Spot == null) {
                return;
            }
            if (Landed) {
                Left--;
                return;
            }
            Bird.getMoveControl().setWantedPosition(Spot.x, Spot.y + 0.1, Spot.z, 0.8);
            if (Bird.position().distanceTo(Spot) < 0.6) {
                Bird.moveTo(Spot.x, Spot.y, Spot.z, Bird.getYRot(), 0.0f);
                Bird.land();
                Landed = true;
            }
        }
    }
}
