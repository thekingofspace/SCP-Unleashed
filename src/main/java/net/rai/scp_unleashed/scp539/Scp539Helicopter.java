package net.rai.scp_unleashed.scp539;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.rai.scp_unleashed.scp539.network.Scp539WarnPayload;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public class Scp539Helicopter extends Entity implements GeoEntity {
    public static final int Flight = 56;
    public static final double Easing = 1.2;
    public static final double WarnRadius = 7.0;
    public static final int WarnEvery = 4;
    public static final int WarnLinger = 12;
    public static final float HitDamage = 8.0f;
    public static final double HitReach = 0.4;
    public static final double HitPush = 1.6;
    public static final double HitLift = 0.75;
    public static final float CrashPower = 3.0f;
    public static final int Overstay = 40;
    public static final float MaxPitch = 55.0f;
    public static final int ThumpEvery = 3;
    public static final float ThumpVolume = 1.4f;
    public static final float CrashVolume = 1.6f;
    public static final float BonkVolume = 1.0f;
    public static final RawAnimation Fly = RawAnimation.begin().thenLoop("fly");

    private static final EntityDataAccessor<BlockPos> OriginData = SynchedEntityData.defineId(Scp539Helicopter.class, EntityDataSerializers.BLOCK_POS);
    private static final EntityDataAccessor<Vector3f> StartData = SynchedEntityData.defineId(Scp539Helicopter.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> ControlData = SynchedEntityData.defineId(Scp539Helicopter.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Vector3f> EndData = SynchedEntityData.defineId(Scp539Helicopter.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Long> LaunchData = SynchedEntityData.defineId(Scp539Helicopter.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DurationData = SynchedEntityData.defineId(Scp539Helicopter.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> WallData = SynchedEntityData.defineId(Scp539Helicopter.class, EntityDataSerializers.BOOLEAN);

    private static Consumer<Scp539Helicopter> ClientHook = Craft -> {
    };

    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final Set<Integer> Struck = new HashSet<>();
    private final Set<UUID> Warned = new HashSet<>();
    @Nullable
    private UUID OwnerId;
    private Vec3 Blast = Vec3.ZERO;
    private boolean Crashed;
    private boolean Placed;

    public Scp539Helicopter(EntityType<? extends Scp539Helicopter> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
    }

    @Nullable
    public static Scp539Helicopter launch(ServerLevel World, Scp539Crash.Route Route, @Nullable UUID Owner, int Duration) {
        Scp539Helicopter Craft = Scp539Registry.HelicopterEntity.get().create(World);
        if (Craft == null) {
            return null;
        }
        BlockPos Origin = BlockPos.containing(Route.End());
        Vec3 Base = Vec3.atLowerCornerOf(Origin);
        Craft.entityData.set(OriginData, Origin);
        Craft.entityData.set(StartData, offset(Route.Start(), Base));
        Craft.entityData.set(ControlData, offset(Route.Control(), Base));
        Craft.entityData.set(EndData, offset(Route.End(), Base));
        Craft.entityData.set(LaunchData, World.getGameTime());
        Craft.entityData.set(DurationData, Math.max(1, Duration));
        Craft.entityData.set(WallData, Route.Wall());
        Craft.OwnerId = Owner;
        Craft.Blast = Route.Blast();
        Vec3 Start = Route.Start();
        Vec3 Ahead = Craft.pathAt(0.05f);
        Craft.moveTo(Start.x, Start.y, Start.z, yawToward(Ahead.subtract(Start)), pitchToward(Ahead.subtract(Start)));
        Craft.Placed = true;
        if (!World.addFreshEntity(Craft)) {
            return null;
        }
        if (Owner != null) {
            Scp539Plan.track(Owner, Craft.getUUID());
        }
        return Craft;
    }

    private static Vector3f offset(Vec3 Point, Vec3 Base) {
        return new Vector3f((float) (Point.x - Base.x), (float) (Point.y - Base.y), (float) (Point.z - Base.z));
    }

    public static void setClientHook(Consumer<Scp539Helicopter> Hook) {
        ClientHook = Hook;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(OriginData, BlockPos.ZERO);
        Builder.define(StartData, new Vector3f());
        Builder.define(ControlData, new Vector3f());
        Builder.define(EndData, new Vector3f());
        Builder.define(LaunchData, 0L);
        Builder.define(DurationData, Flight);
        Builder.define(WallData, false);
    }

    private Vec3 point(EntityDataAccessor<Vector3f> Accessor) {
        Vector3f Offset = entityData.get(Accessor);
        BlockPos Origin = entityData.get(OriginData);
        return new Vec3(Origin.getX() + Offset.x(), Origin.getY() + Offset.y(), Origin.getZ() + Offset.z());
    }

    public Vec3 start() {
        return point(StartData);
    }

    public Vec3 end() {
        return point(EndData);
    }

    public boolean isWall() {
        return entityData.get(WallData);
    }

    public int duration() {
        return Math.max(1, entityData.get(DurationData));
    }

    public long age() {
        return level().getGameTime() - entityData.get(LaunchData);
    }

    public float progress(float PartialTick) {
        return Mth.clamp((age() + PartialTick) / duration(), 0.0f, 1.0f);
    }

    public int ticksToImpact() {
        return (int) Math.max(0L, duration() - age());
    }

    public Vec3 pathAt(float Progress) {
        double T = Math.pow(Mth.clamp(Progress, 0.0f, 1.0f), Easing);
        Vec3 A = point(StartData);
        Vec3 B = point(ControlData);
        Vec3 C = point(EndData);
        double Rest = 1.0 - T;
        return A.scale(Rest * Rest).add(B.scale(2.0 * Rest * T)).add(C.scale(T * T));
    }

    public Vec3 travel(float Progress) {
        float Before = Math.max(0.0f, Progress - 0.02f);
        float After = Math.min(1.0f, Progress + 0.02f);
        Vec3 Direction = pathAt(After).subtract(pathAt(Before));
        return Direction.lengthSqr() < 1.0e-8 ? end().subtract(start()).normalize() : Direction.normalize();
    }

    public static float yawToward(Vec3 Direction) {
        return (float) (Mth.atan2(Direction.z, Direction.x) * Mth.RAD_TO_DEG) - 90.0f;
    }

    public static float pitchToward(Vec3 Direction) {
        double Across = Math.sqrt(Direction.x * Direction.x + Direction.z * Direction.z);
        return Mth.clamp((float) -(Mth.atan2(Direction.y, Across) * Mth.RAD_TO_DEG), -MaxPitch, MaxPitch);
    }

    public Set<UUID> warned() {
        return Warned;
    }

    public Set<Integer> struck() {
        return Struck;
    }

    public boolean hasCrashed() {
        return Crashed;
    }

    public Vec3 blastPoint() {
        return Blast;
    }

    @Nullable
    public UUID owner() {
        return OwnerId;
    }

    public static DamageSource damage(Level World, @Nullable Entity Source) {
        return new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Scp539Registry.HelicopterDamage), Source);
    }

    @Override
    public void tick() {
        super.tick();
        if (!level().isClientSide && !Placed) {
            discard();
            return;
        }
        float Progress = progress(0.0f);
        Vec3 Here = pathAt(Progress);
        Vec3 Heading = travel(Progress);
        setPos(Here.x, Here.y, Here.z);
        setYRot(yawToward(Heading));
        setXRot(pitchToward(Heading));
        if (level().isClientSide) {
            ClientHook.accept(this);
            if (tickCount % ThumpEvery == 0 && Progress < 1.0f) {
                level().playLocalSound(getX(), getY() + 2.0, getZ(), Scp539Registry.Thump.get(), SoundSource.HOSTILE, ThumpVolume, 0.8f + 0.45f * Progress, false);
            }
            return;
        }
        ServerLevel World = (ServerLevel) level();
        if (Crashed) {
            return;
        }
        if (age() % WarnEvery == 0) {
            warn(World);
        }
        strike(World, Heading);
        if (age() >= duration()) {
            crash(World);
        } else if (age() > duration() + Overstay || age() < -Overstay) {
            finish();
        }
    }

    public void warn(ServerLevel World) {
        Vec3 Target = end();
        int Left = ticksToImpact() + WarnLinger;
        double Reach = WarnRadius * WarnRadius;
        for (ServerPlayer User : World.players()) {
            if (User.isSpectator() || !User.isAlive() || User.distanceToSqr(Target) > Reach) {
                continue;
            }
            Warned.add(User.getUUID());
            PacketDistributor.sendToPlayer(User, new Scp539WarnPayload(Left));
        }
    }

    private void strike(ServerLevel World, Vec3 Heading) {
        AABB Swept = getBoundingBox().expandTowards(xo - getX(), yo - getY(), zo - getZ()).inflate(HitReach);
        for (LivingEntity Target : World.getEntitiesOfClass(LivingEntity.class, Swept, Living -> Living.isAlive() && !Living.isSpectator())) {
            if (Target instanceof Player Holder && Holder.getAbilities().invulnerable) {
                continue;
            }
            if (Struck.add(Target.getId())) {
                hit(World, Target, Heading);
            }
        }
    }

    public void hit(ServerLevel World, LivingEntity Target, Vec3 Heading) {
        Vec3 Away = Target.position().subtract(end());
        Away = new Vec3(Away.x, 0.0, Away.z);
        if (Away.lengthSqr() < 0.04) {
            Away = new Vec3(-Heading.x, 0.0, -Heading.z);
        }
        if (Away.lengthSqr() < 1.0e-6) {
            Away = new Vec3(1.0, 0.0, 0.0);
        }
        Away = Away.normalize();
        Target.hurt(damage(World, this), HitDamage);
        Target.push(Away.x * HitPush, HitLift, Away.z * HitPush);
        Target.hurtMarked = true;
        Scp539Registry.playLayers(World, Target.position(), Scp539Registry.BonkLayers, SoundSource.HOSTILE, BonkVolume, 0.9f + World.getRandom().nextFloat() * 0.15f);
    }

    public void crash(ServerLevel World) {
        if (Crashed) {
            return;
        }
        Crashed = true;
        warn(World);
        Vec3 At = Blast.lengthSqr() > 0.0 ? Blast : end();
        strike(World, travel(1.0f));
        World.explode(this, damage(World, this), null, At.x, At.y, At.z, CrashPower, false, Level.ExplosionInteraction.MOB);
        World.sendParticles(ParticleTypes.POOF, At.x, At.y + 0.5, At.z, 40, 1.2, 0.8, 1.2, 0.08);
        World.sendParticles(ParticleTypes.LARGE_SMOKE, At.x, At.y + 0.8, At.z, 24, 1.0, 0.6, 1.0, 0.04);
        World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.RED_CONCRETE)), At.x, At.y + 0.8, At.z, 30, 0.8, 0.6, 0.8, 0.35);
        World.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.IRON_INGOT)), At.x, At.y + 0.8, At.z, 18, 0.8, 0.6, 0.8, 0.35);
        Scp539Registry.playLayers(World, At, Scp539Registry.CrashLayers, SoundSource.HOSTILE, CrashVolume, 0.9f + World.getRandom().nextFloat() * 0.1f);
        finish();
    }

    private void finish() {
        Scp539Plan.release(OwnerId, getUUID());
        discard();
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < 160.0 * 160.0;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Saved) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Saved) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "flight", 0, Event -> Event.setAndContinue(Fly)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
