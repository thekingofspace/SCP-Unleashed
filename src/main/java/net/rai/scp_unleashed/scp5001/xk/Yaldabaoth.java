package net.rai.scp_unleashed.scp5001.xk;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp5001.core.Scp5001Duel;

public class Yaldabaoth extends Mob implements Scp5001Duel {
    public static final float Health = 1024.0f;

    public Yaldabaoth(EntityType<? extends Yaldabaoth> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
        setPersistenceRequired();
        noCulling = true;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 0.0);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (Source.is(DamageTypes.GENERIC_KILL)) {
            if (level() instanceof ServerLevel World) {
                Scp5001Xk.dismissed(World.getServer());
            }
            return super.hurt(Source, Amount);
        }
        if (level() instanceof ServerLevel World && Amount > 0.0f && (Source.getEntity() != null || Source.getDirectEntity() != null)) {
            YaldabaothVoice.hurt(World, position());
        }
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        return !Source.is(DamageTypes.GENERIC_KILL);
    }

    @Override
    public void tick() {
        super.tick();
        setDeltaMovement(Vec3.ZERO);
        setHealth(getMaxHealth());
        clearFire();
    }

    @Override
    public void aiStep() {
    }

    @Override
    public void travel(Vec3 Movement) {
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public void checkDespawn() {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity Other) {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canChangeDimensions(Level From, Level To) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return true;
    }

    public void follow(Vec3 At, Vec3 Facing) {
        float Yaw = (float) (Math.atan2(-Facing.x, Facing.z) * (180.0 / Math.PI));
        float Pitch = (float) (-Math.asin(Math.max(-1.0, Math.min(1.0, Facing.y))) * (180.0 / Math.PI));
        moveTo(At.x, At.y - getBbHeight() * 0.5, At.z, Yaw, Pitch);
        setYHeadRot(Yaw);
        setYBodyRot(Yaw);
    }

    private YaldabaothFlight flight() {
        if (level() instanceof ServerLevel World) {
            return XkState.get(World.getServer()).flight();
        }
        return null;
    }

    @Override
    public Vec3 duelHead() {
        YaldabaothFlight Flight = flight();
        return Flight == null ? position() : Flight.head();
    }

    @Override
    public void duelEngage(Entity Opponent, Vec3 Arena) {
        YaldabaothFlight Flight = flight();
        if (Flight != null) {
            Flight.engage(Opponent == null ? -1 : Opponent.getId(), Arena);
        }
    }

    @Override
    public void duelAttack(int Move, Vec3 Target) {
        YaldabaothFlight Flight = flight();
        if (Flight != null) {
            Flight.attack(Move, Target);
        }
    }

    @Override
    public void duelReact(Vec3 From, float Power) {
        YaldabaothFlight Flight = flight();
        if (Flight != null) {
            Flight.react(From, Power);
        }
    }

    @Override
    public void duelBind(Vec3 Portal, float Progress) {
        YaldabaothFlight Flight = flight();
        if (Flight != null) {
            Flight.bind(Portal, Progress);
        }
    }

    @Override
    public void duelBanish() {
        YaldabaothFlight Flight = flight();
        if (Flight != null) {
            Flight.banish();
        }
    }
}
