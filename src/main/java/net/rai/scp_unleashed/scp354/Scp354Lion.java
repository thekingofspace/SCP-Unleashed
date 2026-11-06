package net.rai.scp_unleashed.scp354;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
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

import java.util.EnumSet;
import java.util.List;

public class Scp354Lion extends Scp354Beast {
    public static final float Width = 1.5f;
    public static final float Height = 1.9f;
    public static final float EyeHeight = 1.55f;
    public static final double Health = 110.0;
    public static final double Armour = 6.0;
    public static final double Toughness = 2.0;
    public static final double KnockbackResistance = 0.6;
    public static final double Speed = 0.4;
    public static final double ChaseModifier = 1.0;
    public static final double StalkModifier = 0.35;
    public static final double WanderModifier = 0.45;
    public static final double FollowRange = 48.0;
    public static final double AcquireRange = 32.0;
    public static final double AcquireHeight = 10.0;
    public static final double StalkRange = 12.0;
    public static final int ProvokedTicks = 200;
    public static final double MaulDamage = 14.0;
    public static final double MaulReach = 1.5;
    public static final float MaulKnock = 0.6f;
    public static final double PounceDamage = 10.0;
    public static final double PounceMin = 4.0;
    public static final double PounceMax = 10.0;
    public static final double PounceRise = 3.0;
    public static final double PounceSpeed = 1.15;
    public static final double PounceLift = 0.42;
    public static final int PounceChance = 3;
    public static final int MaulTicks = 16;
    public static final int MaulHitTick = 8;
    public static final int MaulCooldown = 22;
    public static final int PounceTicks = 24;
    public static final int PounceLaunchTick = 6;
    public static final int PounceCooldown = 80;
    public static final int RoarTicks = 36;
    public static final int RoarTick = 10;
    public static final int RoarCooldown = 600;
    public static final int EmergeTicks = 50;
    public static final int EmergeRoarTick = 34;
    public static final int HaulTicks = 16;
    public static final double SwimSpeed = 0.15;
    public static final double Draft = 1.2;
    public static final double HaulReach = 1.9;
    public static final int AmbientInterval = 240;
    public static final byte ActionMaul = 2;
    public static final byte ActionPounce = 3;
    public static final byte ActionRoar = 4;
    public static final Scp354Gait.Shape LionGait = new Scp354Gait.Shape(
            new Scp354Gait.Profile(20.0, 0.0673, 0.5, 2.4, new double[]{0.0, 0.25, 0.5, 0.75}, new float[]{1.0f, 0.9f, 1.0f, 0.9f}),
            new Scp354Gait.Profile(10.0, 0.2857, 0.6, 1.8, new double[]{0.0, 0.5}, new float[]{1.0f, 1.0f}),
            new Scp354Gait.Profile(28.0, 0.0417, 0.5, 2.4, new double[]{0.0, 0.25, 0.5, 0.75}, new float[]{0.7f, 0.6f, 0.7f, 0.6f}),
            0.2, 0.15, 4, Scp354Registry.LionStep, Scp354Registry.LionStepPad, 0.5f, 0.35f, 1.0f, 24.0);
    private static final EntityDataAccessor<Boolean> StalkingData = SynchedEntityData.defineId(Scp354Lion.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RunLoop = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation StalkLoop = RawAnimation.begin().thenLoop("stalk");
    private static final RawAnimation SwimLoop = RawAnimation.begin().thenLoop("swim");
    private static final RawAnimation HaulPlay = RawAnimation.begin().thenPlayAndHold("haulout");
    private static final RawAnimation EmergePlay = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation MaulPlay = RawAnimation.begin().thenPlayAndHold("maul");
    private static final RawAnimation PouncePlay = RawAnimation.begin().thenPlayAndHold("pounce");
    private static final RawAnimation RoarPlay = RawAnimation.begin().thenPlayAndHold("roar");
    private static final RawAnimation CollapsePlay = RawAnimation.begin().thenPlayAndHold("collapse");
    @Nullable
    private LivingEntity StrikeTarget;
    private int MaulReady;
    private int PounceReady;
    private int RoarReady;
    private int Provoked;
    private boolean PounceHit;

    public Scp354Lion(EntityType<? extends Scp354Lion> Type, Level World) {
        super(Type, World);
        xpReward = 30;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, MaulDamage)
                .add(Attributes.ARMOR, Armour).add(Attributes.ARMOR_TOUGHNESS, Toughness).add(Attributes.KNOCKBACK_RESISTANCE, KnockbackResistance)
                .add(Attributes.FOLLOW_RANGE, FollowRange).add(Attributes.STEP_HEIGHT, 1.5).add(Attributes.ATTACK_KNOCKBACK, 0.5);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(StalkingData, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new Scp354Swim.Escape(this, this));
        goalSelector.addGoal(0, new Scp354Swim.Buoy(this));
        goalSelector.addGoal(1, new Scp354Prey.HoldStill(this));
        goalSelector.addGoal(2, new Pounce(this));
        goalSelector.addGoal(3, new Strike(this));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, WanderModifier));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 16.0f));
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
            case ActionMaul -> "maul";
            case ActionPounce -> "pounce";
            case ActionRoar -> "roar";
            default -> null;
        };
    }

    @Override
    @Nullable
    protected Scp354Gait.Shape gaitShape() {
        return LionGait;
    }

    @Override
    protected List<DeferredHolder<SoundEvent, SoundEvent>> deathLayers() {
        return Scp354Registry.LionDeathLayers;
    }

    @Override
    public boolean locksMovement() {
        byte Current = action();
        return Current == ActionEmerge || Current == ActionMaul || Current == ActionRoar;
    }

    @Override
    public boolean isStalking() {
        if (level().isClientSide) {
            return entityData.get(StalkingData);
        }
        LivingEntity Target = getTarget();
        return Target != null && Provoked <= 0 && distanceTo(Target) > StalkRange;
    }

    public boolean canMaul() {
        return MaulReady <= 0 && !isActing();
    }

    public boolean canPounce() {
        return PounceReady <= 0 && !isActing() && onGround();
    }

    public void beginMaul(LivingEntity Target) {
        StrikeTarget = Target;
        MaulReady = MaulCooldown;
        setAction(ActionMaul, MaulTicks);
    }

    public void beginPounce(LivingEntity Target) {
        StrikeTarget = Target;
        PounceReady = PounceCooldown + random.nextInt(30);
        PounceHit = false;
        Provoked = ProvokedTicks;
        setAction(ActionPounce, PounceTicks);
    }

    public void beginRoar() {
        RoarReady = RoarCooldown + random.nextInt(400);
        setAction(ActionRoar, RoarTicks);
    }

    @Override
    public void setTarget(@Nullable LivingEntity Target) {
        LivingEntity Before = getTarget();
        super.setTarget(Target);
        if (Target != null && Before == null && RoarReady <= 0 && !isActing() && !level().isClientSide && distanceTo(Target) < StalkRange) {
            beginRoar();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !isAlive()) {
            return;
        }
        boolean Stalking = isStalking();
        if (entityData.get(StalkingData) != Stalking) {
            entityData.set(StalkingData, Stalking);
        }
        MaulReady = Math.max(0, MaulReady - 1);
        PounceReady = Math.max(0, PounceReady - 1);
        RoarReady = Math.max(0, RoarReady - 1);
        Provoked = Math.max(0, Provoked - 1);
    }

    @Override
    protected void actionTick(byte Kind, int Elapsed) {
        if (Kind == ActionEmerge) {
            Scp354Swim.halt(this);
            if (Elapsed == EmergeRoarTick) {
                Scp354Sounds.play(this, Scp354Registry.RoarLayers, 2.0f, 0.9f + random.nextFloat() * 0.1f);
            }
            return;
        }
        LivingEntity Target = StrikeTarget;
        if (Kind == ActionRoar) {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
            if (Elapsed == RoarTick) {
                Scp354Sounds.play(this, Scp354Registry.RoarLayers, 2.0f, 0.95f + random.nextFloat() * 0.1f);
            }
            if (getTarget() != null) {
                turnToward(getTarget(), 12.0f);
            }
            return;
        }
        if (Target != null && Target.isAlive() && Kind == ActionMaul) {
            turnToward(Target, 30.0f);
        }
        if (Kind == ActionMaul) {
            setDeltaMovement(getDeltaMovement().multiply(0.0, 1.0, 0.0));
            if (Elapsed == 2) {
                Scp354Sounds.play(this, Scp354Registry.MaulLayers.subList(1, 2), 1.0f, 0.9f + random.nextFloat() * 0.15f);
            }
            if (Elapsed == MaulHitTick) {
                maulHit(Target);
            }
        } else if (Kind == ActionPounce) {
            if (Elapsed < PounceLaunchTick) {
                setDeltaMovement(getDeltaMovement().multiply(0.2, 1.0, 0.2));
                if (Target != null) {
                    turnToward(Target, 40.0f);
                }
            } else if (Elapsed == PounceLaunchTick) {
                launch(Target);
            } else if (!PounceHit && Target != null && Target.isAlive() && getBoundingBox().inflate(0.4).intersects(Target.getBoundingBox())) {
                PounceHit = true;
                if (Target.hurt(damageSources().mobAttack(this), (float) PounceDamage)) {
                    Vec3 Push = getDeltaMovement().multiply(1.0, 0.0, 1.0);
                    Target.knockback(0.8, -Push.x, -Push.z);
                    Target.hurtMarked = true;
                    setLastHurtMob(Target);
                }
            }
        }
    }

    private void maulHit(@Nullable LivingEntity Target) {
        Scp354Sounds.play(this, Scp354Registry.MaulLayers.subList(0, 1), 1.2f, 0.85f + random.nextFloat() * 0.15f);
        if (Target == null || !Target.isAlive() || reachTo(Target) > MaulReach || !hasLineOfSight(Target)) {
            return;
        }
        if (doHurtTarget(Target)) {
            Target.knockback(MaulKnock, getX() - Target.getX(), getZ() - Target.getZ());
            Target.hurtMarked = true;
        }
    }

    private void launch(@Nullable LivingEntity Target) {
        if (Target == null || !onGround()) {
            return;
        }
        Vec3 Toward = new Vec3(Target.getX() - getX(), 0.0, Target.getZ() - getZ());
        double Distance = Toward.length();
        if (Distance < 1.0e-3) {
            return;
        }
        double Push = Math.min(PounceSpeed, 0.25 + Distance * 0.1);
        Vec3 Velocity = Toward.scale(Push / Distance);
        setDeltaMovement(Velocity.x, PounceLift + Math.max(0.0, Target.getY() - getY()) * 0.06, Velocity.z);
        hasImpulse = true;
        Scp354Sounds.play(this, Scp354Registry.PounceLayers, 1.4f, 0.9f + random.nextFloat() * 0.15f);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt) {
            Provoked = ProvokedTicks;
        }
        return Hurt;
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
        Scp354Sounds.play(this, Scp354Registry.LionIdleLayers, 1.1f, 0.9f + random.nextFloat() * 0.15f);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp354Registry.LionIdle.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp354Registry.LionHurt.get();
    }

    @Override
    protected void playHurtSound(DamageSource Source) {
        Scp354Sounds.play(this, Scp354Registry.LionHurtLayers, 1.2f, 0.9f + random.nextFloat() * 0.15f);
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
        Controllers.add(new Scp354StageController<>(this, "body", Scp354Lion::shownClip, Scp354Lion::actionElapsed, Scp354Lion::gait, Event -> {
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
            if (Current == ActionMaul) {
                Event.getController().transitionLength(blendTo("maul", 2));
                return Event.setAndContinue(MaulPlay);
            }
            if (Current == ActionPounce) {
                Event.getController().transitionLength(blendTo("pounce", 2));
                return Event.setAndContinue(PouncePlay);
            }
            if (Current == ActionRoar) {
                Event.getController().transitionLength(blendTo("roar", 2));
                return Event.setAndContinue(RoarPlay);
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
                String Stride = isStalkStriding() ? "stalk" : isRunning() ? "run" : "walk";
                Event.getController().transitionLength(blendTo(Stride, 4));
                return Event.setAndContinue(isStalkStriding() ? StalkLoop : isRunning() ? RunLoop : WalkLoop);
            }
            Event.getController().transitionLength(blendTo("idle", 4));
            return Event.setAndContinue(IdleLoop);
        }));
    }

    public static final class Strike extends MeleeAttackGoal {
        private final Scp354Lion Striker;

        public Strike(Scp354Lion Striker) {
            super(Striker, ChaseModifier, true);
            this.Striker = Striker;
        }

        @Override
        public void tick() {
            super.tick();
            if (Striker.getTarget() != null) {
                Striker.getNavigation().setSpeedModifier(Striker.isStalking() ? StalkModifier : ChaseModifier);
            }
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity Target) {
            if (Striker.isActing() || !Striker.hasLineOfSight(Target)) {
                return;
            }
            if (Striker.reachTo(Target) <= MaulReach && Striker.canMaul()) {
                resetAttackCooldown();
                Striker.beginMaul(Target);
            }
        }
    }

    public static final class Pounce extends Goal {
        private final Scp354Lion Jumper;

        public Pounce(Scp354Lion Jumper) {
            this.Jumper = Jumper;
            setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            LivingEntity Target = Jumper.getTarget();
            if (Target == null || !Target.isAlive() || !Jumper.canPounce() || Math.abs(Target.getY() - Jumper.getY()) > PounceRise || Jumper.isInFluidType()
                    || Scp354Swim.crossesPool(Jumper.level(), Jumper.position(), Target.position())) {
                return false;
            }
            double Distance = Jumper.distanceTo(Target);
            return Distance >= PounceMin && Distance <= PounceMax && Jumper.hasLineOfSight(Target) && Jumper.getRandom().nextInt(reducedTickDelay(PounceChance)) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return Jumper.action() == ActionPounce;
        }

        @Override
        public void start() {
            LivingEntity Target = Jumper.getTarget();
            if (Target != null) {
                Jumper.beginPounce(Target);
            }
        }
    }
}
