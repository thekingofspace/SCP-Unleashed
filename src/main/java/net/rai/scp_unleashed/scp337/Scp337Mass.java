package net.rai.scp_unleashed.scp337;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class Scp337Mass extends Monster implements GeoEntity, IEntityWithComplexSpawn {
    public static final float Width = 2.1f;
    public static final float Height = 1.7f;
    public static final float EyeHeight = 1.05f;
    public static final double Health = 80.0;
    public static final double Armour = 4.0;
    public static final double KnockbackResistance = 0.85;
    public static final double Speed = 0.1;
    public static final double FollowRange = 16.0;
    public static final double LashRange = 7.0;
    public static final double LashSlack = 1.0;
    public static final float LashDamage = 4.0f;
    public static final int LashCooldown = 36;
    public static final int AgitatedCooldown = 22;
    public static final int CooldownSpread = 8;
    public static final int MaxLashes = 4;
    public static final float FeintChance = 0.45f;
    public static final double PullStrength = 0.42;
    public static final double PullLift = 0.12;
    public static final double PullMin = 2.6;
    public static final int RetargetInterval = 10;
    public static final double HoldRange = 4.0;
    public static final int AgitationOnHurt = 100;
    public static final int AgitationOnLash = 40;
    public static final int SatedTicks = 100;
    public static final int DeathTicks = 70;
    public static final int FadeStart = 42;
    public static final int RustleInterval = 50;
    public static final int DragInterval = 16;
    public static final float FireShare = 1.5f;
    public static final float ProjectileShare = 0.6f;
    public static final double MoveThreshold = 0.004;
    public static final double DragReference = 0.02;
    public static final byte StateIdle = 0;
    public static final byte StateReaching = 1;
    public static final byte StateLashing = 2;
    public static final byte StateSettling = 3;
    private static final EntityDataAccessor<CompoundTag> LashesData = SynchedEntityData.defineId(Scp337Mass.class, EntityDataSerializers.COMPOUND_TAG);
    private static final EntityDataAccessor<Boolean> WrithingData = SynchedEntityData.defineId(Scp337Mass.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> StateData = SynchedEntityData.defineId(Scp337Mass.class, EntityDataSerializers.BYTE);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation DragLoop = RawAnimation.begin().thenLoop("drag");
    private static final RawAnimation WritheLoop = RawAnimation.begin().thenLoop("writhe");
    private static final RawAnimation CollapsePlay = RawAnimation.begin().thenPlayAndHold("collapse");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final List<Scp337Lash> Lashes = new ArrayList<>();
    private final List<Feint> Feints = new ArrayList<>();
    private final Map<UUID, Integer> Ready = new HashMap<>();
    private int Agitation;
    private int Sated;
    private int Taken;
    private int RustleReady = RustleInterval;
    private int DragReady;
    private int TickOrigin;
    private float RenderPartial;
    private double ClientSpeed;
    private List<Scp337Lash> ClientLashes = List.of();
    private final Scp337Strands Strands = new Scp337Strands();

    public Scp337Mass(EntityType<? extends Scp337Mass> Type, Level World) {
        super(Type, World);
        xpReward = 15;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, LashDamage)
                .add(Attributes.ARMOR, Armour).add(Attributes.KNOCKBACK_RESISTANCE, KnockbackResistance).add(Attributes.FOLLOW_RANGE, FollowRange)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(LashesData, new CompoundTag());
        Builder.define(WrithingData, false);
        Builder.define(StateData, StateIdle);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(1, new Scp337Goals.Creep(this));
        goalSelector.addGoal(5, new Scp337Goals.Wander(this));
    }

    @Override
    public void setTarget(@Nullable LivingEntity Target) {
        if (Target != null && !Scp337Quarry.isQuarry(this, Target)) {
            return;
        }
        super.setTarget(Target);
    }

    public boolean isWrithing() {
        return entityData.get(WrithingData);
    }

    public byte state() {
        return entityData.get(StateData);
    }

    public int taken() {
        return Taken;
    }

    public int agitation() {
        return Agitation;
    }

    public int sated() {
        return Sated;
    }

    public List<Scp337Lash> lashes() {
        return Lashes;
    }

    public double clientSpeed() {
        return ClientSpeed;
    }

    public float renderPartial() {
        return RenderPartial;
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    public double serverTime(float Partial) {
        return tickCount + TickOrigin + Partial;
    }

    public Scp337Strands strands() {
        return Strands;
    }

    public List<Scp337Lash> clientLashes() {
        return ClientLashes;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (LashesData.equals(Key) && level().isClientSide) {
            ClientLashes = syncedLashes();
        }
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf Buffer) {
        Buffer.writeVarInt(tickCount);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf Buffer) {
        TickOrigin = Buffer.readVarInt() - tickCount;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            double Dx = getX() - xo;
            double Dz = getZ() - zo;
            ClientSpeed += (Math.sqrt(Dx * Dx + Dz * Dz) - ClientSpeed) * 0.35;
            Strands.tick(this);
            return;
        }
        if (isAlive() && level() instanceof ServerLevel World) {
            serverTick(World);
        }
    }

    private void serverTick(ServerLevel World) {
        Agitation = Math.max(0, Agitation - 1);
        Sated = Math.max(0, Sated - 1);
        if (tickCount % RetargetInterval == 0) {
            retarget(World);
        }
        advanceLashes(World);
        if (Sated <= 0) {
            volley(World);
        }
        launchFeints(World);
        murmur();
        updateState();
    }

    private void retarget(ServerLevel World) {
        LivingEntity Nearest = Sated > 0 ? null : Scp337Quarry.nearest(World, this, FollowRange);
        if (Nearest != getTarget()) {
            setTarget(Nearest);
        }
        if (Ready.size() > 16) {
            Ready.values().removeIf(Until -> Until <= tickCount);
        }
    }

    private void volley(ServerLevel World) {
        if (Lashes.size() >= MaxLashes) {
            return;
        }
        for (LivingEntity Candidate : Scp337Quarry.around(World, this, LashRange)) {
            if (Lashes.size() >= MaxLashes) {
                return;
            }
            if (Ready.getOrDefault(Candidate.getUUID(), 0) > tickCount) {
                continue;
            }
            long Seed = random.nextLong();
            Vec3 Aim = Scp337Quarry.openPoint(this, Candidate, Seed);
            if (Aim == null) {
                continue;
            }
            launch(new Scp337Lash(Candidate.getId(), Aim, tickCount, Seed, true));
            boolean Stirred = Agitation > AgitationOnLash;
            Ready.put(Candidate.getUUID(), tickCount + (Stirred ? AgitatedCooldown : LashCooldown) + random.nextInt(CooldownSpread + 1));
            Agitation = Math.max(Agitation, AgitationOnLash);
            if (Stirred || random.nextFloat() < FeintChance) {
                int Count = Stirred ? 1 + random.nextInt(2) : 1;
                for (int Index = 0; Index < Count; Index++) {
                    Feints.add(new Feint(Candidate.getId(), tickCount + 2 + Index * 2 + random.nextInt(2)));
                }
            }
        }
    }

    private void launchFeints(ServerLevel World) {
        Iterator<Feint> Each = Feints.iterator();
        while (Each.hasNext()) {
            Feint Next = Each.next();
            if (Next.When() > tickCount) {
                continue;
            }
            Each.remove();
            if (Lashes.size() >= MaxLashes || !(World.getEntity(Next.Target()) instanceof LivingEntity Target) || !Target.isAlive()) {
                continue;
            }
            Vec3 Body = Scp337Quarry.bodyPoint(Target);
            Vec3 Aim = Body.add((random.nextDouble() - 0.5) * 2.4, (random.nextDouble() - 0.6) * 1.4, (random.nextDouble() - 0.5) * 2.4);
            long Seed = random.nextLong();
            Vec3 From = Scp337Lash.origin(position(), Aim, Seed);
            if (From.distanceTo(Aim) > LashRange + LashSlack || !Scp337Quarry.clear(World, From, Aim, this)) {
                continue;
            }
            launch(new Scp337Lash(Target.getId(), Aim, tickCount, Seed, false));
        }
    }

    private void launch(Scp337Lash Lash) {
        Lashes.add(Lash);
        syncLashes();
        Vec3 From = Scp337Lash.origin(position(), Lash.Aim(), Lash.Seed());
        playLayersAt(From, Scp337Registry.SwishLayers, Lash.Real() ? 0.8f : 0.5f, 0.95f + random.nextFloat() * 0.15f);
    }

    private void advanceLashes(ServerLevel World) {
        boolean Changed = false;
        Iterator<Scp337Lash> Each = Lashes.iterator();
        while (Each.hasNext()) {
            Scp337Lash Lash = Each.next();
            int Elapsed = tickCount - Lash.Start();
            if (Elapsed == Scp337Lash.HitTick) {
                playLayersAt(Lash.Aim(), Scp337Registry.CrackLayers, Lash.Real() ? 1.0f : 0.6f, 0.9f + random.nextFloat() * 0.2f);
                if (Lash.Real()) {
                    strike(World, Lash);
                }
            } else if (Elapsed == Scp337Lash.HitTick + Scp337Lash.HoldTicks + 1) {
                playSound(Scp337Registry.Retract.get(), 0.35f, 0.9f + random.nextFloat() * 0.2f);
            }
            if (Elapsed >= Scp337Lash.Ticks || Elapsed < 0) {
                Each.remove();
                Changed = true;
            }
        }
        if (Changed) {
            syncLashes();
        }
    }

    private void strike(ServerLevel World, Scp337Lash Lash) {
        if (!(World.getEntity(Lash.Target()) instanceof LivingEntity Target) || !Scp337Quarry.isQuarry(this, Target)) {
            return;
        }
        Vec3 Aim = Scp337Quarry.openPoint(this, Target, Lash.Seed());
        if (Aim == null) {
            return;
        }
        DamageSource Source = new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Scp337Registry.LashDamage), this);
        if (!Target.hurt(Source, LashDamage)) {
            return;
        }
        pull(Target);
        playLayersAt(Aim, Scp337Registry.HitLayers, 0.9f, 0.9f + random.nextFloat() * 0.2f);
    }

    public void pull(LivingEntity Target) {
        double Dx = getX() - Target.getX();
        double Dz = getZ() - Target.getZ();
        double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Distance <= PullMin) {
            return;
        }
        double Strength = PullStrength * (1.0 - Target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) * 0.5);
        Target.push(Dx / Distance * Strength, PullLift, Dz / Distance * Strength);
        Target.hurtMarked = true;
    }

    private void syncLashes() {
        ListTag Items = new ListTag();
        for (Scp337Lash Lash : Lashes) {
            Items.add(Lash.save());
        }
        CompoundTag Packed = new CompoundTag();
        Packed.put("L", Items);
        Packed.putInt("N", tickCount);
        entityData.set(LashesData, Packed);
    }

    public List<Scp337Lash> syncedLashes() {
        CompoundTag Packed = entityData.get(LashesData);
        List<Scp337Lash> Found = new ArrayList<>();
        ListTag Items = Packed.getList("L", Tag.TAG_COMPOUND);
        for (int Index = 0; Index < Items.size(); Index++) {
            Found.add(Scp337Lash.load(Items.getCompound(Index)));
        }
        return Found;
    }

    private void murmur() {
        boolean Writhing = isWrithing();
        if (--RustleReady <= 0) {
            playLayers(Scp337Registry.RustleLayers, Writhing ? 0.55f : 0.32f, (Writhing ? 1.05f : 0.9f) + random.nextFloat() * 0.15f);
            RustleReady = (Writhing ? RustleInterval / 2 : RustleInterval) + random.nextInt(30);
        }
        double Dx = getX() - xo;
        double Dz = getZ() - zo;
        if (Dx * Dx + Dz * Dz > MoveThreshold * MoveThreshold && onGround()) {
            if (--DragReady <= 0) {
                playLayers(Scp337Registry.DragLayers, 0.45f, 0.85f + random.nextFloat() * 0.15f);
                DragReady = DragInterval + random.nextInt(6);
            }
        } else {
            DragReady = Math.min(DragReady, 4);
        }
    }

    private void updateState() {
        boolean Writhing = Agitation > 0 || !Lashes.isEmpty();
        if (entityData.get(WrithingData) != Writhing) {
            entityData.set(WrithingData, Writhing);
        }
        byte Next;
        LivingEntity Target = getTarget();
        if (Sated > 0) {
            Next = StateSettling;
        } else if (Target != null && (!Lashes.isEmpty() || Scp337Quarry.reach(this, Target) <= LashRange)) {
            Next = StateLashing;
        } else if (Target != null) {
            Next = StateReaching;
        } else {
            Next = StateIdle;
        }
        if (entityData.get(StateData) != Next) {
            entityData.set(StateData, Next);
        }
    }

    public void recordTaken(LivingEntity Victim) {
        Taken++;
        Sated = SatedTicks;
        Agitation = 0;
        Feints.clear();
        setTarget(null);
        getNavigation().stop();
        playLayers(List.of(Scp337Registry.Settle), 0.7f, 0.8f + random.nextFloat() * 0.1f);
    }

    public void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            playSound(Layer.get(), Volume, Pitch);
        }
    }

    public void playLayersAt(Vec3 Where, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        if (isSilent()) {
            return;
        }
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            level().playSound(null, Where.x, Where.y, Where.z, Layer.get(), getSoundSource(), Volume, Pitch);
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        float Scaled = Amount;
        if (Source.is(DamageTypeTags.IS_FIRE)) {
            Scaled *= FireShare;
        } else if (Source.is(DamageTypeTags.IS_PROJECTILE)) {
            Scaled *= ProjectileShare;
        }
        boolean Hurt = super.hurt(Source, Scaled);
        if (Hurt && !level().isClientSide) {
            Agitation = AgitationOnHurt;
            Sated = 0;
            if (isAlive()) {
                playLayers(Scp337Registry.HurtLayers, 0.8f, 0.85f + random.nextFloat() * 0.2f);
            }
        }
        return Hurt;
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        if (!level().isClientSide) {
            Lashes.clear();
            Feints.clear();
            syncLashes();
            entityData.set(WrithingData, false);
            playLayers(Scp337Registry.DeathLayers, 1.0f, 0.8f);
        }
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= DeathTicks && !level().isClientSide() && !isRemoved()) {
            if (level() instanceof ServerLevel World) {
                World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.BLACK_WOOL.defaultBlockState()), getX(), getY() + 0.25, getZ(), 28, 0.7, 0.12, 0.7, 0.02);
            }
            remove(RemovalReason.KILLED);
        }
    }

    public float fade(float Partial) {
        if (deathTime <= 0) {
            return 1.0f;
        }
        float Time = deathTime + Partial;
        return 1.0f - Mth.clamp((Time - FadeStart) / (DeathTicks - FadeStart), 0.0f, 1.0f);
    }

    @Override
    public void jumpFromGround() {
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        setPersistenceRequired();
        return super.finalizeSpawn(World, Difficulty, Reason, Group);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp337Registry.Hurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp337Registry.Death.get();
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        if (level().isClientSide && !ClientLashes.isEmpty()) {
            return super.getBoundingBoxForCulling().inflate(LashRange + LashSlack);
        }
        return super.getBoundingBoxForCulling().inflate(0.4);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        Saved.putInt("Agitation", Agitation);
        Saved.putInt("Sated", Sated);
        Saved.putInt("Taken", Taken);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        Agitation = Saved.getInt("Agitation");
        Sated = Saved.getInt("Sated");
        Taken = Saved.getInt("Taken");
        Lashes.clear();
        Feints.clear();
        Ready.clear();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "body", 6, Event -> {
            if (isDeadOrDying()) {
                Event.getController().setAnimationSpeed(1.0);
                return Event.setAndContinue(CollapsePlay);
            }
            if (isWrithing()) {
                Event.getController().setAnimationSpeed(1.0);
                return Event.setAndContinue(WritheLoop);
            }
            if (ClientSpeed > MoveThreshold) {
                Event.getController().setAnimationSpeed(Mth.clamp(ClientSpeed / DragReference, 0.5, 1.6));
                return Event.setAndContinue(DragLoop);
            }
            Event.getController().setAnimationSpeed(1.0);
            return Event.setAndContinue(IdleLoop);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }

    private record Feint(int Target, int When) {
    }
}
