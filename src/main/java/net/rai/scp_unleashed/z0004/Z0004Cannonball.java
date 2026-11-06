package net.rai.scp_unleashed.z0004;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
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
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Z0004Cannonball extends Projectile implements GeoEntity {
    public static final double Gravity = 0.05;
    public static final double AirDrag = 0.995;
    public static final double WaterDrag = 0.7;
    public static final float DirectDamage = 8.0f;
    public static final float SplashDamage = 5.0f;
    public static final double SplashRadius = 2.5;
    public static final double Knockback = 0.6;
    public static final int MaxLife = 200;
    public static final int WaterLife = 12;
    public static final RawAnimation SpinLoop = RawAnimation.begin().thenLoop("spin");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int Life;
    private int Wet;

    public Z0004Cannonball(EntityType<? extends Z0004Cannonball> Type, Level World) {
        super(Type, World);
    }

    public Z0004Cannonball(Level World, Entity Shooter, Vec3 From, Vec3 Velocity) {
        this(Z0004Registry.Cannonball.get(), World);
        setOwner(Shooter);
        setPos(From.x, From.y, From.z);
        setDeltaMovement(Velocity);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
    }

    @Override
    public void tick() {
        super.tick();
        Life++;
        Vec3 Motion = getDeltaMovement();
        if (isInWater()) {
            if (Wet == 0 && level() instanceof ServerLevel World) {
                World.sendParticles(ParticleTypes.SPLASH, getX(), getY() + 0.5, getZ(), 40, 0.6, 0.3, 0.6, 0.2);
                World.sendParticles(ParticleTypes.BUBBLE, getX(), getY(), getZ(), 20, 0.4, 0.4, 0.4, 0.1);
                World.playSound(null, getX(), getY(), getZ(), Z0004Registry.Splash.get(), SoundSource.HOSTILE, 2.0f, 0.8f + random.nextFloat() * 0.2f);
            }
            Wet++;
            Motion = Motion.scale(WaterDrag);
            if (!level().isClientSide && Wet > WaterLife) {
                discard();
                return;
            }
        }
        HitResult Hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (Hit.getType() != HitResult.Type.MISS) {
            hitTargetOrDeflectSelf(Hit);
            if (isRemoved()) {
                return;
            }
        }
        Vec3 Next = position().add(Motion);
        if (level().isClientSide && Wet == 0) {
            level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.25, getZ(), 0.0, 0.0, 0.0);
        }
        setPos(Next.x, Next.y, Next.z);
        setDeltaMovement(Motion.scale(Wet > 0 ? 1.0 : AirDrag).add(0.0, -Gravity, 0.0));
        if (!level().isClientSide && Life > MaxLife) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity Target) {
        return super.canHitEntity(Target) && !Z0004Fight.fleet(Target) && !(Target instanceof Z0004ShipPart);
    }

    @Override
    protected void onHitEntity(EntityHitResult Result) {
        if (level() instanceof ServerLevel World) {
            Entity Target = Result.getEntity();
            Target.hurt(source(World), DirectDamage);
            burst(World, Result.getLocation(), Target);
        }
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult Result) {
        super.onHitBlock(Result);
        if (level() instanceof ServerLevel World) {
            burst(World, Result.getLocation(), null);
        }
        discard();
    }

    public DamageSource source(ServerLevel World) {
        return World.damageSources().mobProjectile(this, getOwner() instanceof LivingEntity Living ? Living : null);
    }

    private void burst(ServerLevel World, Vec3 Where, Entity Struck) {
        World.sendParticles(ParticleTypes.EXPLOSION, Where.x, Where.y, Where.z, 2, 0.2, 0.2, 0.2, 0.0);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, Where.x, Where.y, Where.z, 12, 0.6, 0.4, 0.6, 0.03);
        World.playSound(null, Where.x, Where.y, Where.z, Z0004Registry.Impact.get(), SoundSource.HOSTILE, 2.5f, 0.9f + random.nextFloat() * 0.2f);
        for (LivingEntity Near : World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().move(Where.subtract(position())).inflate(SplashRadius),
                Other -> Other != Struck && !Z0004Fight.fleet(Other))) {
            double Distance = Near.position().distanceTo(Where);
            if (Distance > SplashRadius) {
                continue;
            }
            float Share = (float) (1.0 - Distance / SplashRadius);
            if (Near.hurt(source(World), SplashDamage * Share)) {
                Vec3 Push = Near.position().subtract(Where).normalize().scale(Knockback * Share);
                Near.push(Push.x, 0.2 * Share, Push.z);
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "spin", 0, State -> State.setAndContinue(SpinLoop)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
