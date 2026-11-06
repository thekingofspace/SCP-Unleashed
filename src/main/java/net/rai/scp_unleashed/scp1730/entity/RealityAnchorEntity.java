package net.rai.scp_unleashed.scp1730.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp1730.Scp1730Anchors;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.network.Scp1730PulsePayload;
import net.rai.scp_unleashed.scp1730.network.Scp1730ShakePayload;

public class RealityAnchorEntity extends ThrowableItemProjectile {
    public static final int MaxFlight = 80;
    public static final float LobPitch = -4.0f;
    public static final double PulseHeard = 64.0;
    public static final double ShakeReach = 3.0;
    public static final float ShakePeak = 0.45f;
    public static final int ShakeTicks = 10;

    public RealityAnchorEntity(EntityType<? extends RealityAnchorEntity> Type, Level World) {
        super(Type, World);
    }

    public static RealityAnchorEntity thrown(ServerLevel World, LivingEntity Thrower, float Speed) {
        RealityAnchorEntity Charge = new RealityAnchorEntity(Scp1730Registry.AnchorType.get(), World);
        Charge.setOwner(Thrower);
        Vec3 Eye = Thrower.getEyePosition();
        Charge.setPos(Eye.x, Eye.y - 0.15, Eye.z);
        Charge.shootFromRotation(Thrower, Thrower.getXRot(), Thrower.getYRot(), LobPitch, Speed, 1.0f);
        World.addFreshEntity(Charge);
        return Charge;
    }

    public static RealityAnchorEntity dispensed(Level World, double X, double Y, double Z) {
        RealityAnchorEntity Charge = new RealityAnchorEntity(Scp1730Registry.AnchorType.get(), World);
        Charge.setPos(X, Y, Z);
        return Charge;
    }

    @Override
    protected Item getDefaultItem() {
        return Scp1730Registry.Anchor.get();
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide && tickCount > 1) {
            Vec3 Motion = getDeltaMovement();
            if (random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.ELECTRIC_SPARK, getX() - Motion.x * 0.5, getY() + 0.1 - Motion.y * 0.5, getZ() - Motion.z * 0.5,
                        (random.nextDouble() - 0.5) * 0.04, (random.nextDouble() - 0.5) * 0.04, (random.nextDouble() - 0.5) * 0.04);
            }
            if (tickCount % 2 == 0) {
                level().addParticle(Scp1730Registry.AnchorParticle.get(), getX(), getY() + 0.1, getZ(), 0.0, 0.0, 0.0);
            }
        }
        if (!level().isClientSide && tickCount > MaxFlight && isAlive()) {
            deploy(position());
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult Hit) {
        super.onHitBlock(Hit);
        Vec3 Back = Hit.getLocation().add(Vec3.atLowerCornerOf(Hit.getDirection().getNormal()).scale(0.3));
        deploy(Back);
    }

    @Override
    protected void onHitEntity(EntityHitResult Hit) {
        super.onHitEntity(Hit);
        deploy(Hit.getEntity().getBoundingBox().getCenter());
    }

    @Override
    protected void onHit(HitResult Hit) {
        super.onHit(Hit);
    }

    public void deploy(Vec3 At) {
        if (level().isClientSide || !isAlive()) {
            return;
        }
        ServerLevel World = (ServerLevel) level();
        RealityAnchorField.spawn(World, At);
        burst(World, At, Scp1730Anchors.GrenadeRadius);
        discard();
    }

    public static void burst(ServerLevel World, Vec3 At, double Radius) {
        World.playSound(null, At.x, At.y, At.z, Scp1730Registry.AnchorBurst.get(), SoundSource.PLAYERS, 1.6f, 1.0f);
        World.playSound(null, At.x, At.y, At.z, Scp1730Registry.AnchorBoom.get(), SoundSource.PLAYERS, 1.4f, 1.0f);
        World.playSound(null, At.x, At.y, At.z, Scp1730Registry.AnchorThump.get(), SoundSource.PLAYERS, 1.2f, 1.0f);
        World.playSound(null, At.x, At.y, At.z, Scp1730Registry.AnchorRing.get(), SoundSource.PLAYERS, 0.9f, 1.0f);
        PacketDistributor.sendToPlayersNear(World, null, At.x, At.y, At.z, PulseHeard, new Scp1730PulsePayload(At.x, At.y, At.z, (float) Radius));
        for (ServerPlayer Each : World.players()) {
            double Distance = Each.position().distanceTo(At);
            double Reach = Radius * ShakeReach;
            if (Distance < Reach) {
                PacketDistributor.sendToPlayer(Each, new Scp1730ShakePayload((float) (ShakePeak * (1.0 - Distance / Reach)), ShakeTicks));
            }
        }
        for (Scp1730Leech Each : World.getEntitiesOfClass(Scp1730Leech.class, new net.minecraft.world.phys.AABB(At, At).inflate(Radius), Scp1730Leech::latched)) {
            Each.unlatch();
            Vec3 Away = Each.position().subtract(At).multiply(1.0, 0.0, 1.0);
            Away = Away.lengthSqr() < 1.0e-4 ? new Vec3(0.0, 0.0, 0.0) : Away.normalize().scale(0.5);
            Each.setDeltaMovement(Away.x, 0.35, Away.z);
            Each.hurtMarked = true;
        }
    }
}
