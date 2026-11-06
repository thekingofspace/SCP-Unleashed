package net.rai.scp_unleashed.scp610;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class Scp610Spitter extends Scp610Unit {
    public static final int SpitTicks = 16;
    public static final int ReleaseTick = 9;
    public static final int SpitGap = 60;
    public static final double Near = 8.0;
    public static final double Far = 14.0;
    public static final float Velocity = 1.1f;
    public static final float Spread = 2.0f;
    private int SpitReady = 30;

    public Scp610Spitter(EntityType<? extends Scp610Spitter> Type, Level World) {
        super(Type, World, Scp610Form.Spitter);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new KeepRange(this));
    }

    @Override
    public boolean keepsMelee() {
        LivingEntity Target = getTarget();
        return Target != null && distanceTo(Target) < 2.5;
    }

    public Scp610Bile spit(ServerLevel World, LivingEntity Target) {
        Scp610Bile Shot = new Scp610Bile(World, this);
        Vec3 Mouth = getEyePosition().add(getLookAngle().scale(0.5));
        Shot.setPos(Mouth.x, Mouth.y, Mouth.z);
        double Dx = Target.getX() - Mouth.x;
        double Dy = Target.getY(0.5) - Mouth.y;
        double Dz = Target.getZ() - Mouth.z;
        double Flat = Math.sqrt(Dx * Dx + Dz * Dz);
        Shot.shoot(Dx, Dy + Flat * 0.12, Dz, Velocity, Spread);
        World.addFreshEntity(Shot);
        playLayers(Scp610Sounds.SpitLayers, 1.0f, 1.3f);
        return Shot;
    }

    @Override
    protected void actionTick(byte Kind, int Elapsed) {
        if (Kind == ActionSpit && Elapsed == ReleaseTick && level() instanceof ServerLevel World) {
            LivingEntity Target = getTarget();
            if (Target != null && Target.isAlive()) {
                spit(World, Target);
            }
            return;
        }
        super.actionTick(Kind, Elapsed);
    }

    @Override
    protected void serverTick() {
        super.serverTick();
        SpitReady = Math.max(0, SpitReady - 1);
        LivingEntity Target = getTarget();
        if (SpitReady <= 0 && canAct() && Target != null && Target.isAlive() && distanceTo(Target) <= Far + 4.0 && getSensing().hasLineOfSight(Target)) {
            SpitReady = SpitGap;
            faceToward(Target, 60.0f);
            setAction(ActionSpit, SpitTicks);
        }
    }

    public static final class KeepRange extends Goal {
        private final Scp610Spitter Walker;

        public KeepRange(Scp610Spitter Walker) {
            this.Walker = Walker;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Walker.getTarget();
            return Target != null && Target.isAlive() && !Walker.locksMovement() && !Walker.keepsMelee();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            LivingEntity Target = Walker.getTarget();
            if (Target == null) {
                return;
            }
            Walker.getLookControl().setLookAt(Target, 30.0f, 30.0f);
            if (Walker.tickCount % 10 != 0) {
                return;
            }
            double Gap = Walker.distanceTo(Target);
            if (Gap > Far) {
                Walker.getNavigation().moveTo(Target, 1.0);
            } else if (Gap < Near) {
                Vec3 Away = Walker.position().subtract(Target.position()).normalize().scale(6.0).add(Walker.position());
                Walker.getNavigation().moveTo(Away.x, Away.y, Away.z, 1.1);
            } else {
                Walker.getNavigation().stop();
            }
        }
    }
}
