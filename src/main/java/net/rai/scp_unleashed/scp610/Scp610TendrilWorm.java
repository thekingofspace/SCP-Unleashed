package net.rai.scp_unleashed.scp610;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

public class Scp610TendrilWorm extends Scp610Creature {
    public static final double Reach = 3.5;

    public Scp610TendrilWorm(EntityType<? extends Scp610TendrilWorm> Type, Level World) {
        super(Type, World, Scp610Form.TendrilWorm);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new Scp610Goals.HoldStill(this));
        goalSelector.addGoal(2, new Lash(this));
        targetSelector.addGoal(1, new Scp610Goals.Avenge(this));
        targetSelector.addGoal(2, new Scp610Goals.Hunt(this));
    }

    @Override
    public double attackReach() {
        return Reach - 0.5;
    }

    @Override
    protected void serverTick() {
        getNavigation().stop();
        setDeltaMovement(0.0, getDeltaMovement().y, 0.0);
        if (tickCount > 40 && tickCount % 40 == 0 && !isEmerging() && !Scp610Growth.isFlesh(level().getBlockState(blockPosition().below()))) {
            collapse();
        }
        LivingEntity Target = getTarget();
        if (Target != null && distanceTo(Target) > Reach * 3.0) {
            setTarget(null);
        }
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(double X, double Y, double Z) {
    }

    public static final class Lash extends Goal {
        private final Scp610TendrilWorm Worm;

        public Lash(Scp610TendrilWorm Worm) {
            this.Worm = Worm;
            setFlags(EnumSet.of(Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Worm.getTarget();
            return Target != null && Target.isAlive();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = Worm.getTarget();
            if (Target == null) {
                return;
            }
            Worm.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            if (Worm.reachTo(Target) <= Worm.attackReach() && Worm.getSensing().hasLineOfSight(Target)) {
                Worm.beginAttack(Target);
            }
        }
    }
}
