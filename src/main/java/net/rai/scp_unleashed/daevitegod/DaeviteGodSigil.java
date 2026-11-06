package net.rai.scp_unleashed.daevitegod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class DaeviteGodSigil extends Entity implements TraceableEntity {
    public static final byte KindLightning = 0;
    public static final byte KindSpikes = 1;
    public static final byte KindSpirits = 2;
    public static final int FadeIn = 6;
    public static final int BoltTicks = 9;
    public static final int ScorchTicks = 22;
    public static final int SpikeRise = 3;
    public static final int SpikeHold = 14;
    public static final int SpikeSink = 8;
    public static final int SpiritTicks = 20;
    public static final double StrikeHeight = 5.0;
    public static final double SpikeHeight = 2.6;
    public static final float SpikeToss = 0.78f;
    public static final int BurnSeconds = 5;
    protected static final EntityDataAccessor<Byte> KindData = SynchedEntityData.defineId(DaeviteGodSigil.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Integer> DelayData = SynchedEntityData.defineId(DaeviteGodSigil.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Integer> TelegraphData = SynchedEntityData.defineId(DaeviteGodSigil.class, EntityDataSerializers.INT);
    protected static final EntityDataAccessor<Float> RadiusData = SynchedEntityData.defineId(DaeviteGodSigil.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Integer> SeedData = SynchedEntityData.defineId(DaeviteGodSigil.class, EntityDataSerializers.INT);

    private float Damage;
    @Nullable
    private UUID OwnerId;
    @Nullable
    private LivingEntity OwnerCache;
    private boolean Triggered;

    public DaeviteGodSigil(EntityType<? extends DaeviteGodSigil> Type, Level World) {
        super(Type, World);
        noPhysics = true;
    }

    public static DaeviteGodSigil cast(ServerLevel World, Vec3 At, byte Kind, int Delay, int Telegraph, float Radius, float Damage, @Nullable LivingEntity Owner) {
        DaeviteGodSigil Sigil = new DaeviteGodSigil(DaeviteGodRegistry.Sigil.get(), World);
        Sigil.setPos(At.x, At.y + 0.02, At.z);
        Sigil.setYRot(World.random.nextFloat() * 360.0f);
        Sigil.entityData.set(KindData, Kind);
        Sigil.entityData.set(DelayData, Math.max(0, Delay));
        Sigil.entityData.set(TelegraphData, Math.max(4, Telegraph));
        Sigil.entityData.set(RadiusData, Radius);
        Sigil.entityData.set(SeedData, World.random.nextInt(1 << 20));
        Sigil.Damage = Damage;
        if (Owner != null) {
            Sigil.OwnerId = Owner.getUUID();
            Sigil.OwnerCache = Owner;
        }
        World.addFreshEntity(Sigil);
        return Sigil;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(KindData, KindLightning);
        Builder.define(DelayData, 0);
        Builder.define(TelegraphData, 25);
        Builder.define(RadiusData, 1.5f);
        Builder.define(SeedData, 0);
    }

    public byte kind() {
        return entityData.get(KindData);
    }

    public int delay() {
        return entityData.get(DelayData);
    }

    public int telegraph() {
        return entityData.get(TelegraphData);
    }

    public float radius() {
        return entityData.get(RadiusData);
    }

    public int seed() {
        return entityData.get(SeedData);
    }

    public int aftermath() {
        return switch (kind()) {
            case KindLightning -> ScorchTicks;
            case KindSpikes -> SpikeRise + SpikeHold + SpikeSink;
            default -> SpiritTicks;
        };
    }

    public int lifetime() {
        return delay() + telegraph() + aftermath();
    }

    public float elapsed(float Partial) {
        return tickCount + Partial - delay();
    }

    public float appear(float Partial) {
        float Time = elapsed(Partial);
        if (Time <= 0.0f) {
            return 0.0f;
        }
        float In = Mth.clamp(Time / FadeIn, 0.0f, 1.0f);
        float After = Time - telegraph();
        float Out = After <= 0.0f ? 1.0f : Mth.clamp(1.0f - After / aftermath(), 0.0f, 1.0f);
        return In * Out;
    }

    public float charge(float Partial) {
        return Mth.clamp(elapsed(Partial) / telegraph(), 0.0f, 1.0f);
    }

    public float sinceTrigger(float Partial) {
        return elapsed(Partial) - telegraph();
    }

    public float spikeRise(float Partial) {
        float Time = sinceTrigger(Partial);
        if (Time <= 0.0f) {
            return 0.0f;
        }
        if (Time < SpikeRise) {
            return Time / SpikeRise;
        }
        Time -= SpikeRise;
        if (Time < SpikeHold) {
            return 1.0f;
        }
        Time -= SpikeHold;
        return Mth.clamp(1.0f - Time / SpikeSink, 0.0f, 1.0f);
    }

    @Nullable
    @Override
    public LivingEntity getOwner() {
        if (OwnerCache == null && OwnerId != null && level() instanceof ServerLevel World && World.getEntity(OwnerId) instanceof LivingEntity Found) {
            OwnerCache = Found;
        }
        return OwnerCache;
    }

    @Override
    public void tick() {
        super.tick();
        int Time = tickCount - delay();
        if (level().isClientSide) {
            clientEffects(Time);
            return;
        }
        ServerLevel World = (ServerLevel) level();
        if (Time == 0) {
            DeferredHolder<SoundEvent, SoundEvent> Cue = switch (kind()) {
                case KindLightning -> DaeviteGodRegistry.SigilLightning;
                case KindSpikes -> DaeviteGodRegistry.SigilSpikes;
                default -> DaeviteGodRegistry.SigilSpirits;
            };
            World.playSound(null, getX(), getY(), getZ(), Cue.get(), SoundSource.HOSTILE, 1.0f, 0.9f + random.nextFloat() * 0.2f);
            World.playSound(null, getX(), getY(), getZ(), DaeviteGodRegistry.SigilHum.get(), SoundSource.HOSTILE, 0.7f, 0.8f + random.nextFloat() * 0.15f);
        }
        if (Time == telegraph() && !Triggered) {
            Triggered = true;
            switch (kind()) {
                case KindLightning -> smite(World);
                case KindSpikes -> erupt(World);
                default -> conjure(World);
            }
        }
        if (tickCount >= lifetime()) {
            discard();
        }
    }

    private void clientEffects(int Time) {
        if (Time < 0) {
            return;
        }
        float Radius = radius();
        if (Time < telegraph()) {
            float Charge = (float) Time / telegraph();
            if (random.nextFloat() < 0.35f + Charge * 0.6f) {
                double Angle = random.nextDouble() * Math.PI * 2.0;
                double Reach = Radius * (0.85 + random.nextDouble() * 0.15);
                level().addParticle(DaeviteGodRegistry.Ember.get(), getX() + Math.cos(Angle) * Reach, getY() + 0.05, getZ() + Math.sin(Angle) * Reach, 0.0,
                        0.02 + Charge * 0.06, 0.0);
            }
            if (kind() == KindSpikes && Time > telegraph() / 2 && Time % 2 == 0) {
                BlockState Ground = level().getBlockState(BlockPos.containing(getX(), getY() - 0.5, getZ()));
                if (!Ground.isAir()) {
                    level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Ground), getX() + (random.nextDouble() - 0.5) * Radius, getY() + 0.05,
                            getZ() + (random.nextDouble() - 0.5) * Radius, 0.0, 0.1, 0.0);
                }
            }
            return;
        }
        int After = Time - telegraph();
        if (kind() == KindLightning && After < BoltTicks && random.nextFloat() < 0.6f) {
            level().addParticle(ParticleTypes.LARGE_SMOKE, getX() + (random.nextDouble() - 0.5) * Radius, getY() + 0.1, getZ() + (random.nextDouble() - 0.5) * Radius, 0.0, 0.05, 0.0);
        }
        if (kind() == KindSpirits && After < SpiritTicks && random.nextFloat() < 0.5f) {
            level().addParticle(ParticleTypes.SOUL, getX() + (random.nextDouble() - 0.5) * Radius, getY() + 0.1, getZ() + (random.nextDouble() - 0.5) * Radius, 0.0, 0.06, 0.0);
        }
    }

    private List<LivingEntity> victims(ServerLevel World, double Height) {
        double Radius = radius();
        AABB Reach = new AABB(getX() - Radius, getY() - 1.0, getZ() - Radius, getX() + Radius, getY() + Height, getZ() + Radius);
        LivingEntity Owner = getOwner();
        Entity Attacker = Owner != null ? Owner : this;
        return World.getEntitiesOfClass(LivingEntity.class, Reach, Body -> DaeviteGodTargets.canHurt(Attacker, Body)
                && Mth.square(Body.getX() - getX()) + Mth.square(Body.getZ() - getZ()) <= Mth.square(Radius + Body.getBbWidth() * 0.5));
    }

    private void smite(ServerLevel World) {
        playLayers(World, DaeviteGodRegistry.StrikeLayers, 1.0f, 0.9f + random.nextFloat() * 0.2f);
        World.sendParticles(DaeviteGodRegistry.Ember.get(), getX(), getY() + 0.3, getZ(), 40, radius() * 0.4, 0.4, radius() * 0.4, 0.12);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.2, getZ(), 12, radius() * 0.4, 0.2, radius() * 0.4, 0.02);
        LivingEntity Owner = getOwner();
        DamageSource Source = damageSources().source(DaeviteGodRegistry.SmiteDamage, this, Owner);
        for (LivingEntity Body : victims(World, StrikeHeight)) {
            if (Body.hurt(Source, Damage)) {
                Body.igniteForSeconds(BurnSeconds);
                Vec3 Away = new Vec3(Body.getX() - getX(), 0.0, Body.getZ() - getZ());
                if (Away.lengthSqr() > 1.0e-4) {
                    Away = Away.normalize().scale(0.35);
                    Body.setDeltaMovement(Body.getDeltaMovement().add(Away.x, 0.2, Away.z));
                    Body.hurtMarked = true;
                }
            }
        }
    }

    private void erupt(ServerLevel World) {
        playLayers(World, DaeviteGodRegistry.SpikeLayers, 1.0f, 0.85f + random.nextFloat() * 0.25f);
        BlockState Ground = World.getBlockState(BlockPos.containing(getX(), getY() - 0.5, getZ()));
        if (!Ground.isAir()) {
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Ground), getX(), getY() + 0.1, getZ(), 24, radius() * 0.4, 0.1, radius() * 0.4, 0.15);
        }
        World.sendParticles(DaeviteGodRegistry.Ember.get(), getX(), getY() + 0.6, getZ(), 16, radius() * 0.3, 0.5, radius() * 0.3, 0.05);
        LivingEntity Owner = getOwner();
        DamageSource Source = damageSources().source(DaeviteGodRegistry.SpikeDamage, this, Owner);
        for (LivingEntity Body : victims(World, SpikeHeight)) {
            if (Body.hurt(Source, Damage)) {
                Vec3 Motion = Body.getDeltaMovement();
                Body.setDeltaMovement(Motion.x * 0.3, Math.max(Motion.y, SpikeToss), Motion.z * 0.3);
                Body.hurtMarked = true;
            }
        }
    }

    private void conjure(ServerLevel World) {
        LivingEntity Owner = getOwner();
        DaeviteGodSpirit Spirit = new DaeviteGodSpirit(DaeviteGodRegistry.Spirit.get(), World);
        Spirit.moveTo(getX(), getY(), getZ(), getYRot(), 0.0f);
        Spirit.bind(Owner);
        World.addFreshEntity(Spirit);
        Spirit.beginRise();
        World.playSound(null, getX(), getY(), getZ(), DaeviteGodRegistry.SpiritRise.get(), SoundSource.HOSTILE, 1.0f, 0.8f + random.nextFloat() * 0.2f);
        World.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.3, getZ(), 18, 0.4, 0.3, 0.4, 0.04);
    }

    private void playLayers(ServerLevel World, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, getX(), getY(), getZ(), Layer.get(), SoundSource.HOSTILE, Volume, Pitch);
        }
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        double Radius = radius() + 0.5;
        double Height = kind() == KindLightning ? 48.0 : 4.0;
        return new AABB(getX() - Radius, getY() - 0.5, getZ() - Radius, getX() + Radius, getY() + Height, getZ() + Radius);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < 96.0 * 96.0;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Saved) {
        Damage = Saved.getFloat("Damage");
        OwnerId = Saved.hasUUID("Owner") ? Saved.getUUID("Owner") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Saved) {
        Saved.putFloat("Damage", Damage);
        if (OwnerId != null) {
            Saved.putUUID("Owner", OwnerId);
        }
    }
}
