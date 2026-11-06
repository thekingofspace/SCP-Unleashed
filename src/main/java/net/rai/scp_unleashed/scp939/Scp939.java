package net.rai.scp_unleashed.scp939;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.motion.StrideGait;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Scp939 extends AgeableMob implements Enemy, GeoEntity {
    public static final float Width = 1.3f;
    public static final float Height = 1.45f;
    public static final float EyeHeight = 1.15f;
    public static final double MaxHealth = 60.0;
    public static final double Speed = 0.3;
    public static final double AttackDamage = 9.0;
    public static final double FollowRange = 32.0;
    public static final double Armor = 4.0;
    public static final double KnockbackResistance = 0.4;
    public static final double StepHeight = 1.0;
    public static final double ProwlModifier = 0.55;
    public static final double RoamModifier = 0.8;
    public static final double SprintModifier = 1.3;
    public static final double WalkDistance = 1.2188;
    public static final double RunDistance = 2.1562;
    public static final StrideGait.Shape Gait = new StrideGait.Shape(0.004, 0.2, 0.16, WalkDistance, RunDistance, 0.6, 1);
    public static final StrideGait.Shape YoungGait = new StrideGait.Shape(0.002, 0.2, 0.16, WalkDistance * 0.5, RunDistance * 0.5, 0.6, 1);
    public static final int GrowTicks = 24000;
    public static final float YoungMealGrowth = 0.1f;
    public static final int MealsForBrood = 4;
    public static final int BroodCooldown = 24000;
    public static final int BroodRoll = 400;
    public static final int BroodChance = 6;
    public static final int AreaCap = 6;
    public static final int LevelCap = 24;
    public static final double AreaRadius = 32.0;
    public static final int HeaveTicks = 40;
    public static final int EatTicks = 48;
    public static final float MealHeal = 8.0f;
    public static final int ScanInterval = 40;
    public static final float HazeRise = 0.005f;
    public static final float HazeVent = 0.05f;
    public static final float HazeVisible = 0.25f;
    public static final int HazeSources = 4;
    public static final int AmbientInterval = 220;
    public static final float MimicChance = 0.4f;
    public static final int MaxVoices = 3;
    public static final int AttackCooldown = 20;
    public static final int SniffTicks = 60;
    public static final double[] WalkContacts = {0.0, 0.25, 0.5, 0.75};
    public static final double[] SprintContacts = {0.0, 0.1, 0.48, 0.58};
    public static final float ProwlStepVolume = 0.12f;
    public static final float WalkStepVolume = 0.3f;
    public static final float SprintStepVolume = 0.55f;
    public static final float LureChance = 0.5f;
    public static final int LureRetry = 200;
    public static final int LureMin = 300;
    public static final int LureMax = 600;
    public static final int LureFirstMin = 30;
    public static final int LureFirstMax = 50;
    public static final int LureCallMin = 60;
    public static final int LureCallMax = 140;
    public static final int LureCooldown = 900;
    public static final int LureOffPitch = 8;
    public static final double LureLunge = 6.0;
    public static final double LureLoseRange = 36.0;
    public static final DustParticleOptions BreathMote = new DustParticleOptions(new Vector3f(0.86f, 0.74f, 0.74f), 0.7f);
    private static final EntityDataAccessor<Float> HazeData = SynchedEntityData.defineId(Scp939.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> ContainedData = SynchedEntityData.defineId(Scp939.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> ProwlData = SynchedEntityData.defineId(Scp939.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> SniffData = SynchedEntityData.defineId(Scp939.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation SniffLoop = RawAnimation.begin().thenLoop("sniff");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation ProwlLoop = RawAnimation.begin().thenLoop("prowl");
    private static final RawAnimation SprintLoop = RawAnimation.begin().thenLoop("sprint");
    private static final RawAnimation BiteOnce = RawAnimation.begin().thenPlay("bite");
    private static final RawAnimation EatOnce = RawAnimation.begin().thenPlay("eat");
    private static final RawAnimation ChewOnce = RawAnimation.begin().thenPlay("chew");
    private static final RawAnimation ShakeOnce = RawAnimation.begin().thenPlay("shake");
    private static final RawAnimation CallOnce = RawAnimation.begin().thenPlay("call");

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final StrideGait Stride = new StrideGait();
    private final List<String> Voices = new ArrayList<>();
    private int Meals;
    private int BroodWait;
    private int Heave;
    private int Eating;
    private int LungeWait;
    private int AttackWait;
    private int Sniffing;
    private int Fed;
    private int LureLeft;
    private int LureWait;
    private int LureCall;
    private int Calls;
    @Nullable
    private LivingEntity LurePrey;
    private float RenderPartial;
    @Nullable
    private Scp939Rooms.Room Room;

    public Scp939(EntityType<? extends Scp939> Type, Level World) {
        super(Type, World);
        xpReward = 20;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, AttackDamage)
                .add(Attributes.FOLLOW_RANGE, FollowRange).add(Attributes.ARMOR, Armor).add(Attributes.KNOCKBACK_RESISTANCE, KnockbackResistance)
                .add(Attributes.STEP_HEIGHT, StepHeight);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(HazeData, 0.0f);
        Builder.define(ContainedData, false);
        Builder.define(ProwlData, false);
        Builder.define(SniffData, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp939Goals.Busy(this));
        goalSelector.addGoal(2, new Scp939Goals.Lure(this));
        goalSelector.addGoal(2, new Scp939Goals.Stalk(this));
        goalSelector.addGoal(3, new Scp939VoiceAttraction.Investigate(this));
        goalSelector.addGoal(4, new Scp939Goals.Sniff(this));
        goalSelector.addGoal(5, new Scp939Goals.Lurk(this));
        goalSelector.addGoal(7, new Scp939Goals.Quiet<>(this, new RandomLookAroundGoal(this)));
        targetSelector.addGoal(1, new HurtByTargetGoal(this, Scp939.class).setAlertOthers());
        targetSelector.addGoal(2, new Scp939Goals.Hunt(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Group == null ? new AgeableMobGroupData(false) : Group);
        setPersistenceRequired();
        return Result;
    }

    public float haze() {
        return entityData.get(HazeData);
    }

    public void setHaze(float Value) {
        entityData.set(HazeData, Mth.clamp(Value, 0.0f, 1.0f));
    }

    public boolean isHazed() {
        return haze() >= HazeVisible;
    }

    public boolean isContained() {
        return entityData.get(ContainedData);
    }

    public boolean isProwling() {
        return entityData.get(ProwlData);
    }

    public void setProwling(boolean Value) {
        entityData.set(ProwlData, Value);
    }

    public boolean isSniffing() {
        return entityData.get(SniffData);
    }

    @Nullable
    public Scp939Rooms.Room room() {
        return Room;
    }

    public int meals() {
        return Meals;
    }

    public void setMeals(int Count) {
        Meals = Math.max(0, Count);
    }

    public int broodWait() {
        return BroodWait;
    }

    public void setBroodWait(int Ticks) {
        BroodWait = Math.max(0, Ticks);
    }

    public int fed() {
        return Fed;
    }

    public boolean isEating() {
        return Eating > 0;
    }

    public boolean isBusy() {
        return Eating > 0 || Heave > 0;
    }

    public List<String> voices() {
        return Voices;
    }

    public static int contactsCrossed(double From, double To, double[] Contacts) {
        int Crossed = 0;
        for (double Contact : Contacts) {
            if (Math.floor(To - Contact) > Math.floor(From - Contact)) {
                Crossed++;
            }
        }
        return Crossed;
    }

    public boolean isLuring() {
        return LureLeft > 0 && LurePrey != null;
    }

    @Nullable
    public LivingEntity lurePrey() {
        return LurePrey;
    }

    public int lureLeft() {
        return LureLeft;
    }

    public void setLureLeft(int Ticks) {
        LureLeft = Math.max(0, Ticks);
    }

    public int lureWait() {
        return LureWait;
    }

    public void setLureWait(int Ticks) {
        LureWait = Math.max(0, Ticks);
    }

    public int calls() {
        return Calls;
    }

    public double lungeRange() {
        return isContained() ? Scp939Senses.ContainedReach : LureLunge;
    }

    public boolean lureReady() {
        return !isBaby() && !isBusy() && !isLuring() && LureWait <= 0;
    }

    public void beginLure(LivingEntity Prey) {
        LurePrey = Prey;
        LureLeft = Mth.nextInt(random, LureMin, LureMax);
        LureCall = Mth.nextInt(random, LureFirstMin, LureFirstMax);
        setTarget(null);
        getNavigation().stop();
        setProwling(true);
    }

    public void endLure(boolean Strike) {
        LivingEntity Prey = LurePrey;
        LurePrey = null;
        LureLeft = 0;
        LureWait = LureCooldown;
        setProwling(false);
        if (Strike && Prey != null && Prey.isAlive()) {
            setTarget(Prey);
            if (lungeReady() && distanceTo(Prey) >= Scp939Goals.LungeMin && distanceTo(Prey) <= Scp939Goals.LungeMax) {
                lunge(Prey);
            }
        }
    }

    public boolean tickLure() {
        if (--LureLeft <= 0) {
            return false;
        }
        if (--LureCall <= 0) {
            LureCall = Mth.nextInt(random, LureCallMin, LureCallMax);
            if (level() instanceof ServerLevel World) {
                lureCall(World);
            }
        }
        return true;
    }

    public void lureCall(ServerLevel World) {
        LivingEntity Prey = LurePrey;
        if (Scp939VoiceLure.call(World, this, Prey)) {
            Calls++;
            return;
        }
        Vec3 Origin = position().add(0.0, getBbHeight() * 0.6, 0.0);
        if (Prey != null) {
            Vec3 Away = position().subtract(Prey.position()).multiply(1.0, 0.0, 1.0);
            if (Away.lengthSqr() > 1.0e-4) {
                Away = Away.normalize();
                Vec3 Side = new Vec3(-Away.z, 0.0, Away.x);
                Origin = Origin.add(Away.scale(1.2)).add(Side.scale((random.nextDouble() - 0.5) * 2.0));
            }
        }
        boolean Off = random.nextInt(LureOffPitch) == 0;
        float Pitch = Off ? 0.82f + random.nextFloat() * 0.04f : 0.92f + random.nextFloat() * 0.16f;
        Scp939Sounds.play(World, Origin, Scp939Mimicry.lureSound(random).get(), getSoundSource(), 0.9f, Pitch);
        Calls++;
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

    public void sniff() {
        Sniffing = SniffTicks;
        entityData.set(SniffData, true);
        if (level() instanceof ServerLevel World) {
            Scp939Sounds.play(World, position(), Scp939Registry.SniffLayers, 0.5f, voicePitch());
        }
    }

    public float voicePitch() {
        return (isBaby() ? 1.35f : 1.0f) * (0.94f + random.nextFloat() * 0.12f);
    }

    public void scan(ServerLevel World) {
        Optional<Scp939Rooms.Room> Found = Scp939Rooms.roomAt(World, blockPosition());
        Room = Found.orElse(null);
        boolean Kept = Room != null;
        entityData.set(ContainedData, Kept);
        float Step = ScanInterval / 20.0f;
        if (Kept) {
            int Present = Math.min(HazeSources, Math.max(1, Scp939Rooms.members(World, Room).size()));
            setHaze(haze() + HazeRise * Present * Step / 2.0f);
        } else {
            setHaze(haze() - HazeVent * Step / 2.0f);
        }
    }

    public void scrub(float Amount) {
        setHaze(haze() - Amount);
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
        if (BroodWait > 0) {
            BroodWait--;
        }
        if (LungeWait > 0) {
            LungeWait--;
        }
        if (LureWait > 0) {
            LureWait--;
        }
        if (LurePrey != null && (!LurePrey.isAlive() || LurePrey.isRemoved())) {
            endLure(false);
        }
        if (AttackWait > 0) {
            AttackWait--;
        }
        if (Sniffing > 0 && --Sniffing == 0) {
            entityData.set(SniffData, false);
        }
        if (Eating > 0) {
            Eating--;
            if (Eating % 12 == 6) {
                Scp939Sounds.play(World, position(), Scp939Registry.EatLayers, 0.7f, voicePitch());
            }
        }
        if (Heave > 0) {
            Heave--;
            if (Heave == 0) {
                Scp939Brood.produce(World, this);
                Meals = 0;
                BroodWait = BroodCooldown;
            }
        } else if (canBrood(World) && (tickCount + getId()) % BroodRoll == 0 && random.nextInt(BroodChance) == 0) {
            heave(World);
        }
    }

    public boolean canBrood(ServerLevel World) {
        if (isBaby() || !isContained() || Meals < MealsForBrood || BroodWait > 0 || isBusy() || getTarget() != null) {
            return false;
        }
        return Scp939Population.room(World, position()) < AreaCap && Scp939Population.level(World) < LevelCap;
    }

    public void heave(ServerLevel World) {
        Heave = HeaveTicks;
        getNavigation().stop();
        triggerAnim("action", "shake");
        Scp939Sounds.play(World, position(), Scp939Registry.HeaveLayers, 0.8f, voicePitch());
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            double Dx = getX() - xo;
            double Dz = getZ() - zo;
            boolean Moving = Stride.tick(Math.sqrt(Dx * Dx + Dz * Dz), isAlive(), isBaby() ? YoungGait : Gait);
            if (Moving && isAlive() && !isSilent()) {
                int Steps = contactsCrossed(Stride.from(), Stride.to(), isRunning() ? SprintContacts : WalkContacts);
                for (int Index = 0; Index < Steps; Index++) {
                    footstep();
                }
            }
            float Chance = 0.03f + haze() * 0.12f;
            if (isAlive() && random.nextFloat() < Chance) {
                Vec3 Mouth = position().add(Vec3.directionFromRotation(0.0f, yBodyRot).scale(getBbWidth() * 0.95)).add(0.0, getBbHeight() * 0.62, 0.0);
                level().addParticle(BreathMote, Mouth.x, Mouth.y, Mouth.z, (random.nextDouble() - 0.5) * 0.02, 0.01, (random.nextDouble() - 0.5) * 0.02);
            }
        }
    }

    public boolean strike(LivingEntity Target) {
        if (AttackWait > 0 || !isWithinMeleeAttackRange(Target)) {
            return false;
        }
        AttackWait = AttackCooldown;
        swing(InteractionHand.MAIN_HAND);
        return doHurtTarget(Target);
    }

    public boolean lungeReady() {
        return LungeWait <= 0 && onGround() && !isBusy();
    }

    public void lunge(LivingEntity Target) {
        LungeWait = Scp939Goals.LungeCooldown;
        Vec3 Way = Target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        if (Way.lengthSqr() > 1.0e-4) {
            Way = Way.normalize();
        }
        double Power = Scp939Goals.LungePower * (isBaby() ? 0.7 : 1.0);
        setDeltaMovement(Way.x * Power, Scp939Goals.LungeLift, Way.z * Power);
        triggerAnim("action", "bite");
        if (level() instanceof ServerLevel World) {
            Scp939Sounds.play(World, position(), Scp939Registry.LungeLayers, 1.0f, voicePitch());
        }
    }

    @Override
    public boolean doHurtTarget(Entity Target) {
        boolean Hit = super.doHurtTarget(Target);
        if (level() instanceof ServerLevel World) {
            triggerAnim("action", "bite");
            Scp939Sounds.play(World, Target.position(), Scp939Registry.BiteLayers, 0.9f, voicePitch());
        }
        return Hit;
    }

    @Override
    public boolean killedEntity(ServerLevel World, LivingEntity Victim) {
        boolean Result = super.killedEntity(World, Victim);
        feed(World, Victim);
        return Result;
    }

    public void feed(ServerLevel World, LivingEntity Victim) {
        Eating = EatTicks;
        Fed++;
        Meals++;
        getNavigation().stop();
        setTarget(null);
        triggerAnim("action", "chew");
        heal(MealHeal);
        String Voice = Scp939Mimicry.voiceOf(Victim);
        if (Voice != null) {
            Voices.remove(Voice);
            Voices.add(0, Voice);
            while (Voices.size() > MaxVoices) {
                Voices.remove(Voices.size() - 1);
            }
        }
        if (isBaby()) {
            ageUp(Math.max(1, (int) (-getAge() * YoungMealGrowth / 20.0f)), true);
        }
        Scp939Sounds.play(World, position(), Scp939Registry.EatLayers, 0.9f, voicePitch());
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && Eating > 0) {
            Eating = 0;
        }
        return Hurt;
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel World, AgeableMob Partner) {
        return null;
    }

    @Override
    public boolean canBreed() {
        return false;
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
    }

    private void footstep() {
        float Young = isBaby() ? 0.5f : 1.0f;
        float Pitch = voicePitch();
        if (isRunning()) {
            stepSound(Scp939Registry.Step.get(), SprintStepVolume * Young, Pitch * 0.86f);
            stepSound(Scp939Registry.StepHeavy.get(), SprintStepVolume * 0.8f * Young, Pitch * 0.9f);
        } else if (isProwling()) {
            stepSound(Scp939Registry.StepPad.get(), ProwlStepVolume * Young, Pitch * 1.06f);
        } else {
            stepSound(Scp939Registry.Step.get(), WalkStepVolume * Young, Pitch);
            stepSound(Scp939Registry.StepPad.get(), WalkStepVolume * 0.7f * Young, Pitch);
        }
    }

    private void stepSound(SoundEvent Event, float Volume, float Pitch) {
        level().playLocalSound(getX(), getY(), getZ(), Event, getSoundSource(), Volume, Pitch, false);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return Scp939Registry.Breathe.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return AmbientInterval;
    }

    @Override
    public void playAmbientSound() {
        if (!(level() instanceof ServerLevel World) || isLuring()) {
            return;
        }
        if (!Voices.isEmpty() && random.nextFloat() < (isContained() ? MimicChance * 0.5f : MimicChance)) {
            DeferredHolder<SoundEvent, SoundEvent> Voice = Scp939Mimicry.sound(Voices.get(random.nextInt(Voices.size())));
            if (Voice != null) {
                triggerAnim("action", "call");
                Scp939Sounds.play(World, position(), Voice.get(), getSoundSource(), 1.0f, 0.86f + random.nextFloat() * 0.1f);
                return;
            }
        }
        int Roll = random.nextInt(3);
        if (Roll == 0) {
            Scp939Sounds.play(World, position(), Scp939Registry.GrowlLayers, 0.7f, voicePitch());
        } else if (Roll == 1) {
            Scp939Sounds.play(World, position(), Scp939Registry.TeethLayers, 0.5f, voicePitch());
        } else {
            Scp939Sounds.play(World, position(), Scp939Registry.BreatheLayers, 0.55f, voicePitch());
        }
    }

    @Override
    protected void playHurtSound(DamageSource Source) {
        if (level() instanceof ServerLevel World) {
            Scp939Sounds.play(World, position(), Scp939Registry.HurtLayers, 0.9f, voicePitch());
        }
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp939Registry.Hurt.get();
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return Scp939Registry.Death.get();
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        if (level() instanceof ServerLevel World) {
            Scp939Sounds.play(World, position(), Scp939Registry.DeathRattle.get(), getSoundSource(), 0.9f, voicePitch());
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("Meals", Meals);
        Tag.putInt("BroodWait", BroodWait);
        Tag.putInt("Fed", Fed);
        Tag.putFloat("Haze", haze());
        ListTag Saved = new ListTag();
        for (String Voice : Voices) {
            Saved.add(StringTag.valueOf(Voice));
        }
        Tag.put("Voices", Saved);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Meals = Math.max(0, Tag.getInt("Meals"));
        BroodWait = Math.max(0, Tag.getInt("BroodWait"));
        Fed = Math.max(0, Tag.getInt("Fed"));
        setHaze(Tag.getFloat("Haze"));
        Voices.clear();
        ListTag Saved = Tag.getList("Voices", Tag.TAG_STRING);
        for (int Index = 0; Index < Saved.size() && Voices.size() < MaxVoices; Index++) {
            if (Scp939Mimicry.sound(Saved.getString(Index)) != null) {
                Voices.add(Saved.getString(Index));
            }
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp939GaitController(this, "body", 4, Event -> {
            if (isStriding()) {
                if (isRunning()) {
                    return Event.setAndContinue(SprintLoop);
                }
                return Event.setAndContinue(isProwling() ? ProwlLoop : WalkLoop);
            }
            return Event.setAndContinue(isSniffing() ? SniffLoop : IdleLoop);
        }));
        Controllers.add(new AnimationController<>(this, "action", 2, Event -> PlayState.STOP).triggerableAnim("bite", BiteOnce).triggerableAnim("eat", EatOnce).triggerableAnim("chew", ChewOnce)
                .triggerableAnim("shake", ShakeOnce).triggerableAnim("call", CallOnce));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
