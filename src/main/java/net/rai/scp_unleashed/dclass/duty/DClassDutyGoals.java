package net.rai.scp_unleashed.dclass.duty;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.dclass.DClass;
import net.rai.scp_unleashed.dclass.DClassGoals;
import net.rai.scp_unleashed.scp173.Scp173Statue;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class DClassDutyGoals {
    public static final int CleanRadius = 8;
    public static final int CleanHeight = 3;
    public static final int HomeRadius = 10;
    public static final int HomeHeight = 3;
    public static final int EatPatience = 600;
    public static final int ScanTicks = 20;
    public static final int StatueCheck = 5;
    public static final int PauseMin = 15;
    public static final int PauseSpread = 20;
    public static final int PatrolMin = 160;
    public static final int PatrolSpread = 200;
    public static final int PatrolReach = 6;
    public static final double HomeSlack = 2.5;
    public static final double PatrolSpeed = 0.5;
    public static final double GreetRange = 6.0;
    public static final int EmptyScans = 2;

    private DClassDutyGoals() {
    }

    public static boolean hungry(DClass Owner) {
        int Food = Owner.food().food();
        boolean Hungry = Food < Owner.trait().hungerThreshold() || Food < 18 && Owner.getHealth() < Owner.getMaxHealth() * 0.6f;
        return Hungry && !Owner.isUsingItem() && Owner.findFood(Food <= 6) >= 0;
    }

    private static boolean ready(DClass Owner) {
        return DClassDuties.available(Owner) && !Owner.isRestingPose() && Owner.getTarget() == null && !Owner.isPassenger();
    }

    private static final class Sight {
        private final DClass Owner;
        @Nullable
        private Scp173Statue Statue;
        private boolean Fresh = true;

        private Sight(DClass Owner) {
            this.Owner = Owner;
        }

        @Nullable
        private Scp173Statue update() {
            if (Fresh || (Owner.tickCount + Owner.getId()) % StatueCheck == 0 || Statue != null && (!Statue.isAlive() || Statue.isRemoved())) {
                Fresh = false;
                Statue = DClassGaze.statueInView(Owner, DClassGaze.WatchRange);
            }
            return Statue;
        }

        private void clear() {
            Statue = null;
            Fresh = true;
        }
    }

    public static final class Orders extends Goal {
        private final DClass Owner;
        private final DClassCleaning.Worker Worker;
        private final Sight Eyes;
        private boolean Started;
        private int Before;
        private int Pause;
        private int Empty;
        private int Scan;

        public DClassCleaning.Worker worker() {
            return Worker;
        }

        public Orders(DClass Owner) {
            this.Owner = Owner;
            this.Worker = new DClassCleaning.Worker(Owner);
            this.Eyes = new Sight(Owner);
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        private DClassTask task() {
            return DClassDuties.task(Owner);
        }

        @Override
        public boolean canUse() {
            DClassTask Task = task();
            if (!ready(Owner)) {
                return false;
            }
            return Task == DClassTask.EatThis || Task == DClassTask.CleanArea && !hungry(Owner);
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            Started = false;
            Before = 0;
            Pause = 0;
            Empty = 0;
            Scan = 0;
            Worker.release();
            Eyes.clear();
        }

        @Override
        public void stop() {
            Worker.stop();
            Eyes.clear();
        }

        @Override
        public void tick() {
            if (!(Owner.level() instanceof ServerLevel World)) {
                return;
            }
            DClassTask Task = task();
            if (Task == DClassTask.EatThis) {
                eat(World);
            } else if (Task == DClassTask.CleanArea) {
                clean(World);
            }
        }

        private int mealSlot(ItemStack Meal) {
            if (Meal.isEmpty()) {
                return -1;
            }
            if (ItemStack.isSameItemSameComponents(Owner.getMainHandItem(), Meal)) {
                return Owner.selected();
            }
            for (int Slot = 0; Slot < DClass.MainSize; Slot++) {
                if (ItemStack.isSameItemSameComponents(Owner.main().get(Slot), Meal)) {
                    return Slot;
                }
            }
            return -1;
        }

        private void eat(ServerLevel World) {
            DClassDuty Duty = DClassDuties.of(Owner);
            ItemStack Meal = Duty.Meal();
            Player Issuer = DClassDuties.issuer(Owner);
            if (Issuer != null && Issuer.distanceToSqr(Owner) <= GreetRange * GreetRange * 4.0) {
                Owner.getLookControl().setLookAt(Issuer, 20.0f, 20.0f);
            }
            if (World.getGameTime() - Duty.Since() > EatPatience) {
                DClassDuties.clearTask(Owner);
                return;
            }
            Owner.getNavigation().stop();
            if (!Started) {
                if (Owner.isUsingItem()) {
                    return;
                }
                int Slot = mealSlot(Meal);
                if (Slot < 0) {
                    DClassDuties.clearTask(Owner);
                    return;
                }
                Owner.beginEating(Slot);
                Started = true;
                Before = Owner.getMainHandItem().getCount();
                if (!Owner.isUsingItem()) {
                    refuse(Issuer);
                }
                return;
            }
            if (Owner.isUsingItem()) {
                return;
            }
            ItemStack Now = Owner.getMainHandItem();
            if (ItemStack.isSameItemSameComponents(Now, Meal) && Now.getCount() >= Before) {
                if (!DClassDuties.emptyContainer(Owner, Now)) {
                    refuse(Issuer);
                    return;
                }
            }
            DClassDuties.clearTask(Owner);
        }

        private void refuse(@Nullable Player Issuer) {
            ItemStack Now = Owner.getMainHandItem();
            DClassDuty Duty = DClassDuties.of(Owner);
            if (ItemStack.isSameItemSameComponents(Now, Duty.Meal())) {
                DClassDuties.handBack(Owner, Now.split(1), Issuer);
            }
            DClassDuties.warn(Issuer, "unusable", Owner.getName());
            DClassDuties.clearTask(Owner);
        }

        private void clean(ServerLevel World) {
            Scp173Statue Statue = Eyes.update();
            if (Statue != null) {
                DClassGaze.lookAt(Owner, Statue);
            }
            DClassCleaning.Outcome Result = Worker.tick(World, Statue);
            if (Result == DClassCleaning.Outcome.Cleaned) {
                Pause = PauseMin + Owner.getRandom().nextInt(PauseSpread);
                Empty = 0;
            }
            if (Worker.hasTarget()) {
                return;
            }
            if (Pause > 0) {
                Pause--;
                return;
            }
            if (--Scan > 0) {
                return;
            }
            Scan = ScanTicks / 2;
            BlockPos Spot = DClassDuties.of(Owner).Spot().orElse(Owner.blockPosition());
            if (Worker.pick(World, Spot, CleanRadius, CleanHeight)) {
                Empty = 0;
                return;
            }
            if (++Empty >= EmptyScans) {
                DClassDuties.clearTask(Owner);
            }
        }
    }

    public static final class Follow extends DClassGoals.FollowPlayer {
        public static final double CloseSqr = 6.25;

        public Follow(DClass Owner) {
            super(Owner);
        }

        @Override
        public boolean canUse() {
            if (!ready(Owner) || hungry(Owner)) {
                return false;
            }
            Player Ordered = DClassDuties.leader(Owner);
            if (Ordered == null || Ordered.level() != Owner.level()) {
                return false;
            }
            Leader = Ordered;
            return true;
        }

        @Override
        public boolean canContinueToUse() {
            return Leader != null && Leader == DClassDuties.leader(Owner) && Leader.level() == Owner.level() && ready(Owner) && !hungry(Owner);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            if (Leader == null) {
                return;
            }
            if (Owner.distanceToSqr(Leader) > CloseSqr) {
                super.tick();
                return;
            }
            Owner.getNavigation().stop();
            Owner.getLookControl().setLookAt(Leader, 20.0f, Owner.getMaxHeadXRot());
        }
    }

    public static final class Janitor extends Goal {
        private final DClass Owner;
        private final DClassCleaning.Worker Worker;
        private final Sight Eyes;
        private int Pause;
        private int Scan;
        private int Patrol;
        private int Homing;
        private boolean Strolling;

        public DClassCleaning.Worker worker() {
            return Worker;
        }

        public Janitor(DClass Owner) {
            this.Owner = Owner;
            this.Worker = new DClassCleaning.Worker(Owner);
            this.Eyes = new Sight(Owner);
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            DClassDuty Duty = DClassDuties.of(Owner);
            return Duty.Role() == DClassRole.Janitor && !Duty.Task().active() && Duty.Home().isPresent() && ready(Owner) && !hungry(Owner);
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            Pause = 0;
            Scan = 0;
            Patrol = PatrolMin / 2 + Owner.getRandom().nextInt(PatrolSpread);
            Strolling = false;
            Worker.release();
            Eyes.clear();
        }

        @Override
        public void stop() {
            Worker.stop();
            Eyes.clear();
            Strolling = false;
        }

        @Override
        public void tick() {
            if (!(Owner.level() instanceof ServerLevel World)) {
                return;
            }
            BlockPos Home = DClassDuties.home(Owner);
            if (Home == null) {
                return;
            }
            Scp173Statue Statue = Eyes.update();
            if (Statue != null) {
                DClassGaze.lookAt(Owner, Statue);
            }
            DClassCleaning.Outcome Result = Worker.tick(World, Statue);
            if (Result == DClassCleaning.Outcome.Cleaned) {
                Pause = PauseMin + Owner.getRandom().nextInt(PauseSpread);
            }
            if (Worker.hasTarget()) {
                Strolling = false;
                return;
            }
            if (Pause > 0) {
                Pause--;
                return;
            }
            if (--Scan <= 0) {
                Scan = ScanTicks;
                if (Worker.pick(World, Home, HomeRadius, HomeHeight)) {
                    Strolling = false;
                    return;
                }
            }
            idle(Home, Statue);
        }

        private void idle(BlockPos Home, @Nullable Scp173Statue Statue) {
            Vec3 Base = Vec3.atBottomCenterOf(Home);
            double Away = Base.distanceTo(Owner.position());
            if (Strolling) {
                if (Owner.getNavigation().isDone() || Statue != null) {
                    Strolling = false;
                    Owner.getNavigation().stop();
                }
                return;
            }
            if (Away > HomeSlack) {
                Worker.approach(Home, Statue, ++Homing, PatrolSpeed);
                return;
            }
            Homing = 0;
            Worker.halt();
            if (Statue != null) {
                return;
            }
            if (Owner.tickCount % 40 == 0) {
                Player Near = Owner.level().getNearestPlayer(Owner, GreetRange);
                if (Near != null && !Near.isSpectator()) {
                    Owner.getLookControl().setLookAt(Near, 20.0f, 20.0f);
                }
            }
            if (--Patrol > 0) {
                return;
            }
            Patrol = PatrolMin + Owner.getRandom().nextInt(PatrolSpread);
            int Dx = Owner.getRandom().nextInt(PatrolReach * 2 + 1) - PatrolReach;
            int Dz = Owner.getRandom().nextInt(PatrolReach * 2 + 1) - PatrolReach;
            BlockPos Spot = Home.offset(Dx, 0, Dz);
            Strolling = Owner.getNavigation().moveTo(Spot.getX() + 0.5, Spot.getY(), Spot.getZ() + 0.5, PatrolSpeed);
        }
    }
}
