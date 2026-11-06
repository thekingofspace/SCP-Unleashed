package net.rai.scp_unleashed.scp610;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public class Scp610Tender extends Scp610Utility {
    public static final double Reach = 16.0;
    public static final float Heal = 4.0f;
    public static final int HealGap = 40;
    public static final int TendTicks = 24;
    public static final double Close = 2.0;
    private int HealReady;
    @Nullable
    private Scp610Creature Patient;

    public Scp610Tender(EntityType<? extends Scp610Tender> Type, Level World) {
        super(Type, World, Scp610Form.Tender);
    }

    @Override
    protected void registerWork() {
        goalSelector.addGoal(3, new Tend(this));
    }

    @Nullable
    public Scp610Creature patient() {
        return Patient;
    }

    @Nullable
    public Scp610Creature findPatient() {
        AABB Area = getBoundingBox().inflate(Reach, 6.0, Reach);
        List<Scp610Creature> Found = level().getEntitiesOfClass(Scp610Creature.class, Area, Each -> Each != this && Each.isAlive() && !Each.isDeadOrDying()
                && Each.getHealth() < Each.getMaxHealth() && !(Each instanceof Scp610HiveLord) && Each.distanceToSqr(this) <= Reach * Reach);
        Scp610Creature Best = null;
        double BestGap = Double.MAX_VALUE;
        for (Scp610Creature Each : Found) {
            double Gap = Each.distanceToSqr(this);
            if (Gap < BestGap) {
                BestGap = Gap;
                Best = Each;
            }
        }
        return Best;
    }

    public boolean tend(Scp610Creature Target) {
        if (!(level() instanceof ServerLevel World) || HealReady > 0 || !Target.isAlive() || Target.getHealth() >= Target.getMaxHealth()) {
            return false;
        }
        HealReady = HealGap;
        Target.heal(Heal);
        faceToward(Target, 60.0f);
        if (canAct()) {
            setAction(ActionTend, TendTicks);
        }
        World.sendParticles(Scp610Infection.Speck, Target.getX(), Target.getY() + Target.getBbHeight() * 0.6, Target.getZ(), 8, Target.getBbWidth() * 0.3,
                Target.getBbHeight() * 0.2, Target.getBbWidth() * 0.3, 0.0);
        playLayers(Scp610Sounds.WetLayers, 0.6f, 1.2f);
        return true;
    }

    @Override
    protected void work() {
        HealReady = Math.max(0, HealReady - 1);
        if (Patient != null && (!Patient.isAlive() || Patient.getHealth() >= Patient.getMaxHealth())) {
            Patient = null;
        }
        if (Patient == null && tickCount % 20 == 0 && !fleeing()) {
            Patient = findPatient();
        }
    }

    public static final class Tend extends Goal {
        private final Scp610Tender Nurse;

        public Tend(Scp610Tender Nurse) {
            this.Nurse = Nurse;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Nurse.patient() != null && !Nurse.fleeing() && !Nurse.locksMovement();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Scp610Creature Target = Nurse.patient();
            if (Target == null) {
                return;
            }
            Nurse.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            if (Nurse.reachTo(Target) <= Close) {
                Nurse.getNavigation().stop();
                Nurse.tend(Target);
            } else {
                Nurse.walkTo(Target.position(), 1.0);
            }
        }
    }
}
