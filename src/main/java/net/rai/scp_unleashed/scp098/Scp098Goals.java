package net.rai.scp_unleashed.scp098;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp148.Scp148Shield;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class Scp098Goals {
    public static final int RepathTicks = 10;
    public static final double ArriveReach = 0.75;
    public static final double SideReach = 1.5;
    public static final int StuckTicks = 40;
    public static final int GiveUpTicks = 1200;
    public static final double ReturnModifier = 0.95;
    public static final int RoamInterval = 60;
    public static final int SeekInterval = 20;
    public static final double SeekModifier = 0.7;
    public static final double SeekNear = 24.0;
    public static final double SeekStop = 2.5;

    private Scp098Goals() {
    }

    public static final class HoldStill extends Goal {
        private final Scp098Crab Holder;

        public HoldStill(Scp098Crab Holder) {
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
            if (!Holder.isEmerging() || Holder.onGround()) {
                Holder.setDeltaMovement(Holder.getDeltaMovement().multiply(0.0, 1.0, 0.0));
            }
        }
    }

    public static final class Strike extends MeleeAttackGoal {
        private final Scp098Crab Striker;

        public Strike(Scp098Crab Striker) {
            super(Striker, Scp098Crab.HuntModifier, true);
            this.Striker = Striker;
        }

        @Override
        public boolean canUse() {
            return !Striker.locksMovement() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !Striker.locksMovement() && super.canContinueToUse();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity Target) {
            if (Striker.isActing() || !Striker.hasLineOfSight(Target)) {
                return;
            }
            if (Striker.reachTo(Target) <= Scp098Crab.SnipReach && Striker.canSnip()) {
                resetAttackCooldown();
                Striker.beginSnip(Target);
            }
        }
    }

    public static final class Feed extends Goal {
        private final Scp098Crab Eater;
        @Nullable
        private ItemEntity Morsel;
        private int Repath;

        public Feed(Scp098Crab Eater) {
            this.Eater = Eater;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        private boolean hungry() {
            return Eater.mode() == Scp098Crab.ModeFeed && !Eater.isActing() && Eater.getTarget() == null;
        }

        @Override
        public boolean canUse() {
            if (!hungry() || Eater.tickCount % 5 != 0) {
                return false;
            }
            Morsel = Eater.nearestMorsel();
            return Morsel != null;
        }

        @Override
        public boolean canContinueToUse() {
            return hungry() && Morsel != null && Scp098Crab.isMorsel(Morsel);
        }

        @Override
        public void start() {
            Repath = 0;
        }

        @Override
        public void stop() {
            Morsel = null;
            Eater.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            ItemEntity Target = Morsel;
            if (Target == null) {
                return;
            }
            Eater.sawMorsel();
            Eater.getLookControl().setLookAt(Target.getX(), Target.getY(), Target.getZ());
            if (Eater.mouthReach(Target) <= Scp098Crab.EatReach && Math.abs(Target.getY() - Eater.getY()) < 1.0) {
                Eater.beginEat(Target);
                Morsel = null;
                return;
            }
            if (--Repath <= 0) {
                Repath = RepathTicks;
                if (!Eater.getNavigation().moveTo(Target.getX(), Target.getY(), Target.getZ(), Scp098Crab.HuntModifier)) {
                    Eater.getMoveControl().setWantedPosition(Target.getX(), Target.getY(), Target.getZ(), Scp098Crab.HuntModifier);
                }
            }
        }
    }

    public static final class GoHome extends Goal {
        private final Scp098Crab Walker;
        private int Repath;
        private int Stuck;
        private int Failing;

        public GoHome(Scp098Crab Walker) {
            this.Walker = Walker;
            setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        private boolean homeward() {
            byte Mode = Walker.mode();
            return (Mode == Scp098Crab.ModeRush || Mode == Scp098Crab.ModeReturn) && Walker.home() != null && !Walker.isActing();
        }

        @Override
        public boolean canUse() {
            return homeward() && Walker.getTarget() == null;
        }

        @Override
        public boolean canContinueToUse() {
            return homeward();
        }

        @Override
        public void start() {
            Repath = 0;
            Stuck = 0;
            Failing = 0;
        }

        @Override
        public void stop() {
            Walker.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            BlockPos Home = Walker.home();
            if (Home == null) {
                return;
            }
            Vec3 Top = Vec3.atBottomCenterOf(Home.above());
            double Dx = Top.x - Walker.getX();
            double Dz = Top.z - Walker.getZ();
            double Across = Math.sqrt(Dx * Dx + Dz * Dz);
            double Rise = Math.abs(Top.y - Walker.getY());
            boolean OnTop = Across <= ArriveReach && Rise <= 0.6;
            boolean Beside = Across <= SideReach && Rise <= 1.6;
            Stuck = Beside ? Stuck + 1 : 0;
            if ((OnTop || Stuck > StuckTicks) && Walker.onGround()) {
                if (Walker.readyToBurrow()) {
                    Walker.beginBurrow();
                }
                return;
            }
            double Speed = Walker.mode() == Scp098Crab.ModeRush ? Scp098Crab.RushModifier : ReturnModifier;
            if (--Repath <= 0) {
                Repath = RepathTicks;
                boolean Moving = Walker.getNavigation().moveTo(Top.x, Top.y, Top.z, Speed);
                if (!Moving && Across < 3.0) {
                    Walker.getMoveControl().setWantedPosition(Top.x, Top.y, Top.z, Speed);
                }
                Failing = Moving || Across < 3.0 ? 0 : Failing + RepathTicks;
                if (Failing > GiveUpTicks) {
                    Walker.setHome(null);
                    Walker.setMode(Scp098Crab.ModeStray);
                }
            }
        }
    }

    public static final class Roam extends WaterAvoidingRandomStrollGoal {
        private final Scp098Crab Wanderer;

        public Roam(Scp098Crab Wanderer) {
            super(Wanderer, Scp098Crab.WanderModifier);
            this.Wanderer = Wanderer;
            setInterval(RoamInterval);
        }

        private boolean idle() {
            byte Mode = Wanderer.mode();
            return (Mode == Scp098Crab.ModeForage || Mode == Scp098Crab.ModeStray) && !Wanderer.isActing();
        }

        @Override
        public boolean canUse() {
            return idle() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return idle() && super.canContinueToUse();
        }
    }

    public static final class Retaliate extends HurtByTargetGoal {
        public Retaliate(Scp098Crab Victim) {
            super(Victim, Scp098Crab.class);
            setAlertOthers();
        }
    }

    public static final class Prey extends NearestAttackableTargetGoal<Animal> {
        private final Scp098Crab Hunter;

        public Prey(Scp098Crab Hunter) {
            super(Hunter, Animal.class, 10, true, false, Scp098Crab::isPrey);
            this.Hunter = Hunter;
        }

        private boolean hunting() {
            byte Mode = Hunter.mode();
            return (Mode == Scp098Crab.ModeHunt || Mode == Scp098Crab.ModeForage || Mode == Scp098Crab.ModeStray) && !Hunter.locksMovement();
        }

        @Override
        public boolean canUse() {
            if (!hunting() || !super.canUse()) {
                return false;
            }
            BlockPos Home = Hunter.home();
            return Home == null || target == null || target.distanceToSqr(Vec3.atCenterOf(Home)) <= Scp098Crab.HomeRange * Scp098Crab.HomeRange;
        }

        @Override
        public void start() {
            super.start();
            if (Hunter.home() != null) {
                Hunter.setMode(Scp098Crab.ModeHunt);
            }
        }
    }

    public static final class SeekHive extends Goal {
        private final PathfinderMob Walker;
        private int Repath;

        public SeekHive(PathfinderMob Walker) {
            this.Walker = Walker;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        private boolean compelled() {
            Scp098Host.State Current = Scp098Host.stateOf(Walker);
            return Current.Stage() >= 3 && Scp098Host.inRange(Walker, Current) && !Scp148Shield.blocks(Walker);
        }

        @Override
        public boolean canUse() {
            return Walker.tickCount % SeekInterval == Math.floorMod(Walker.getId(), SeekInterval) && compelled();
        }

        @Override
        public boolean canContinueToUse() {
            return compelled();
        }

        @Override
        public void start() {
            Repath = 0;
        }

        @Override
        public void stop() {
            Walker.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Scp098Host.State Current = Scp098Host.stateOf(Walker);
            Vec3 Centre = Scp098Host.hiveCentre(Current);
            Walker.getLookControl().setLookAt(Centre.x, Centre.y, Centre.z, 30.0f, Walker.getMaxHeadXRot());
            double Distance = Walker.position().distanceTo(Centre);
            if (Distance <= SeekStop) {
                Walker.getNavigation().stop();
                return;
            }
            if (--Repath > 0) {
                return;
            }
            Repath = SeekInterval;
            if (Distance <= SeekNear) {
                Walker.getNavigation().moveTo(Centre.x, Current.Hive().getY() + 1.0, Centre.z, SeekModifier);
                return;
            }
            Vec3 Step = DefaultRandomPos.getPosTowards(Walker, 16, 7, Centre, Math.PI / 2.0);
            if (Step != null) {
                Walker.getNavigation().moveTo(Step.x, Step.y, Step.z, SeekModifier);
            }
        }
    }
}
