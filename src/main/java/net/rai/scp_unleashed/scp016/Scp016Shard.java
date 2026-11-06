package net.rai.scp_unleashed.scp016;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.registries.DeferredHolder;

public class Scp016Shard extends ThrowableProjectile {
    public static final float Damage = 4.0f;
    public static final int TrailEvery = 3;
    public static final int MaxAge = 100;

    public Scp016Shard(EntityType<? extends Scp016Shard> Type, Level World) {
        super(Type, World);
    }

    public Scp016Shard(Level World, LivingEntity Thrower) {
        super(Scp016Registry.ShardType.get(), Thrower, World);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            if (tickCount % TrailEvery == 0) {
                level().addParticle(Scp016Registry.Fleck.get(), getX(), getY() + getBbHeight() * 0.5, getZ(), 0.0, 0.0, 0.0);
            }
            return;
        }
        if (tickCount > MaxAge) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity Target) {
        return !(Target instanceof Scp016Spawn) && !(Target instanceof Scp016Shard) && super.canHitEntity(Target);
    }

    @Override
    protected void onHitEntity(EntityHitResult Result) {
        super.onHitEntity(Result);
        Entity Target = Result.getEntity();
        Entity Thrower = getOwner();
        if (Target.hurt(damageSources().thrown(this, Thrower), Damage) && Target instanceof LivingEntity Victim && !level().isClientSide) {
            Scp016Affliction.afflict(Victim);
        }
    }

    @Override
    protected void onHit(HitResult Result) {
        super.onHit(Result);
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        float Pitch = 1.0f + random.nextFloat() * 0.2f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Scp016Registry.ShardLayers) {
            World.playSound(null, getX(), getY(), getZ(), Layer.get(), SoundSource.HOSTILE, 0.8f, Pitch);
        }
        World.sendParticles(Scp016Registry.Fleck.get(), getX(), getY(), getZ(), 8, 0.15, 0.15, 0.15, 0.06);
        discard();
    }
}
