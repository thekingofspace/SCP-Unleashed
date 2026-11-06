package net.rai.scp_unleashed.scp1730.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp1730.Scp1730Anchors;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.block.Scp1730CrackBlock;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Scp1730Tentacle extends Entity implements GeoEntity {
    public static final float Width = 1.4f;
    public static final float Height = 1.4f;
    public static final double Reach = 9.6;
    public static final double HitRadius = 1.3;
    public static final float Damage = 9.0f;
    public static final byte PhaseEmerge = 0;
    public static final byte PhaseStrike = 1;
    public static final byte PhaseSlam = 2;
    public static final byte PhaseWithdraw = 3;
    public static final int EmergeTicks = 16;
    public static final int StrikeTicks = 9;
    public static final int StrikeContact = 6;
    public static final int SlamTicks = 14;
    public static final int SlamContact = 9;
    public static final int WithdrawTicks = 14;
    public static final double AimCone = 50.0;
    private static final EntityDataAccessor<Byte> DataPhase = SynchedEntityData.defineId(Scp1730Tentacle.class, EntityDataSerializers.BYTE);
    private static final RawAnimation Emerge = RawAnimation.begin().thenPlayAndHold("emerge");
    private static final RawAnimation Strike = RawAnimation.begin().thenPlayAndHold("strike");
    private static final RawAnimation Slam = RawAnimation.begin().thenPlayAndHold("slam");
    private static final RawAnimation Withdraw = RawAnimation.begin().thenPlayAndHold("withdraw");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int PhaseTicks;
    private int TargetId = -1;
    private BlockPos Crack = BlockPos.ZERO;
    private Direction Side = Direction.NORTH;

    public Scp1730Tentacle(EntityType<? extends Scp1730Tentacle> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
    }

    public static Scp1730Tentacle emerge(ServerLevel World, BlockPos Crack, Direction Side, LivingEntity Target) {
        Scp1730Tentacle Made = new Scp1730Tentacle(Scp1730Registry.TentacleType.get(), World);
        Vec3 Mouth = Scp1730CrackBlock.mouth(Crack, Side);
        Made.Crack = Crack.immutable();
        Made.Side = Side;
        Made.TargetId = Target.getId();
        Made.moveTo(Mouth.x, Mouth.y - Height / 2.0, Mouth.z, Side.toYRot(), 0.0f);
        Made.aimAt(Target.getEyePosition());
        World.addFreshEntity(Made);
        World.playSound(null, Mouth.x, Mouth.y, Mouth.z, Scp1730Registry.TentacleRumble.get(), SoundSource.HOSTILE, 1.6f, 0.7f);
        return Made;
    }

    public Vec3 mouth() {
        return position().add(0.0, Height / 2.0, 0.0);
    }

    public void aimAt(Vec3 Point) {
        Vec3 Way = Point.subtract(mouth());
        double Flat = Math.sqrt(Way.x * Way.x + Way.z * Way.z);
        float Yaw = (float) (Mth.atan2(Way.z, Way.x) * Mth.RAD_TO_DEG) - 90.0f;
        float Base = Side.toYRot();
        Yaw = Base + Mth.clamp(Mth.wrapDegrees(Yaw - Base), (float) -AimCone, (float) AimCone);
        float Pitch = (float) -(Mth.atan2(Way.y, Flat) * Mth.RAD_TO_DEG);
        setYRot(Yaw);
        setXRot(Mth.clamp(Pitch, -60.0f, 60.0f));
        yRotO = getYRot();
        xRotO = getXRot();
    }

    public Vec3 direction() {
        return Vec3.directionFromRotation(getXRot(), getYRot());
    }

    public byte phase() {
        return entityData.get(DataPhase);
    }

    public int phaseTicks() {
        return PhaseTicks;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(DataPhase, PhaseEmerge);
    }

    private void setPhase(byte Phase) {
        entityData.set(DataPhase, Phase);
        PhaseTicks = 0;
    }

    public void repel() {
        if (phase() != PhaseWithdraw) {
            setPhase(PhaseWithdraw);
            level().playSound(null, getX(), getY(), getZ(), Scp1730Registry.TentacleWithdraw.get(), SoundSource.HOSTILE, 1.2f, 1.0f);
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (!level().isClientSide && Source.getEntity() != null) {
            repel();
        }
        return true;
    }

    @Override
    public boolean isPickable() {
        return phase() != PhaseWithdraw;
    }

    @Override
    public boolean isAttackable() {
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        PhaseTicks++;
        if (level().isClientSide) {
            if (phase() == PhaseEmerge) {
                Vec3 At = mouth();
                level().addParticle(ParticleTypes.SQUID_INK, At.x + (random.nextDouble() - 0.5), At.y + (random.nextDouble() - 0.5), At.z + (random.nextDouble() - 0.5), 0.0, 0.02, 0.0);
            }
            return;
        }
        ServerLevel World = (ServerLevel) level();
        if (Scp1730Anchors.anchored(World, mouth()) && phase() != PhaseWithdraw) {
            repel();
        }
        switch (phase()) {
            case PhaseEmerge -> {
                if (PhaseTicks == EmergeTicks - 3) {
                    Entity Target = World.getEntity(TargetId);
                    if (Target != null) {
                        aimAt(Target.getEyePosition().add(0.0, -0.4, 0.0));
                    }
                }
                if (PhaseTicks >= EmergeTicks) {
                    setPhase(random.nextFloat() < 0.35f ? PhaseSlam : PhaseStrike);
                    World.playSound(null, getX(), getY(), getZ(), Scp1730Registry.TentacleStrike.get(), SoundSource.HOSTILE, 1.6f, 0.8f);
                }
            }
            case PhaseStrike -> {
                if (PhaseTicks == StrikeContact) {
                    contact(World, false);
                }
                if (PhaseTicks >= StrikeTicks) {
                    repel();
                }
            }
            case PhaseSlam -> {
                if (PhaseTicks == SlamContact) {
                    contact(World, true);
                }
                if (PhaseTicks >= SlamTicks) {
                    repel();
                }
            }
            default -> {
                if (PhaseTicks >= WithdrawTicks) {
                    discard();
                }
            }
        }
    }

    public static double segmentDistance(Vec3 Point, Vec3 Start, Vec3 End) {
        Vec3 Line = End.subtract(Start);
        double Length = Line.lengthSqr();
        double Share = Length < 1.0e-9 ? 0.0 : Mth.clamp(Point.subtract(Start).dot(Line) / Length, 0.0, 1.0);
        return Point.distanceTo(Start.add(Line.scale(Share)));
    }

    private void contact(ServerLevel World, boolean Slamming) {
        Vec3 Start = mouth();
        Vec3 Way = direction();
        Vec3 End = Start.add(Way.scale(Reach));
        DamageSource Hurt = new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Scp1730Registry.TentacleDamage), this);
        AABB Span = new AABB(Start, End).inflate(HitRadius + 1.0);
        for (LivingEntity Each : World.getEntitiesOfClass(LivingEntity.class, Span, LivingEntity::isAlive)) {
            if (Each instanceof Player User && (User.isCreative() || User.isSpectator())) {
                continue;
            }
            Vec3 Body = Each.position().add(0.0, Each.getBbHeight() * 0.5, 0.0);
            if (segmentDistance(Body, Start, End) > HitRadius + Each.getBbWidth() * 0.5) {
                continue;
            }
            if (Each.hurt(Hurt, Damage)) {
                if (Slamming) {
                    Each.setDeltaMovement(Each.getDeltaMovement().add(0.0, -0.6, 0.0));
                } else {
                    Each.setDeltaMovement(Each.getDeltaMovement().add(Way.x * 1.1, 0.35, Way.z * 1.1));
                }
                Each.hurtMarked = true;
            }
        }
        World.sendParticles(ParticleTypes.SQUID_INK, End.x, End.y, End.z, 12, 0.5, 0.5, 0.5, 0.05);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
        discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "main", 2, Animated -> switch (phase()) {
            case PhaseStrike -> Animated.setAndContinue(Strike);
            case PhaseSlam -> Animated.setAndContinue(Slam);
            case PhaseWithdraw -> Animated.setAndContinue(Withdraw);
            default -> Animated.setAndContinue(Emerge);
        }));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
