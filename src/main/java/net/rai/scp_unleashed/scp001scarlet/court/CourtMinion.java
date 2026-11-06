package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

public abstract class CourtMinion extends Monster implements GeoEntity, CourtMember, CourtStrider {
    public static final float LargeWidth = 1.0f;
    private final CourtKeeper.Watch Keeper = new CourtKeeper.Watch();
    public static final int None = 0;
    public static final int Attack = 1;
    public static final int SpecialA = 2;
    public static final int SpecialB = 3;
    public static final int SpecialC = 4;
    public static final int SpawnTicks = 30;
    protected static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    protected static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    protected static final RawAnimation DeathHold = RawAnimation.begin().thenPlayAndHold("death");
    protected static final RawAnimation SpawnOnce = RawAnimation.begin().thenPlay("spawn").thenLoop("idle");
    private static final EntityDataAccessor<Integer> ActionData = SynchedEntityData.defineId(CourtMinion.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SerialData = SynchedEntityData.defineId(CourtMinion.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> RiseData = SynchedEntityData.defineId(CourtMinion.class, EntityDataSerializers.LONG);
    protected final CourtKind Kind;
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private List<Class<? extends Goal>> Behaviours;
    private int ActionTime;
    private int ActionLength;
    private int SeenSerial = -1;
    private double Travel;
    private double TravelO;
    private float RenderPartial = 1.0f;

    protected CourtMinion(EntityType<? extends Monster> Type, Level World, CourtKind Kind) {
        super(Type, World);
        this.Kind = Kind;
        xpReward = Math.max(1, (int) (Kind.health() / 8.0));
    }

    public static AttributeSupplier.Builder attributes(CourtKind Kind) {
        AttributeSupplier.Builder Builder = Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Kind.health()).add(Attributes.ATTACK_DAMAGE, Kind.damage())
                .add(Attributes.MOVEMENT_SPEED, Kind.speed()).add(Attributes.ARMOR, Kind.armor()).add(Attributes.FOLLOW_RANGE, 48.0);
        if (Kind.flying()) {
            Builder.add(Attributes.FLYING_SPEED, Kind.speed());
        }
        if (Kind == CourtKind.ThroneGolem) {
            Builder.add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.STEP_HEIGHT, 1.1);
        }
        if (Kind == CourtKind.CrimsonKnight || Kind == CourtKind.BrideHusk) {
            Builder.add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
        }
        return Builder;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, None);
        Builder.define(SerialData, 0);
        Builder.define(RiseData, 0L);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        addBehaviours();
        goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.7));
        goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 10.0f));
        goalSelector.addGoal(10, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return super.canUse() && CourtFx.validTarget(mob.getLastHurtByMob());
            }
        });
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 10, false, false, Seen -> CourtFx.validTarget(Seen)));
    }

    protected abstract void addBehaviours();

    protected void behaviour(int Priority, Goal Added) {
        goalSelector.addGoal(Priority, Added);
        if (Behaviours == null) {
            Behaviours = new ArrayList<>();
        }
        Behaviours.add(Added.getClass());
    }

    public List<Class<? extends Goal>> behaviours() {
        return Behaviours == null ? List.of() : List.copyOf(Behaviours);
    }

    public CourtKind kind() {
        return Kind;
    }

    @Override
    public String courtId() {
        return Kind.id();
    }

    public int action() {
        return entityData.get(ActionData);
    }

    public int serial() {
        return entityData.get(SerialData);
    }

    public int actionTime() {
        return ActionTime;
    }

    public boolean busy() {
        return action() != None;
    }

    public void startAction(int Action, int Length) {
        if (rising()) {
            return;
        }
        entityData.set(ActionData, Action);
        entityData.set(SerialData, serial() + 1);
        ActionTime = 0;
        ActionLength = Length;
    }

    public void endAction() {
        entityData.set(ActionData, None);
        ActionTime = 0;
    }

    public void rise(int Ticks) {
        entityData.set(RiseData, level().getGameTime() + Ticks);
    }

    public boolean rising() {
        return level().getGameTime() < entityData.get(RiseData);
    }

    protected void actionTick(int Action, int Elapsed) {
        LivingEntity Target = getTarget();
        if (Action == Attack && Elapsed == hitTick() && Target != null && Target.isAlive() && distanceToSqr(Target) <= reachSqr(Target)) {
            hit(Target);
        }
    }

    protected int hitTick() {
        return 8;
    }

    protected double reach() {
        return 1.6 + getBbWidth() * 0.5;
    }

    protected double reachSqr(LivingEntity Target) {
        double Reach = reach() + Target.getBbWidth() * 0.5 + 0.6;
        return Reach * Reach;
    }

    protected void hit(LivingEntity Target) {
        if (CourtFx.strike(this, Target, (float) getAttributeValue(Attributes.ATTACK_DAMAGE))) {
            onHit(Target);
        }
    }

    protected void onHit(LivingEntity Target) {
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (rising()) {
            getNavigation().stop();
        } else if (Kind.width() >= LargeWidth && !Kind.flying() && level() instanceof ServerLevel World && ScarletCourt.Instance.active()) {
            CourtKeeper.keep(World, this, ScarletCourt.Instance.dais(), Keeper, getNavigation().isInProgress());
        }
        int Action = action();
        if (Action != None) {
            ActionTime++;
            actionTick(Action, ActionTime);
            if (ActionTime >= ActionLength && action() == Action) {
                endAction();
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            TravelO = Travel;
            double Dx = getX() - xo;
            double Dz = getZ() - zo;
            Travel += Math.sqrt(Dx * Dx + Dz * Dz);
        }
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    @Override
    public double strideFor(String Clip) {
        double Stride = CourtGaits.stride(Kind.id());
        if (Stride <= 0.0) {
            return 0.0;
        }
        if ("walk".equals(Clip)) {
            return Stride;
        }
        if ("run".equals(Clip)) {
            return Stride * 1.6;
        }
        return 0.0;
    }

    @Override
    public double cycle(double Stride) {
        double Distance = TravelO + (Travel - TravelO) * RenderPartial;
        double Phase = Distance / Stride;
        return Phase - Math.floor(Phase);
    }

    @Override
    public boolean isAlliedTo(Entity Other) {
        return CourtFx.allied(this, Other) || super.isAlliedTo(Other);
    }

    @Override
    public boolean canAttack(LivingEntity Other) {
        return CourtFx.validTarget(Other) && super.canAttack(Other);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        Entity Attacker = Source.getEntity();
        if (Attacker != null && Attacker != this && CourtFx.allied(this, Attacker)) {
            return false;
        }
        return super.hurt(Source, Amount);
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return !getTags().contains(CourtFx.Tag) && super.removeWhenFarAway(Distance);
    }

    @Override
    public boolean doHurtTarget(Entity Victim) {
        if (Victim instanceof LivingEntity Living) {
            return CourtFx.strike(this, Living, (float) getAttributeValue(Attributes.ATTACK_DAMAGE));
        }
        return super.doHurtTarget(Victim);
    }

    @Nullable
    protected SoundEvent courtSound(String Suffix) {
        String Name = Kind.soundKey() + "_" + Suffix;
        return ScarletCourtSounds.has(Name) ? ScarletCourtSounds.get(Name) : null;
    }

    protected void play(String Suffix, float Volume, float Pitch) {
        if (level() instanceof ServerLevel World) {
            String Name = Kind.soundKey() + "_" + Suffix;
            if (ScarletCourtSounds.has(Name)) {
                CourtFx.sound(World, position(), Name, Volume, Pitch * (0.92f + random.nextFloat() * 0.16f));
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return courtSound("idle");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        SoundEvent Found = courtSound("hurt");
        return Found != null ? Found : courtSound("idle");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return courtSound("death");
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    protected Vec3 front(double Distance) {
        return position().add(Vec3.directionFromRotation(0.0f, getYRot()).scale(Distance));
    }

    protected boolean inFront(Entity Other, double MinDot) {
        Vec3 Facing = Vec3.directionFromRotation(0.0f, getYRot());
        Vec3 Way = CourtFx.flat(Other.position().subtract(position()));
        return Facing.dot(Way) >= MinDot;
    }

    protected abstract String clip(int Action);

    protected RawAnimation moving() {
        return WalkLoop;
    }

    protected boolean holds(int Action) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new CourtController<>(this, "main", 4, State -> {
            CourtMinion Owner = State.getAnimatable();
            if (Owner.isDeadOrDying()) {
                return State.setAndContinue(DeathHold);
            }
            if (Owner.serial() != Owner.SeenSerial) {
                boolean First = Owner.SeenSerial < 0;
                Owner.SeenSerial = Owner.serial();
                if (!First) {
                    State.getController().forceAnimationReset();
                }
            }
            if (Owner.rising()) {
                return State.setAndContinue(SpawnOnce);
            }
            int Action = Owner.action();
            if (Action != None) {
                String Clip = Owner.clip(Action);
                if (Clip != null) {
                    return State.setAndContinue(Owner.holds(Action) ? RawAnimation.begin().thenPlayAndHold(Clip) : RawAnimation.begin().thenPlay(Clip).thenLoop("idle"));
                }
            }
            if (State.isMoving()) {
                return State.setAndContinue(Owner.moving());
            }
            return State.setAndContinue(IdleLoop);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

}
