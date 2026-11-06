package net.rai.scp_unleashed.scp354;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;

public final class Scp354Prey {
    public static final int ScanTicks = 10;
    public static final double SwitchShare = 0.64;
    public static final int GiveUpTicks = 1200;

    private Scp354Prey() {
    }

    public static boolean isKin(LivingEntity Found) {
        return Found.getType().is(Scp354Registry.Spawns);
    }

    public static boolean isPrey(LivingEntity Found) {
        if (!Found.isAlive() || Found.isRemoved() || isKin(Found) || !Targeting.isTargetable(Found)) {
            return false;
        }
        if (Found instanceof Player User) {
            return !User.isCreative() && !User.isSpectator() && !Scp268.isNeglected(User);
        }
        return ControlBodies.isPersonLike(Found) || Found instanceof AbstractVillager;
    }

    public static final class Hunt extends TargetGoal {
        private final Mob Hunter;
        private final double Range;
        private final double Height;
        private final TargetingConditions Sight;
        private int Rescan;

        public Hunt(Mob Hunter, double Range, double Height) {
            super(Hunter, false, false);
            this.Hunter = Hunter;
            this.Range = Range;
            this.Height = Height;
            this.Sight = TargetingConditions.forCombat().range(Range).selector(Scp354Prey::isPrey);
            setFlags(EnumSet.of(Goal.Flag.TARGET));
            setUnseenMemoryTicks(GiveUpTicks);
        }

        @Override
        public boolean canUse() {
            if (Hunter.getRandom().nextInt(reducedTickDelay(ScanTicks)) != 0) {
                return false;
            }
            targetMob = closest();
            return targetMob != null;
        }

        @Override
        public void start() {
            Hunter.setTarget(targetMob);
            Rescan = ScanTicks;
            super.start();
        }

        @Override
        public boolean canContinueToUse() {
            LivingEntity Current = Hunter.getTarget();
            if (Current != null && !isPrey(Current) && Current != Hunter.getLastHurtByMob()) {
                return false;
            }
            return super.canContinueToUse();
        }

        @Override
        public void tick() {
            if (--Rescan > 0) {
                return;
            }
            Rescan = ScanTicks;
            LivingEntity Current = Hunter.getTarget();
            LivingEntity Closer = closest();
            if (Closer == null || Closer == Current) {
                return;
            }
            if (Current == null || !Current.isAlive() || Hunter.distanceToSqr(Closer) < Hunter.distanceToSqr(Current) * SwitchShare) {
                Hunter.setTarget(Closer);
                targetMob = Closer;
            }
        }

        @Nullable
        public LivingEntity closest() {
            AABB Area = Hunter.getBoundingBox().inflate(Range, Height, Range);
            return Hunter.level().getNearestEntity(Hunter.level().getEntitiesOfClass(LivingEntity.class, Area, Scp354Prey::isPrey), Sight, Hunter,
                    Hunter.getX(), Hunter.getEyeY(), Hunter.getZ());
        }
    }

    public static final class Retaliate extends HurtByTargetGoal {
        public Retaliate(Scp354Beast Owner) {
            super(Owner);
        }

        @Override
        public boolean canUse() {
            LivingEntity Attacker = mob.getLastHurtByMob();
            return Attacker != null && !isKin(Attacker) && super.canUse();
        }
    }

    public static final class HoldStill extends Goal {
        private final Scp354Beast Holder;

        public HoldStill(Scp354Beast Holder) {
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
            Scp354Swim.halt(Holder);
        }
    }
}
