package net.rai.scp_unleashed.scp173;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.BodyRotationControl;
import net.minecraft.world.entity.ai.control.LookControl;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class Scp173Statue extends PathfinderMob implements GeoEntity {
    public static final float Width = 0.8f;
    public static final float Height = 1.95f;
    public static final float EyeHeight = 1.85f;
    public static final double VisualHeight = 2.6;
    public static final double CeilingProbe = 0.3;
    public static final int StandPoses = 10;
    public static final int ClingPoses = 3;
    public static final int PoseCount = StandPoses + ClingPoses;
    public static final int JoinGrace = 20;
    public static final int RestingPose = 0;
    public static final double Health = 100.0;
    public static final double HuntRange = 40.0;
    public static final float MineWork = 6.0f;
    public static final float BaseDigSpeed = 8.0f;
    public static final int CrackStages = 3;
    public static final double RenderRange = 128.0;
    public static final int KillPause = 30;
    public static final int RecentKill = 60;
    public static final float SoundVolume = 1.1f;
    private static final EntityDataAccessor<Byte> PoseData = SynchedEntityData.defineId(Scp173Statue.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> CloneData = SynchedEntityData.defineId(Scp173Statue.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> CrackData = SynchedEntityData.defineId(Scp173Statue.class, EntityDataSerializers.BYTE);
    private static final RawAnimation[] PoseAnimations = poseAnimations("");
    private static final RawAnimation[] LowAnimations = poseAnimations("_low");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final Scp173Hunt Hunt = new Scp173Hunt(this);
    private final Set<UUID> Trackers = new HashSet<>();
    private final Map<Integer, Scp173Witness.Memo> Glances = new HashMap<>();
    private float Work;
    private boolean Watched = true;
    private long WatchedTick = -1L;

    public Scp173Statue(EntityType<? extends Scp173Statue> Type, Level World) {
        super(Type, World);
        xpReward = 0;
        lookControl = new StillLook(this);
        setPersistenceRequired();
        setPathfindingMalus(PathType.DOOR_WOOD_CLOSED, 0.0f);
        setPathfindingMalus(PathType.WATER, 4.0f);
        setPathfindingMalus(PathType.LAVA, -1.0f);
        setPathfindingMalus(PathType.DANGER_FIRE, 0.0f);
        setPathfindingMalus(PathType.DAMAGE_FIRE, 0.0f);
    }

    private static RawAnimation[] poseAnimations(String Suffix) {
        RawAnimation[] Found = new RawAnimation[PoseCount];
        for (int Index = 0; Index < PoseCount; Index++) {
            Found[Index] = RawAnimation.begin().thenPlayAndHold("animation.scp173.pose_" + Index + Suffix);
        }
        return Found;
    }

    public boolean lowCeiling() {
        AABB Above = new AABB(getX() - CeilingProbe, getY() + Height, getZ() - CeilingProbe, getX() + CeilingProbe, getY() + VisualHeight, getZ() + CeilingProbe);
        return !level().noBlockCollision(this, Above);
    }

    public AABB sightBox() {
        AABB Box = getBoundingBox();
        return Box.setMaxY(lowCeiling() ? Box.maxY : getY() + VisualHeight);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, 0.0).add(Attributes.FOLLOW_RANGE, HuntRange)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.STEP_HEIGHT, 1.0).add(Attributes.SAFE_FALL_DISTANCE, 256.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(PoseData, (byte) RestingPose);
        Builder.define(CloneData, false);
        Builder.define(CrackData, (byte) 0);
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        Scp173Navigation Navigation = new Scp173Navigation(this, World);
        Navigation.setCanOpenDoors(true);
        Navigation.setCanPassDoors(true);
        Navigation.setCanFloat(true);
        return Navigation;
    }

    @Override
    protected BodyRotationControl createBodyControl() {
        return new StillBody(this);
    }

    public int getPoseIndex() {
        return Mth.clamp(entityData.get(PoseData), 0, PoseCount - 1);
    }

    public void setPoseIndex(int Index) {
        entityData.set(PoseData, (byte) Mth.clamp(Index, 0, PoseCount - 1));
    }

    public void pickNewPose() {
        pickNewPose(isClinging());
    }

    public void pickNewPose(boolean Cling) {
        int First = Cling ? StandPoses : 0;
        int Count = Cling ? ClingPoses : StandPoses;
        int Current = getPoseIndex() - First;
        if (Current < 0 || Current >= Count) {
            setPoseIndex(First + random.nextInt(Count));
            return;
        }
        setPoseIndex(First + (Current + 1 + random.nextInt(Count - 1)) % Count);
    }

    public static boolean isClingPose(int Index) {
        return Index >= StandPoses;
    }

    public boolean isClinging() {
        return isNoGravity();
    }

    public void setClinging(boolean Value) {
        if (isNoGravity() != Value) {
            setNoGravity(Value);
        }
        if (Value) {
            setDeltaMovement(Vec3.ZERO);
            resetFallDistance();
        }
    }

    public boolean isClone() {
        return entityData.get(CloneData);
    }

    public void setClone(boolean Value) {
        entityData.set(CloneData, Value);
    }

    public int getCrackStage() {
        return entityData.get(CrackData);
    }

    public Scp173Hunt hunt() {
        return Hunt;
    }

    public Set<UUID> trackers() {
        return Trackers;
    }

    public Map<Integer, Scp173Witness.Memo> glances() {
        return Glances;
    }

    public boolean isWatched() {
        return Watched;
    }

    public boolean watchedNow(ServerLevel World) {
        long Now = World.getServer().getTickCount();
        if (WatchedTick != Now) {
            WatchedTick = Now;
            Watched = Scp173Sight.isObserved(World, this) || tickCount < JoinGrace && Scp173Sight.anyoneCouldSee(World, getBoundingBox());
        }
        return Watched;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel World && isAlive()) {
            if (tickCount % Scp173Witness.PruneTicks == 0) {
                Scp173Witness.prune(Glances, World.getGameTime());
            }
            Hunt.tick(World, watchedNow(World));
        }
        yRotO = getYRot();
        yBodyRot = getYRot();
        yBodyRotO = getYRot();
        yHeadRot = getYRot();
        yHeadRotO = getYRot();
        setXRot(0.0f);
        xRotO = 0.0f;
    }

    public void play(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, Vec3 Where, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            level().playSound(null, Where.x, Where.y, Where.z, Layer.get(), SoundSource.HOSTILE, Volume, Pitch);
        }
    }

    public void faceTowards(Vec3 Where) {
        double Dx = Where.x - getX();
        double Dz = Where.z - getZ();
        if (Dx * Dx + Dz * Dz < 1.0e-6) {
            return;
        }
        setYRot((float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f);
    }

    @Override
    public void lerpTo(double X, double Y, double Z, float Yaw, float Pitch, int Steps) {
        super.lerpTo(X, Y, Z, Yaw, Pitch, 0);
        setPos(X, Y, Z);
        setYRot(Yaw);
        setXRot(Pitch);
        xo = X;
        yo = Y;
        zo = Z;
        xOld = X;
        yOld = Y;
        zOld = Z;
        yRotO = Yaw;
        yBodyRot = Yaw;
        yBodyRotO = Yaw;
    }

    @Override
    public void lerpHeadTo(float Yaw, int Steps) {
        yHeadRot = Yaw;
        yHeadRotO = Yaw;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(Source, Amount);
        }
        if (level().isClientSide || !isAlive()) {
            return false;
        }
        if (Source.getDirectEntity() instanceof Player User && Source.getEntity() == User && Source.is(DamageTypes.PLAYER_ATTACK) && level() instanceof ServerLevel World) {
            return struck(World, User, Amount);
        }
        return false;
    }

    public static boolean canMine(ItemStack Tool) {
        return Tool.is(ItemTags.PICKAXES) && Tool.isCorrectToolForDrops(Blocks.OBSIDIAN.defaultBlockState());
    }

    private boolean struck(ServerLevel World, Player User, float Amount) {
        ItemStack Tool = User.getMainHandItem();
        Vec3 Hit = position().add(0.0, Height * 0.6, 0.0);
        BlockParticleOption Dust = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SMOOTH_SANDSTONE.defaultBlockState());
        if (!isClone() || !canMine(Tool)) {
            play(Scp173Registry.GlanceLayers, Hit, 0.9f, 0.9f + random.nextFloat() * 0.2f);
            World.sendParticles(Dust, Hit.x, Hit.y, Hit.z, 3, 0.2, 0.3, 0.2, 0.02);
            return false;
        }
        float Strength = Mth.clamp(Amount / 5.0f, 0.25f, 1.5f);
        float Speed = Math.max(1.0f, User.getDestroySpeed(Blocks.STONE.defaultBlockState())) / BaseDigSpeed;
        Work += Strength * Speed;
        play(Scp173Registry.ChipLayers, Hit, 1.0f, 0.85f + random.nextFloat() * 0.2f);
        World.sendParticles(Dust, Hit.x, Hit.y, Hit.z, 10, 0.25, 0.4, 0.25, 0.05);
        if (Work >= MineWork) {
            crumble(World);
            return true;
        }
        int Stage = Mth.clamp((int) (Work / MineWork * (CrackStages + 1)), 0, CrackStages);
        if (Stage != getCrackStage()) {
            entityData.set(CrackData, (byte) Stage);
        }
        return true;
    }

    private void crumble(ServerLevel World) {
        BlockPos Feet = blockPosition();
        World.levelEvent(2001, Feet, Block.getId(Blocks.SMOOTH_SANDSTONE.defaultBlockState()));
        World.levelEvent(2001, Feet.above(), Block.getId(Blocks.GRAVEL.defaultBlockState()));
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SANDSTONE.defaultBlockState()), getX(), getY() + 1.0, getZ(), 40, 0.3, 0.7, 0.3, 0.1);
        play(Scp173Registry.CrumbleLayers, position().add(0.0, 1.0, 0.0), 1.2f, 0.9f + random.nextFloat() * 0.15f);
        spawnAtLocation(new ItemStack(Items.GRAVEL, 1 + random.nextInt(2)));
        spawnAtLocation(new ItemStack(Items.IRON_NUGGET, 2 + random.nextInt(3)));
        if (random.nextFloat() < 0.35f) {
            spawnAtLocation(new ItemStack(Items.RED_DYE));
        }
        Scp173Clones.get(World.getServer()).remove(getUUID());
        discard();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !Source.is(DamageTypes.PLAYER_ATTACK);
    }

    @Override
    public boolean ignoreExplosion(Explosion Blast) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity Other) {
    }

    @Override
    public void push(double X, double Y, double Z) {
    }

    @Override
    public void knockback(double Strength, double X, double Z) {
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return isAlive();
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean Passengers) {
        return false;
    }

    @Override
    public boolean isPersistenceRequired() {
        return true;
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
    public boolean shouldRenderAtSqrDistance(double Distance) {
        double Range = RenderRange * getViewScale();
        return Distance < Range * Range;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp173Registry.Crumble.get();
    }

    @Override
    public int getMaxHeadYRot() {
        return 0;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        setPersistenceRequired();
        LivingEntity Nearest = Scp173Hunt.nearestPerson(World.getLevel(), position(), 16.0);
        if (Nearest != null) {
            faceTowards(Nearest.position());
        }
        return super.finalizeSpawn(World, Difficulty, Reason, Group);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putByte("StatuePose", (byte) getPoseIndex());
        Tag.putBoolean("Clone", isClone());
        Tag.putFloat("Work", Work);
        Hunt.save(Tag);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        setPoseIndex(Tag.getByte("StatuePose"));
        setClone(Tag.getBoolean("Clone"));
        Work = Tag.getFloat("Work");
        int Stage = Mth.clamp((int) (Work / MineWork * (CrackStages + 1)), 0, CrackStages);
        entityData.set(CrackData, (byte) (isClone() ? Stage : 0));
        Hunt.load(Tag);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "pose", 0, State -> State.setAndContinue((lowCeiling() ? LowAnimations : PoseAnimations)[getPoseIndex()])));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        AABB Box = super.getBoundingBoxForCulling();
        return Box.setMaxY(Box.minY + VisualHeight + 0.3).inflate(0.6, 0.0, 0.6);
    }

    public List<ServerPlayer> watchers(ServerLevel World) {
        return Scp173Sight.playerObservers(World, this);
    }

    public static final class StillLook extends LookControl {
        public StillLook(Scp173Statue Statue) {
            super(Statue);
        }

        @Override
        public void tick() {
        }
    }

    public static final class StillBody extends BodyRotationControl {
        private final Scp173Statue Statue;

        public StillBody(Scp173Statue Statue) {
            super(Statue);
            this.Statue = Statue;
        }

        @Override
        public void clientTick() {
            Statue.yBodyRot = Statue.getYRot();
            Statue.yHeadRot = Statue.getYRot();
        }
    }
}
