package net.rai.scp_unleashed.raid.serpentshand;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.raid.Raid;
import net.rai.scp_unleashed.raid.RaidWorld;
import net.rai.scp_unleashed.raid.hostile.HostileRegistry;

public class SerpentSeal extends Entity {
    public static final float Radius = 1.75f;
    public static final int OpenTicks = 24;
    public static final int CloseTicks = 30;
    public static final int HumEvery = 70;
    public static final int OrphanTicks = 100;
    private static final EntityDataAccessor<Boolean> ClosingData = SynchedEntityData.defineId(SerpentSeal.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FlareData = SynchedEntityData.defineId(SerpentSeal.class, EntityDataSerializers.BOOLEAN);

    private int RaidId = -1;
    private int Closing;
    private int Lonely;
    private float Shown;
    private float ShownO;

    public SerpentSeal(EntityType<? extends SerpentSeal> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
    }

    public static SerpentSeal open(ServerLevel World, Vec3 Floor, int RaidNumber, boolean Quiet) {
        SerpentSeal Made = new SerpentSeal(HostileRegistry.SealType.get(), World);
        Made.moveTo(Floor.x, Floor.y + 0.02, Floor.z, World.random.nextFloat() * 360.0f, 0.0f);
        Made.RaidId = RaidNumber;
        World.addFreshEntity(Made);
        if (!Quiet) {
            World.playSound(null, Floor.x, Floor.y, Floor.z, HostileRegistry.SealOpen.get(), SoundSource.HOSTILE, 2.0f, 1.0f);
            World.playSound(null, Floor.x, Floor.y, Floor.z, HostileRegistry.SealChime.get(), SoundSource.HOSTILE, 1.6f, 0.9f);
            World.sendParticles(SerpentPods.Teal, Floor.x, Floor.y + 0.2, Floor.z, 40, Radius * 0.6, 0.1, Radius * 0.6, 0.0);
            World.sendParticles(SerpentPods.Gold, Floor.x, Floor.y + 0.3, Floor.z, 20, Radius * 0.5, 0.2, Radius * 0.5, 0.0);
            World.sendParticles(ParticleTypes.END_ROD, Floor.x, Floor.y + 0.5, Floor.z, 12, 0.4, 0.6, 0.4, 0.03);
        }
        return Made;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(ClosingData, false);
        Builder.define(FlareData, false);
    }

    public int raidId() {
        return RaidId;
    }

    public boolean closing() {
        return entityData.get(ClosingData);
    }

    public void setFlare(boolean Value) {
        entityData.set(FlareData, Value);
    }

    public boolean flaring() {
        return entityData.get(FlareData);
    }

    public void close() {
        if (closing()) {
            return;
        }
        entityData.set(ClosingData, true);
        Closing = 0;
        if (level() instanceof ServerLevel World) {
            World.playSound(null, getX(), getY(), getZ(), HostileRegistry.SealClose.get(), SoundSource.HOSTILE, 1.8f, 1.0f);
        }
    }

    public float glow(float Partial) {
        return Mth.lerp(Partial, ShownO, Shown);
    }

    @Override
    public void tick() {
        super.tick();
        ShownO = Shown;
        if (closing()) {
            Shown = Math.max(0.0f, Shown - 1.0f / CloseTicks);
        } else {
            Shown = Math.min(flaring() ? 1.0f : 0.85f, Shown + 1.0f / OpenTicks);
        }
        if (level().isClientSide) {
            motes();
            return;
        }
        ServerLevel World = (ServerLevel) level();
        if (closing()) {
            if (++Closing >= CloseTicks) {
                discard();
            }
            return;
        }
        if (tickCount % HumEvery == 0) {
            World.playSound(null, getX(), getY(), getZ(), HostileRegistry.SealHum.get(), SoundSource.HOSTILE, 0.9f, 0.95f + random.nextFloat() * 0.1f);
        }
        if (tickCount % 20 == 0) {
            Raid Found = RaidId < 0 ? null : RaidWorld.get(World.getServer()).raid(RaidId);
            if (Found == null || !Found.live()) {
                Lonely += 20;
                if (Lonely >= OrphanTicks) {
                    close();
                }
            } else {
                Lonely = 0;
            }
        }
    }

    private void motes() {
        if (Shown <= 0.05f) {
            return;
        }
        int Count = flaring() ? 3 : 1;
        for (int Index = 0; Index < Count; Index++) {
            double Angle = random.nextDouble() * Mth.TWO_PI;
            double Reach = Radius * (0.85 + random.nextDouble() * 0.15);
            double X = getX() + Math.cos(Angle) * Reach;
            double Z = getZ() + Math.sin(Angle) * Reach;
            if (random.nextInt(3) == 0) {
                level().addParticle(ParticleTypes.END_ROD, X, getY() + 0.05, Z, 0.0, 0.02 + random.nextDouble() * 0.03, 0.0);
            } else {
                level().addParticle(random.nextBoolean() ? SerpentPods.Teal : SerpentPods.Gold, X, getY() + 0.1, Z, 0.0, 0.0, 0.0);
            }
        }
        if (random.nextInt(4) == 0) {
            level().addParticle(ParticleTypes.ENCHANT, getX() + (random.nextDouble() - 0.5) * Radius, getY() + 1.2, getZ() + (random.nextDouble() - 0.5) * Radius,
                    0.0, -0.6, 0.0);
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return true;
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
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean isIgnoringBlockTriggers() {
        return true;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return getBoundingBox().inflate(Radius, 1.0, Radius);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        double Range = 96.0 * getViewScale();
        return Distance < Range * Range;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Saved) {
        RaidId = Saved.getInt("Raid");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Saved) {
        Saved.putInt("Raid", RaidId);
    }
}
