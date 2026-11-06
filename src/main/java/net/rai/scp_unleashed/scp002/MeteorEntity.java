package net.rai.scp_unleashed.scp002;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;
import java.util.function.Consumer;

public class MeteorEntity extends Entity {
    public static final int FlightTicks = 55;
    public static final int SmolderTicks = 1200;
    public static final double LaunchHeight = 135.0;
    public static final double LaunchReach = 105.0;
    public static final float Easing = 1.6f;
    public static final int PhaseFlight = 0;
    public static final int PhaseSmolder = 1;

    private static final EntityDataAccessor<Vector3f> DataStart = SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Long> DataLaunch = SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DataDuration = SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DataPhase = SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DataLanded = SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Float> DataRadius = SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DataSeed = SynchedEntityData.defineId(MeteorEntity.class, EntityDataSerializers.INT);

    private static Consumer<MeteorEntity> ClientHook = Meteor -> {
    };

    @Nullable
    private UUID StrikeId;
    private Direction DoorFront = Direction.SOUTH;
    private Vec3 LastHead;
    private boolean Sounding;
    private boolean Swooping;

    public MeteorEntity(EntityType<? extends MeteorEntity> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
    }

    @Nullable
    public static MeteorEntity launch(ServerLevel World, BlockPos Ground, Vec3 ApproachFrom, @Nullable UUID StrikeId, Direction DoorFront) {
        if (!Scp002Strikes.fallsIn(World)) {
            return null;
        }
        MeteorEntity Meteor = new MeteorEntity(Scp002Registry.Meteor.get(), World);
        RandomSource Random = World.random;
        Vec3 Impact = Vec3.atBottomCenterOf(Ground.above());
        double Dx = ApproachFrom.x - Impact.x;
        double Dz = ApproachFrom.z - Impact.z;
        double Angle = Dx * Dx + Dz * Dz < 1.0 ? Random.nextDouble() * Math.PI * 2.0 : Math.atan2(Dz, Dx);
        Angle += (Random.nextDouble() - 0.5) * Math.toRadians(70.0);
        double Reach = LaunchReach * (0.9 + Random.nextDouble() * 0.2);
        double Height = LaunchHeight * (0.92 + Random.nextDouble() * 0.16);
        Vector3f Offset = new Vector3f((float) (Math.cos(Angle) * Reach), (float) Height, (float) (Math.sin(Angle) * Reach));
        Meteor.moveTo(Impact.x, Impact.y, Impact.z, 0.0f, 0.0f);
        Meteor.entityData.set(DataStart, Offset);
        Meteor.entityData.set(DataLaunch, World.getGameTime());
        Meteor.entityData.set(DataDuration, FlightTicks);
        Meteor.entityData.set(DataSeed, Random.nextInt(1 << 20));
        Meteor.StrikeId = StrikeId;
        Meteor.DoorFront = DoorFront;
        World.addFreshEntity(Meteor);
        return Meteor;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(DataStart, new Vector3f(0.0f, (float) LaunchHeight, 0.0f));
        Builder.define(DataLaunch, 0L);
        Builder.define(DataDuration, FlightTicks);
        Builder.define(DataPhase, PhaseFlight);
        Builder.define(DataLanded, 0L);
        Builder.define(DataRadius, 9.0f);
        Builder.define(DataSeed, 0);
    }

    public static void setClientHook(Consumer<MeteorEntity> Hook) {
        ClientHook = Hook;
    }

    public boolean isLaunched() {
        return entityData.get(DataLaunch) > 0L;
    }

    public boolean claimSounds() {
        if (Sounding) {
            return false;
        }
        Sounding = true;
        return true;
    }

    public boolean claimSwoop() {
        if (Swooping) {
            return false;
        }
        Swooping = true;
        return true;
    }

    public int getPhase() {
        return entityData.get(DataPhase);
    }

    public int getSeed() {
        return entityData.get(DataSeed);
    }

    public int getDuration() {
        return Math.max(1, entityData.get(DataDuration));
    }

    public float getCraterRadius() {
        return entityData.get(DataRadius);
    }

    public long getLanded() {
        return entityData.get(DataLanded);
    }

    public Vec3 getStart() {
        Vector3f Offset = entityData.get(DataStart);
        return position().add(Offset.x(), Offset.y(), Offset.z());
    }

    public float getProgress(float PartialTick) {
        float Age = (float) (level().getGameTime() - entityData.get(DataLaunch)) + PartialTick;
        return Mth.clamp(Age / getDuration(), 0.0f, 1.0f);
    }

    public static double ease(float Progress) {
        return Math.pow(Progress, Easing);
    }

    public Vec3 getHead(float PartialTick) {
        Vector3f Offset = entityData.get(DataStart);
        double Remaining = 1.0 - ease(getProgress(PartialTick));
        return position().add(Offset.x() * Remaining, Offset.y() * Remaining, Offset.z() * Remaining);
    }

    public Vec3 getHeading() {
        Vector3f Offset = entityData.get(DataStart);
        return new Vec3(-Offset.x(), -Offset.y(), -Offset.z()).normalize();
    }

    public int ticksToImpact() {
        return (int) (entityData.get(DataLaunch) + getDuration() - level().getGameTime());
    }

    @Override
    public void tick() {
        if (!level().isClientSide && !Scp002Strikes.fallsIn(level())) {
            discard();
            return;
        }
        super.tick();
        if (level().isClientSide) {
            if (getPhase() == PhaseFlight) {
                if (isLaunched()) {
                    trail();
                }
            } else {
                smolder();
            }
            ClientHook.accept(this);
            return;
        }
        ServerLevel World = (ServerLevel) level();
        long Now = World.getGameTime();
        if (getPhase() == PhaseFlight) {
            if (Now - entityData.get(DataLaunch) >= getDuration()) {
                strike(World, Now);
            }
            return;
        }
        long Since = Now - getLanded();
        float Radius = getCraterRadius();
        if (Since < 70L && Since % 3L == 0L) {
            Scp002Sounds.debris(World, craterPoint(Radius * 1.6f, 1.5), random);
        }
        if (Since > 40L && Since % 37L == 0L && Since < SmolderTicks * 3L / 4L) {
            Scp002Sounds.sizzle(World, craterPoint(Radius * 0.8f, 0.3), random);
        }
        if (Since > SmolderTicks || Since < 0L) {
            discard();
        }
    }

    private Vec3 craterPoint(float Radius, double Lift) {
        double Angle = random.nextDouble() * Math.PI * 2.0;
        double Distance = Math.sqrt(random.nextDouble()) * Radius;
        return position().add(Math.cos(Angle) * Distance, Lift - 3.0, Math.sin(Angle) * Distance);
    }

    private void strike(ServerLevel World, long Now) {
        if (StrikeId != null && !Scp002Strikes.get(World.getServer()).isInbound(StrikeId)) {
            discard();
            return;
        }
        entityData.set(DataPhase, PhaseSmolder);
        entityData.set(DataLanded, Now);
        BlockPos Ground = blockPosition().below();
        Scp002Impact.blast(World, this);
        Scp002Impact.Result Result = Scp002Impact.strike(World, Ground, DoorFront, random);
        entityData.set(DataRadius, Result.Radius());
        Scp002Sounds.impact(World, position(), random);
        Scp002Impact.announce(World, Result, position());
        if (StrikeId != null) {
            Scp002Strikes.get(World.getServer()).landed(StrikeId, World, Result.Door());
        }
    }

    private void trail() {
        Vec3 Head = getHead(0.0f);
        Vec3 Previous = LastHead == null ? Head : LastHead;
        LastHead = Head;
        Vec3 Back = getHeading().scale(-1.0);
        RandomSource Random = random;
        Level World = level();
        int Steps = 3;
        for (int Step = 0; Step < Steps; Step++) {
            Vec3 At = Previous.lerp(Head, (Step + 1.0) / Steps);
            for (int Index = 0; Index < 2; Index++) {
                World.addParticle(Scp002Registry.Ember.get(), true, At.x + Random.nextGaussian() * 0.45, At.y + Random.nextGaussian() * 0.45, At.z + Random.nextGaussian() * 0.45,
                        Back.x * 0.12 + Random.nextGaussian() * 0.04, Back.y * 0.12 + Random.nextGaussian() * 0.04, Back.z * 0.12 + Random.nextGaussian() * 0.04);
            }
            World.addParticle(ParticleTypes.FLAME, true, At.x + Random.nextGaussian() * 0.5, At.y + Random.nextGaussian() * 0.5, At.z + Random.nextGaussian() * 0.5,
                    Back.x * 0.05, Back.y * 0.05, Back.z * 0.05);
            if (Random.nextInt(2) == 0) {
                World.addParticle(ParticleTypes.LARGE_SMOKE, true, At.x + Random.nextGaussian() * 0.6, At.y + Random.nextGaussian() * 0.6, At.z + Random.nextGaussian() * 0.6,
                        Random.nextGaussian() * 0.02, 0.01, Random.nextGaussian() * 0.02);
            }
        }
        if (Random.nextInt(2) == 0) {
            World.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, Head.x + Back.x * 2.0, Head.y + Back.y * 2.0, Head.z + Back.z * 2.0,
                    Random.nextGaussian() * 0.01, 0.015, Random.nextGaussian() * 0.01);
        }
        if (Random.nextInt(5) == 0) {
            World.addParticle(ParticleTypes.LAVA, true, Head.x, Head.y, Head.z, 0.0, 0.0, 0.0);
        }
    }

    private void smolder() {
        long Since = level().getGameTime() - getLanded();
        if (Since < 0L || Since > SmolderTicks) {
            return;
        }
        float Fade = 1.0f - (float) Since / SmolderTicks;
        RandomSource Random = random;
        Level World = level();
        float Radius = getCraterRadius();
        if (Random.nextFloat() < 0.35f + Fade * 0.6f) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Distance = Math.sqrt(Random.nextDouble()) * Radius * 0.9;
            World.addParticle(Random.nextInt(3) == 0 ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                    getX() + Math.cos(Angle) * Distance, getY() - 3.0 + Random.nextDouble(), getZ() + Math.sin(Angle) * Distance, 0.0, 0.03 + Random.nextDouble() * 0.02, 0.0);
        }
        if (Random.nextFloat() < Fade * 0.5f) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Distance = Math.sqrt(Random.nextDouble()) * Radius * 0.7;
            World.addParticle(Scp002Registry.Ember.get(), true, getX() + Math.cos(Angle) * Distance, getY() - 3.5 + Random.nextDouble() * 2.0, getZ() + Math.sin(Angle) * Distance,
                    Random.nextGaussian() * 0.02, 0.05 + Random.nextDouble() * 0.06, Random.nextGaussian() * 0.02);
        }
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
    public boolean isAttackable() {
        return false;
    }

    @Override
    public boolean ignoreExplosion(Explosion Blast) {
        return true;
    }

    @Override
    public PushReaction getPistonPushReaction() {
        return PushReaction.IGNORE;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < 512.0 * 512.0;
    }

    @Override
    public boolean displayFireAnimation() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
        entityData.set(DataStart, new Vector3f(Tag.getFloat("StartX"), Tag.getFloat("StartY"), Tag.getFloat("StartZ")));
        entityData.set(DataLaunch, Tag.getLong("Launch"));
        entityData.set(DataDuration, Tag.contains("Duration") ? Tag.getInt("Duration") : FlightTicks);
        entityData.set(DataPhase, Tag.getInt("Phase"));
        entityData.set(DataLanded, Tag.getLong("Landed"));
        entityData.set(DataRadius, Tag.contains("Radius") ? Tag.getFloat("Radius") : 9.0f);
        entityData.set(DataSeed, Tag.getInt("Seed"));
        StrikeId = Tag.hasUUID("Strike") ? Tag.getUUID("Strike") : null;
        DoorFront = Direction.from2DDataValue(Tag.getInt("Front"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
        Vector3f Offset = entityData.get(DataStart);
        Tag.putFloat("StartX", Offset.x());
        Tag.putFloat("StartY", Offset.y());
        Tag.putFloat("StartZ", Offset.z());
        Tag.putLong("Launch", entityData.get(DataLaunch));
        Tag.putInt("Duration", getDuration());
        Tag.putInt("Phase", getPhase());
        Tag.putLong("Landed", getLanded());
        Tag.putFloat("Radius", getCraterRadius());
        Tag.putInt("Seed", getSeed());
        if (StrikeId != null) {
            Tag.putUUID("Strike", StrikeId);
        }
        Tag.putInt("Front", DoorFront.get2DDataValue());
    }
}
