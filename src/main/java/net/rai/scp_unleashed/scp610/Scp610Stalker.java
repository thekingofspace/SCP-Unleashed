package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class Scp610Stalker extends Scp610Unit {
    public static final int StillTicks = 20;
    public static final double PounceReach = 6.0;
    public static final double PounceSpeed = 0.9;
    public static final double PounceLift = 0.42;
    public static final int PounceGap = 60;
    private static final EntityDataAccessor<Boolean> HiddenData = SynchedEntityData.defineId(Scp610Stalker.class, EntityDataSerializers.BOOLEAN);
    private int Still;
    private int PounceReady;

    public Scp610Stalker(EntityType<? extends Scp610Stalker> Type, Level World) {
        super(Type, World, Scp610Form.Stalker);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(HiddenData, false);
    }

    public boolean hidden() {
        return entityData.get(HiddenData);
    }

    public boolean onFlesh() {
        BlockPos Below = BlockPos.containing(getX(), getY() - 0.2, getZ());
        return Scp610Growth.isFlesh(level().getBlockState(Below));
    }

    public int stillTicks() {
        return Still;
    }

    public void settle(int Ticks) {
        Still = Ticks;
    }

    public boolean pounce(LivingEntity Target) {
        if (PounceReady > 0 || !canAct()) {
            return false;
        }
        Vec3 Toward = Target.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        if (Toward.lengthSqr() < 1.0e-4) {
            return false;
        }
        Vec3 Leap = Toward.normalize().scale(PounceSpeed);
        setDeltaMovement(Leap.x, PounceLift, Leap.z);
        hasImpulse = true;
        faceToward(Target, 90.0f);
        PounceReady = PounceGap;
        Still = 0;
        playLayers(voice().Attack(), 1.0f, voice().Pitch());
        return true;
    }

    @Override
    protected void serverTick() {
        super.serverTick();
        if (!(level() instanceof ServerLevel)) {
            return;
        }
        PounceReady = Math.max(0, PounceReady - 1);
        double Moved = (getX() - xo) * (getX() - xo) + (getZ() - zo) * (getZ() - zo);
        if (Moved < 1.0e-4 && !isActing()) {
            Still++;
        } else {
            Still = 0;
        }
        boolean Hide = Still >= StillTicks && onFlesh();
        if (Hide != hidden()) {
            entityData.set(HiddenData, Hide);
        }
        LivingEntity Target = getTarget();
        if (Hide && Target != null && Target.isAlive() && distanceTo(Target) <= PounceReach && getSensing().hasLineOfSight(Target)) {
            pounce(Target);
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt) {
            Still = 0;
        }
        return Hurt;
    }
}
