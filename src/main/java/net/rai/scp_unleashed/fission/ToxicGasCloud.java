package net.rai.scp_unleashed.fission;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ToxicGasCloud extends Entity {
    public static final float MaxGas = 900.0f;
    public static final float MinRadius = 1.25f;
    public static final float MaxRadius = 4.5f;
    public static final float Decay = 0.9975f;
    public static final float LingerDecay = 0.9993f;
    public static final float Fade = 0.05f;
    public static final float StrongShare = 0.5f;
    public static final int PoisonInterval = 10;
    public static final int PoisonTicks = 100;
    public static final int RefreshBelow = 40;
    public static final double MergeReach = 2.0;
    public static final int FalloutClouds = 7;
    public static final double FalloutSpread = 7.0;
    private static final EntityDataAccessor<Float> DataGas = SynchedEntityData.defineId(ToxicGasCloud.class, EntityDataSerializers.FLOAT);

    private float Gas;
    private float SyncedGas;
    private boolean Lingering;

    public ToxicGasCloud(EntityType<? extends ToxicGasCloud> Type, Level World) {
        super(Type, World);
        noPhysics = true;
    }

    public static void feed(ServerLevel World, Vec3 Point, float Amount) {
        if (Amount <= 0.0f || !World.isLoaded(BlockPos.containing(Point))) {
            return;
        }
        ToxicGasCloud Nearest = null;
        double Best = MergeReach * MergeReach;
        for (ToxicGasCloud Cloud : World.getEntitiesOfClass(ToxicGasCloud.class, new AABB(Point, Point).inflate(MergeReach))) {
            double Distance = Cloud.position().distanceToSqr(Point);
            if (Distance <= Best) {
                Best = Distance;
                Nearest = Cloud;
            }
        }
        if (Nearest != null) {
            Nearest.addGas(Amount);
            return;
        }
        ToxicGasCloud Fresh = FissionRegistry.ToxicGas.get().create(World);
        if (Fresh != null) {
            Fresh.moveTo(Point.x, Point.y, Point.z);
            Fresh.addGas(Amount);
            World.addFreshEntity(Fresh);
        }
    }

    public static void fallout(ServerLevel World, Vec3 Centre) {
        RandomSource Random = World.getRandom();
        for (int Index = 0; Index < FalloutClouds; Index++) {
            double Angle = Index * Mth.TWO_PI / FalloutClouds + Random.nextDouble() * 0.5;
            double Reach = Index == 0 ? 0.0 : FalloutSpread * (0.5 + Random.nextDouble() * 0.5);
            ToxicGasCloud Cloud = FissionRegistry.ToxicGas.get().create(World);
            if (Cloud == null) {
                continue;
            }
            Cloud.moveTo(Centre.x + Math.cos(Angle) * Reach, Centre.y + Random.nextDouble() * 2.0 - 1.0, Centre.z + Math.sin(Angle) * Reach);
            Cloud.Lingering = true;
            Cloud.addGas(MaxGas);
            World.addFreshEntity(Cloud);
        }
    }

    public void addGas(float Amount) {
        Gas = Math.min(MaxGas, Gas + Amount);
        if (!level().isClientSide) {
            pushGas(false);
        }
    }

    public float getGas() {
        return level().isClientSide ? entityData.get(DataGas) : Gas;
    }

    public float drainGas(float Amount) {
        float Taken = Math.min(Math.max(0.0f, Amount), Gas);
        Gas -= Taken;
        if (Gas <= Fade) {
            discard();
        } else {
            pushGas(false);
        }
        return Taken;
    }

    public float getDensity() {
        return Mth.clamp(getGas() / MaxGas, 0.0f, 1.0f);
    }

    public float getRadius() {
        return MinRadius + (MaxRadius - MinRadius) * Mth.sqrt(getDensity());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(DataGas, 0.0f);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            drift();
            return;
        }
        Gas = Gas * (Lingering ? LingerDecay : Decay) - Fade;
        if (Gas <= 0.0f) {
            discard();
            return;
        }
        pushGas(false);
        if (tickCount % PoisonInterval == 0) {
            poison();
        }
    }

    private void pushGas(boolean Force) {
        if (Force || Math.abs(Gas - SyncedGas) > Math.max(2.0f, SyncedGas * 0.05f)) {
            SyncedGas = Gas;
            entityData.set(DataGas, Gas);
        }
    }

    private void poison() {
        float Radius = getRadius();
        double Reach = (Radius + 0.4) * (Radius + 0.4);
        int Amplifier = Gas >= MaxGas * StrongShare ? 1 : 0;
        Vec3 Centre = position();
        for (LivingEntity Target : level().getEntitiesOfClass(LivingEntity.class, new AABB(Centre, Centre).inflate(Radius + 1.0))) {
            if (!Target.isAlive() || Target.position().add(0.0, Target.getBbHeight() * 0.5, 0.0).distanceToSqr(Centre) > Reach) {
                continue;
            }
            if (Target instanceof Player User && (User.isCreative() || User.isSpectator())) {
                continue;
            }
            if (FissionTags.isProtected(Target)) {
                continue;
            }
            MobEffectInstance Current = Target.getEffect(MobEffects.POISON);
            if (Current == null || Current.getDuration() < RefreshBelow || Current.getAmplifier() < Amplifier) {
                Target.addEffect(new MobEffectInstance(MobEffects.POISON, PoisonTicks, Amplifier), this);
            }
        }
    }

    private void drift() {
        float Density = getDensity();
        if (Density <= 0.0f) {
            return;
        }
        RandomSource Random = random;
        float Radius = getRadius();
        float Rate = 0.35f + Density * 3.2f;
        int Count = (int) Rate + (Random.nextFloat() < Rate - (int) Rate ? 1 : 0);
        for (int Index = 0; Index < Count; Index++) {
            double OffsetX;
            double OffsetY;
            double OffsetZ;
            do {
                OffsetX = Random.nextDouble() * 2.0 - 1.0;
                OffsetY = Random.nextDouble() * 2.0 - 1.0;
                OffsetZ = Random.nextDouble() * 2.0 - 1.0;
            } while (OffsetX * OffsetX + OffsetY * OffsetY + OffsetZ * OffsetZ > 1.0);
            double Spread = Radius * 0.85;
            level().addParticle(FissionRegistry.ToxicGasParticle.get(), getX() + OffsetX * Spread, getY() + OffsetY * Spread * 0.7, getZ() + OffsetZ * Spread,
                    (Random.nextDouble() - 0.5) * 0.012, 0.002 + Random.nextDouble() * 0.006, (Random.nextDouble() - 0.5) * 0.012);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
        Gas = Math.max(0.0f, Math.min(MaxGas, Tag.getFloat("Gas")));
        Lingering = Tag.getBoolean("Lingering");
        pushGas(true);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
        Tag.putFloat("Gas", Gas);
        Tag.putBoolean("Lingering", Lingering);
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
    public boolean isAttackable() {
        return false;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < 4096.0;
    }
}
