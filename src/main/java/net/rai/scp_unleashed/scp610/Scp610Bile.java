package net.rai.scp_unleashed.scp610;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

public class Scp610Bile extends ThrowableProjectile {
    public static final float Damage = 4.0f;
    public static final float InfectChance = 0.5f;
    public static final int Lifetime = 80;

    public Scp610Bile(EntityType<? extends Scp610Bile> Type, Level World) {
        super(Type, World);
    }

    public Scp610Bile(Level World, LivingEntity Shooter) {
        super(Scp610Registry.BileType.get(), Shooter, World);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return 0.03;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            level().addParticle(Scp610Registry.SporeParticle.get(), getX(), getY(), getZ(), 0.0, 0.0, 0.0);
        } else if (tickCount > Lifetime) {
            discard();
        }
    }

    public static boolean infects(LivingEntity Victim, LivingEntity Shooter, float Roll) {
        if (Victim instanceof Scp610Creature || Scp610Infection.isSuited(Victim) || Scp610Infection.isSuited(Shooter)) {
            return false;
        }
        return Roll < InfectChance && Scp610Infection.infect(Victim);
    }

    @Override
    protected void onHitEntity(EntityHitResult Hit) {
        super.onHitEntity(Hit);
        Entity Struck = Hit.getEntity();
        Entity Shooter = getOwner();
        if (!(Struck instanceof LivingEntity Victim) || Struck instanceof Scp610Creature || level().isClientSide) {
            return;
        }
        Victim.hurt(damageSources().mobProjectile(this, Shooter instanceof LivingEntity Owner ? Owner : null), Damage);
        if (Shooter instanceof LivingEntity Owner) {
            infects(Victim, Owner, random.nextFloat());
        }
    }

    @Override
    protected boolean canHitEntity(Entity Target) {
        return !(Target instanceof Scp610Creature) && super.canHitEntity(Target);
    }

    @Override
    protected void onHit(HitResult Result) {
        super.onHit(Result);
        if (level() instanceof ServerLevel World) {
            Scp610Infection.cloud(World, position(), 0.6, 8);
            playSound(Scp610Sounds.Wet.get(), 0.8f, 1.2f);
            discard();
        }
    }
}
