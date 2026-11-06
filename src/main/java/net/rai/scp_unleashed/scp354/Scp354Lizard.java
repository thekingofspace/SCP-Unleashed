package net.rai.scp_unleashed.scp354;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

public class Scp354Lizard extends Scp354Beast {
    public static final float Width = 2.0f;
    public static final float Height = 1.25f;
    public static final float EyeHeight = 0.9f;
    public static final double Health = 150.0;
    public static final double Armour = 14.0;
    public static final double Toughness = 4.0;
    public static final double KnockbackResistance = 0.9;
    public static final double Speed = 0.3;
    public static final double ChaseModifier = 1.0;
    public static final double WanderModifier = 0.5;
    public static final double FollowRange = 40.0;
    public static final double AcquireRange = 28.0;
    public static final double AcquireHeight = 8.0;
    public static final double BiteDamage = 16.0;
    public static final double BiteReach = 1.9;
    public static final int BiteSlowTicks = 60;
    public static final int BiteSlowLevel = 1;
    public static final double WhipDamage = 10.0;
    public static final double WhipReach = 4.2;
    public static final double WhipArc = 0.2;
    public static final float WhipKnock = 1.8f;
    public static final int BiteTicks = 14;
    public static final int BiteHitTick = 7;
    public static final int BiteCooldown = 24;
    public static final int WhipTicks = 22;
    public static final int WhipHitTick = 11;
    public static final int WhipCooldown = 50;
    public static final int EmergeTicks = 50;
    public static final int EmergeHissTick = 30;
    public static final int HaulTicks = 14;
    public static final double SwimSpeed = 0.17;
    public static final double Draft = 0.85;
    public static final double HaulReach = 1.6;
    public static final int AmbientInterval = 280;
    public static final byte ActionBite = 2;
    public static final byte ActionWhip = 3;
    public static final Scp354Gait.Shape LizardGait = new Scp354Gait.Shape(
            new Scp354Gait.Profile(24.0, 0.0521, 0.5, 2.4, new double[]{0.0, 0.5}, new float[]{1.0f, 1.0f}),
            new Scp354Gait.Profile(14.0, 0.1339, 0.6, 2.0, new double[]{0.0, 0.5}, new float[]{1.0f, 1.0f}),
            null, 0.12, 0.09, 4, Scp354Registry.LizardStep, Scp354Registry.LizardStepScrape, 0.55f, 0.35f, 1.0f, 24.0);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RunLoop = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation SwimLoop = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation HaulPlay = RawAnimation.begin().thenPlayAndHold("haulout");
    private static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation BitePlay = RawAnimation.begin().thenPlayAndHold("bite");
    private static final RawAnimation WhipPlay = RawAnimation.begin().thenPlayAndHold("whip");
    private static final RawAnimation CollapsePlay = RawAnimation.begin().thenPlayAndHold("collapse");
    @Nullable
    private LivingEntity StrikeTarget;
    private int BiteReady;
    private int WhipReady;

    public Scp354Lizard(EntityType<? extends Scp354Lizard> Type, Level World) {
        super(Type, World);
        xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, BiteDamage)
                .add(Attributes.ARMOR, Armour).add(Attributes.ARMOR_TOUGHNESS, Toughness).add(Attributes.KNOCKBACK_RESISTANCE, KnockbackResistance)
                .add(Attributes.FOLLOW_RANGE, FollowRange).add(Attributes.STEP_HEIGHT, 1.25).add(Attributes.ATTACK_KNOCKBACK, 0.3);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new Scp354Swim.Escape(this, this));
        goalSelector.addGoal(0, new Scp354Swim.Buoy(this));
        goalSelector.addGoal(1, new Scp354Prey.HoldStill(this));
        goalSelector.addGoal(3, new Strike(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, WanderModifier));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new Scp354Prey.Retaliate(this));
        targetSelector.addGoal(2, new Scp354Prey.Hunt(this, AcquireRange, AcquireHeight));
    }

    @Override
    public boolean swimsOut() {
        return true;
    }

    @Override
    public double swimSpeed() {
        return SwimSpeed;
    }

    @Override
    public double draft() {
        return Draft;
    }

    @Override
    public double haulReach() {
        return HaulReach;
    }

    @Override
    public int haulTicks() {
        return HaulTicks;
    }

    @Override
    public int emergeTicks() {
        return EmergeTicks;
    }

    @Override
    @Nullable
    public String actionClip() {
        return switch (action()) {
            case ActionEmerge -> "emerge";
            case ActionBite -> "bite";
            case ActionWhip -> "whip";
            default -> null;
        };
    }

    @Override
    @Nullable
    protected Scp354Gait.Shape gaitShape() {
        return LizardGait;
    }

    @Override
    protected List<DeferredHolder<SoundEvent, SoundEvent>> deathLayers() {
        return Scp354Registry.LizardDeathLayers;
    }

    @Override
    public boolean locksMovement() {
        return isActing();
    }

    public boolean canBite() {
        return BiteReady <= 0 && !isActing();
    }

    public boolean canWhip() {
        return WhipReady <= 0 && !isActing();
    }

    public void beginBite(LivingEntity Target) {
        StrikeTarget = Target;
        BiteReady = BiteCooldown;
        setAction(ActionBite, BiteTicks);
        Scp354Sounds.play(this, List.of(Scp354Registry.LizardHiss), 0.9f, 0.9f + random.nextFloat() * 0.15f);
    }

    public void beginWhip(LivingEntity Target) {
        StrikeTarget = Target;
        WhipReady = WhipCooldown + random.nextInt(20);
        setAction(ActionWhip, WhipTicks);
    }

    public boolean behind(LivingEntity Target) {
        Vec3 Facing = Vec3.directionFromRotation(0.0f, getYRot());
        Vec3 Toward = new Vec3(Target.getX() - getX(), 0.0, Target.getZ() - getZ()).normalize();
        return Facing.dot(Toward) < WhipArc;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !isAlive()) {
            return;
        }
        BiteReady = Math.max(0, BiteReady - 1);
        WhipReady = Math.max(0, WhipReady - 1);
    }

    @Override
    protected void actionTick(byte Kind, int Elapsed) {
        Scp354Swim.halt(this);
        if (Kind == ActionEmerge) {
            if (Elapsed == EmergeHissTick) {
                Scp354Sounds.play(this, List.of(Scp354Registry.LizardHiss, Scp354Registry.LizardIdleCroak), 1.6f, 0.8f + random.nextFloat() * 0.1f);
            }
            return;
        }
        LivingEntity Target = StrikeTarget;
        if (Kind == ActionBite) {
            if (Target != null && Target.isAlive() && Elapsed < BiteHitTick) {
                turnToward(Target, 20.0f);
            }
            if (Elapsed == BiteHitTick) {
                biteHit(Target);
            }
        } else if (Kind == ActionWhip) {
            if (Elapsed == 3) {
                Scp354Sounds.play(this, Scp354Registry.WhipLayers.subList(1, 2), 1.2f, 0.85f + random.nextFloat() * 0.15f);
            }
            if (Elapsed == WhipHitTick) {
                whipHit();
            }
        }
    }

    private void biteHit(@Nullable LivingEntity Target) {
        Scp354Sounds.play(this, Scp354Registry.BiteLayers, 1.2f, 0.85f + random.nextFloat() * 0.15f);
        if (Target == null || !Target.isAlive() || reachTo(Target) > BiteReach || !hasLineOfSight(Target)) {
            return;
        }
        if (doHurtTarget(Target)) {
            Target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, BiteSlowTicks, BiteSlowLevel), this);
        }
    }

    private void whipHit() {
        Scp354Sounds.play(this, Scp354Registry.WhipLayers.subList(0, 1), 1.4f, 0.85f + random.nextFloat() * 0.15f);
        for (LivingEntity Found : level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(WhipReach, 1.0, WhipReach), this::whippable)) {
            double Dx = Found.getX() - getX();
            double Dz = Found.getZ() - getZ();
            if (Math.sqrt(Dx * Dx + Dz * Dz) - Found.getBbWidth() * 0.5 > WhipReach || !behind(Found)) {
                continue;
            }
            if (Found.hurt(damageSources().mobAttack(this), (float) WhipDamage)) {
                Found.knockback(WhipKnock, -Dx, -Dz);
                Found.setDeltaMovement(Found.getDeltaMovement().add(0.0, 0.25, 0.0));
                Found.hurtMarked = true;
                setLastHurtMob(Found);
            }
        }
    }

    private boolean whippable(LivingEntity Found) {
        return Found != this && Found.isAlive() && !Scp354Prey.isKin(Found) && !(Found instanceof Player User && (User.isCreative() || User.isSpectator()));
    }

    @Override
    public int getAmbientSoundInterval() {
        return AmbientInterval;
    }

    @Override
    public void playAmbientSound() {
        if (isActing()) {
            return;
        }
        Scp354Sounds.play(this, Scp354Registry.LizardIdleLayers, 1.0f, 0.85f + random.nextFloat() * 0.15f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp354Registry.LizardIdle.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp354Registry.LizardHurt.get();
    }

    @Override
    protected void playHurtSound(DamageSource Source) {
        Scp354Sounds.play(this, Scp354Registry.LizardHurtLayers, 1.1f, 0.85f + random.nextFloat() * 0.15f);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp354StageController<>(this, "body", Scp354Lizard::shownClip, Scp354Lizard::actionElapsed, Scp354Lizard::gait, Event -> {
            byte Current = shownAction();
            Event.getController().transitionLength(Current == ActionEmerge ? 0 : Current == ActionNone ? 4 : 2);
            if (isDeadOrDying()) {
                Event.getController().transitionLength(blendTo("collapse", 3));
                return Event.setAndContinue(CollapsePlay);
            }
            if (Current == ActionEmerge) {
                blendTo("emerge", 0);
                return Event.setAndContinue(EmergePlay);
            }
            if (Current == ActionBite) {
                Event.getController().transitionLength(blendTo("bite", 2));
                return Event.setAndContinue(BitePlay);
            }
            if (Current == ActionWhip) {
                Event.getController().transitionLength(blendTo("whip", 2));
                return Event.setAndContinue(WhipPlay);
            }
            if (isHauling()) {
                Event.getController().transitionLength(blendTo("haulout", 2));
                return Event.setAndContinue(HaulPlay);
            }
            if (isInWater() || isInFluidType()) {
                Event.getController().transitionLength(blendTo("swim", 4));
                return Event.setAndContinue(SwimLoop);
            }
            if (isStriding()) {
                Event.getController().transitionLength(blendTo(isRunning() ? "run" : "walk", 4));
                return Event.setAndContinue(isRunning() ? RunLoop : WalkLoop);
            }
            Event.getController().transitionLength(blendTo("idle", 4));
            return Event.setAndContinue(IdleLoop);
        }));
    }

    public static final class Strike extends MeleeAttackGoal {
        private final Scp354Lizard Striker;
        private int Crowd;

        public Strike(Scp354Lizard Striker) {
            super(Striker, ChaseModifier, true);
            this.Striker = Striker;
        }

        @Override
        public void tick() {
            super.tick();
            if (Striker.isActing() || !Striker.canWhip() || ++Crowd % 5 != 0) {
                return;
            }
            for (LivingEntity Found : Striker.level().getEntitiesOfClass(LivingEntity.class, Striker.getBoundingBox().inflate(WhipReach * 0.8, 1.0, WhipReach * 0.8),
                    Scp354Prey::isPrey)) {
                if (Striker.behind(Found)) {
                    Striker.beginWhip(Found);
                    return;
                }
            }
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity Target) {
            if (Striker.isActing() || !Striker.hasLineOfSight(Target)) {
                return;
            }
            double Reach = Striker.reachTo(Target);
            if (Reach <= BiteReach && !Striker.behind(Target) && Striker.canBite()) {
                resetAttackCooldown();
                Striker.beginBite(Target);
            } else if (Reach <= WhipReach * 0.8 && Striker.behind(Target) && Striker.canWhip()) {
                resetAttackCooldown();
                Striker.beginWhip(Target);
            }
        }
    }
}
