package net.rai.scp_unleashed.daeviteruins.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.daeviteruins.DaeviteRuinsRegistry;
import org.jetbrains.annotations.Nullable;

public class DaeviteThrownSpear extends AbstractArrow {
    public static final float Damage = 6.0f;
    private boolean Struck;

    public DaeviteThrownSpear(EntityType<? extends DaeviteThrownSpear> Type, Level World) {
        super(Type, World);
    }

    public DaeviteThrownSpear(Level World, LivingEntity Thrower, ItemStack Stack) {
        super(DaeviteRuinsRegistry.ThrownSpear.get(), Thrower, World, Stack, null);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(DaeviteRuinsRegistry.Spear.get());
    }

    public ItemStack displayStack() {
        return getPickupItemStackOrigin();
    }

    @Override
    public void tick() {
        if (inGroundTime > 4) {
            Struck = true;
        }
        super.tick();
        if (level().isClientSide && !inGround && tickCount % 2 == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
        }
    }

    @Nullable
    @Override
    protected EntityHitResult findHitEntity(Vec3 Start, Vec3 End) {
        return Struck ? null : super.findHitEntity(Start, End);
    }

    @Override
    protected void onHitEntity(EntityHitResult Result) {
        Entity Target = Result.getEntity();
        Entity Thrower = getOwner();
        DamageSource Source = damageSources().source(DaeviteRuinsRegistry.SpearDamage, this, Thrower == null ? this : Thrower);
        Struck = true;
        if (Target.hurt(Source, Damage) && Target instanceof LivingEntity Body) {
            doKnockback(Body, Source);
            doPostHurtEffects(Body);
        }
        setDeltaMovement(getDeltaMovement().multiply(-0.01, -0.1, -0.01));
        playSound(DaeviteRuinsRegistry.SpearHit.get(), 1.0f, 1.0f);
        if (level() instanceof ServerLevel World) {
            World.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 6, 0.1, 0.1, 0.1, 0.1);
        }
    }

    @Override
    protected boolean tryPickup(Player User) {
        return super.tryPickup(User) || isNoPhysics() && ownedBy(User) && User.getInventory().add(getPickupItem());
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return SoundEvents.TRIDENT_HIT_GROUND;
    }

    @Override
    public void playerTouch(Player User) {
        if (ownedBy(User) || getOwner() == null) {
            super.playerTouch(User);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        Struck = Saved.getBoolean("Struck");
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        Saved.putBoolean("Struck", Struck);
    }

    @Override
    protected float getWaterInertia() {
        return 0.9f;
    }

    @Override
    public boolean shouldRender(double X, double Y, double Z) {
        return true;
    }
}
