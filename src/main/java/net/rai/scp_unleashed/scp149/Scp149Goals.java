package net.rai.scp_unleashed.scp149;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public final class Scp149Goals {
    public static final double DiveRange = 6.0;
    public static final int Repath = 10;
    public static final int BuzzChance = 12;
    public static final int BuzzReach = 6;
    public static final int BuzzRise = 2;
    public static final int ReachCheck = 40;
    public static final int PlanCheck = 100;
    public static final double PerchReach = 1.6;
    public static final int GatherCheck = 20;
    public static final int CocoonSearch = 32;
    public static final double CocoonTouch = 1.6;
    public static final double NestReach = 48.0;
    public static final double CircleRadius = 2.2;
    public static final double CircleHeight = 1.4;
    public static final double CircleSpeed = 0.06;
    public static final double BiteRange = 2.0;
    public static final int PerchChance = 30;
    public static final int PerchDepth = 8;
    public static final int RestMin = 300;
    public static final int RestMax = 600;

    private Scp149Goals() {
    }

    public static final class BiteRun extends Goal {
        private final Scp149Fly Fly;
        private boolean Retreating;
        private int Timer;
        private int Repaths;
        private int Submerged;
        @Nullable
        private Vec3 Away;

        public BiteRun(Scp149Fly Fly) {
            this.Fly = Fly;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Fly.getTarget();
            return Target != null && Target.isAlive() && !Fly.chewing();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Retreating = false;
            Timer = 0;
            Repaths = 0;
        }

        @Override
        public void stop() {
            Fly.getNavigation().stop();
            Fly.setBiting(false);
            Away = null;
            Submerged = 0;
        }

        private boolean dryLine(Vec3 From, Vec3 To) {
            return Fly.level().clip(new ClipContext(From, To, ClipContext.Block.COLLIDER, ClipContext.Fluid.WATER, Fly)).getType() == HitResult.Type.MISS;
        }

        private void circle(LivingEntity Target) {
            double Angle = Fly.tickCount * CircleSpeed + Fly.getId();
            double Top = Fly.waterTop(Target);
            Fly.getMoveControl().setWantedPosition(Target.getX() + Math.cos(Angle) * CircleRadius, Top + CircleHeight, Target.getZ() + Math.sin(Angle) * CircleRadius,
                    Scp149Fly.WanderModifier * 1.5);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        private void retreat(LivingEntity Target, int Ticks) {
            Retreating = true;
            Timer = Ticks;
            Away = Fly.retreatFrom(Target);
            Fly.getNavigation().stop();
        }

        @Override
        public void tick() {
            LivingEntity Target = Fly.getTarget();
            if (Target == null) {
                return;
            }
            RandomSource Random = Fly.getRandom();
            if (Fly.spooked() && !Retreating) {
                retreat(Target, Scp149Fly.MissMin + Random.nextInt(Scp149Fly.MissMax - Scp149Fly.MissMin + 1));
            }
            if (Retreating) {
                Timer--;
                if (Away == null || Timer <= 0) {
                    Retreating = false;
                    Away = null;
                    return;
                }
                if (Fly.position().distanceToSqr(Away) > 0.5) {
                    Fly.getMoveControl().setWantedPosition(Away.x, Away.y, Away.z, Scp149Fly.RetreatModifier);
                }
                return;
            }
            Vec3 Aim = Fly.biteAim(Target);
            if (Aim == null) {
                Fly.setBiting(false);
                if (++Submerged >= Scp149Fly.SubmergedLimit) {
                    Submerged = 0;
                    Fly.shun(Target);
                    Fly.setTarget(null);
                    return;
                }
                circle(Target);
                return;
            }
            Submerged = Math.max(0, Submerged - 2);
            Vec3 Middle = Fly.position().add(0.0, Fly.getBbHeight() * 0.5, 0.0);
            double Distance = Middle.distanceTo(Aim);
            if (Distance < 3.0) {
                Fly.getLookControl().setLookAt(Target, 60.0f, 60.0f);
                float Yaw = (float) (Mth.atan2(Target.getZ() - Fly.getZ(), Target.getX() - Fly.getX()) * Mth.RAD_TO_DEG) - 90.0f;
                Fly.setYRot(Mth.approachDegrees(Fly.getYRot(), Yaw, 30.0f));
                Fly.yBodyRot = Fly.getYRot();
                Fly.yHeadRot = Fly.getYRot();
            }
            boolean Clear = Distance < DiveRange && Fly.hasLineOfSight(Target) && dryLine(Middle, Aim);
            Fly.setBiting(Clear && Distance < BiteRange);
            if (Clear) {
                Fly.getNavigation().stop();
                Fly.getMoveControl().setWantedPosition(Aim.x, Aim.y - Fly.getBbHeight() * 0.5, Aim.z, Scp149Fly.ChaseModifier);
            } else if (--Repaths <= 0) {
                Repaths = Repath;
                Fly.getNavigation().moveTo(Target, Scp149Fly.ChaseModifier);
            }
            if (Fly.touching(Target) && !Fly.isActing()) {
                boolean Landed = Fly.bite(Target);
                retreat(Target, Landed ? Scp149Fly.RetreatMin + Random.nextInt(Scp149Fly.RetreatMax - Scp149Fly.RetreatMin + 1)
                        : Scp149Fly.MissMin + Random.nextInt(Scp149Fly.MissMax - Scp149Fly.MissMin + 1));
            }
        }
    }

    public static final class Seek extends NearestAttackableTargetGoal<LivingEntity> {
        public static final double HostHandicap = 1.5;
        public static final double HostSlack = 2.0;
        public static final int Recheck = 20;
        private final Scp149Fly Fly;
        private int Again;

        public Seek(Scp149Fly Fly) {
            super(Fly, LivingEntity.class, 10, true, false, Scp149Fly::isPerson);
            this.Fly = Fly;
        }

        public static boolean fresh(LivingEntity Candidate) {
            return !Scp149Host.isHost(Candidate) && !Scp149Host.shielded(Candidate);
        }

        public static double score(double Distance, boolean Fresh) {
            return Fresh ? Distance : Distance * HostHandicap + HostSlack;
        }

        @Override
        public boolean canUse() {
            return !Fly.isGorged() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !Fly.isGorged() && super.canContinueToUse();
        }

        @Override
        protected void findTarget() {
            target = choose();
        }

        @Nullable
        private LivingEntity choose() {
            LivingEntity Best = null;
            double BestScore = Double.MAX_VALUE;
            for (LivingEntity Candidate : Fly.level().getEntitiesOfClass(LivingEntity.class, getTargetSearchArea(getFollowDistance()), Found -> Found != Fly)) {
                if (!targetConditions.test(Fly, Candidate) || !Fly.reachable(Candidate)) {
                    continue;
                }
                double Score = score(Math.sqrt(Fly.distanceToSqr(Candidate)), fresh(Candidate));
                if (Score < BestScore) {
                    BestScore = Score;
                    Best = Candidate;
                }
            }
            return Best;
        }

        @Override
        public void start() {
            super.start();
            Again = Recheck;
        }

        @Override
        public void tick() {
            if (--Again > 0) {
                return;
            }
            Again = Recheck;
            LivingEntity Current = Fly.getTarget();
            if (Current == null || fresh(Current)) {
                return;
            }
            LivingEntity Better = choose();
            if (Better != null && Better != Current && fresh(Better)) {
                target = Better;
                Fly.setTarget(Better);
            }
        }
    }

    public static final class Buzz extends Goal {
        private final Scp149Fly Fly;
        @Nullable
        private Vec3 Spot;
        private int Patience;

        public Buzz(Scp149Fly Fly) {
            this.Fly = Fly;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (Fly.getTarget() != null || Fly.chewing() || Fly.getRandom().nextInt(reducedTickDelay(BuzzChance)) != 0) {
                return false;
            }
            Spot = pick();
            return Spot != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Spot != null && Patience > 0 && Fly.getTarget() == null && !Fly.chewing() && Fly.position().distanceToSqr(Spot) > 0.4;
        }

        @Override
        public void start() {
            Patience = 80;
        }

        @Override
        public void stop() {
            Spot = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Patience--;
            if (Spot != null) {
                Fly.getMoveControl().setWantedPosition(Spot.x, Spot.y, Spot.z, Scp149Fly.WanderModifier * (Fly.isRestless() ? 1.4 : 1.0));
            }
        }

        @Nullable
        private Vec3 pick() {
            RandomSource Random = Fly.getRandom();
            int Reach = Fly.isRestless() ? BuzzReach + 3 : BuzzReach;
            for (int Attempt = 0; Attempt < 10; Attempt++) {
                Vec3 Candidate = Fly.position().add((Random.nextDouble() * 2.0 - 1.0) * Reach, (Random.nextDouble() * 2.0 - 1.0) * BuzzRise, (Random.nextDouble() * 2.0 - 1.0) * Reach);
                AABB Box = Fly.getDimensions(Fly.getPose()).makeBoundingBox(Candidate);
                if (!Fly.level().noCollision(Fly, Box) || Fly.wet(Box.expandTowards(0.0, -Scp149Fly.WaterMargin, 0.0))) {
                    continue;
                }
                HitResult Hit = Fly.level().clip(new ClipContext(Fly.position().add(0.0, Fly.getBbHeight() * 0.5, 0.0), Candidate.add(0.0, Fly.getBbHeight() * 0.5, 0.0),
                        ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, Fly));
                if (Hit.getType() == HitResult.Type.MISS) {
                    return Candidate;
                }
            }
            return null;
        }
    }

    public static final class Perch extends Goal {
        private final Scp149Fly Fly;
        @Nullable
        private Vec3 Spot;
        private int Left;
        private boolean Landed;

        public Perch(Scp149Fly Fly) {
            this.Fly = Fly;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
        }

        private boolean calm() {
            return Fly.isGorged() && Fly.getTarget() == null && !Fly.isRestless() && !Fly.chewing() && !Fly.spooked() && !Fly.isInWater();
        }

        @Override
        public boolean canUse() {
            if (!calm() || Fly.getRandom().nextInt(reducedTickDelay(PerchChance)) != 0) {
                return false;
            }
            Spot = landing();
            return Spot != null;
        }

        @Nullable
        private Vec3 landing() {
            Vec3 From = Fly.position();
            BlockHitResult Hit = Fly.level().clip(new ClipContext(From, From.subtract(0.0, PerchDepth, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, Fly));
            if (Hit.getType() != HitResult.Type.BLOCK || Hit.getDirection() != Direction.UP || !Fly.level().getFluidState(Hit.getBlockPos()).isEmpty()) {
                return null;
            }
            Vec3 Place = new Vec3(From.x, Hit.getLocation().y + 0.01, From.z);
            AABB Box = Fly.getDimensions(Fly.getPose()).makeBoundingBox(Place);
            return Fly.level().noCollision(Fly, Box) && !Fly.wet(Box.inflate(0.3)) ? Place : null;
        }

        @Override
        public boolean canContinueToUse() {
            return Spot != null && Left > 0 && calm();
        }

        @Override
        public void start() {
            Left = RestMin + Fly.getRandom().nextInt(RestMax - RestMin + 1);
            Landed = false;
        }

        @Override
        public void stop() {
            Fly.setResting(false);
            Spot = null;
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
            if (!Landed) {
                Fly.getMoveControl().setWantedPosition(Spot.x, Spot.y, Spot.z, Scp149Fly.WanderModifier);
                if (Fly.position().distanceToSqr(Spot) < 0.01) {
                    Landed = true;
                    Fly.setResting(true);
                }
                return;
            }
            Left--;
            Fly.setDeltaMovement(Vec3.ZERO);
        }
    }

    public static final class Chew extends Goal {
        private final Scp149Fly Fly;
        @Nullable
        private Scp149Escape.Plan Plan;
        private int Check;
        private int Reach;
        private boolean Stuck;

        public Chew(Scp149Fly Fly) {
            this.Fly = Fly;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        private boolean allowed() {
            return Fly.isRestless() && Fly.level() instanceof ServerLevel World && EventHooks.canEntityGrief(World, Fly);
        }

        private boolean preyReachable() {
            LivingEntity Target = Fly.getTarget();
            if (Target == null || !Target.isAlive()) {
                return false;
            }
            if (--Reach > 0) {
                return !Stuck;
            }
            Reach = ReachCheck;
            Path Route = Fly.getNavigation().createPath(Target, 0);
            Stuck = Route == null || !Route.canReach();
            return !Stuck;
        }

        @Override
        public boolean canUse() {
            if (!allowed() || preyReachable() || --Check > 0) {
                return false;
            }
            Check = PlanCheck + Fly.getRandom().nextInt(20);
            ServerLevel World = (ServerLevel) Fly.level();
            LivingEntity Target = Fly.getTarget();
            Plan = Scp149Escape.plan(World, Fly, Target != null ? Target.position() : null);
            return Plan != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Plan != null && allowed() && Fly.level() instanceof ServerLevel World && Scp149Escape.chewable(World, Plan.Block(), Fly);
        }

        @Override
        public void start() {
            Fly.setChewing(true);
            Fly.getNavigation().stop();
        }

        @Override
        public void stop() {
            if (Plan != null && Fly.level() instanceof ServerLevel World) {
                World.destroyBlockProgress(Fly.getId(), Plan.Block(), -1);
            }
            Fly.setChewing(false);
            Plan = null;
            Check = PlanCheck / 2;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Plan == null || !(Fly.level() instanceof ServerLevel World)) {
                return;
            }
            Vec3 Perch = Vec3.atCenterOf(Plan.Perch()).subtract(0.0, Fly.getBbHeight() * 0.5, 0.0);
            Vec3 Face = Vec3.atCenterOf(Plan.Block());
            Fly.getLookControl().setLookAt(Face.x, Face.y, Face.z);
            float Yaw = (float) (Mth.atan2(Face.z - Fly.getZ(), Face.x - Fly.getX()) * Mth.RAD_TO_DEG) - 90.0f;
            Fly.setYRot(Mth.approachDegrees(Fly.getYRot(), Yaw, 20.0f));
            Fly.yBodyRot = Fly.getYRot();
            if (Fly.position().add(0.0, Fly.getBbHeight() * 0.5, 0.0).distanceTo(Face) > PerchReach) {
                Fly.getMoveControl().setWantedPosition(Perch.x, Perch.y, Perch.z, Scp149Fly.WanderModifier);
                return;
            }
            Fly.setDeltaMovement(Fly.getDeltaMovement().scale(0.6));
            if (Fly.tickCount % 6 == 0) {
                Fly.playLayers(Scp149Registry.ChewLayers, 0.5f, 0.9f + Fly.getRandom().nextFloat() * 0.25f);
            }
            if (Fly.tickCount % 10 == 0) {
                BlockPos Block = Plan.Block();
                World.playSound(null, Block, World.getBlockState(Block).getSoundType(World, Block, Fly).getHitSound(), Fly.getSoundSource(), 0.35f, 1.2f);
            }
            if (Scp149Escape.gnaw(World, Plan.Block(), Fly)) {
                Plan = null;
                Check = 10;
            }
        }
    }

    public static final class Gather extends Goal {
        private final Scp149Larva Larva;
        private int Check;

        public Gather(Scp149Larva Larva) {
            this.Larva = Larva;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Larva.gathering() && Larva.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (!(Larva.level() instanceof ServerLevel World) || --Check > 0) {
                return;
            }
            Check = GatherCheck;
            Scp149CocoonBlockEntity Cocoon = Scp149Brood.cocoonFor(World, Larva.brood(), Larva.blockPosition(), CocoonSearch);
            if (Cocoon != null) {
                Vec3 Centre = Vec3.atBottomCenterOf(Cocoon.getBlockPos());
                Larva.setLonely(0);
                if (Larva.position().distanceTo(Centre) <= CocoonTouch) {
                    Scp149Brood.absorb(World, Cocoon, Larva);
                    return;
                }
                Larva.getNavigation().moveTo(Centre.x, Centre.y, Centre.z, Scp149Larva.WanderModifier);
                return;
            }
            List<Scp149Larva> Huddle = Scp149Brood.huddle(Larva);
            if (Huddle.size() + 1 >= Scp149Brood.Quorum && Scp149Brood.build(World, Larva, Huddle)) {
                return;
            }
            Scp149Larva Mate = nearestMate(World);
            Larva.setLonely(Mate == null ? Larva.lonely() + GatherCheck : 0);
            BlockPos Nest = Larva.nest();
            Vec3 Home = Vec3.atBottomCenterOf(Nest);
            if (Larva.position().distanceTo(Home) <= NestReach && Larva.position().distanceTo(Home) > 1.0) {
                Larva.getNavigation().moveTo(Home.x, Home.y, Home.z, Scp149Larva.WanderModifier);
            } else if (Mate != null && Mate.distanceTo(Larva) > 1.2) {
                Larva.getNavigation().moveTo(Mate, Scp149Larva.WanderModifier);
            }
        }

        @Nullable
        private Scp149Larva nearestMate(ServerLevel World) {
            Scp149Larva Best = null;
            double Closest = Scp149Larva.LonelyRange * Scp149Larva.LonelyRange;
            for (Scp149Larva Other : World.getEntitiesOfClass(Scp149Larva.class, Larva.getBoundingBox().inflate(Scp149Larva.LonelyRange),
                    Found -> Found != Larva && Found.isAlive() && Larva.brood().equals(Found.brood()))) {
                double Distance = Other.distanceToSqr(Larva);
                if (Distance < Closest) {
                    Closest = Distance;
                    Best = Other;
                }
            }
            return Best;
        }
    }
}
