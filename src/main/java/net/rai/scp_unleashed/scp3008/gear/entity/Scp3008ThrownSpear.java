package net.rai.scp_unleashed.scp3008.gear.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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
import net.rai.scp_unleashed.scp3008.gear.Scp3008GearRegistry;
import org.jetbrains.annotations.Nullable;

public class Scp3008ThrownSpear extends AbstractArrow {
    public static final float Damage = 7.0f;
    private boolean Struck;

    public Scp3008ThrownSpear(EntityType<? extends Scp3008ThrownSpear> Type, Level World) {
        super(Type, World);
    }

    public Scp3008ThrownSpear(Level World, LivingEntity Thrower, ItemStack Stack) {
        super(Scp3008GearRegistry.ThrownSpear.get(), Thrower, World, Stack, null);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(Scp3008GearRegistry.Spear.get());
    }

    public ItemStack displayStack() {
        return getPickupItemStackOrigin();
    }

    public boolean struck() {
        return Struck;
    }

    @Override
    public void tick() {
        if (inGroundTime > 4) {
            Struck = true;
        }
        super.tick();
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
        DamageSource Source = damageSources().trident(this, Thrower == null ? this : Thrower);
        Struck = true;
        if (Target.hurt(Source, Damage) && Target instanceof LivingEntity Body) {
            doKnockback(Body, Source);
            doPostHurtEffects(Body);
        }
        setDeltaMovement(getDeltaMovement().multiply(-0.01, -0.1, -0.01));
        playSound(Scp3008GearRegistry.SpearHit.get(), 1.0f, 1.0f);
        if (level() instanceof ServerLevel World) {
            World.sendParticles(ParticleTypes.CRIT, getX(), getY(), getZ(), 5, 0.1, 0.1, 0.1, 0.1);
        }
    }

    @Override
    protected boolean tryPickup(Player User) {
        return super.tryPickup(User) || isNoPhysics() && ownedBy(User) && User.getInventory().add(getPickupItem());
    }

    @Override
    protected SoundEvent getDefaultHitGroundSoundEvent() {
        return Scp3008GearRegistry.SpearLand.get();
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
        return 0.85f;
    }

    @Override
    public boolean shouldRender(double X, double Y, double Z) {
        return true;
    }
}
