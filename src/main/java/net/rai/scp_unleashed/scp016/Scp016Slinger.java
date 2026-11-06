package net.rai.scp_unleashed.scp016;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.RangedAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;

public class Scp016Slinger extends Scp016Spawn implements RangedAttackMob {
    public static final double Health = 20.0;
    public static final double Speed = 0.27;
    public static final double Damage = 2.0;
    public static final int ThrowTicks = 16;
    public static final int ReleaseAt = 8;
    public static final double KeepRange = 6.0;
    public static final float AttackRange = 16.0f;
    public static final int IntervalMin = 40;
    public static final int IntervalMax = 64;
    public static final float ShardSpeed = 1.3f;
    public static final double Lob = 0.12;
    public static final double HandReach = 0.55;
    public static final Scp016Gait.Profile SlingerGait = new Scp016Gait.Profile(20.0, 0.0573, 0.5, 3.5, 4,
            new double[]{0.994, 0.522}, new float[]{1.0f, 0.85f},
            Scp016Registry.SlingerStep, null, null, null, 0.3f, 0.0f, 0.0f, 0.0f, 1.1f, 0.0f);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ShudderLoop = RawAnimation.begin().thenLoop("shudder");
    private static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation ThrowPlay = RawAnimation.begin().thenPlayAndHold("throw");
    @Nullable
    private LivingEntity ThrowTarget;

    public Scp016Slinger(EntityType<? extends Scp016Slinger> Type, Level World) {
        super(Type, World);
        xpReward = 8;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, Damage)
                .add(Attributes.FOLLOW_RANGE, Follow).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    public String variant() {
        return "slinger";
    }

    @Override
    protected void registerGoals() {
        registerKinGoals();
        goalSelector.addGoal(2, new Scp016Goals.KeepDistance(this, KeepRange, 1.1));
        goalSelector.addGoal(3, new RangedAttackGoal(this, 1.0, IntervalMin, IntervalMax, AttackRange));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
    }

    @Override
    public void performRangedAttack(LivingEntity Target, float Power) {
        if (isActing() || !Target.isAlive()) {
            return;
        }
        ThrowTarget = Target;
        setAction(ActionStrike, ThrowTicks);
    }

    @Override
    protected void actionTick(byte Kind, int Remaining) {
        if (Kind != ActionStrike || ThrowTarget == null) {
            return;
        }
        if (ThrowTarget.isAlive()) {
            getLookControl().setLookAt(ThrowTarget, 30.0f, 30.0f);
            double Dx = ThrowTarget.getX() - getX();
            double Dz = ThrowTarget.getZ() - getZ();
            float Facing = (float) (Math.atan2(Dz, Dx) * (180.0 / Math.PI)) - 90.0f;
            setYRot(Facing);
            setYBodyRot(Facing);
            setYHeadRot(Facing);
        }
        if (Remaining == ReleaseAt && ThrowTarget.isAlive()) {
            release(ThrowTarget);
        }
    }

    @Override
    protected void actionFinished(byte Kind) {
        if (Kind == ActionStrike) {
            ThrowTarget = null;
        }
    }

    private void release(LivingEntity Target) {
        Vec3 Side = Vec3.directionFromRotation(0.0f, getYRot() + 90.0f);
        Scp016Shard Shard = new Scp016Shard(level(), this);
        Shard.setPos(getX() + Side.x * HandReach, getEyeY() + 0.15, getZ() + Side.z * HandReach);
        double Dx = Target.getX() - Shard.getX();
        double Dy = Target.getY(0.4) - Shard.getY();
        double Dz = Target.getZ() - Shard.getZ();
        double Flat = Math.sqrt(Dx * Dx + Dz * Dz);
        Shard.shoot(Dx, Dy + Flat * Lob, Dz, ShardSpeed, 12 - level().getDifficulty().getId() * 3);
        level().addFreshEntity(Shard);
        float Pitch = 0.95f + random.nextFloat() * 0.15f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp016Registry.ThrowLayers) {
            level().playSound(null, getX(), getY(), getZ(), Layer.get(), SoundSource.HOSTILE, 1.0f, Pitch);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp016Registry.SlingerAmbient.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp016Registry.SlingerHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp016Registry.SlingerDeath.get();
    }

    @Override
    protected Scp016Gait.Profile gaitProfile() {
        return SlingerGait;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp016GaitController<>(this, "body", 3, Event -> {
            byte Current = action();
            if (Current == ActionEmerge) {
                return Event.setAndContinue(EmergePlay);
            }
            if (Current == ActionStrike) {
                return Event.setAndContinue(ThrowPlay);
            }
            if (isStriding()) {
                return Event.setAndContinue(WalkLoop);
            }
            return Event.setAndContinue(isCollapsing() ? ShudderLoop : IdleLoop);
        }));
    }
}
