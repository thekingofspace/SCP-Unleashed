package net.rai.scp_unleashed.scp610;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.labware.LabExposure;

import java.util.EnumSet;

public class Scp610Herald extends Scp610Unit {
    public static final int SpewTicks = 32;
    public static final int ReleaseTick = 16;
    public static final int SpewGap = 160;
    public static final double Near = 6.0;
    public static final double Far = 10.0;
    public static final double CloudRadius = 3.5;
    private int SpewReady = 40;

    public Scp610Herald(EntityType<? extends Scp610Herald> Type, Level World) {
        super(Type, World, Scp610Form.Herald);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        goalSelector.addGoal(2, new KeepAway(this));
    }

    @Override
    public boolean keepsMelee() {
        LivingEntity Target = getTarget();
        return Target != null && distanceTo(Target) < 2.5;
    }

    @Override
    protected void serverTick() {
        SpewReady = Math.max(0, SpewReady - 1);
        LivingEntity Target = getTarget();
        if (SpewReady <= 0 && canAct() && Target != null && distanceTo(Target) <= Far + 2.0 && getSensing().hasLineOfSight(Target)) {
            SpewReady = SpewGap;
            faceToward(Target, 60.0f);
            setAction(ActionSpew, SpewTicks);
        }
    }

    @Override
    protected void actionTick(byte Kind, int Elapsed) {
        if (Kind == ActionSpew && Elapsed == ReleaseTick && level() instanceof ServerLevel World) {
            LivingEntity Target = getTarget();
            Vec3 Vent = position().add(0.0, getBbHeight(), 0.0);
            Vec3 Aim = Target != null ? Vent.add(Target.position().subtract(Vent).scale(0.7)) : Vent.add(getLookAngle().scale(4.0));
            for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp610Sounds.SpewLayers) {
                World.playSound(null, Vent.x, Vent.y, Vent.z, Layer.get(), SoundSource.HOSTILE, 1.4f, 0.9f);
            }
            Scp610Infection.cloud(World, Vent, 1.0, 20);
            Scp610Infection.cloud(World, Aim, CloudRadius, 60);
            Scp610Infection.exposeAround(World, Aim, LabExposure.NothingIgnored, CloudRadius);
            return;
        }
        super.actionTick(Kind, Elapsed);
    }

    public static final class KeepAway extends Goal {
        private final Scp610Herald Walker;

        public KeepAway(Scp610Herald Walker) {
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
            double Gap = Walker.distanceTo(Target);
            if (Walker.tickCount % 10 != 0) {
                return;
            }
            if (Gap > Far) {
                Walker.getNavigation().moveTo(Target, 1.0);
            } else if (Gap < Near) {
                Vec3 Away = Walker.position().subtract(Target.position()).normalize().scale(5.0).add(Walker.position());
                Walker.getNavigation().moveTo(Away.x, Away.y, Away.z, 1.1);
            } else {
                Walker.getNavigation().stop();
            }
        }
    }
}
