package net.rai.scp_unleashed.scp016;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;

public class Scp016Brute extends Scp016Spawn {
    public static final double Health = 44.0;
    public static final double Speed = 0.23;
    public static final double Damage = 9.0;
    public static final double Armour = 8.0;
    public static final double Toughness = 2.0;
    public static final double Steadiness = 0.85;
    public static final double Knockback = 1.0;
    public static final int SlamTicks = 20;
    public static final int ImpactAt = 10;
    public static final double SlamReach = 3.2;
    public static final double SlamForward = 1.7;
    public static final double SplashRadius = 1.6;
    public static final float SplashShare = 0.5f;
    public static final Scp016Gait.Profile BruteGait = new Scp016Gait.Profile(24.0, 0.0504, 0.5, 3.0, 4,
            new double[]{0.993, 0.53}, new float[]{1.0f, 0.8f},
            Scp016Registry.BruteStep, Scp016Registry.BruteThud, Scp016Registry.BruteGrind, null, 0.3f, 0.5f, 0.18f, 0.0f, 0.9f, 0.25f);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ShudderLoop = RawAnimation.begin().thenLoop("shudder");
    private static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation SlamPlay = RawAnimation.begin().thenPlayAndHold("slam");
    @Nullable
    private LivingEntity SlamTarget;

    public Scp016Brute(EntityType<? extends Scp016Brute> Type, Level World) {
        super(Type, World);
        xpReward = 14;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, Damage)
                .add(Attributes.ARMOR, Armour).add(Attributes.ARMOR_TOUGHNESS, Toughness).add(Attributes.KNOCKBACK_RESISTANCE, Steadiness)
                .add(Attributes.ATTACK_KNOCKBACK, Knockback).add(Attributes.FOLLOW_RANGE, Follow).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public String variant() {
        return "brute";
    }

    @Override
    protected void registerGoals() {
        registerKinGoals();
        goalSelector.addGoal(2, new Scp016Goals.Slam(this, 1.0));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
    }

    public void beginSlam(LivingEntity Target) {
        SlamTarget = Target;
        setAction(ActionStrike, SlamTicks);
        playSound(Scp016Registry.BruteGrind.get(), 0.9f, 0.7f + random.nextFloat() * 0.1f);
    }

    @Override
    protected void actionTick(byte Kind, int Remaining) {
        if (Kind != ActionStrike) {
            return;
        }
        if (SlamTarget != null && SlamTarget.isAlive() && Remaining > ImpactAt) {
            getLookControl().setLookAt(SlamTarget, 30.0f, 30.0f);
            double Dx = SlamTarget.getX() - getX();
            double Dz = SlamTarget.getZ() - getZ();
            float Facing = (float) (Math.atan2(Dz, Dx) * (180.0 / Math.PI)) - 90.0f;
            setYRot(Facing);
            setYBodyRot(Facing);
            setYHeadRot(Facing);
        }
        if (Remaining == ImpactAt) {
            impact();
        }
    }

    @Override
    protected void actionFinished(byte Kind) {
        if (Kind == ActionStrike) {
            SlamTarget = null;
        }
    }

    private void impact() {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        Vec3 Forward = Vec3.directionFromRotation(0.0f, getYRot());
        Vec3 Point = position().add(Forward.scale(SlamForward));
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp016Registry.SlamLayers) {
            World.playSound(null, Point.x, Point.y, Point.z, Layer.get(), SoundSource.HOSTILE, 1.0f, 0.9f + random.nextFloat() * 0.1f);
        }
        BlockPos Below = BlockPos.containing(Point.x, getY() - 0.2, Point.z);
        BlockState Ground = World.getBlockState(Below);
        if (!Ground.isAir()) {
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Ground), Point.x, getY() + 0.1, Point.z, 30, 0.6, 0.05, 0.6, 0.15);
        }
        World.sendParticles(Scp016Registry.Fleck.get(), Point.x, getY() + 0.3, Point.z, 12, 0.5, 0.2, 0.5, 0.05);
        LivingEntity Main = SlamTarget;
        if (Main != null && Main.isAlive() && distanceTo(Main) <= SlamReach + Main.getBbWidth() * 0.5 && getSensing().hasLineOfSight(Main)) {
            swing(InteractionHand.MAIN_HAND);
            doHurtTarget(Main);
        }
        float Splash = (float) getAttributeValue(Attributes.ATTACK_DAMAGE) * SplashShare;
        AABB Zone = AABB.ofSize(Point, SplashRadius * 2.0, 2.0, SplashRadius * 2.0);
        for (LivingEntity Caught : World.getEntitiesOfClass(LivingEntity.class, Zone, Found -> Found != this && Found != Main && Found.isAlive() && !isKin(Found)
                && !(Found instanceof Player User && (User.isCreative() || User.isSpectator())) && Found.distanceToSqr(Point) <= SplashRadius * SplashRadius * 2.0)) {
            if (Caught.hurt(damageSources().mobAttack(this), Splash)) {
                Vec3 Push = Caught.position().subtract(Point).multiply(1.0, 0.0, 1.0);
                if (Push.lengthSqr() > 1.0E-6) {
                    Push = Push.normalize();
                    Caught.knockback(0.6, -Push.x, -Push.z);
                }
                Scp016Affliction.afflict(Caught);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp016Registry.BruteAmbient.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp016Registry.BruteHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp016Registry.BruteDeath.get();
    }

    @Override
    protected float getSoundVolume() {
        return 1.2f;
    }

    @Override
    protected Scp016Gait.Profile gaitProfile() {
        return BruteGait;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp016GaitController<>(this, "body", 4, Event -> {
            byte Current = action();
            if (Current == ActionEmerge) {
                return Event.setAndContinue(EmergePlay);
            }
            if (Current == ActionStrike) {
                return Event.setAndContinue(SlamPlay);
            }
            if (isStriding()) {
                return Event.setAndContinue(WalkLoop);
            }
            return Event.setAndContinue(isCollapsing() ? ShudderLoop : IdleLoop);
        }));
    }
}
