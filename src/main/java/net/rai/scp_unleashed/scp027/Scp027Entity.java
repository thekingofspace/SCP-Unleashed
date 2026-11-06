package net.rai.scp_unleashed.scp027;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.AnomalyRealm;
import net.rai.scp_unleashed.control.ControlRefusing;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class Scp027Entity extends DClassLike implements ControlRefusing {
    public static final float Width = 0.6f;
    public static final float Height = 1.8f;
    public static final float EyeHeight = 1.5f;
    public static final double MaxHealth = 100.0;
    public static final double Armor = 2.0;
    public static final double MoveSpeed = 0.2;
    public static final double Sturdiness = 0.4;
    public static final double DriftSpeed = 0.75;
    public static final double WanderSpeed = 0.6;
    public static final int WanderInterval = 100;
    public static final float LookRange = 10.0f;
    public static final float Glance = 0.35f;
    public static final float WaterCost = 8.0f;
    public static final int RegenDelay = 200;
    public static final int RegenEvery = 40;
    public static final float RegenAmount = 1.0f;
    public static final int Lines = 8;
    public static final int LineCooldown = 60;
    public static final int AmbientInterval = 240;
    public static final int ShuffleEvery = 9;
    public static final double ShuffleMotion = 0.015;
    public static final int FlyShortest = 70;
    public static final int FlyLongest = 110;
    public static final int SkitterShortest = 60;
    public static final int SkitterLongest = 100;
    public static final double HearRange = 16.0;
    public static final int SkitterSwarm = 3;
    public static final String Prefix = "message.scp_unleashed.scp027.";
    protected static final EntityDataAccessor<Integer> SwarmData = SynchedEntityData.defineId(Scp027Entity.class, EntityDataSerializers.INT);

    private int NextPulse = 20;
    private int NextWilt = Scp027Blight.WiltInterval;
    private int NextSpoil = Scp027Blight.SpoilInterval;
    private int NextFlies = FlyLongest;
    private int NextSkitter = SkitterLongest;
    private int LastHurt = -RegenDelay;
    private int LineReady;
    private int LastLine = -1;
    private int Steps;

    public Scp027Entity(EntityType<? extends Scp027Entity> Type, Level World) {
        super(Type, World);
        setPathfindingMalus(PathType.WATER, WaterCost);
        setPersistenceRequired();
        xpReward = 10;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MaxHealth)
                .add(Attributes.ARMOR, Armor)
                .add(Attributes.MOVEMENT_SPEED, MoveSpeed)
                .add(Attributes.KNOCKBACK_RESISTANCE, Sturdiness)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(SwarmData, 0);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new Scp027Goals.Drift(this, DriftSpeed));
        goalSelector.addGoal(4, new Scp027Goals.Wander(this, WanderSpeed, WanderInterval));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, LookRange, 0.06f));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Group);
        setPersistenceRequired();
        return Result;
    }

    @Override
    public float glanceRate() {
        return Glance;
    }

    @Override
    public Component controlRefusal() {
        return Component.translatable(Prefix + "refuses");
    }

    public int swarmSize() {
        return entityData.get(SwarmData);
    }

    public void setSwarmSize(int Size) {
        if (Size != swarmSize()) {
            entityData.set(SwarmData, Size);
        }
    }

    public static boolean active(ServerLevel World) {
        return active(World.dimension(), World.dimensionTypeRegistration(), World.getDifficulty());
    }

    public static boolean active(ResourceKey<Level> Dimension, Holder<DimensionType> Type, Difficulty Setting) {
        return AnomalyRealm.allows(Dimension, Type) && Setting != Difficulty.PEACEFUL;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        boolean Active = active(World);
        if (--NextPulse <= 0) {
            pulse(World, Active);
        }
        if (Active && --NextWilt <= 0) {
            NextWilt = Scp027Blight.WiltInterval + getRandom().nextInt(40);
            Scp027Blight.wilt(World, this);
        }
        if (Active && --NextSpoil <= 0) {
            NextSpoil = Scp027Blight.SpoilInterval;
            Scp027Blight.spoil(World, this);
        }
        if (tickCount - LastHurt > RegenDelay && tickCount % RegenEvery == 0 && getHealth() < getMaxHealth()) {
            heal(RegenAmount);
        }
        sounds(World);
    }

    public void pulse(ServerLevel World, boolean Active) {
        if (!isAlive()) {
            setSwarmSize(0);
            return;
        }
        boolean Awake = Scp027Goals.nearestPerson(this, Scp027Swarm.AwakeRange) != null;
        boolean Watched = Awake || World.getNearestPlayer(this, Scp027Swarm.WatchRange) != null;
        NextPulse = Awake ? Scp027Swarm.AwakeInterval : Scp027Swarm.IdleInterval;
        List<Mob> Swarm = Scp027Swarm.owned(World, this);
        int Size = Swarm.size();
        if (Active && Watched && Size < Scp027Swarm.cap(Awake) && Scp027Swarm.crowd(World, position()) < Scp027Swarm.CrowdCap) {
            if (Scp027Swarm.summon(World, this, Scp027Swarm.pick(Swarm, getRandom())).isPresent()) {
                Size++;
            }
        }
        setSwarmSize(Size);
    }

    private void sounds(ServerLevel World) {
        double Dx = getX() - xo;
        double Dz = getZ() - zo;
        if (onGround() && Dx * Dx + Dz * Dz > ShuffleMotion * ShuffleMotion && ++Steps % ShuffleEvery == 0) {
            playLayers(Scp027Registry.ShuffleLayers, Steps % (ShuffleEvery * 2) == 0 ? 0.4f : 0.3f, 0.9f + getRandom().nextFloat() * 0.15f, position());
        }
        boolean Heard = World.getNearestPlayer(this, HearRange) != null;
        if (--NextFlies <= 0) {
            NextFlies = FlyShortest + getRandom().nextInt(FlyLongest - FlyShortest);
            if (Heard) {
                playLayers(Scp027Registry.FlyLayers, 0.35f, 0.92f + getRandom().nextFloat() * 0.12f, getEyePosition());
            }
        }
        if (--NextSkitter <= 0) {
            NextSkitter = SkitterShortest + getRandom().nextInt(SkitterLongest - SkitterShortest);
            if (Heard && swarmSize() >= SkitterSwarm) {
                playLayers(Scp027Registry.SwarmLayers, 0.4f, 0.9f + getRandom().nextFloat() * 0.2f, position());
            }
        }
    }

    private void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch, Vec3 Where) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            level().playSound(null, Where.x, Where.y, Where.z, Layer.get(), SoundSource.HOSTILE, Volume, Pitch);
        }
    }

    public static boolean isHit(DamageSource Source) {
        return !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) && (Source.getEntity() != null || Source.getDirectEntity() != null);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (!(level() instanceof ServerLevel World) || isInvulnerableTo(Source) || isDeadOrDying() || !isHit(Source)) {
            return super.hurt(Source, Amount);
        }
        LastHurt = tickCount;
        float Taken = Amount;
        int Near = Scp027Swarm.near(World, this);
        if (Near > 0 && getRandom().nextFloat() < Scp027Swarm.soakChance(Near)) {
            Optional<Mob> Shield = Scp027Swarm.interceptor(World, this);
            if (Shield.isPresent()) {
                intercept(World, Shield.get(), Source, Amount);
                Taken = Amount * Scp027Swarm.Leak;
            }
        }
        rally(World, Source);
        return super.hurt(Source, Taken);
    }

    private void intercept(ServerLevel World, Mob Body, DamageSource Source, float Amount) {
        Entity Striker = Source.getDirectEntity() != null ? Source.getDirectEntity() : Source.getEntity();
        if (Striker != null && !((Scp027Vermin) Body).flies()) {
            Vec3 Toward = Striker.position().subtract(position());
            Vec3 Flat = new Vec3(Toward.x, 0.0, Toward.z);
            if (Flat.lengthSqr() > 1.0e-4) {
                Vec3 Spot = position().add(Flat.normalize().scale(0.7));
                if (World.noCollision(Body, Body.getDimensions(Body.getPose()).makeBoundingBox(Spot))) {
                    Body.teleportTo(Spot.x, Spot.y, Spot.z);
                }
            }
        }
        World.sendParticles(ParticleTypes.POOF, Body.getX(), Body.getY() + Body.getBbHeight() * 0.5, Body.getZ(), 5, 0.15, 0.1, 0.15, 0.03);
        Scp027Swarm.play(World, Body.position(), Scp027Registry.SoakLayers, 0.7f, 0.9f + getRandom().nextFloat() * 0.2f);
        Body.hurt(Source, Math.max(Amount, Body.getMaxHealth() * 2.0f));
        if (Body.isAlive()) {
            Body.kill();
        }
    }

    private void rally(ServerLevel World, DamageSource Source) {
        if (!(Source.getEntity() instanceof LivingEntity Attacker) || Attacker instanceof Scp027Vermin || Attacker == this) {
            return;
        }
        if (Attacker instanceof Player User && (User.isCreative() || User.isSpectator())) {
            return;
        }
        for (Mob Body : Scp027Swarm.owned(World, this)) {
            if (Body.getTarget() == null && !((Scp027Vermin) Body).flies()) {
                Body.setTarget(Attacker);
            }
        }
    }

    @Override
    public void die(DamageSource Source) {
        boolean Before = dead;
        super.die(Source);
        if (!Before && dead && !level().isClientSide) {
            Scp027Death.end(this);
        }
    }

    @Override
    protected InteractionResult mobInteract(Player User, InteractionHand Hand) {
        ItemStack Held = User.getItemInHand(Hand);
        if (Hand != InteractionHand.MAIN_HAND || User.isSpectator() || Held.is(Items.NAME_TAG) || Held.is(Items.LEAD)) {
            return InteractionResult.PASS;
        }
        if (User instanceof ServerPlayer Speaker && tickCount >= LineReady) {
            LineReady = tickCount + LineCooldown;
            int Line = getRandom().nextInt(Lines);
            if (Line == LastLine) {
                Line = (Line + 1 + getRandom().nextInt(Lines - 1)) % Lines;
            }
            LastLine = Line;
            Component Name = Component.translatable(Prefix + "speaker");
            Speaker.displayClientMessage(Component.translatable(Prefix + "speech", Name, Component.translatable(Prefix + "line." + Line))
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            playLayers(Scp027Registry.MumbleLayers, 0.7f, 0.95f + getRandom().nextFloat() * 0.1f, getEyePosition());
            getNavigation().stop();
            getLookControl().setLookAt(Speaker, 20.0f, 20.0f);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    @Override
    public void playAmbientSound() {
        playLayers(Scp027Registry.MumbleLayers, 0.55f, 0.9f + getRandom().nextFloat() * 0.15f, getEyePosition());
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return Scp027Registry.Mumble.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return AmbientInterval;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return DClassRegistry.Hurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return DClassRegistry.Death.get();
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * 0.88f;
    }

    @Override
    public boolean wantsToPickUp(ItemStack Stack) {
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
    protected boolean shouldDropLoot() {
        return false;
    }

    public float hunch() {
        return Mth.clamp(swarmSize() / (float) Scp027Swarm.MaxSwarm, 0.0f, 1.0f);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        Saved.putInt("Swarm", swarmSize());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        setSwarmSize(Saved.getInt("Swarm"));
        setPersistenceRequired();
    }
}
