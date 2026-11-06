package net.rai.scp_unleashed.scp131;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.Scp_unleashed;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.control.ControlBodies;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Scp131Pod extends PathfinderMob implements GeoEntity {
    public static final float Width = 0.5f;
    public static final float Height = 0.62f;
    public static final float EyeHeight = 0.28f;
    public static final double Health = 12.0;
    public static final double Speed = 0.3;
    public static final double FollowRange = 24.0;
    public static final double WanderModifier = 0.36;
    public static final double CruiseModifier = 0.68;
    public static final double ZoomModifier = 1.15;
    public static final double FleeModifier = 1.25;
    public static final double RaceLimit = 1.3;
    public static final double GriefPace = -0.5;
    public static final ResourceLocation GriefPaceId = ResourceLocation.fromNamespaceAndPath(Scp_unleashed.ModId, "scp131_grief");
    public static final byte MoodContent = 0;
    public static final byte MoodHappy = 1;
    public static final byte MoodCurious = 2;
    public static final byte MoodStartled = 3;
    public static final byte MoodShy = 4;
    public static final byte MoodSad = 5;
    public static final byte MoodPlayful = 6;
    public static final byte EventBump = 70;
    public static final byte EventHop = 71;
    public static final byte EventPetted = 72;
    public static final byte EventCheer = 73;
    public static final byte EventBob = 74;
    public static final byte EventStartle = 75;
    public static final int StartleTicks = 40;
    public static final int WaryTicks = 1200;
    public static final int WitnessWaryTicks = 600;
    public static final double WitnessRange = 10.0;
    public static final int FleeTicks = 70;
    public static final int HappyTicks = 60;
    public static final int PetCooldown = 14;
    public static final int FocusHold = 12;
    public static final int AmbientInterval = 150;
    public static final int SadAmbientInterval = 340;
    public static final int ChirpSpacing = 24;
    public static final double ShyRange = 8.0;
    public static final double Hearing = 20.0;
    public static final float ChirpVolume = 0.55f;
    public static final float SadPitch = 0.82f;
    public static final float SadVolume = 0.42f;
    private static final EntityDataAccessor<Integer> ColourData = SynchedEntityData.defineId(Scp131Pod.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> VoiceData = SynchedEntityData.defineId(Scp131Pod.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> MoodData = SynchedEntityData.defineId(Scp131Pod.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> FocusData = SynchedEntityData.defineId(Scp131Pod.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Byte> PlayData = SynchedEntityData.defineId(Scp131Pod.class, EntityDataSerializers.BYTE);
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final Map<UUID, Long> Wary = new HashMap<>();
    private final Set<Integer> Mourned = new HashSet<>();
    @Nullable
    private Scp131Play Session;
    @Nullable
    private UUID FleeFrom;
    private int FleeLeft;
    private long StartledUntil;
    private long TimedUntil;
    private byte TimedMood = MoodContent;
    private int FocusId = -1;
    private int FocusLeft;
    private boolean Curious;
    private int PlayReady = 200;
    private int PersonReady = 60;
    private int ZoomReady = 100;
    private int PetReady;
    private int ChirpReady;
    @Nullable
    private UUID Beckon;
    private int BeckonLeft;
    private boolean WasSad;
    private boolean Zooming;
    @Nullable
    private Object Face;

    public Scp131Pod(EntityType<? extends Scp131Pod> Type, Level World) {
        super(Type, World);
        moveControl = new Scp131MoveControl(this);
        xpReward = 0;
        setPersistenceRequired();
        setPathfindingMalus(PathType.WATER, -1.0f);
        setPathfindingMalus(PathType.WATER_BORDER, 8.0f);
        setPathfindingMalus(PathType.LAVA, -1.0f);
        setPathfindingMalus(PathType.DANGER_FIRE, 16.0f);
        setPathfindingMalus(PathType.DAMAGE_FIRE, -1.0f);
        setPathfindingMalus(PathType.POWDER_SNOW, -1.0f);
        setPathfindingMalus(PathType.DANGER_POWDER_SNOW, 8.0f);
        setPathfindingMalus(PathType.DANGER_OTHER, 8.0f);
        setPathfindingMalus(PathType.DAMAGE_OTHER, -1.0f);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.FOLLOW_RANGE, FollowRange)
                .add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.SAFE_FALL_DISTANCE, 2.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ColourData, 0);
        Builder.define(VoiceData, 0);
        Builder.define(MoodData, MoodContent);
        Builder.define(FocusData, -1);
        Builder.define(PlayData, Scp131Play.None);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp131Goals.Flee(this));
        goalSelector.addGoal(2, new Scp131Goals.KeepAway(this));
        goalSelector.addGoal(3, new Scp131Goals.Mourn(this));
        goalSelector.addGoal(4, new Scp131Goals.Play(this));
        goalSelector.addGoal(5, new Scp131Goals.Person(this));
        goalSelector.addGoal(6, new Scp131Goals.Zoom(this));
        goalSelector.addGoal(7, new Scp131Goals.Wander(this));
        goalSelector.addGoal(8, new Scp131Goals.Glance(this));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new Scp131Goals.Turning(this);
    }

    public Scp131MoveControl motion() {
        return (Scp131MoveControl) moveControl;
    }

    public Scp131Colour getColour() {
        return Scp131Colour.byIndex(entityData.get(ColourData));
    }

    public void setColour(Scp131Colour Colour) {
        entityData.set(ColourData, Colour.ordinal());
    }

    public int getVoice() {
        return entityData.get(VoiceData);
    }

    public void setVoice(int Value) {
        entityData.set(VoiceData, Value);
    }

    public byte getMood() {
        return entityData.get(MoodData);
    }

    public byte getPlay() {
        return entityData.get(PlayData);
    }

    public void setPlay(byte Kind) {
        entityData.set(PlayData, Kind);
    }

    @Nullable
    public Entity getFocus() {
        int Id = entityData.get(FocusData);
        return Id < 0 ? null : level().getEntity(Id);
    }

    public void setFocus(@Nullable Entity Target) {
        if (Target == null || Target == this) {
            return;
        }
        FocusId = Target.getId();
        FocusLeft = FocusHold;
    }

    public void setCurious(boolean Value) {
        Curious = Value;
    }

    public void setZooming(boolean Value) {
        Zooming = Value;
    }

    public boolean isZooming() {
        return Zooming;
    }

    public boolean isSad() {
        return Scp131Grief.isGrieving(level());
    }

    public boolean isStartled() {
        return StartledUntil > level().getGameTime();
    }

    public void alarm(int Ticks) {
        StartledUntil = Math.max(StartledUntil, level().getGameTime() + Ticks);
    }

    public static float voiceFor(Scp131Colour Colour, int Seed) {
        float Personal = ((Seed >>> 8) % 1000) / 1000.0f * 0.16f - 0.08f;
        return 1.0f + Colour.getVoice() + Personal;
    }

    public float voicePitch() {
        return voiceFor(getColour(), getVoice());
    }

    @Nullable
    public Scp131Play session() {
        return Session;
    }

    public void setSession(@Nullable Scp131Play Value) {
        Session = Value;
        setPlay(Value == null ? Scp131Play.None : Value.kindFor(this));
    }

    public boolean canPlay() {
        return isAlive() && Session == null && PlayReady <= 0 && !isSad() && !isStartled() && FleeLeft <= 0 && onGround() && !isInWater() && !isPassenger();
    }

    public boolean isFree() {
        return isAlive() && Session == null && !isSad() && FleeLeft <= 0;
    }

    public void restAfterPlay(RandomSource Random) {
        PlayReady = Scp131Play.CooldownMin + Random.nextInt(Scp131Play.CooldownSpread);
    }

    public boolean playReady() {
        return PlayReady <= 0;
    }

    public void delayPlay(int Ticks) {
        PlayReady = Math.max(PlayReady, Ticks);
    }

    public boolean personReady() {
        return PersonReady <= 0;
    }

    public void restAfterPerson(RandomSource Random) {
        PersonReady = Scp131Goals.PersonRestMin + Random.nextInt(Scp131Goals.PersonRestSpread);
    }

    public boolean zoomReady() {
        return ZoomReady <= 0;
    }

    public void restAfterZoom(RandomSource Random) {
        ZoomReady = Scp131Goals.ZoomRestMin + Random.nextInt(Scp131Goals.ZoomRestSpread);
    }

    @Nullable
    public UUID fleeingFrom() {
        return FleeLeft > 0 ? FleeFrom : null;
    }

    public int fleeLeft() {
        return FleeLeft;
    }

    public void endFlee() {
        FleeLeft = 0;
        FleeFrom = null;
    }

    public void beckon(UUID Person, int Ticks) {
        Beckon = Person;
        BeckonLeft = Ticks;
        PersonReady = 0;
    }

    @Nullable
    public UUID beckoned() {
        return BeckonLeft > 0 ? Beckon : null;
    }

    public void clearBeckon() {
        Beckon = null;
        BeckonLeft = 0;
    }

    public boolean hasMourned(int Spot) {
        return Mourned.contains(Spot);
    }

    public void markMourned(int Spot) {
        Mourned.add(Spot);
    }

    public void cheer(int Ticks) {
        TimedMood = MoodHappy;
        TimedUntil = level().getGameTime() + Ticks;
    }

    public boolean isWaryOf(Entity Target) {
        if (Target == null || Target == this || !Target.isAlive()) {
            return false;
        }
        long Now = level().getGameTime();
        Long Until = Wary.get(Target.getUUID());
        if (Until != null && Until > Now) {
            return true;
        }
        return level() instanceof ServerLevel World && isSad() && Scp131Grief.get(World.getServer()).isKiller(Target.getUUID(), Now);
    }

    public void beWaryOf(Entity Target, int Ticks) {
        long Until = level().getGameTime() + Ticks;
        Wary.merge(Target.getUUID(), Until, Math::max);
    }

    public static boolean isPerson(Entity Target) {
        if (!(Target instanceof LivingEntity Living) || !Living.isAlive() || Living.isSpectator() || Living.isInvisible() || !Targeting.isTargetable(Living)) {
            return false;
        }
        return Target instanceof Player || ControlBodies.isPersonLike(Target);
    }

    public static boolean isPod(Entity Target) {
        return Target instanceof Scp131Pod;
    }

    public boolean chirpReady() {
        return ChirpReady <= 0;
    }

    public void voice(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        if (isSilent()) {
            return;
        }
        ChirpReady = ChirpSpacing;
        boolean Sad = isSad() && Layers != Scp131Registry.DeathLayers;
        float Shift = Sad ? SadPitch : 1.0f;
        float Loud = Sad ? Volume * SadVolume / ChirpVolume : Volume;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            level().playSound(null, getX(), getY() + 0.3, getZ(), Layer.get(), SoundSource.NEUTRAL, Loud, Pitch * Shift * voicePitch());
        }
    }

    public void chirp(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume) {
        voice(Layers, Volume, 0.95f + random.nextFloat() * 0.1f);
    }

    public void bumped() {
        if (level() instanceof ServerLevel World) {
            World.broadcastEntityEvent(this, EventBump);
        }
    }

    public void event(byte Kind) {
        if (level() instanceof ServerLevel World) {
            World.broadcastEntityEvent(this, Kind);
        }
    }

    @Override
    public void handleEntityEvent(byte Kind) {
        if (Kind >= EventBump && Kind <= EventStartle) {
            if (Kind == EventPetted) {
                for (int Index = 0; Index < 2; Index++) {
                    level().addParticle(ParticleTypes.HEART, getX() + (random.nextDouble() - 0.5) * 0.3, getY() + Height + 0.15, getZ() + (random.nextDouble() - 0.5) * 0.3, 0.0, 0.04, 0.0);
                }
            }
            Scp131Cues.Handler.accept(this, Kind);
            return;
        }
        super.handleEntityEvent(Kind);
    }

    @Nullable
    public Object face() {
        return Face;
    }

    public void setFace(Object Value) {
        Face = Value;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide || !isAlive()) {
            return;
        }
        long Now = level().getGameTime();
        PlayReady = Math.max(0, PlayReady - 1);
        PersonReady = Math.max(0, PersonReady - 1);
        ZoomReady = Math.max(0, ZoomReady - 1);
        PetReady = Math.max(0, PetReady - 1);
        ChirpReady = Math.max(0, ChirpReady - 1);
        if (BeckonLeft > 0 && --BeckonLeft == 0) {
            Beckon = null;
        }
        if (FleeLeft > 0 && --FleeLeft == 0) {
            FleeFrom = null;
        }
        if (tickCount % 40 == 0) {
            Iterator<Map.Entry<UUID, Long>> Entries = Wary.entrySet().iterator();
            while (Entries.hasNext()) {
                if (Entries.next().getValue() <= Now) {
                    Entries.remove();
                }
            }
        }
        boolean Sad = isSad();
        if (Sad != WasSad) {
            WasSad = Sad;
            griefChanged(Sad);
        }
        if (tickCount % 10 == 0) {
            refreshPace(Sad);
        }
        if (Session != null && (Session.isOver() || Sad)) {
            Session.end(!Sad);
        }
        updateMood(Now, Sad);
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (FocusLeft > 0) {
            FocusLeft--;
        } else {
            FocusId = -1;
        }
        if (entityData.get(FocusData) != FocusId) {
            entityData.set(FocusData, FocusId);
        }
    }

    private void griefChanged(boolean Sad) {
        if (Sad) {
            Zooming = false;
            Curious = false;
            motion().stop();
            getNavigation().stop();
            voice(Scp131Registry.SadLayers, ChirpVolume, 0.9f + random.nextFloat() * 0.08f);
        } else {
            cheer(HappyTicks * 2);
            voice(Scp131Registry.GreetLayers, ChirpVolume, 1.1f + random.nextFloat() * 0.1f);
            event(EventCheer);
        }
    }

    private void refreshPace(boolean Sad) {
        AttributeInstance Pace = getAttribute(Attributes.MOVEMENT_SPEED);
        if (Pace == null) {
            return;
        }
        if (Sad) {
            Pace.addOrUpdateTransientModifier(new AttributeModifier(GriefPaceId, GriefPace, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            Pace.removeModifier(GriefPaceId);
        }
    }

    private void updateMood(long Now, boolean Sad) {
        byte Mood = MoodContent;
        if (StartledUntil > Now) {
            Mood = MoodStartled;
        } else if (Sad) {
            Mood = MoodSad;
        } else if (TimedUntil > Now) {
            Mood = TimedMood;
        } else if (Session != null || Zooming) {
            Mood = MoodPlayful;
        } else if (wary()) {
            Mood = MoodShy;
        } else if (Curious) {
            Mood = MoodCurious;
        }
        if (entityData.get(MoodData) != Mood) {
            entityData.set(MoodData, Mood);
        }
    }

    private boolean wary() {
        if (Wary.isEmpty() && !isSad()) {
            return false;
        }
        AABB Around = getBoundingBox().inflate(ShyRange);
        return !level().getEntitiesOfClass(LivingEntity.class, Around, this::isWaryOf).isEmpty();
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (!Hurt || level().isClientSide || !isAlive()) {
            return Hurt;
        }
        StartledUntil = level().getGameTime() + StartleTicks;
        Entity Attacker = Source.getEntity();
        if (Session != null) {
            Session.end(false);
        }
        Zooming = false;
        Curious = false;
        clearBeckon();
        if (Attacker != null && Attacker != this) {
            beWaryOf(Attacker, WaryTicks);
            FleeFrom = Attacker.getUUID();
            FleeLeft = FleeTicks;
            setFocus(Attacker);
            for (Scp131Pod Witness : level().getEntitiesOfClass(Scp131Pod.class, getBoundingBox().inflate(WitnessRange), Other -> Other != this && Other.isAlive())) {
                Witness.beWaryOf(Attacker, WitnessWaryTicks);
                Witness.setFocus(Attacker);
            }
        }
        event(EventStartle);
        return true;
    }

    @Override
    protected void playHurtSound(DamageSource Source) {
        voice(Scp131Registry.HurtLayers, 0.8f, 1.0f + random.nextFloat() * 0.08f);
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        if (level() instanceof ServerLevel World) {
            World.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 0.3, getZ(), 6, 0.15, 0.1, 0.15, 0.01);
        }
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp131Registry.Death.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp131Registry.Hurt.get();
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp131Registry.Chirp.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return isSad() ? SadAmbientInterval : AmbientInterval;
    }

    @Override
    public void playAmbientSound() {
        if (!chirpReady() || isStartled()) {
            return;
        }
        if (isSad()) {
            voice(Scp131Registry.SadLayers, ChirpVolume, 0.88f + random.nextFloat() * 0.1f);
        } else if (getMood() == MoodCurious) {
            chirp(Scp131Registry.TrillLayers, ChirpVolume);
        } else {
            chirp(Scp131Registry.ChirpLayers, ChirpVolume);
        }
    }

    @Override
    public float getVoicePitch() {
        return voicePitch() * (isSad() ? SadPitch : 1.0f) * (0.95f + random.nextFloat() * 0.1f);
    }

    @Override
    protected float getSoundVolume() {
        return 0.7f;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    protected InteractionResult mobInteract(Player User, InteractionHand Hand) {
        if (Hand != InteractionHand.MAIN_HAND || !User.getMainHandItem().isEmpty() || User.isSpectator() || !isAlive()) {
            return super.mobInteract(User, Hand);
        }
        if (User.isSecondaryUseActive()) {
            if (!level().isClientSide && PetReady <= 0) {
                pet(User);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        if (!level().isClientSide) {
            pickUp(User);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private void pet(Player User) {
        PetReady = PetCooldown;
        cheer(HappyTicks);
        setFocus(User);
        getNavigation().stop();
        voice(Scp131Registry.PurrLayers, 0.7f, 1.0f + random.nextFloat() * 0.06f);
        event(EventPetted);
    }

    private void pickUp(Player User) {
        if (Session != null) {
            Session.end(false);
        }
        User.setItemInHand(InteractionHand.MAIN_HAND, Scp131PodItem.capture(this));
        voice(Scp131Registry.PickupLayers, 0.7f, 1.0f + random.nextFloat() * 0.08f);
        discard();
    }

    public void released(Player User) {
        cheer(HappyTicks);
        setFocus(User);
        PersonReady = 100;
        event(EventHop);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        RandomSource Random = World.getRandom();
        if (Reason != MobSpawnType.BUCKET) {
            setColour(Scp131Colour.random(Random));
            setVoice(Random.nextInt());
        }
        setPersistenceRequired();
        PlayReady = 160 + Random.nextInt(200);
        return super.finalizeSpawn(World, Difficulty, Reason, Group);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public int getMaxFallDistance() {
        return Scp131MoveControl.SafeDrop;
    }

    @Override
    public int getMaxHeadYRot() {
        return 1;
    }

    @Override
    public int getHeadRotSpeed() {
        return 14;
    }

    @Override
    public boolean isPushable() {
        return super.isPushable() && Session == null;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(0.3, 0.2, 0.3);
    }

    @Override
    public void remove(RemovalReason Reason) {
        if (Session != null && !level().isClientSide) {
            Session.end(false);
        }
        super.remove(Reason);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putString("Colour", getColour().getSerializedName());
        Tag.putInt("Voice", getVoice());
        Tag.putInt("PlayReady", PlayReady);
        Tag.putInt("PersonReady", PersonReady);
        long Now = level().getGameTime();
        ListTag Remembered = new ListTag();
        for (Map.Entry<UUID, Long> Entry : Wary.entrySet()) {
            if (Entry.getValue() > Now) {
                CompoundTag One = new CompoundTag();
                One.putUUID("Id", Entry.getKey());
                One.putLong("Until", Entry.getValue());
                Remembered.add(One);
            }
        }
        Tag.put("Wary", Remembered);
        Tag.put("Mourned", new IntArrayTag(Mourned.stream().mapToInt(Integer::intValue).toArray()));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        setColour(Scp131Colour.byName(Tag.getString("Colour")));
        setVoice(Tag.getInt("Voice"));
        PlayReady = Tag.getInt("PlayReady");
        PersonReady = Tag.getInt("PersonReady");
        Wary.clear();
        ListTag Remembered = Tag.getList("Wary", net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Remembered.size(); Index++) {
            CompoundTag One = Remembered.getCompound(Index);
            if (One.hasUUID("Id")) {
                Wary.put(One.getUUID("Id"), One.getLong("Until"));
            }
        }
        Mourned.clear();
        for (int Spot : Tag.getIntArray("Mourned")) {
            Mourned.add(Spot);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    public Vec3 lookFrom() {
        return position().add(0.0, EyeHeight, 0.0);
    }

    public float yawTo(double X, double Z) {
        return (float) (Mth.atan2(Z - getZ(), X - getX()) * Mth.RAD_TO_DEG) - 90.0f;
    }
}
