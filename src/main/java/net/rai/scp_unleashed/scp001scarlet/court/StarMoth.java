package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class StarMoth extends CourtMinion {
    public static final int DustTicks = 40;
    public static final double DiveReach = 1.4;
    private boolean Touched;

    public StarMoth(EntityType<? extends Monster> Type, Level World) {
        super(Type, World, CourtKind.StarMoth);
        moveControl = new FlyingMoveControl(this, 20, true);
        setNoGravity(true);
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        FlyingPathNavigation Navigation = new FlyingPathNavigation(this, World);
        Navigation.setCanOpenDoors(false);
        Navigation.setCanFloat(true);
        Navigation.setCanPassDoors(true);
        return Navigation;
    }

    @Override
    protected void addBehaviours() {
        behaviour(2, new Flutter(this));
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    protected void checkFallDamage(double Y, boolean Ground, BlockState State, BlockPos Pos) {
    }

    @Override
    protected void actionTick(int Action, int Elapsed) {
        LivingEntity Target = getTarget();
        if (Action != Attack || Target == null) {
            return;
        }
        if (Elapsed < 8) {
            setDeltaMovement(getDeltaMovement().scale(0.5).add(0.0, 0.04, 0.0));
            if (Elapsed == 2) {
                play("idle", 0.8f, 1.5f);
            }
        } else if (Elapsed == 8) {
            setDeltaMovement(Target.getEyePosition().subtract(position()).normalize().scale(1.0));
            Touched = false;
        } else if (!Touched && distanceToSqr(Target) < DiveReach * DiveReach * 2.0) {
            Touched = true;
            if (CourtFx.strike(this, Target, (float) Kind.damage())) {
                Target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, DustTicks, 0, false, true, true));
                if (level() instanceof ServerLevel World) {
                    CourtFx.burst(World, CourtFx.Moonlight, Target.position().add(0.0, Target.getBbHeight() * 0.6, 0.0), 14, 0.35, 0.0);
                    CourtFx.burst(World, ParticleTypes.END_ROD, Target.position().add(0.0, Target.getBbHeight() * 0.6, 0.0), 4, 0.3, 0.02);
                }
            }
            setDeltaMovement(getDeltaMovement().scale(-0.5).add(0.0, 0.35, 0.0));
        }
    }

    @Override
    protected String clip(int Action) {
        return Action == Attack ? "attack" : null;
    }

    public static class Flutter extends Goal {
        private final StarMoth Owner;
        private int Wait = 40;
        private double Orbit;
        private Vec3 Jitter = Vec3.ZERO;

        public Flutter(StarMoth Owner) {
            this.Owner = Owner;
            this.Orbit = Owner.getRandom().nextDouble() * Math.PI * 2.0;
            setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Owner.getTarget();
            return Target != null && CourtFx.validTarget(Target);
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
            Owner.getLookControl().setLookAt(Target, 40.0f, 40.0f);
            if (Owner.busy()) {
                return;
            }
            if (Owner.tickCount % 10 == 0) {
                Jitter = new Vec3(Owner.getRandom().nextGaussian() * 0.9, Owner.getRandom().nextGaussian() * 0.6, Owner.getRandom().nextGaussian() * 0.9);
            }
            Orbit += 0.09 + Math.sin(Owner.tickCount * 0.2) * 0.04;
            Vec3 Spot = Target.position().add(Math.cos(Orbit) * 4.0, 2.4, Math.sin(Orbit) * 4.0).add(Jitter);
            Owner.getMoveControl().setWantedPosition(Spot.x, Spot.y, Spot.z, 1.2);
            if (--Wait <= 0 && Owner.distanceTo(Target) < 7.0) {
                Owner.startAction(Attack, 22);
                Wait = 40 + Owner.getRandom().nextInt(25);
            }
        }
    }
}
