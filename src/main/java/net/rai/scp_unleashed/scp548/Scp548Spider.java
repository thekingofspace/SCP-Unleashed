package net.rai.scp_unleashed.scp548;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.navigation.WallClimberNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.rai.scp_unleashed.cooler.CoolerClimate;
import net.rai.scp_unleashed.motion.StrideGait;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class Scp548Spider extends Animal implements GeoEntity {
    public static final float Width = 0.45f;
    public static final float Height = 0.25f;
    public static final float EyeHeight = 0.17f;
    public static final double MaxHealth = 8.0;
    public static final double Speed = 0.2;
    public static final double AttackDamage = 2.0;
    public static final double FollowRange = 16.0;
    public static final double ChaseRange = 20.0;
    public static final double RoamModifier = 0.55;
    public static final double ChaseModifier = 1.13;
    public static final double RetreatModifier = 1.13;
    public static final double BreedModifier = 0.65;
    public static final int RoamInterval = 40;
    public static final int CalmRoamInterval = 160;
    public static final int GroomChance = 400;
    public static final int RetreatTicks = 6;
    public static final int BiteCooldown = 20;
    public static final float LungeLift = 0.3f;
    public static final int ProvokeTicks = 160;
    public static final double SettleRange = 6.0;
    public static final int SettleRate = 4;
    public static final double AlertRadius = 6.0;
    public static final int ScanInterval = 10;
    public static final int ThawDelay = 200;
    public static final int BreedCooldown = 6000;
    public static final int Young = 2;
    public static final int AmbientInterval = 160;
    public static final double WalkDistance = 0.105;
    public static final double RunDistance = 0.1453;
    public static final StrideGait.Shape Gait = new StrideGait.Shape(0.004, 0.055, 0.048, WalkDistance, RunDistance, 0.8, 1);
    public static final byte Contained = 0;
    public static final byte Thawing = 1;
    public static final byte Hostile = 2;
    private static final EntityDataAccessor<Byte> MoodData = SynchedEntityData.defineId(Scp548Spider.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> ProvokedData = SynchedEntityData.defineId(Scp548Spider.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation ChillLoop = RawAnimation.begin().thenLoop("chill");
    private static final RawAnimation ThreatLoop = RawAnimation.begin().thenLoop("threat");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation RunLoop = RawAnimation.begin().thenLoop("run");
    private static final RawAnimation BiteOnce = RawAnimation.begin().thenPlay("bite");
    private static final RawAnimation EatOnce = RawAnimation.begin().thenPlay("eat");
    private static final RawAnimation GroomOnce = RawAnimation.begin().thenPlay("groom");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final StrideGait Stride = new StrideGait();
    private int Chill = ThawDelay;
    private int RetreatLeft;
    private int BiteLeft;
    private int Bites;
    private float RenderPartial;
    @Nullable
    private UUID ProvokedBy;
    private int ProvokeLeft;
    private CoolerClimate Climate = CoolerClimate.Mild;

    public Scp548Spider(EntityType<? extends Scp548Spider> Type, Level World) {
        super(Type, World);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createMobAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, AttackDamage)
                .add(Attributes.FOLLOW_RANGE, FollowRange);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(MoodData, Thawing);
        Builder.define(ProvokedData, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp548Goals.Retreat(this));
        goalSelector.addGoal(2, new Scp548Goals.Lunge(this));
        goalSelector.addGoal(3, new Scp548Goals.Strike(this));
        goalSelector.addGoal(4, new BreedGoal(this, BreedModifier));
        goalSelector.addGoal(5, new Scp548Goals.Nestle(this));
        goalSelector.addGoal(6, new Scp548Goals.Roam(this));
        goalSelector.addGoal(7, new Scp548Goals.Awake<>(this, new LookAtPlayerGoal(this, Player.class, 6.0f)));
        goalSelector.addGoal(8, new Scp548Goals.Awake<>(this, new RandomLookAroundGoal(this)));
        targetSelector.addGoal(1, new Scp548Goals.Hunt(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Group);
        setPersistenceRequired();
        return Result;
    }

    public byte mood() {
        return entityData.get(MoodData);
    }

    public boolean isContained() {
        return mood() == Contained;
    }

    public boolean isHostile() {
        return mood() == Hostile;
    }

    public boolean isProvoked() {
        return entityData.get(ProvokedData);
    }

    public boolean isAggressive() {
        return isHostile() || isProvoked();
    }

    public CoolerClimate climate() {
        return Climate;
    }

    public void provoke(LivingEntity Attacker) {
        ProvokedBy = Attacker.getUUID();
        ProvokeLeft = ProvokeTicks;
        entityData.set(ProvokedData, true);
        setTarget(Attacker);
    }

    public boolean withinReach(LivingEntity Who) {
        return Who.distanceToSqr(this) <= SettleRange * SettleRange;
    }

    @Nullable
    public LivingEntity provoker() {
        if (ProvokedBy == null || !(level() instanceof ServerLevel World)) {
            return null;
        }
        return World.getEntity(ProvokedBy) instanceof LivingEntity Who && Who.isAlive() ? Who : null;
    }

    private void settle() {
        ProvokedBy = null;
        ProvokeLeft = 0;
        entityData.set(ProvokedData, false);
        if (!isHostile()) {
            setTarget(null);
            getNavigation().stop();
        }
    }

    public int chill() {
        return Chill;
    }

    public void chillFor(int Ticks) {
        Chill = Math.max(Chill, Ticks);
        if (mood() == Hostile && Chill > 0) {
            setMood(Thawing);
        }
    }

    public void setChill(int Ticks) {
        Chill = Math.max(0, Ticks);
    }

    public int bites() {
        return Bites;
    }

    public int retreating() {
        return RetreatLeft;
    }

    public boolean biteReady() {
        return BiteLeft == 0;
    }

    public StrideGait stride() {
        return Stride;
    }

    public boolean isStriding() {
        return Stride.stride() != StrideGait.Idle;
    }

    public boolean isRunning() {
        return Stride.stride() == StrideGait.Run;
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    public float renderPartial() {
        return RenderPartial;
    }

    private void setMood(byte Mood) {
        byte Was = mood();
        if (Was == Mood) {
            return;
        }
        entityData.set(MoodData, Mood);
        if (Mood != Hostile) {
            setTarget(null);
            getNavigation().stop();
        }
        if (level() instanceof ServerLevel World) {
            if (Mood == Hostile) {
                Scp548Swarm.playLayers(World, position(), Scp548Registry.ChitterLayers, 0.8f, 0.95f + random.nextFloat() * 0.1f);
            } else if (Mood == Contained) {
                Scp548Swarm.playLayers(World, position(), Scp548Registry.CrackleLayers, 0.5f, 0.9f + random.nextFloat() * 0.1f);
            }
        }
    }

    public void scan(ServerLevel World) {
        Climate = CoolerClimate.at(World, blockPosition());
        boolean Cooled = Scp548Swarm.cooled(World, position());
        if (Cooled) {
            Chill = ThawDelay;
        } else {
            Chill = Math.max(0, Chill - ScanInterval);
        }
        setMood(Cooled ? Contained : Chill > 0 ? Thawing : Hostile);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        if ((tickCount + getId()) % ScanInterval == 0) {
            scan(World);
        }
        if (RetreatLeft > 0) {
            RetreatLeft--;
        }
        if (BiteLeft > 0) {
            BiteLeft--;
        }
        if (ProvokeLeft > 0) {
            LivingEntity Who = provoker();
            boolean Away = Who == null || !Scp548Swarm.isPrey(Who) || !withinReach(Who);
            if (Away && !isHostile() && getTarget() != null) {
                setTarget(null);
                getNavigation().stop();
            }
            ProvokeLeft = Math.max(0, ProvokeLeft - (Away ? SettleRate : 1));
            if (ProvokeLeft == 0) {
                settle();
            }
        }
        if (isHostile()) {
            int Pulse = Scp548Frost.pulseFor(Climate);
            if (Pulse > 0 && (tickCount + getId()) % Pulse == 0 && Scp548Frost.allowed(World, this)) {
                BlockPos Target = Scp548Frost.pick(World, this);
                if (Target != null) {
                    Scp548Frost.frost(World, Target, this);
                }
            }
        }
        if ((tickCount + getId()) % Scp548Frost.WaterPulse == 0 && Scp548Frost.allowed(World, this)) {
            Scp548Frost.chillWater(World, this);
        }
        if (!isAggressive() && getNavigation().isDone() && random.nextInt(GroomChance) == 0) {
            triggerAnim("action", "groom");
        }
        Scp548Chill.radiate(World, this);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            double Dx = getX() - xo;
            double Dz = getZ() - zo;
            Stride.tick(Math.sqrt(Dx * Dx + Dz * Dz), isAlive(), Gait);
        }
    }

    @Override
    public boolean doHurtTarget(Entity Target) {
        boolean Hit = super.doHurtTarget(Target);
        if (level() instanceof ServerLevel World) {
            triggerAnim("action", "bite");
            Scp548Swarm.playLayers(World, Target.position(), Scp548Registry.BiteLayers, 0.8f, 0.95f + random.nextFloat() * 0.1f);
            if (Hit && Target instanceof LivingEntity Victim) {
                Bites++;
                if (Scp548Chill.bite(Victim, this)) {
                    World.sendParticles(ParticleTypes.SNOWFLAKE, Victim.getX(), Victim.getY() + Victim.getBbHeight() * 0.4, Victim.getZ(), 12, 0.25, 0.25, 0.25, 0.02);
                    Scp548Swarm.playLayers(World, Victim.position(), Scp548Registry.FrostLayers, 0.6f, 1.1f);
                }
            }
            RetreatLeft = RetreatTicks;
            BiteLeft = BiteCooldown;
        }
        return Hit;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && Source.getEntity() instanceof LivingEntity Attacker && Scp548Swarm.isPrey(Attacker) && level() instanceof ServerLevel World) {
            if (isHostile()) {
                setTarget(Attacker);
            } else {
                provoke(Attacker);
                for (Scp548Spider Other : World.getEntitiesOfClass(Scp548Spider.class, getBoundingBox().inflate(AlertRadius), Each -> Each != this && Each.isAlive() && !Each.isHostile() && Each.distanceToSqr(this) <= AlertRadius * AlertRadius)) {
                    Other.provoke(Attacker);
                }
            }
        }
        return Hurt;
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new WallClimberNavigation(this, World);
    }

    @Override
    public boolean onClimbable() {
        return isHostile() && horizontalCollision;
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    public boolean isFood(ItemStack Stack) {
        return Stack.is(Items.SNOWBALL);
    }

    @Override
    public boolean canFallInLove() {
        return super.canFallInLove() && !(level() instanceof ServerLevel World && Scp548Swarm.crowded(World, position()));
    }

    @Override
    public InteractionResult mobInteract(Player User, InteractionHand Hand) {
        ItemStack Stack = User.getItemInHand(Hand);
        boolean Food = isFood(Stack);
        boolean WasInLove = isInLove();
        int AgeBefore = getAge();
        InteractionResult Result = super.mobInteract(User, Hand);
        if (Food && level() instanceof ServerLevel World && Result.consumesAction() && (isInLove() && !WasInLove || getAge() != AgeBefore)) {
            fed(World);
        }
        return Result;
    }

    public void fed(ServerLevel World) {
        triggerAnim("action", "eat");
        Scp548Swarm.playLayers(World, position(), Scp548Registry.EatLayers, 0.7f, 0.95f + random.nextFloat() * 0.1f);
        World.sendParticles(ParticleTypes.ITEM_SNOWBALL, getX(), getY() + getBbHeight() * 0.5, getZ(), 6, 0.15, 0.1, 0.15, 0.02);
    }

    @Nullable
    @Override
    public Scp548Spider getBreedOffspring(ServerLevel World, AgeableMob Partner) {
        return Scp548Registry.Spider.get().create(World);
    }

    @Override
    public void spawnChildFromBreeding(ServerLevel World, Animal Partner) {
        int Room = Scp548Swarm.allowed(World, position(), Young);
        int Inherited = Partner instanceof Scp548Spider Other ? Math.max(Chill, Other.Chill) : Chill;
        Scp548Spider First = null;
        for (int Index = 0; Index < Room; Index++) {
            Scp548Spider Child = getBreedOffspring(World, Partner);
            if (Child == null) {
                break;
            }
            Child.setBaby(true);
            double Angle = random.nextDouble() * Math.PI * 2.0;
            Child.moveTo(getX() + Math.cos(Angle) * 0.35, getY(), getZ() + Math.sin(Angle) * 0.35, random.nextFloat() * 360.0f, 0.0f);
            if (!World.noCollision(Child)) {
                Child.moveTo(getX(), getY(), getZ(), Child.getYRot(), 0.0f);
            }
            Child.setPersistenceRequired();
            Child.setChill(Inherited);
            World.addFreshEntityWithPassengers(Child);
            if (First == null) {
                First = Child;
            }
        }
        finalizeSpawnChildFromBreeding(World, Partner, First);
        setAge(BreedCooldown);
        Partner.setAge(BreedCooldown);
        if (First != null) {
            World.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + 0.2, getZ(), 16, 0.4, 0.15, 0.4, 0.02);
            Scp548Swarm.play(World, position(), Scp548Registry.Hatch.get(), 0.7f, 1.0f);
        }
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
        if (level() instanceof ServerLevel World) {
            float Volume = isBaby() ? 0.12f : 0.22f;
            Scp548Swarm.playLayers(World, position(), Scp548Registry.SkitterLayers, Volume, (isBaby() ? 1.25f : 1.0f) * (0.92f + random.nextFloat() * 0.16f));
        }
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return isHostile() ? Scp548Registry.Chitter.get() : Scp548Registry.Crackle.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return AmbientInterval;
    }

    @Override
    public void playAmbientSound() {
        if (level() instanceof ServerLevel World) {
            float Pitch = (isBaby() ? 1.25f : 1.0f) * (0.95f + random.nextFloat() * 0.1f);
            Scp548Swarm.playLayers(World, position(), isHostile() ? Scp548Registry.ChitterLayers : Scp548Registry.CrackleLayers, isHostile() ? 0.6f : 0.35f, Pitch);
        }
    }

    @Override
    protected void playHurtSound(DamageSource Source) {
        if (level() instanceof ServerLevel World) {
            Scp548Swarm.playLayers(World, position(), Scp548Registry.HurtLayers, 0.7f, (isBaby() ? 1.25f : 1.0f) * (0.95f + random.nextFloat() * 0.1f));
        }
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp548Registry.Hurt.get();
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return Scp548Registry.Death.get();
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        if (level() instanceof ServerLevel World) {
            Scp548Swarm.play(World, position(), Scp548Registry.DeathShatter.get(), 0.8f, 1.0f);
            World.sendParticles(Scp548Swarm.Shards, getX(), getY() + getBbHeight() * 0.5, getZ(), 24, getBbWidth() * 0.35, getBbHeight() * 0.3, getBbWidth() * 0.35, 0.08);
            World.sendParticles(ParticleTypes.SNOWFLAKE, getX(), getY() + getBbHeight() * 0.5, getZ(), 14, getBbWidth() * 0.4, getBbHeight() * 0.3, getBbWidth() * 0.4, 0.03);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("Chill", Chill);
        Tag.putByte("Mood", mood());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        if (Tag.contains("Chill")) {
            Chill = Math.max(0, Tag.getInt("Chill"));
        }
        if (Tag.contains("Mood")) {
            entityData.set(MoodData, (byte) Mth.clamp(Tag.getByte("Mood"), Contained, Hostile));
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp548GaitController(this, "body", 4, Event -> {
            if (isStriding()) {
                return Event.setAndContinue(isRunning() ? RunLoop : WalkLoop);
            }
            byte Mood = mood();
            if (Mood == Hostile || isProvoked()) {
                return Event.setAndContinue(ThreatLoop);
            }
            if (Mood == Contained) {
                return Event.setAndContinue(ChillLoop);
            }
            return Event.setAndContinue(IdleLoop);
        }));
        Controllers.add(new AnimationController<>(this, "action", 2, Event -> PlayState.STOP).triggerableAnim("bite", BiteOnce).triggerableAnim("eat", EatOnce)
                .triggerableAnim("groom", GroomOnce));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
