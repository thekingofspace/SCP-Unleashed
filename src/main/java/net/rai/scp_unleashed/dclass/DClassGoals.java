package net.rai.scp_unleashed.dclass;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class DClassGoals {
    public static final int BedRange = 24;
    public static final double BedReach = 2.2;
    public static final int BedPatience = 400;
    public static final int BedRetry = 200;

    private DClassGoals() {
    }

    public static final class Stroll extends WaterAvoidingRandomStrollGoal {
        private final DClass Owner;

        public Stroll(DClass Owner) {
            super(Owner, 0.6);
            this.Owner = Owner;
        }

        @Override
        public boolean canUse() {
            setInterval(Owner.trait().strollInterval());
            return !Owner.isRestingPose() && super.canUse();
        }

        @Nullable
        @Override
        protected Vec3 getPosition() {
            return LandRandomPos.getPos(mob, Owner.trait().strollRange(), 7);
        }
    }

    public static final class Watch extends LookAtPlayerGoal {
        public static final float WatchRange = 8.0f;
        public static final float WatchChance = 0.06f;

        private final DClass Owner;

        public Watch(DClass Owner) {
            super(Owner, Player.class, WatchRange, WatchChance);
            this.Owner = Owner;
        }

        @Override
        public boolean canUse() {
            return !Owner.isRestingPose() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !Owner.isRestingPose() && super.canContinueToUse();
        }
    }

    public static final class Glance extends RandomLookAroundGoal {
        private final DClass Owner;

        public Glance(DClass Owner) {
            super(Owner);
            this.Owner = Owner;
        }

        @Override
        public boolean canUse() {
            return !Owner.isRestingPose() && super.canUse();
        }
    }

    public static final class Defend extends HurtByTargetGoal {
        private final DClass Owner;

        public Defend(DClass Owner) {
            super(Owner);
            this.Owner = Owner;
        }

        @Override
        public boolean canUse() {
            return Owner.trait().fightsBack() && super.canUse();
        }
    }

    public static final class Panic extends Goal {
        private final DClass Owner;
        @Nullable
        private Vec3 Escape;

        public Panic(DClass Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            LivingEntity Attacker = Scp268.isNeglected(Owner.getLastHurtByMob()) ? null : Owner.getLastHurtByMob();
            boolean Scared = Attacker != null && Owner.tickCount - Owner.getLastHurtByMobTimestamp() < 100 || Owner.isOnFire() || Owner.isFreezing();
            if (!Scared || Owner.getHealth() > Owner.getMaxHealth() * Owner.trait().fleeHealth() && !Owner.isOnFire()) {
                return false;
            }
            if (Owner.trait().fightsBack() && Owner.getHealth() > Owner.getMaxHealth() * 0.2f) {
                return false;
            }
            Escape = Attacker != null ? DefaultRandomPos.getPosAway(Owner, 16, 7, Attacker.position()) : DefaultRandomPos.getPos(Owner, 8, 4);
            return Escape != null;
        }

        @Override
        public void start() {
            Owner.wake();
            Owner.getNavigation().moveTo(Escape.x, Escape.y, Escape.z, 1.35);
            Owner.setSprinting(true);
        }

        @Override
        public boolean canContinueToUse() {
            return !Owner.getNavigation().isDone();
        }

        @Override
        public void stop() {
            Owner.setSprinting(false);
            Escape = null;
        }
    }

    public static final class Eat extends Goal {
        private final DClass Owner;

        public Eat(DClass Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (Owner.isUsingItem() || Owner.getTarget() != null || Owner.isRestingPose() || Owner.tickCount % 20 != 0) {
                return false;
            }
            int Food = Owner.food().food();
            boolean Hungry = Food < Owner.trait().hungerThreshold() || Food < 18 && Owner.getHealth() < Owner.getMaxHealth() * 0.6f;
            return Hungry && Owner.findFood(Food <= 6) >= 0;
        }

        @Override
        public void start() {
            Owner.getNavigation().stop();
            int Slot = Owner.findFood(Owner.food().food() <= 6);
            if (Slot >= 0) {
                Owner.beginEating(Slot);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return Owner.isUsingItem();
        }
    }

    public static final class Sleep extends Goal {
        private final DClass Owner;
        @Nullable
        private DClassBeds.Spot Bed;
        private int Walking;
        private int Retry;

        public Sleep(DClass Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return Owner.trait().sleeps() && Owner.isBedtime() && Owner.getTarget() == null && Owner.onGround() && !Owner.isInWater()
                    && Owner.tickCount - Owner.getLastHurtByMobTimestamp() > 200 && !Owner.isUsingItem();
        }

        @Override
        public void start() {
            Walking = 0;
            Retry = 0;
            if (Owner.isSleeping()) {
                Bed = DClassBeds.sleepingSpot(Owner);
                claim();
                return;
            }
            Bed = findBed();
            if (Bed == null) {
                Owner.lieDown();
            } else {
                claim();
                Vec3 Target = Vec3.atBottomCenterOf(Bed.Head());
                Owner.getNavigation().moveTo(Target.x, Target.y, Target.z, 0.6);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return Owner.isBedtime() && Owner.getTarget() == null && Owner.tickCount - Owner.getLastHurtByMobTimestamp() > 40;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Bed == null && Owner.isResting() && !Owner.isSleeping() && ++Retry >= BedRetry) {
                Retry = 0;
                DClassBeds.Spot Found = findBed();
                if (Found != null) {
                    Bed = Found;
                    Walking = 0;
                    Owner.setResting(false);
                    claim();
                    Vec3 Target = Vec3.atBottomCenterOf(Bed.Head());
                    Owner.getNavigation().moveTo(Target.x, Target.y, Target.z, 0.6);
                    return;
                }
            }
            if (Owner.isRestingPose() || Bed == null) {
                Owner.getNavigation().stop();
                if (Bed != null && Owner.isSleeping()) {
                    claim();
                }
                return;
            }
            Walking++;
            if (!(Owner.level() instanceof ServerLevel World) || !DClassBeds.available(World, Bed, Owner)) {
                drop();
                Owner.lieDown();
                return;
            }
            claim();
            if (Bed.reachSqr(Owner) < BedReach * BedReach) {
                Owner.getNavigation().stop();
                Owner.startSleeping(Bed.Head());
                return;
            }
            if (Walking > BedPatience || Owner.getNavigation().isDone() && Walking > 40) {
                drop();
                Owner.lieDown();
            }
        }

        @Override
        public void stop() {
            Owner.wake();
            drop();
        }

        private void claim() {
            if (Bed != null && Owner.level() instanceof ServerLevel World) {
                DClassBeds.claim(World, Bed, Owner);
            }
        }

        private void drop() {
            Bed = null;
            if (Owner.level() instanceof ServerLevel World) {
                DClassBeds.release(World, Owner);
            }
        }

        @Nullable
        private DClassBeds.Spot findBed() {
            if (!(Owner.level() instanceof ServerLevel World)) {
                return null;
            }
            BlockPos Here = Owner.blockPosition();
            Map<BlockPos, DClassBeds.Spot> Options = new HashMap<>();
            World.getPoiManager().findAll(Kind -> Kind.is(PoiTypes.HOME), Where -> true, Here, BedRange, PoiManager.Occupancy.ANY)
                    .forEach(Where -> offer(World, Options, Where));
            for (BlockPos Where : DClassBeds.nearby(World, Here, BedRange)) {
                offer(World, Options, Where);
            }
            DClassBeds.Spot Best = null;
            double Closest = Double.MAX_VALUE;
            for (DClassBeds.Spot Option : Options.values()) {
                if (!DClassBeds.available(World, Option, Owner)) {
                    continue;
                }
                double Distance = Owner.distanceToSqr(Option.centre());
                if (Distance < Closest) {
                    Closest = Distance;
                    Best = Option;
                }
            }
            return Best;
        }

        private static void offer(ServerLevel World, Map<BlockPos, DClassBeds.Spot> Options, BlockPos Pos) {
            DClassBeds.Spot Found = DClassBeds.spotAt(World, Pos);
            if (Found != null) {
                Options.putIfAbsent(Found.Head(), Found);
            }
        }
    }

    public static class FollowPlayer extends Goal {
        protected final DClass Owner;
        @Nullable
        protected Player Leader;

        public FollowPlayer(DClass Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (Owner.trait() != DClassTrait.Obedient || Owner.isRestingPose()) {
                return false;
            }
            Leader = Owner.level().getNearestPlayer(Owner.getX(), Owner.getY(), Owner.getZ(), 16.0, Found -> !Scp268.isNeglected(Found));
            return Leader != null && !Leader.isSpectator() && Owner.distanceToSqr(Leader) > 9.0;
        }

        @Override
        public boolean canContinueToUse() {
            return Leader != null && Leader.isAlive() && !Scp268.isNeglected(Leader) && !Owner.isRestingPose() && Owner.distanceToSqr(Leader) > 6.25 && Owner.distanceToSqr(Leader) < 400.0;
        }

        @Override
        public void tick() {
            if (Leader == null) {
                return;
            }
            Owner.getLookControl().setLookAt(Leader, 20.0f, Owner.getMaxHeadXRot());
            if (Owner.tickCount % 10 == 0) {
                Owner.getNavigation().moveTo(Leader, Owner.distanceToSqr(Leader) > 64.0 ? 1.0 : 0.75);
            }
        }

        @Override
        public void stop() {
            Leader = null;
            Owner.getNavigation().stop();
        }
    }

    public static final class Approach extends Goal {
        private final DClass Owner;
        @Nullable
        private LivingEntity Interest;
        private int Lingering;

        public Approach(DClass Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (Owner.isRestingPose() || Owner.getRandom().nextInt(160) != 0) {
                return false;
            }
            DClassTrait Trait = Owner.trait();
            if (Trait == DClassTrait.Curious || Trait == DClassTrait.Cheerful) {
                Interest = Owner.level().getNearestPlayer(Owner.getX(), Owner.getY(), Owner.getZ(), Trait == DClassTrait.Curious ? 14.0 : 10.0, Found -> !Scp268.isNeglected(Found));
            } else if (Trait == DClassTrait.Sociable) {
                List<DClass> Company = Owner.level().getEntitiesOfClass(DClass.class, Owner.getBoundingBox().inflate(16.0), Found -> Found != Owner && Found.isAlive());
                Interest = Company.stream().min(Comparator.comparingDouble(Owner::distanceToSqr)).orElse(null);
            } else {
                Interest = null;
            }
            return Interest != null && !Interest.isSpectator() && Owner.distanceToSqr(Interest) > 16.0;
        }

        @Override
        public void start() {
            Lingering = 60 + Owner.getRandom().nextInt(60);
        }

        @Override
        public boolean canContinueToUse() {
            return Interest != null && Interest.isAlive() && Lingering > 0 && !Owner.isRestingPose();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Interest == null) {
                return;
            }
            Owner.getLookControl().setLookAt(Interest, 30.0f, Owner.getMaxHeadXRot());
            if (Owner.distanceToSqr(Interest) > 9.0) {
                if (Owner.tickCount % 10 == 0) {
                    Owner.getNavigation().moveTo(Interest, 0.65);
                }
            } else {
                Owner.getNavigation().stop();
                Lingering--;
            }
        }

        @Override
        public void stop() {
            Interest = null;
        }
    }

    public static final class SeekItems extends Goal {
        private final DClass Owner;
        @Nullable
        private ItemEntity Wanted;

        public SeekItems(DClass Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            boolean Collector = Owner.crime().hoards() || Owner.trait() == DClassTrait.Curious || Owner.trait() == DClassTrait.Gluttonous;
            if (!Collector || Owner.isRestingPose() || !Owner.canPickUpLoot() || Owner.tickCount % 15 != 0) {
                return false;
            }
            double Range = Owner.crime().hoards() ? 12.0 : 6.0;
            List<ItemEntity> Loot = Owner.level().getEntitiesOfClass(ItemEntity.class, Owner.getBoundingBox().inflate(Range),
                    Found -> !Found.hasPickUpDelay() && Owner.wantsToPickUp(Found.getItem()) && (Owner.crime().hoards() || Owner.trait() != DClassTrait.Gluttonous || Found.getItem().getFoodProperties(Owner) != null));
            Wanted = Loot.stream().min(Comparator.comparingDouble(Owner::distanceToSqr)).orElse(null);
            return Wanted != null;
        }

        @Override
        public void start() {
            if (Wanted != null) {
                Owner.getNavigation().moveTo(Wanted, 0.8);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return Wanted != null && Wanted.isAlive() && !Owner.getNavigation().isDone();
        }

        @Override
        public void stop() {
            Wanted = null;
        }
    }
}
