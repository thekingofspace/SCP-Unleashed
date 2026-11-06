package net.rai.scp_unleashed.z0004;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
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
import net.rai.scp_unleashed.guns.GunDamage;
import net.rai.scp_unleashed.guns.GunKind;

public class Z0004Shot extends Projectile {
    public static final double BallSpeed = 2.4;
    public static final double PelletSpeed = 2.0;
    public static final double AirDrag = 0.99;
    public static final double WaterDrag = 0.62;
    public static final double Gravity = 0.012;
    public static final double Spent = 0.22;
    public static final int AirLife = 24;
    public static final int PelletLife = 10;
    private float Damage = 5.0f;
    private boolean Pellet;
    private int Life;

    public Z0004Shot(EntityType<? extends Z0004Shot> Type, Level World) {
        super(Type, World);
    }

    public Z0004Shot(Level World, LivingEntity Shooter, Vec3 From, Vec3 Velocity, float Damage, boolean Pellet) {
        this(Z0004Registry.Shot.get(), World);
        setOwner(Shooter);
        setPos(From.x, From.y, From.z);
        setDeltaMovement(Velocity);
        this.Damage = Damage;
        this.Pellet = Pellet;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
    }

    @Override
    public void tick() {
        super.tick();
        Life++;
        boolean Water = isInWater();
        Vec3 Motion = getDeltaMovement();
        HitResult Hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
        if (Hit.getType() != HitResult.Type.MISS) {
            hitTargetOrDeflectSelf(Hit);
            if (isRemoved()) {
                return;
            }
        }
        Vec3 Next = position().add(Motion);
        if (level().isClientSide) {
            if (Water) {
                level().addParticle(ParticleTypes.BUBBLE, getX(), getY(), getZ(), 0.0, 0.02, 0.0);
                level().addParticle(ParticleTypes.BUBBLE, (getX() + Next.x) * 0.5, (getY() + Next.y) * 0.5, (getZ() + Next.z) * 0.5, 0.0, 0.02, 0.0);
            } else {
                level().addParticle(ParticleTypes.SMOKE, getX(), getY(), getZ(), 0.0, 0.0, 0.0);
            }
        }
        setPos(Next.x, Next.y, Next.z);
        Motion = Water ? Motion.scale(WaterDrag) : Motion.scale(AirDrag).add(0.0, -Gravity, 0.0);
        setDeltaMovement(Motion);
        if (!level().isClientSide && (Motion.length() < Spent || Life > (Pellet ? PelletLife : AirLife))) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity Target) {
        return super.canHitEntity(Target) && !Z0004Fight.fleet(Target) && !(Target instanceof Z0004ShipPart);
    }

    @Override
    protected void onHitEntity(EntityHitResult Result) {
        if (level() instanceof ServerLevel World && getOwner() != null) {
            Entity Target = Result.getEntity();
            GunDamage.hurt(World, getOwner(), Target, Pellet ? GunKind.DoubleBarrel : GunKind.Revolver, Damage, getDeltaMovement().normalize());
        }
        discard();
    }

    @Override
    protected void onHitBlock(BlockHitResult Result) {
        super.onHitBlock(Result);
        if (level() instanceof ServerLevel World) {
            Vec3 Where = Result.getLocation();
            World.sendParticles(isInWater() ? ParticleTypes.BUBBLE : ParticleTypes.SMOKE, Where.x, Where.y, Where.z, 4, 0.05, 0.05, 0.05, 0.02);
        }
        discard();
    }

    public DamageSource source(ServerLevel World) {
        return GunDamage.sourceFor(World, getOwner() == null ? this : getOwner(), Pellet ? GunKind.DoubleBarrel : GunKind.Revolver);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putFloat("Damage", Damage);
        Tag.putBoolean("Pellet", Pellet);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Damage = Tag.getFloat("Damage");
        Pellet = Tag.getBoolean("Pellet");
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }
}
