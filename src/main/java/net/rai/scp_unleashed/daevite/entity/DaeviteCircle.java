package net.rai.scp_unleashed.daevite.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.daevite.DaeviteCollapse;
import net.rai.scp_unleashed.daevite.DaeviteRegistry;
import net.rai.scp_unleashed.daevite.DaeviteTargets;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

public class DaeviteCircle extends Entity {
    public static final byte Binding = 0;
    public static final byte Ritual = 1;
    public static final byte Channel = 2;
    public static final byte Summoning = 3;
    public static final byte Mending = 4;
    public static final int FadeTicks = 14;
    public static final int BindArm = 25;
    public static final int RootTicks = 50;
    public static final float BindDamage = 3.0f;
    public static final float BindRadius = 1.7f;
    public static final float RitualRadius = 6.5f;
    public static final float SummoningRadius = 4.5f;
    public static final float ChannelRadius = 1.3f;
    public static final float MendingRadius = 0.9f;
    protected static final EntityDataAccessor<Byte> KindData = SynchedEntityData.defineId(DaeviteCircle.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Float> RadiusData = SynchedEntityData.defineId(DaeviteCircle.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Float> PowerData = SynchedEntityData.defineId(DaeviteCircle.class, EntityDataSerializers.FLOAT);
    protected static final EntityDataAccessor<Boolean> FadingData = SynchedEntityData.defineId(DaeviteCircle.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID Owner;
    private int Life = -1;
    private int Follow = -1;
    private int FadeStart = -1;
    private int LocalFade = -1;
    private boolean Struck;

    public DaeviteCircle(EntityType<? extends DaeviteCircle> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
    }

    private static DaeviteCircle make(ServerLevel World, Vec3 At, byte Kind, float Radius, int Life) {
        DaeviteCircle Circle = new DaeviteCircle(DaeviteRegistry.Circle.get(), World);
        Circle.setPos(At.x, At.y + 0.02, At.z);
        Circle.setYRot(World.random.nextFloat() * 360.0f);
        Circle.entityData.set(KindData, Kind);
        Circle.entityData.set(RadiusData, Radius);
        Circle.Life = Life;
        World.addFreshEntity(Circle);
        return Circle;
    }

    public static Vec3 groundUnder(Entity Body) {
        Optional<Double> Ground = DaeviteCollapse.groundAt((ServerLevel) Body.level(), Body.getX(), Body.getZ(), Body.getY());
        return new Vec3(Body.getX(), Ground.orElse(Body.getY()), Body.getZ());
    }

    public static DaeviteCircle binding(ServerLevel World, DaeviteOverseer Caster, LivingEntity Target) {
        DaeviteCircle Circle = make(World, groundUnder(Target), Binding, BindRadius, -1);
        Circle.Owner = Caster.getUUID();
        World.playSound(null, Circle.getX(), Circle.getY(), Circle.getZ(), DaeviteRegistry.BindForm.get(), SoundSource.HOSTILE, 1.0f, 0.9f);
        return Circle;
    }

    public static DaeviteCircle mending(ServerLevel World, LivingEntity Ally) {
        DaeviteCircle Circle = make(World, Ally.position(), Mending, MendingRadius, DaeviteOverseer.Length[DaeviteOverseer.MoveHeal]);
        Circle.Follow = Ally.getId();
        return Circle;
    }

    public static DaeviteCircle ritual(ServerLevel World, Vec3 At, float Radius) {
        return make(World, At, Ritual, Radius, -1);
    }

    public static DaeviteCircle summoning(ServerLevel World, Vec3 At) {
        return make(World, At, Summoning, SummoningRadius, -1);
    }

    public static DaeviteCircle channel(ServerLevel World, Vec3 At) {
        return make(World, At, Channel, ChannelRadius, -1);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(KindData, Binding);
        Builder.define(RadiusData, 1.0f);
        Builder.define(PowerData, 0.0f);
        Builder.define(FadingData, false);
    }

    public byte kind() {
        return entityData.get(KindData);
    }

    public float radius() {
        return entityData.get(RadiusData);
    }

    public float power() {
        return entityData.get(PowerData);
    }

    public void setPower(float Value) {
        entityData.set(PowerData, Mth.clamp(Value, 0.0f, 1.0f));
    }

    public boolean fading() {
        return entityData.get(FadingData);
    }

    public float fade(float Partial) {
        if (!fading()) {
            return 1.0f;
        }
        int Start = level().isClientSide ? LocalFade : FadeStart;
        if (Start < 0) {
            return 1.0f;
        }
        return Mth.clamp(1.0f - (tickCount - Start + Partial) / FadeTicks, 0.0f, 1.0f);
    }

    public void dismiss() {
        if (!fading()) {
            FadeStart = tickCount;
            entityData.set(FadingData, true);
        }
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Accessor) {
        super.onSyncedDataUpdated(Accessor);
        if (FadingData.equals(Accessor) && fading()) {
            LocalFade = tickCount;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            ambient();
            return;
        }
        ServerLevel World = (ServerLevel) level();
        if (Follow >= 0) {
            Entity Found = World.getEntity(Follow);
            if (Found != null && Found.isAlive()) {
                setPos(Found.getX(), Found.getY() + 0.02, Found.getZ());
            } else {
                dismiss();
            }
        }
        if (fading()) {
            if (tickCount - FadeStart >= FadeTicks) {
                discard();
            }
            return;
        }
        if (kind() == Binding) {
            if (tickCount < BindArm && tickCount % 3 == 0) {
                ring(World, radius(), 6, 0.05);
            }
            if (tickCount >= BindArm && !Struck) {
                snap(World);
            }
        }
        if (Life >= 0 && tickCount >= Life) {
            dismiss();
        }
    }

    private void ring(ServerLevel World, float Radius, int Count, double Lift) {
        double Turn = random.nextDouble() * Mth.TWO_PI;
        for (int Step = 0; Step < Count; Step++) {
            double Angle = Turn + Step * Mth.TWO_PI / Count;
            World.sendParticles(DaeviteCollapse.Cinder, getX() + Math.cos(Angle) * Radius, getY() + 0.1, getZ() + Math.sin(Angle) * Radius, 1, 0.02, 0.0, 0.02, Lift);
        }
    }

    private void snap(ServerLevel World) {
        Struck = true;
        World.playSound(null, getX(), getY(), getZ(), DaeviteRegistry.BindSnap.get(), SoundSource.HOSTILE, 1.2f, 0.9f + random.nextFloat() * 0.15f);
        float Radius = radius();
        for (int Step = 0; Step < 14; Step++) {
            double Angle = Step * Mth.TWO_PI / 14;
            double X = getX() + Math.cos(Angle) * Radius * 0.85;
            double Z = getZ() + Math.sin(Angle) * Radius * 0.85;
            World.sendParticles(DaeviteCollapse.Shard, X, getY() + 0.3, Z, 3, 0.05, 0.25, 0.05, 0.02);
        }
        World.sendParticles(ParticleTypes.CRIT, getX(), getY() + 0.5, getZ(), 16, Radius * 0.5, 0.3, Radius * 0.5, 0.2);
        Entity Caster = Owner != null ? World.getEntity(Owner) : null;
        DamageSource Source = Caster != null ? damageSources().source(DaeviteRegistry.BindingDamage, this, Caster) : damageSources().source(DaeviteRegistry.BindingDamage, this);
        AABB Area = new AABB(getX() - Radius, getY() - 0.5, getZ() - Radius, getX() + Radius, getY() + 2.5, getZ() + Radius);
        for (LivingEntity Body : World.getEntitiesOfClass(LivingEntity.class, Area, Found -> !DaeviteTargets.isAlly(Found) && Found.isAlive() && !Found.isSpectator())) {
            double Dx = Body.getX() - getX();
            double Dz = Body.getZ() - getZ();
            if (Dx * Dx + Dz * Dz > Radius * Radius) {
                continue;
            }
            if (Caster != null && !DaeviteTargets.isPrey(Caster, Body, true)) {
                continue;
            }
            Body.hurt(Source, BindDamage);
            Body.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, RootTicks, 6, false, true, true), Caster);
            Body.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, RootTicks * 2, 1, false, true, true), Caster);
            Body.setDeltaMovement(Body.getDeltaMovement().multiply(0.0, 1.0, 0.0));
            Body.hurtMarked = true;
        }
        dismiss();
    }

    private void ambient() {
        if (fading()) {
            return;
        }
        float Radius = radius();
        byte Kind = kind();
        int Every = Kind == Ritual || Kind == Summoning ? 1 : Kind == Binding ? 2 : 4;
        if (tickCount % Every != 0) {
            return;
        }
        double Angle = random.nextDouble() * Mth.TWO_PI;
        double X = getX() + Math.cos(Angle) * Radius;
        double Z = getZ() + Math.sin(Angle) * Radius;
        double Lift = Kind == Ritual || Kind == Summoning ? 0.06 + power() * 0.08 : 0.03;
        level().addParticle(DaeviteCollapse.Cinder, X, getY() + 0.05, Z, 0.0, Lift, 0.0);
        if ((Kind == Ritual || Kind == Summoning) && random.nextInt(3) == 0) {
            double Inner = random.nextDouble() * Radius * 0.6;
            level().addParticle(ParticleTypes.SMALL_FLAME, getX() + Math.cos(Angle) * Inner, getY() + 0.05, getZ() + Math.sin(Angle) * Inner, 0.0, 0.02, 0.0);
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion Blast) {
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        double Reach = 64.0 + radius() * 8.0;
        return Distance < Reach * Reach;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        float Radius = radius() + 0.5f;
        return new AABB(getX() - Radius, getY() - 0.2, getZ() - Radius, getX() + Radius, getY() + 1.0, getZ() + Radius);
    }

    public BlockPos centre() {
        return BlockPos.containing(getX(), getY(), getZ());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Saved) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Saved) {
    }
}
