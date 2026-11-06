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

public class Eyeling extends CourtMinion {
    public static final int Flash = SpecialA;
    public static final int MarkTicks = 160;
    public static final int BlindTicks = 25;
    public static final double FlashRadius = 3.0;
    private boolean Touched;

    public Eyeling(EntityType<? extends Monster> Type, Level World) {
        super(Type, World, CourtKind.Eyeling);
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
        behaviour(2, new Dart(this));
        behaviour(3, new Swell(this));
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
        if (Action == Attack && Target != null) {
            if (Elapsed < 8) {
                Vec3 Back = CourtFx.flat(position().subtract(Target.position())).scale(0.05);
                setDeltaMovement(getDeltaMovement().scale(0.6).add(Back));
            } else if (Elapsed == 8) {
                Vec3 Way = Target.getEyePosition().subtract(position()).normalize();
                setDeltaMovement(Way.scale(0.9));
                Touched = false;
            } else if (!Touched && distanceToSqr(Target) < 1.6 * 1.6) {
                Touched = true;
                CourtFx.mark(Target, MarkTicks);
                Target.addEffect(new MobEffectInstance(MobEffects.GLOWING, MarkTicks, 0, false, false, true));
                CourtFx.strike(this, Target, (float) Kind.damage());
                setDeltaMovement(getDeltaMovement().scale(-0.6).add(0.0, 0.3, 0.0));
            }
            return;
        }
        if (Action == Flash) {
            if (Elapsed % 4 == 0 && level() instanceof ServerLevel World) {
                CourtFx.burst(World, CourtFx.Amber, position().add(0.0, 0.3, 0.0), 3, 0.25, 0.0);
            }
            if (Elapsed >= 20) {
                flash();
                discard();
            }
        }
    }

    public void flash() {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        Vec3 Centre = position().add(0.0, 0.3, 0.0);
        World.sendParticles(ParticleTypes.FLASH, Centre.x, Centre.y, Centre.z, 1, 0.0, 0.0, 0.0, 0.0);
        CourtFx.burst(World, ParticleTypes.END_ROD, Centre, 14, 0.3, 0.12);
        CourtFx.sound(World, Centre, "eyeling_flash", 1.2f, 1.0f + random.nextFloat() * 0.2f);
        for (LivingEntity Victim : CourtFx.victimsNear(World, Centre, FlashRadius)) {
            Victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BlindTicks, 0, false, false, true));
        }
    }

    @Override
    public void die(DamageSource Source) {
        boolean Was = !dead;
        super.die(Source);
        if (Was && !level().isClientSide) {
            flash();
        }
    }

    @Override
    protected String clip(int Action) {
        return switch (Action) {
            case Attack -> "attack";
            case Flash -> "flash";
            default -> null;
        };
    }

    public static class Dart extends Goal {
        private final Eyeling Owner;
        private int Wait = 30;
        private double Orbit;

        public Dart(Eyeling Owner) {
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
            Orbit += 0.05;
            Vec3 Spot = Target.position().add(Math.cos(Orbit) * 3.0, 2.2 + Math.sin(Orbit * 1.7) * 0.6, Math.sin(Orbit) * 3.0);
            Owner.getMoveControl().setWantedPosition(Spot.x, Spot.y, Spot.z, 1.0);
            if (--Wait <= 0 && Owner.distanceTo(Target) < 6.0) {
                Owner.startAction(Attack, 24);
                Wait = 50 + Owner.getRandom().nextInt(30);
            }
        }
    }

    public static class Swell extends Goal {
        private final Eyeling Owner;

        public Swell(Eyeling Owner) {
            this.Owner = Owner;
            setFlags(EnumSet.noneOf(Flag.class));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Owner.getTarget();
            return Owner.tickCount > 400 && Target != null && !Owner.busy() && Owner.distanceTo(Target) < 4.0;
        }

        @Override
        public void start() {
            Owner.startAction(Flash, 24);
            Owner.play("idle", 1.0f, 1.6f);
        }

        @Override
        public boolean canContinueToUse() {
            return false;
        }
    }
}
