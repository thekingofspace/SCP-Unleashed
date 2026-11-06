package net.rai.scp_unleashed.scp3008.staff;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public final class Scp3008StaffGoals {
    public static final TagKey<Block> Shelves = TagKey.create(Registries.BLOCK, Scp3008StaffRegistry.location("scp3008_liftable"));
    public static final float GlanceRange = 10.0f;
    public static final float GlanceChance = 0.35f;
    public static final double StareRange = 10.0;
    public static final int StareMin = 60;
    public static final int StareSpread = 100;
    public static final int StareRest = 240;
    public static final int StareRestSpread = 300;
    public static final int TidyReach = 8;
    public static final int TidyRise = 2;
    public static final int TidyAttempts = 28;
    public static final int TidyMin = 80;
    public static final int TidySpread = 100;
    public static final int PostMin = 160;
    public static final int PostSpread = 240;
    public static final int TidyRest = 400;
    public static final int TidyRestSpread = 800;
    public static final int TravelLimit = 200;
    public static final double ArriveDistance = 1.3;
    public static final int TidySoundInterval = 24;
    public static final int WanderInterval = 140;
    public static final int ProwlInterval = 50;
    public static final int HuntInterval = 10;
    public static final double ChaseRange = 56.0;
    public static final int RepathNear = 4;
    public static final int RepathFar = 12;
    public static final double NearChase = 8.0;
    public static final float LookTurn = 30.0f;

    private Scp3008StaffGoals() {
    }

    public static boolean isPrey(@Nullable LivingEntity Target) {
        if (Target == null || !Target.isAlive() || Target.isRemoved() || !Targeting.isTargetable(Target)) {
            return false;
        }
        if (Target instanceof Player User) {
            return !User.isCreative() && !User.isSpectator();
        }
        return ControlBodies.isPersonLike(Target);
    }

    @Nullable
    public static LivingEntity nearestPrey(ServerLevel World, Scp3008Staff Staff, double Range) {
        LivingEntity Best = null;
        double BestDistance = Range * Range;
        for (ServerPlayer User : World.players()) {
            double Distance = User.distanceToSqr(Staff);
            if (Distance < BestDistance && isPrey(User)) {
                Best = User;
                BestDistance = Distance;
            }
        }
        List<LivingEntity> People = World.getEntitiesOfClass(LivingEntity.class, Staff.getBoundingBox().inflate(Range), Each -> !(Each instanceof Player) && isPrey(Each));
        for (LivingEntity Person : People) {
            double Distance = Person.distanceToSqr(Staff);
            if (Distance < BestDistance) {
                Best = Person;
                BestDistance = Distance;
            }
        }
        return Best;
    }

    public static boolean idle(Scp3008Staff Staff) {
        return !Staff.isHostile() && !Staff.hostile() && !Staff.engaged() && !Staff.announcing();
    }

    public static void face(Scp3008Staff Staff, Vec3 Target) {
        float Yaw = (float) (Mth.atan2(Target.z - Staff.getZ(), Target.x - Staff.getX()) * Mth.RAD_TO_DEG) - 90.0f;
        float Turned = Mth.approachDegrees(Staff.getYRot(), Yaw, 12.0f);
        Staff.setYRot(Turned);
        Staff.setYBodyRot(Turned);
    }

    public static final class Calm<T extends Goal> extends Goal {
        private final Scp3008Staff Staff;
        private final T Inner;

        public Calm(Scp3008Staff Staff, T Inner) {
            this.Staff = Staff;
            this.Inner = Inner;
            setFlags(Inner.getFlags());
        }

        @Override
        public boolean canUse() {
            return idle(Staff) && Inner.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return idle(Staff) && Inner.canContinueToUse();
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

    public static final class Announce extends Goal {
        private final Scp3008Staff Staff;

        public Announce(Scp3008Staff Staff) {
            this.Staff = Staff;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return Staff.announcing();
        }

        @Override
        public void start() {
            Staff.getNavigation().stop();
        }

        @Override
        public boolean canContinueToUse() {
            return Staff.announcing();
        }
    }

    public static final class Strike extends Goal {
        private final Scp3008Staff Staff;
        private int Repath;

        public Strike(Scp3008Staff Staff) {
            this.Staff = Staff;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Staff.getTarget();
            return isPrey(Target) && Staff.engaged() && !Staff.announcing();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity Target = Staff.getTarget();
            return isPrey(Target) && Staff.engaged() && !Staff.announcing() && Target.distanceToSqr(Staff) < ChaseRange * ChaseRange;
        }

        @Override
        public void start() {
            Staff.setAggressive(true);
            Staff.setActivity(Scp3008Staff.ActivityIdle);
            Repath = 0;
        }

        @Override
        public void stop() {
            Staff.setAggressive(false);
            Staff.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = Staff.getTarget();
            if (Target == null) {
                return;
            }
            Staff.getLookControl().setLookAt(Target, LookTurn, LookTurn);
            double Distance = Staff.distanceToSqr(Target);
            if (--Repath <= 0) {
                Repath = Distance < NearChase * NearChase ? RepathNear : RepathFar;
                Staff.getNavigation().moveTo(Target, Staff.chaseModifier());
            }
            if (Staff.canSwing() && Staff.isWithinMeleeAttackRange(Target) && Staff.getSensing().hasLineOfSight(Target)) {
                Staff.swingAt(Target);
            }
        }
    }

    public static final class Stare extends Goal {
        private final Scp3008Staff Staff;
        @Nullable
        private LivingEntity Focus;
        private int Ticks;
        private int Rest;

        public Stare(Scp3008Staff Staff) {
            this.Staff = Staff;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!idle(Staff)) {
                return false;
            }
            LivingEntity Forced = Staff.stareFocus();
            if (Forced != null && isPrey(Forced)) {
                Focus = Forced;
                Ticks = Staff.stareTicks();
                return true;
            }
            if (Rest > 0) {
                Rest--;
                return false;
            }
            if ((Staff.tickCount + Staff.getId()) % 10 != 0) {
                return false;
            }
            Player Near = Staff.level().getNearestPlayer(Staff.getX(), Staff.getY(), Staff.getZ(), StareRange,
                    Each -> Each instanceof Player User && isPrey(User) && Staff.getSensing().hasLineOfSight(User));
            if (Near == null) {
                return false;
            }
            Focus = Near;
            Ticks = StareMin + Staff.getRandom().nextInt(StareSpread);
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return idle(Staff) && Ticks > 0 && isPrey(Focus) && Focus.distanceToSqr(Staff) < StareRange * StareRange * 2.25;
        }

        @Override
        public void start() {
            Staff.getNavigation().stop();
            Staff.setActivity(Scp3008Staff.ActivityStare);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Ticks--;
            if (Focus != null) {
                Staff.getLookControl().setLookAt(Focus, 6.0f, 40.0f);
                if (Ticks % 3 == 0) {
                    face(Staff, Focus.position());
                }
            }
        }

        @Override
        public void stop() {
            Staff.setActivity(Scp3008Staff.ActivityIdle);
            Focus = null;
            Rest = StareRest + Staff.getRandom().nextInt(StareRestSpread);
            Staff.clearStare();
        }
    }

    public static final class Tidy extends Goal {
        private final Scp3008Staff Staff;
        private final boolean Post;
        @Nullable
        private BlockPos Spot;
        @Nullable
        private BlockPos Shelf;
        private int Ticks;
        private int Travel;
        private int Rest;
        private boolean Working;

        public Tidy(Scp3008Staff Staff, boolean Post) {
            this.Staff = Staff;
            this.Post = Post;
            this.Rest = Staff.getRandom().nextInt(TidyRestSpread);
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (!idle(Staff)) {
                return false;
            }
            if (Rest > 0) {
                Rest--;
                return false;
            }
            if ((Staff.tickCount + Staff.getId()) % 20 != 0) {
                return false;
            }
            if (!find()) {
                Rest = TidyRest / 2;
                return false;
            }
            return true;
        }

        private boolean find() {
            Level World = Staff.level();
            BlockPos Here = Staff.blockPosition();
            BlockPos Plain = null;
            BlockPos PlainShelf = null;
            for (int Attempt = 0; Attempt < TidyAttempts; Attempt++) {
                BlockPos Pos = Here.offset(Staff.getRandom().nextInt(TidyReach * 2 + 1) - TidyReach, Staff.getRandom().nextInt(TidyRise * 2 + 1) - TidyRise,
                        Staff.getRandom().nextInt(TidyReach * 2 + 1) - TidyReach);
                if (!standable(World, Pos, Staff.getBbHeight())) {
                    continue;
                }
                for (Direction Side : Direction.Plane.HORIZONTAL) {
                    BlockPos Neighbour = Pos.relative(Side);
                    BlockPos Height = Post ? Neighbour : Neighbour.above();
                    BlockState Found = World.getBlockState(Height);
                    if (Found.is(Shelves) || World.getBlockState(Neighbour).is(Shelves)) {
                        Spot = Pos;
                        Shelf = Height;
                        return true;
                    }
                    if (Plain == null && !Found.getCollisionShape(World, Height).isEmpty() && (!Post || World.getBlockState(Neighbour.above()).getCollisionShape(World, Neighbour.above()).isEmpty())) {
                        Plain = Pos;
                        PlainShelf = Height;
                    }
                }
            }
            Spot = Plain;
            Shelf = PlainShelf;
            return Spot != null;
        }

        @Override
        public void start() {
            Travel = 0;
            Working = false;
            Ticks = Post ? PostMin + Staff.getRandom().nextInt(PostSpread) : TidyMin + Staff.getRandom().nextInt(TidySpread);
            if (Spot != null) {
                Staff.getNavigation().moveTo(Spot.getX() + 0.5, Spot.getY(), Spot.getZ() + 0.5, Scp3008Staff.WanderModifier);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return idle(Staff) && Spot != null && Shelf != null && Ticks > 0 && Travel < TravelLimit;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Spot == null || Shelf == null) {
                return;
            }
            Vec3 Centre = Vec3.atBottomCenterOf(Spot);
            Vec3 Target = Vec3.atCenterOf(Shelf);
            if (!Working) {
                Travel++;
                if (Staff.position().distanceToSqr(Centre.x, Staff.getY(), Centre.z) < ArriveDistance * ArriveDistance) {
                    Working = true;
                    Staff.getNavigation().stop();
                    Staff.setActivity(Post ? Scp3008Staff.ActivityPost : Scp3008Staff.ActivityTidy);
                } else if (Staff.getNavigation().isDone()) {
                    Travel = TravelLimit;
                }
                return;
            }
            Ticks--;
            Vec3 Look = Post ? Centre.add(Centre.subtract(Target.x, Centre.y, Target.z).scale(3.0)).add(0.0, Staff.getEyeHeight(), 0.0) : Target;
            Staff.getLookControl().setLookAt(Look.x, Look.y, Look.z);
            face(Staff, Look);
            if (!Post && Ticks % TidySoundInterval == 0) {
                Staff.play(Scp3008StaffRegistry.TidyLayers, Target, 0.45f, 0.9f + Staff.getRandom().nextFloat() * 0.2f);
            }
        }

        @Override
        public void stop() {
            Staff.setActivity(Scp3008Staff.ActivityIdle);
            Spot = null;
            Shelf = null;
            Working = false;
            Rest = TidyRest + Staff.getRandom().nextInt(TidyRestSpread);
        }
    }

    public static boolean standable(Level World, BlockPos Pos, float Height) {
        BlockPos Below = Pos.below();
        if (!World.getBlockState(Below).isFaceSturdy(World, Below, Direction.UP)) {
            return false;
        }
        int Rows = Mth.ceil(Height);
        for (int Row = 0; Row < Rows; Row++) {
            BlockPos Free = Pos.above(Row);
            if (!World.getBlockState(Free).getCollisionShape(World, Free).isEmpty() || !World.getFluidState(Free).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public static final class Wander extends WaterAvoidingRandomStrollGoal {
        private final Scp3008Staff Staff;

        public Wander(Scp3008Staff Staff) {
            super(Staff, Scp3008Staff.WanderModifier);
            this.Staff = Staff;
            setInterval(WanderInterval);
        }

        @Override
        public boolean canUse() {
            if (Staff.engaged() || Staff.announcing()) {
                return false;
            }
            setInterval(Staff.hostile() ? ProwlInterval : WanderInterval);
            return super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !Staff.engaged() && !Staff.announcing() && super.canContinueToUse();
        }

        @Override
        public void start() {
            Staff.getNavigation().moveTo(wantedX, wantedY, wantedZ, Staff.hostile() ? Scp3008Staff.ProwlModifier : Scp3008Staff.WanderModifier);
        }
    }

    public static final class Hunt extends TargetGoal {
        private final Scp3008Staff Staff;

        public Hunt(Scp3008Staff Staff) {
            super(Staff, false, false);
            this.Staff = Staff;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (!Staff.hostile() || (Staff.tickCount + Staff.getId()) % HuntInterval != 0 || !(Staff.level() instanceof ServerLevel World)) {
                return false;
            }
            LivingEntity Found = nearestPrey(World, Staff, Scp3008Staff.FollowRange);
            if (Found == null || Found == Staff.getTarget()) {
                return false;
            }
            targetMob = Found;
            return true;
        }

        @Override
        public void start() {
            Staff.setTarget(targetMob);
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity Current = Staff.getTarget();
            return Staff.hostile() && isPrey(Current) && Current.distanceToSqr(Staff) < ChaseRange * ChaseRange;
        }

        @Override
        public void stop() {
            if (!Staff.provokedBy(Staff.getTarget())) {
                Staff.setTarget(null);
            }
            targetMob = null;
        }
    }

    public static final class Retaliate extends TargetGoal {
        private final Scp3008Staff Staff;

        public Retaliate(Scp3008Staff Staff) {
            super(Staff, false, false);
            this.Staff = Staff;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (Staff.hostile() || Staff.provoker() == null || !(Staff.level() instanceof ServerLevel World)) {
                return false;
            }
            if (!(World.getEntity(Staff.provoker()) instanceof LivingEntity Living) || !isPrey(Living) || Living.distanceToSqr(Staff) > ChaseRange * ChaseRange) {
                return false;
            }
            if (Living == Staff.getTarget()) {
                return false;
            }
            targetMob = Living;
            return true;
        }

        @Override
        public void start() {
            Staff.setTarget(targetMob);
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity Current = Staff.getTarget();
            return !Staff.hostile() && Staff.provokedBy(Current) && isPrey(Current);
        }

        @Override
        public void stop() {
            if (!Staff.hostile()) {
                Staff.setTarget(null);
            }
            targetMob = null;
        }
    }
}
