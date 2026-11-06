package net.rai.scp_unleashed.scp001scarlet.court;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class CourtBolt extends ThrowableProjectile {
    public static final int Crimson = 0;
    public static final int MaxLife = 100;
    private static final EntityDataAccessor<Integer> KindData = SynchedEntityData.defineId(CourtBolt.class, EntityDataSerializers.INT);
    private float Damage = 6.0f;

    public CourtBolt(EntityType<? extends ThrowableProjectile> Type, Level World) {
        super(Type, World);
    }

    public static CourtBolt launch(ServerLevel World, LivingEntity Owner, Vec3 From, Vec3 To, int Kind, double Speed, float Damage) {
        CourtBolt Bolt = new CourtBolt(ScarletCourtRegistry.BoltType.get(), World);
        Bolt.setOwner(Owner);
        Bolt.setPos(From);
        Bolt.entityData.set(KindData, Kind);
        Bolt.Damage = Damage;
        Vec3 Way = To.subtract(From);
        Bolt.shoot(Way.x, Way.y, Way.z, (float) Speed, 0.5f);
        World.addFreshEntity(Bolt);
        return Bolt;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(KindData, Crimson);
    }

    public int kind() {
        return entityData.get(KindData);
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(CourtFx.Crimson, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
            level().addParticle(CourtFx.DeepCrimson, xo, yo, zo, 0.0, 0.0, 0.0);
        } else if (tickCount > MaxLife) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity Other) {
        return super.canHitEntity(Other) && !CourtFx.allied(this, Other) && !(Other instanceof CourtMember);
    }

    @Override
    protected void onHitEntity(EntityHitResult Result) {
        super.onHitEntity(Result);
        if (Result.getEntity() instanceof LivingEntity Victim && !level().isClientSide) {
            Entity Shooter = getOwner();
            CourtFx.strike(damageSources().mobProjectile(this, Shooter instanceof LivingEntity Living ? Living : null), Victim, Damage);
        }
    }

    @Override
    protected void onHit(HitResult Result) {
        super.onHit(Result);
        if (level() instanceof ServerLevel World) {
            Vec3 At = Result.getLocation();
            CourtFx.burst(World, CourtFx.Crimson, At, 10, 0.25, 0.0);
            discard();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("BoltKind", kind());
        Tag.putFloat("BoltDamage", Damage);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        entityData.set(KindData, Tag.getInt("BoltKind"));
        Damage = Tag.getFloat("BoltDamage");
    }
}
