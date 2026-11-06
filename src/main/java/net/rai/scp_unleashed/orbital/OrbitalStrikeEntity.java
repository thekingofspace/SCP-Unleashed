package net.rai.scp_unleashed.orbital;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Consumer;

public class OrbitalStrikeEntity extends Entity {
    public static final int LingerTicks = 30;
    public static final int FadeTicks = 24;
    public static final int SyncTicks = 5;
    public static final int BurnTicks = 5;
    public static final double RenderReach = 512.0;
    public static final byte StrikeEvent = 71;

    private static final EntityDataAccessor<Integer> DataAge = SynchedEntityData.defineId(OrbitalStrikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DataCharge = SynchedEntityData.defineId(OrbitalStrikeEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DataRadius = SynchedEntityData.defineId(OrbitalStrikeEntity.class, EntityDataSerializers.FLOAT);
    private static Consumer<OrbitalStrikeEntity> ClientTick = Strike -> {
    };
    private static Consumer<OrbitalStrikeEntity> ClientStrike = Strike -> {
    };

    private int Age;
    @Nullable
    private UUID Owner;
    private OrbitalStrike.Settings Values = OrbitalStrike.Settings.current();
    private boolean Struck;

    public OrbitalStrikeEntity(EntityType<? extends OrbitalStrikeEntity> Type, Level World) {
        super(Type, World);
        this.noPhysics = true;
        setNoGravity(true);
    }

    public static void setClientHooks(Consumer<OrbitalStrikeEntity> Tick, Consumer<OrbitalStrikeEntity> Strike) {
        ClientTick = Tick;
        ClientStrike = Strike;
    }

    public static OrbitalStrikeEntity create(ServerLevel World, Vec3 At, @Nullable UUID Owner, OrbitalStrike.Settings Values, int Charge) {
        OrbitalStrikeEntity Strike = new OrbitalStrikeEntity(OrbitalRegistry.StrikeType.get(), World);
        Strike.moveTo(At.x, At.y, At.z, 0.0F, 0.0F);
        Strike.Owner = Owner;
        Strike.Values = Values;
        Strike.entityData.set(DataCharge, Charge);
        Strike.entityData.set(DataRadius, (float) Values.Radius());
        return Strike;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(DataAge, 0);
        Builder.define(DataCharge, OrbitalConfig.ChargeTicks.getDefault());
        Builder.define(DataRadius, (float) (double) OrbitalConfig.BeamRadius.getDefault());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (level().isClientSide && Key.equals(DataAge)) {
            Age = Math.max(Age, entityData.get(DataAge));
        }
    }

    public int getAge() {
        return Age;
    }

    public int getCharge() {
        return entityData.get(DataCharge);
    }

    public float getRadius() {
        return entityData.get(DataRadius);
    }

    public int getLife() {
        return getCharge() + LingerTicks + FadeTicks;
    }

    public boolean hasStruck() {
        return Age >= getCharge();
    }

    @Nullable
    public UUID getOwner() {
        return Owner;
    }

    public OrbitalStrike.Settings getValues() {
        return Values;
    }

    @Override
    public void tick() {
        super.tick();
        Age++;
        if (level().isClientSide) {
            ClientTick.accept(this);
            return;
        }
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (Age == 1 || Age % SyncTicks == 0) {
            entityData.set(DataAge, Age);
        }
        int Charge = getCharge();
        if (Age == 1) {
            play(World, OrbitalRegistry.ChargeHum.get(), 1.0F, 0.6F);
            play(World, OrbitalRegistry.ChargeLock.get(), 1.0F, 1.3F);
        }
        if (Age < Charge && (Age == 1 || Age % Math.max(1, Charge / 4) == 0)) {
            play(World, OrbitalRegistry.ChargeRise.get(), 1.0F, 0.7F + 0.9F * Age / (float) Charge);
        }
        if (Age == Math.max(1, Charge - 4)) {
            play(World, OrbitalRegistry.Descend.get(), 1.0F, 0.55F);
        }
        if (Age == Charge && !Struck) {
            strike(World);
        }
        if (Age > Charge && Age < Charge + LingerTicks && Age % BurnTicks == 0) {
            burn(World);
        }
        if (Age == Charge + LingerTicks) {
            play(World, OrbitalRegistry.BeamFade.get(), 1.0F, 0.5F);
        }
        if (Age >= getLife()) {
            discard();
        }
    }

    private void play(ServerLevel World, SoundEvent Sound, float Volume, float Pitch) {
        World.playSound(null, getX(), getY(), getZ(), Sound, SoundSource.BLOCKS, Volume, Pitch);
    }

    @Nullable
    public ServerPlayer owner(ServerLevel World) {
        return Owner == null ? null : World.getServer().getPlayerList().getPlayer(Owner);
    }

    public void strike(ServerLevel World) {
        Struck = true;
        Vec3 Centre = position();
        World.broadcastEntityEvent(this, StrikeEvent);
        play(World, OrbitalRegistry.StrikeBoom.get(), 1.0F, 0.5F);
        play(World, OrbitalRegistry.StrikeThunder.get(), 1.0F, 0.6F);
        play(World, OrbitalRegistry.StrikeBeam.get(), 1.0F, 0.5F);
        play(World, OrbitalRegistry.StrikeBlast.get(), 1.0F, 0.55F);
        play(World, OrbitalRegistry.StrikeCrack.get(), 1.0F, 0.6F);
        play(World, OrbitalRegistry.StrikeRumble.get(), 1.0F, 0.5F);
        ServerPlayer Caller = owner(World);
        OrbitalStrike.hurt(World, this, Caller, Centre, Values);
        if (Values.Crater() && EventHooks.canEntityGrief(World, this)) {
            OrbitalStrike.carve(World, BlockPos.containing(Centre.x, Centre.y - 0.5, Centre.z), Values.CraterRadius(), Caller, World.getRandom());
        }
        World.gameEvent(this, GameEvent.EXPLODE, Centre);
    }

    private void burn(ServerLevel World) {
        double Radius = Values.Radius();
        AABB Column = new AABB(getX() - Radius, getY() - OrbitalStrike.ColumnDepth, getZ() - Radius, getX() + Radius, World.getMaxBuildHeight() + 16.0, getZ() + Radius);
        boolean Any = false;
        for (LivingEntity Target : World.getEntitiesOfClass(LivingEntity.class, Column, Found -> Found.isAlive() && !Found.isSpectator())) {
            double Dx = Target.getX() - getX();
            double Dz = Target.getZ() - getZ();
            if (Dx * Dx + Dz * Dz <= Radius * Radius) {
                Target.igniteForSeconds(OrbitalStrike.BeamFireSeconds);
                Any = true;
            }
        }
        if (Any || Age % (BurnTicks * 2) == 0) {
            play(World, OrbitalRegistry.Sizzle.get(), 0.6F, 0.8F + random.nextFloat() * 0.3F);
        }
    }

    @Override
    public void handleEntityEvent(byte Id) {
        if (Id == StrikeEvent) {
            Age = Math.max(Age, getCharge());
            ClientStrike.accept(this);
            return;
        }
        super.handleEntityEvent(Id);
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
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean isOnFire() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < RenderReach * RenderReach;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
        Age = Tag.getInt("Age");
        Struck = Tag.getBoolean("Struck");
        Owner = Tag.hasUUID("Owner") ? Tag.getUUID("Owner") : null;
        OrbitalStrike.Settings Defaults = OrbitalStrike.Settings.current();
        Values = new OrbitalStrike.Settings(Tag.contains("Damage") ? Tag.getFloat("Damage") : Defaults.Damage(),
                Tag.contains("Radius") ? Tag.getDouble("Radius") : Defaults.Radius(), Tag.contains("EdgeShare") ? Tag.getDouble("EdgeShare") : Defaults.EdgeShare(),
                Tag.contains("Splash") ? Tag.getDouble("Splash") : Defaults.Splash(), Tag.contains("SplashDamage") ? Tag.getFloat("SplashDamage") : Defaults.SplashDamage(),
                Tag.contains("Crater") ? Tag.getBoolean("Crater") : Defaults.Crater(), Tag.contains("CraterRadius") ? Tag.getInt("CraterRadius") : Defaults.CraterRadius());
        entityData.set(DataAge, Age);
        entityData.set(DataCharge, Tag.contains("Charge") ? Tag.getInt("Charge") : OrbitalConfig.chargeTicks());
        entityData.set(DataRadius, (float) Values.Radius());
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
        Tag.putInt("Age", Age);
        Tag.putBoolean("Struck", Struck);
        if (Owner != null) {
            Tag.putUUID("Owner", Owner);
        }
        Tag.putInt("Charge", getCharge());
        Tag.putFloat("Damage", Values.Damage());
        Tag.putDouble("Radius", Values.Radius());
        Tag.putDouble("EdgeShare", Values.EdgeShare());
        Tag.putDouble("Splash", Values.Splash());
        Tag.putFloat("SplashDamage", Values.SplashDamage());
        Tag.putBoolean("Crater", Values.Crater());
        Tag.putInt("CraterRadius", Values.CraterRadius());
    }
}
