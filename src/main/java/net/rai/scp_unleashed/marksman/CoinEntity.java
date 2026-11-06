package net.rai.scp_unleashed.marksman;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.guns.GunSounds;

import java.util.ArrayList;
import java.util.List;

public class CoinEntity extends Entity {
    public static final double Gravity = 0.035;
    public static final double Drag = 0.99;
    public static final int MaxLife = 80;
    public static final double HalfHeight = 0.125;
    public static final double RenderReach = 96.0;
    public static final float SparkleChance = 0.45F;
    public static final int HistoryTicks = 4;
    private final Vec3[] History = new Vec3[HistoryTicks];
    private int Life;
    private boolean Spent;

    public CoinEntity(EntityType<? extends CoinEntity> Type, Level World) {
        super(Type, World);
    }

    public CoinEntity(Level World, Vec3 Centre, Vec3 Velocity, float Yaw) {
        this(MarksmanRegistry.Coin.get(), World);
        moveTo(Centre.x, Centre.y - HalfHeight, Centre.z, Yaw, 0.0F);
        setDeltaMovement(Velocity);
    }

    public Vec3 centre() {
        return position().add(0.0, HalfHeight, 0.0);
    }

    public Vec3 centre(float Partial) {
        return getPosition(Partial).add(0.0, HalfHeight, 0.0);
    }

    public List<Vec3> recentCentres() {
        List<Vec3> Centres = new ArrayList<>();
        Centres.add(centre());
        for (Vec3 Past : History) {
            if (Past != null) {
                Centres.add(Past);
            }
        }
        return Centres;
    }

    public boolean isSpent() {
        return Spent || isRemoved();
    }

    public void spend() {
        Spent = true;
        discard();
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide) {
            History[Life % HistoryTicks] = centre();
        }
        setDeltaMovement(getDeltaMovement().add(0.0, -Gravity, 0.0));
        move(MoverType.SELF, getDeltaMovement());
        boolean Struck = horizontalCollision || verticalCollision || onGround();
        setDeltaMovement(getDeltaMovement().scale(Drag));
        Life++;
        if (level().isClientSide) {
            sparkle();
            return;
        }
        if (Spent) {
            return;
        }
        if (Struck || isInWater() || isInLava()) {
            finish(CoinSounds.land(), 6);
        } else if (Life >= MaxLife) {
            finish(CoinSounds.fade(), 4);
        }
    }

    private void finish(List<GunSounds.Layer> Sounds, int Sparkles) {
        if (level() instanceof ServerLevel World) {
            Vec3 Centre = centre();
            GunSounds.play(World, null, Centre.x, Centre.y, Centre.z, Sounds, 1.0F);
            World.sendParticles(MarksmanRegistry.Glint.get(), Centre.x, Centre.y, Centre.z, Sparkles, 0.08, 0.05, 0.08, 0.02);
        }
        Spent = true;
        discard();
    }

    private void sparkle() {
        RandomSource Random = getRandom();
        if (Random.nextFloat() >= SparkleChance) {
            return;
        }
        Vec3 Centre = centre();
        level().addParticle(MarksmanRegistry.Glint.get(), Centre.x + (Random.nextDouble() - 0.5) * 0.3, Centre.y + (Random.nextDouble() - 0.5) * 0.3,
                Centre.z + (Random.nextDouble() - 0.5) * 0.3, 0.0, -0.01, 0.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
        Life = Tag.getInt("Life");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
        Tag.putInt("Life", Life);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        double Reach = RenderReach * getViewScale();
        return Distance < Reach * Reach;
    }

    @Override
    public boolean isAttackable() {
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
    public boolean canBeHitByProjectile() {
        return false;
    }

    @Override
    protected MovementEmission getMovementEmission() {
        return MovementEmission.NONE;
    }
}
