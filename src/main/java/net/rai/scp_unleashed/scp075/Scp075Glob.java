package net.rai.scp_unleashed.scp075;

import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.Targeting;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public class Scp075Glob extends Projectile implements GeoEntity {
    public static final float Size = 0.3f;
    public static final double Gravity = 0.04;
    public static final float Damage = 2.0f;
    public static final int FlightBase = 8;
    public static final double FlightPerBlock = 1.5;
    public static final int FlightMin = 8;
    public static final int FlightMax = 30;
    public static final double LeadShare = 0.6;
    public static final double Spread = 0.35;
    public static final int MaxAge = 100;
    public static final int TrailCount = 3;
    public static final int SplatCount = 14;
    private static final RawAnimation Wobble = RawAnimation.begin().thenLoop("wobble");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int Flown;

    public Scp075Glob(EntityType<? extends Scp075Glob> Type, Level World) {
        super(Type, World);
    }

    public static void fire(ServerLevel World, Scp075Snail Shooter, LivingEntity Target) {
        Scp075Glob Glob = Scp075Registry.Glob.get().create(World);
        if (Glob == null) {
            return;
        }
        RandomSource Random = Shooter.getRandom();
        Vec3 Mouth = Shooter.mouth(Scp075Snail.SpitMouthForward, Scp075Snail.SpitMouthUp);
        Vec3 Aim = Target.position().add(0.0, Target.getBbHeight() * 0.55, 0.0);
        double Distance = Math.sqrt(Mth.square(Aim.x - Mouth.x) + Mth.square(Aim.z - Mouth.z));
        int Flight = Mth.clamp((int) Math.round(FlightBase + Distance * FlightPerBlock), FlightMin, FlightMax);
        Vec3 Drift = new Vec3(Target.getX() - Target.xo, 0.0, Target.getZ() - Target.zo);
        double Scatter = Spread * Distance / 10.0;
        Aim = Aim.add(Drift.scale(Flight * LeadShare)).add((Random.nextDouble() - 0.5) * 2.0 * Scatter, (Random.nextDouble() - 0.5) * Scatter * 0.5, (Random.nextDouble() - 0.5) * 2.0 * Scatter);
        double Rise = (Aim.y - Mouth.y + Gravity * Flight * (Flight - 1) / 2.0) / Flight;
        Glob.setOwner(Shooter);
        Glob.moveTo(Mouth.x, Mouth.y, Mouth.z, Shooter.getYRot(), 0.0f);
        Glob.setDeltaMovement((Aim.x - Mouth.x) / Flight, Rise, (Aim.z - Mouth.z) / Flight);
        World.addFreshEntity(Glob);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
    }

    @Override
    protected double getDefaultGravity() {
        return Gravity;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 Motion = getDeltaMovement();
        HitResult Hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (Hit.getType() != HitResult.Type.MISS && !EventHooks.onProjectileImpact(this, Hit)) {
            hitTargetOrDeflectSelf(Hit);
        }
        if (isRemoved()) {
            return;
        }
        double X = getX() + Motion.x;
        double Y = getY() + Motion.y;
        double Z = getZ() + Motion.z;
        updateRotation();
        if (isInWaterOrBubble()) {
            dissolve();
            return;
        }
        setDeltaMovement(Motion.x, Motion.y - Gravity, Motion.z);
        setPos(X, Y, Z);
        if (level().isClientSide) {
            trail();
        } else if (++Flown > MaxAge) {
            discard();
        }
    }

    private void trail() {
        RandomSource Random = getRandom();
        for (int Index = 0; Index < TrailCount; Index++) {
            level().addParticle(Scp075Registry.Drip.get(), getX() + (Random.nextDouble() - 0.5) * 0.15, getY() + Size * 0.5 + (Random.nextDouble() - 0.5) * 0.15,
                    getZ() + (Random.nextDouble() - 0.5) * 0.15, (Random.nextDouble() - 0.5) * 0.02, 0.0, (Random.nextDouble() - 0.5) * 0.02);
        }
    }

    @Override
    protected boolean canHitEntity(Entity Target) {
        return !(Target instanceof Scp075Snail) && !(Target instanceof Scp075Glob) && super.canHitEntity(Target);
    }

    @Override
    protected void onHitEntity(EntityHitResult Result) {
        super.onHitEntity(Result);
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        Entity Struck = Result.getEntity();
        LivingEntity Owner = getOwner() instanceof LivingEntity Living ? Living : null;
        DamageSource Source = damageSources().source(Scp075Registry.SpitDamage, this, Owner);
        if (Struck instanceof LivingEntity Living) {
            boolean Shielded = Living.isDamageSourceBlocked(Source);
            Living.hurt(Source, Damage);
            if (!Shielded && !Living.isInvulnerableTo(Source) && Targeting.isTargetable(Living)) {
                Scp075Corrosion.afflict(Living, Owner);
            }
        } else {
            Struck.hurt(Source, Damage);
        }
        splat(World, position());
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult Result) {
        super.onHitBlock(Result);
        if (level() instanceof ServerLevel World) {
            splat(World, Result.getLocation());
            discard();
        }
    }

    private void dissolve() {
        if (level() instanceof ServerLevel World) {
            World.sendParticles(Scp075Registry.Drip.get(), getX(), getY(), getZ(), 5, 0.1, 0.1, 0.1, 0.01);
            playAt(World, position(), Scp075Registry.SizzleLayers, 0.4f, 1.3f);
            discard();
        }
    }

    public static void splat(ServerLevel World, Vec3 Spot) {
        World.sendParticles(Scp075Registry.Drip.get(), Spot.x, Spot.y + 0.1, Spot.z, SplatCount, 0.18, 0.08, 0.18, 0.09);
        playAt(World, Spot, Scp075Registry.SplatLayers, 0.9f, 0.9f + World.getRandom().nextFloat() * 0.2f);
    }

    public static void playAt(ServerLevel World, Vec3 Spot, List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            World.playSound(null, Spot.x, Spot.y, Spot.z, Layer.get(), SoundSource.HOSTILE, Volume, Pitch);
        }
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket Packet) {
        super.recreateFromPacket(Packet);
        setDeltaMovement(Packet.getXa(), Packet.getYa(), Packet.getZa());
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "glob", 0, Event -> Event.setAndContinue(Wobble)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
