package net.rai.scp_unleashed.scp939;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class Scp939Goals {
    public static final int HuntInterval = 10;
    public static final int RepathTicks = 10;
    public static final double SprintRange = 9.0;
    public static final double LungeMin = 2.5;
    public static final double LungeMax = 6.0;
    public static final int LungeCooldown = 50;
    public static final double LungePower = 0.85;
    public static final double LungeLift = 0.32;
    public static final int LurkTries = 6;
    public static final int LurkReach = 10;
    public static final int LurkRise = 4;
    public static final int CalmReach = 5;
    public static final int RoamInterval = 90;
    public static final int SniffChance = 220;
    public static final int LureRepath = 20;
    public static final double LureModifier = 0.45;

    private Scp939Goals() {
    }

    public static final class Busy extends Goal {
        private final Scp939 Owner;

        public Busy(Scp939 Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK, Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            return Owner.isBusy();
        }

        @Override
        public void start() {
            Owner.getNavigation().stop();
        }

        @Override
        public void tick() {
            Owner.getNavigation().stop();
        }
    }

    public static final class Stalk extends Goal {
        private final Scp939 Owner;
        private int Repath;

        public Stalk(Scp939 Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Owner.getTarget();
            return Target != null && Target.isAlive() && !Owner.isBusy();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Repath = 0;
        }

        @Override
        public void stop() {
            Owner.setProwling(false);
            Owner.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = Owner.getTarget();
            if (Target == null) {
                return;
            }
            Owner.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            double Distance = Owner.distanceTo(Target);
            boolean Far = Distance > SprintRange;
            Owner.setProwling(Far);
            if (--Repath <= 0 || Owner.getNavigation().isDone()) {
                Repath = RepathTicks;
                Owner.getNavigation().moveTo(Target, Far ? Scp939.ProwlModifier : Scp939.SprintModifier);
            }
            if (Distance >= LungeMin && Distance <= LungeMax && Owner.lungeReady() && Owner.getSensing().hasLineOfSight(Target)) {
                Owner.lunge(Target);
            }
            Owner.strike(Target);
        }
    }

    public static final class Lure extends Goal {
        private final Scp939 Owner;
        private int Repath;

        public Lure(Scp939 Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Owner.isLuring() && !Owner.isBusy();
        }

        @Override
        public boolean canContinueToUse() {
            return canUse();
        }

        @Override
        public void start() {
            Repath = 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Prey = Owner.lurePrey();
            if (Prey == null || !(Owner.level() instanceof ServerLevel World) || !Scp939Senses.isPrey(Owner, Prey)) {
                Owner.endLure(false);
                return;
            }
            Owner.getLookControl().setLookAt(Prey, 20.0f, 20.0f);
            double Distance = Owner.distanceTo(Prey);
            boolean Reachable = !Owner.isContained() || Owner.room() != null && Owner.room().holds(Prey.blockPosition());
            if (Distance <= Owner.lungeRange() && Reachable) {
                Owner.endLure(true);
                return;
            }
            if (Distance > Scp939.LureLoseRange || !Owner.tickLure()) {
                Owner.endLure(false);
                return;
            }
            if (--Repath <= 0) {
                Repath = LureRepath;
                if (Reachable) {
                    Owner.getNavigation().moveTo(Prey, LureModifier);
                } else {
                    Owner.getNavigation().stop();
                }
            }
        }

        @Override
        public void stop() {
            Owner.getNavigation().stop();
        }
    }

    public static final class Hunt extends TargetGoal {
        private final Scp939 Owner;

        public Hunt(Scp939 Owner) {
            super(Owner, false, false);
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.TARGET));
        }

        @Override
        public boolean canUse() {
            if (Owner.isBusy() || Owner.isLuring() || (Owner.tickCount + Owner.getId()) % HuntInterval != 0 || !(Owner.level() instanceof ServerLevel World)) {
                return false;
            }
            LivingEntity Found = Scp939Senses.nearest(World, Owner);
            if (Owner.lureReady()) {
                LivingEntity Lured = Found instanceof Player && Scp939Senses.lureable(World, Owner, Found) ? Found
                        : Found == null ? Scp939Senses.lureCandidate(World, Owner) : null;
                if (Lured != null) {
                    if (Owner.getRandom().nextFloat() < Scp939.LureChance) {
                        Owner.beginLure(Lured);
                        return false;
                    }
                    Owner.setLureWait(Scp939.LureRetry);
                }
            }
            if (Found == null) {
                return false;
            }
            targetMob = Found;
            return true;
        }

        @Override
        public void start() {
            Owner.setTarget(targetMob);
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            return Owner.level() instanceof ServerLevel World && Scp939Senses.keeps(World, Owner, Owner.getTarget());
        }

        @Override
        public void stop() {
            Owner.setTarget(null);
            targetMob = null;
        }
    }

    public static final class Sniff extends Goal {
        private final Scp939 Owner;

        public Sniff(Scp939 Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.of(Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Owner.getTarget() == null && !Owner.isBusy() && !Owner.isSniffing() && Owner.getNavigation().isDone() && Owner.getRandom().nextInt(SniffChance) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return Owner.isSniffing() && Owner.getTarget() == null;
        }

        @Override
        public void start() {
            Owner.sniff();
        }
    }

    public static final class Lurk extends WaterAvoidingRandomStrollGoal {
        private final Scp939 Owner;

        public Lurk(Scp939 Owner) {
            super(Owner, Scp939.RoamModifier);
            this.Owner = Owner;
            setInterval(RoamInterval);
        }

        @Override
        public boolean canUse() {
            return Owner.getTarget() == null && !Owner.isBusy() && super.canUse();
        }

        @Nullable
        @Override
        protected Vec3 getPosition() {
            if (Owner.isContained()) {
                return DefaultRandomPos.getPos(Owner, CalmReach, 2);
            }
            Vec3 Best = null;
            int BestLight = Integer.MAX_VALUE;
            for (int Try = 0; Try < LurkTries; Try++) {
                Vec3 Found = DefaultRandomPos.getPos(Owner, LurkReach, LurkRise);
                if (Found == null) {
                    continue;
                }
                BlockPos Spot = BlockPos.containing(Found);
                int Light = Owner.level().getBrightness(LightLayer.BLOCK, Spot) + Owner.level().getBrightness(LightLayer.SKY, Spot);
                if (Light < BestLight) {
                    Best = Found;
                    BestLight = Light;
                }
            }
            return Best;
        }
    }

    public static final class Quiet<T extends Goal> extends Goal {
        private final Scp939 Owner;
        private final T Inner;

        public Quiet(Scp939 Owner, T Inner) {
            this.Owner = Owner;
            this.Inner = Inner;
            setFlags(Inner.getFlags());
        }

        @Override
        public boolean canUse() {
            return !Owner.isBusy() && !Owner.isSniffing() && Inner.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return !Owner.isBusy() && !Owner.isSniffing() && Inner.canContinueToUse();
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
}
