package net.rai.scp_unleashed.scp548;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

public final class Scp548Goals {
    public static final int RetreatReach = 6;
    public static final int RetreatRise = 3;
    public static final int HuntInterval = 10;
    public static final int CalmReach = 3;
    public static final int CalmRise = 1;
    public static final int NestleReach = 4;
    public static final int NestleMin = 300;
    public static final int NestleSpread = 400;

    private Scp548Goals() {
    }

    public static final class Strike extends MeleeAttackGoal {
        private final Scp548Spider Spider;

        public Strike(Scp548Spider Spider) {
            super(Spider, Scp548Spider.ChaseModifier, false);
            this.Spider = Spider;
        }

        @Override
        public boolean canUse() {
            return Spider.isAggressive() && Spider.retreating() == 0 && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return Spider.isAggressive() && Spider.retreating() == 0 && super.canContinueToUse();
        }

        @Override
        protected boolean canPerformAttack(LivingEntity Target) {
            return Spider.biteReady() && super.canPerformAttack(Target);
        }
    }

    public static final class Lunge extends LeapAtTargetGoal {
        private final Scp548Spider Spider;

        public Lunge(Scp548Spider Spider) {
            super(Spider, Scp548Spider.LungeLift);
            this.Spider = Spider;
        }

        @Override
        public boolean canUse() {
            return Spider.isAggressive() && Spider.retreating() == 0 && super.canUse();
        }
    }

    public static final class Retreat extends Goal {
        private final Scp548Spider Spider;
        @Nullable
        private Vec3 Away;

        public Retreat(Scp548Spider Spider) {
            this.Spider = Spider;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return Spider.retreating() > 0 && Spider.getTarget() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Spider.retreating() > 0 && Spider.getTarget() != null;
        }

        @Override
        public void start() {
            pick();
        }

        @Override
        public void tick() {
            if (Away == null || Spider.getNavigation().isDone()) {
                pick();
            }
        }

        private void pick() {
            LivingEntity Threat = Spider.getTarget();
            if (Threat == null) {
                return;
            }
            Away = DefaultRandomPos.getPosAway(Spider, RetreatReach, RetreatRise, Threat.position());
            if (Away != null) {
                Spider.getNavigation().moveTo(Away.x, Away.y, Away.z, Scp548Spider.RetreatModifier);
            }
        }

        @Override
        public void stop() {
            Away = null;
        }
    }

    public static final class Roam extends WaterAvoidingRandomStrollGoal {
        private final Scp548Spider Spider;

        public Roam(Scp548Spider Spider) {
            super(Spider, Scp548Spider.RoamModifier);
            this.Spider = Spider;
            setInterval(Scp548Spider.RoamInterval);
        }

        @Override
        public boolean canUse() {
            setInterval(Spider.isHostile() ? Scp548Spider.RoamInterval : Scp548Spider.CalmRoamInterval);
            return !Spider.isInLove() && super.canUse();
        }

        @Override
        protected Vec3 getPosition() {
            if (Spider.isHostile()) {
                return super.getPosition();
            }
            return DefaultRandomPos.getPos(Spider, CalmReach, CalmRise);
        }
    }

    public static final class Nestle extends Goal {
        private final Scp548Spider Spider;
        @Nullable
        private BlockPos Corner;
        private int Wait;

        public Nestle(Scp548Spider Spider) {
            this.Spider = Spider;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (!Spider.isContained() || Spider.isInLove() || --Wait > 0) {
                return false;
            }
            Wait = NestleMin + Spider.getRandom().nextInt(NestleSpread);
            Corner = corner(Spider);
            return Corner != null;
        }

        @Override
        public boolean canContinueToUse() {
            return Spider.isContained() && !Spider.isInLove() && Corner != null && !Spider.getNavigation().isDone();
        }

        @Override
        public void start() {
            if (Corner != null) {
                Spider.getNavigation().moveTo(Corner.getX() + 0.5, Corner.getY(), Corner.getZ() + 0.5, Scp548Spider.RoamModifier);
            }
        }

        @Override
        public void stop() {
            Corner = null;
        }
    }

    public static boolean isCorner(Level World, BlockPos Pos) {
        if (!World.getBlockState(Pos).getCollisionShape(World, Pos).isEmpty() || World.getBlockState(Pos.below()).getCollisionShape(World, Pos.below()).isEmpty()) {
            return false;
        }
        boolean North = solid(World, Pos.north());
        boolean South = solid(World, Pos.south());
        boolean East = solid(World, Pos.east());
        boolean West = solid(World, Pos.west());
        return (North || South) && (East || West);
    }

    private static boolean solid(Level World, BlockPos Pos) {
        return !World.getBlockState(Pos).getCollisionShape(World, Pos).isEmpty();
    }

    @Nullable
    public static BlockPos corner(Scp548Spider Spider) {
        Level World = Spider.level();
        BlockPos Here = Spider.blockPosition();
        List<BlockPos> Found = new ArrayList<>();
        for (BlockPos Each : BlockPos.betweenClosed(Here.offset(-NestleReach, -1, -NestleReach), Here.offset(NestleReach, 1, NestleReach))) {
            if (isCorner(World, Each) && World.getEntitiesOfClass(Scp548Spider.class, new AABB(Each), Other -> Other != Spider).isEmpty()) {
                Found.add(Each.immutable());
            }
        }
        if (Found.isEmpty()) {
            return null;
        }
        Found.sort(Comparator.comparingDouble(Each -> Each.distSqr(Here)));
        return Found.get(Spider.getRandom().nextInt(Math.min(3, Found.size())));
    }

    public static final class Awake<T extends Goal> extends Goal {
        private final Scp548Spider Spider;
        private final T Inner;

        public Awake(Scp548Spider Spider, T Inner) {
            this.Spider = Spider;
            this.Inner = Inner;
            setFlags(Inner.getFlags());
        }

        @Override
        public boolean canUse() {
            return !Spider.isContained() && Inner.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !Spider.isContained() && Inner.canContinueToUse();
        }

        @Override
        public void start() {
            Inner.start();
        }

        @Override
        public void stop() {
            Inner.stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return Inner.requiresUpdateEveryTick();
        }

        @Override
        public void tick() {
            Inner.tick();
        }
    }

    public static final class Hunt extends TargetGoal {
        private final Scp548Spider Spider;

        public Hunt(Scp548Spider Spider) {
            super(Spider, true, false);
            this.Spider = Spider;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!Spider.isAggressive() || (Spider.tickCount + Spider.getId()) % HuntInterval != 0 || !(Spider.level() instanceof ServerLevel World)) {
                return false;
            }
            LivingEntity Found = Spider.isHostile() ? Scp548Swarm.nearestPrey(World, Spider, Scp548Spider.FollowRange) : Spider.provoker();
            if (Found != null && (!Scp548Swarm.isPrey(Found) || !Spider.isHostile() && !Spider.withinReach(Found))) {
                Found = null;
            }
            if (Found == null) {
                return false;
            }
            targetMob = Found;
            return true;
        }

        @Override
        public void start() {
            Spider.setTarget(targetMob);
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity Current = Spider.getTarget();
            if (!Spider.isAggressive() || !Scp548Swarm.isPrey(Current) || Current.distanceToSqr(Spider) > Scp548Spider.ChaseRange * Scp548Spider.ChaseRange) {
                return false;
            }
            return Spider.isHostile() || Current == Spider.provoker() && Spider.withinReach(Current);
        }

        @Override
        public void stop() {
            Spider.setTarget(null);
            targetMob = null;
        }
    }
}
