package net.rai.scp_unleashed.scp163;

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
import net.rai.scp_unleashed.scp002.Scp002Impact;
import net.rai.scp_unleashed.scp002.Scp002Strikes;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;
import java.util.function.Consumer;

public class Scp163CraftEntity extends Entity {
    public static final int FlightTicks = 60;
    public static final int SmolderTicks = 1200;
    public static final double LaunchReach = 150.0;
    public static final double LaunchHeight = 72.0;
    public static final double LaunchSway = 18.0;
    public static final float Easing = 1.15f;
    public static final int PhaseFlight = 0;
    public static final int PhaseSmolder = 1;

    private static final EntityDataAccessor<Vector3f> DataStart = SynchedEntityData.defineId(Scp163CraftEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Long> DataLaunch = SynchedEntityData.defineId(Scp163CraftEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DataDuration = SynchedEntityData.defineId(Scp163CraftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DataPhase = SynchedEntityData.defineId(Scp163CraftEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DataLanded = SynchedEntityData.defineId(Scp163CraftEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DataSeed = SynchedEntityData.defineId(Scp163CraftEntity.class, EntityDataSerializers.INT);

    private static Consumer<Scp163CraftEntity> ClientHook = Craft -> {
    };

    @Nullable
    private UUID StrikeId;
    private Direction Front = Direction.SOUTH;
    private Vec3 LastHead;
    private boolean Sounding;

    public Scp163CraftEntity(EntityType<? extends Scp163CraftEntity> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
    }

    @Nullable
    public static Scp163CraftEntity launch(ServerLevel World, BlockPos Ground, @Nullable UUID StrikeId, Direction Front) {
        if (!Scp002Strikes.fallsIn(World)) {
            return null;
        }
        Scp163CraftEntity Craft = new Scp163CraftEntity(Scp163Registry.Craft.get(), World);
        RandomSource Random = World.random;
        Vec3 Impact = Vec3.atBottomCenterOf(Ground.above());
        Vec3 Along = Scp163Impact.along(Front);
        Vec3 Across = new Vec3(-Along.z, 0.0, Along.x);
        double Reach = LaunchReach * (0.92 + Random.nextDouble() * 0.16);
        double Sway = (Random.nextDouble() - 0.5) * LaunchSway;
        Vector3f Offset = new Vector3f((float) (-Along.x * Reach + Across.x * Sway), (float) (LaunchHeight * (0.9 + Random.nextDouble() * 0.2)),
                (float) (-Along.z * Reach + Across.z * Sway));
        Craft.moveTo(Impact.x, Impact.y, Impact.z, 0.0f, 0.0f);
        Craft.entityData.set(DataStart, Offset);
        Craft.entityData.set(DataLaunch, World.getGameTime());
        Craft.entityData.set(DataDuration, FlightTicks);
        Craft.entityData.set(DataSeed, Random.nextInt(1 << 20));
        Craft.StrikeId = StrikeId;
        Craft.Front = Front;
        World.addFreshEntity(Craft);
        return Craft;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(DataStart, new Vector3f(0.0f, (float) LaunchHeight, 0.0f));
        Builder.define(DataLaunch, 0L);
        Builder.define(DataDuration, FlightTicks);
        Builder.define(DataPhase, PhaseFlight);
        Builder.define(DataLanded, 0L);
        Builder.define(DataSeed, 0);
    }

    public static void setClientHook(Consumer<Scp163CraftEntity> Hook) {
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

    public int getPhase() {
        return entityData.get(DataPhase);
    }

    public int getSeed() {
        return entityData.get(DataSeed);
    }

    public int getDuration() {
        return Math.max(1, entityData.get(DataDuration));
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
        if (Since < 60L && Since % 4L == 0L) {
            Scp163Sounds.debris(World, craterPoint(12.0, 1.5), random);
        }
        if (Since > 40L && Since % 53L == 0L && Since < SmolderTicks * 3L / 4L) {
            Scp163Sounds.hullTick(World, position().add(0.0, 2.0, 0.0), random);
        }
        if (Since > SmolderTicks || Since < 0L) {
            discard();
        }
    }

    private Vec3 craterPoint(double Radius, double Lift) {
        double Angle = random.nextDouble() * Math.PI * 2.0;
        double Distance = Math.sqrt(random.nextDouble()) * Radius;
        return position().add(Math.cos(Angle) * Distance, Lift - 3.0, Math.sin(Angle) * Distance);
    }

    private void strike(ServerLevel World, long Now) {
        if (StrikeId != null && !Scp163Crashes.get(World.getServer()).isInbound(StrikeId)) {
            discard();
            return;
        }
        entityData.set(DataPhase, PhaseSmolder);
        entityData.set(DataLanded, Now);
        BlockPos Ground = blockPosition().below();
        Scp002Impact.blast(World, this);
        Scp163Impact.Result Result = Scp163Impact.crash(World, Ground, Front, random);
        Scp163Sounds.impact(World, position(), random);
        Scp163Impact.announce(World, blockPosition(), Result.Radius());
        if (StrikeId != null) {
            Scp163Crashes.get(World.getServer()).landed(StrikeId, World, Result.Hatch());
        }
    }

    private void trail() {
        Vec3 Head = getHead(0.0f);
        Vec3 Previous = LastHead == null ? Head : LastHead;
        LastHead = Head;
        Vec3 Back = getHeading().scale(-1.0);
        RandomSource Random = random;
        Level World = level();
        for (int Step = 0; Step < 3; Step++) {
            Vec3 At = Previous.lerp(Head, (Step + 1.0) / 3.0);
            World.addParticle(ParticleTypes.FLAME, true, At.x + Random.nextGaussian() * 0.6, At.y + Random.nextGaussian() * 0.6, At.z + Random.nextGaussian() * 0.6,
                    Back.x * 0.06, Back.y * 0.06, Back.z * 0.06);
            World.addParticle(ParticleTypes.LARGE_SMOKE, true, At.x + Random.nextGaussian() * 0.7, At.y + Random.nextGaussian() * 0.7, At.z + Random.nextGaussian() * 0.7,
                    Random.nextGaussian() * 0.02, 0.01, Random.nextGaussian() * 0.02);
            if (Random.nextInt(2) == 0) {
                World.addParticle(ParticleTypes.END_ROD, true, At.x + Random.nextGaussian() * 0.5, At.y + Random.nextGaussian() * 0.5, At.z + Random.nextGaussian() * 0.5,
                        Back.x * 0.15 + Random.nextGaussian() * 0.05, Back.y * 0.15 + Random.nextGaussian() * 0.05, Back.z * 0.15 + Random.nextGaussian() * 0.05);
            }
        }
        if (Random.nextInt(2) == 0) {
            World.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, Head.x + Back.x * 3.0, Head.y + Back.y * 3.0, Head.z + Back.z * 3.0,
                    Random.nextGaussian() * 0.01, 0.015, Random.nextGaussian() * 0.01);
        }
    }

    private void smolder() {
        long Since = level().getGameTime() - getLanded();
        if (Since < 0L || Since > SmolderTicks) {
            return;
        }
        float Fade = 1.0f - (float) Since / SmolderTicks;
        RandomSource Random = random;
        if (Random.nextFloat() < 0.3f + Fade * 0.6f) {
            double Angle = Random.nextDouble() * Math.PI * 2.0;
            double Distance = Math.sqrt(Random.nextDouble()) * 9.0;
            level().addParticle(Random.nextInt(3) == 0 ? ParticleTypes.CAMPFIRE_SIGNAL_SMOKE : ParticleTypes.CAMPFIRE_COSY_SMOKE, true,
                    getX() + Math.cos(Angle) * Distance, getY() - 2.5 + Random.nextDouble(), getZ() + Math.sin(Angle) * Distance, 0.0, 0.03 + Random.nextDouble() * 0.02, 0.0);
        }
        if (Random.nextFloat() < Fade * 0.35f) {
            level().addParticle(ParticleTypes.WHITE_ASH, true, getX() + Random.nextGaussian() * 4.0, getY() + Random.nextDouble() * 3.0, getZ() + Random.nextGaussian() * 4.0,
                    0.0, 0.02, 0.0);
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
        entityData.set(DataSeed, Tag.getInt("Seed"));
        StrikeId = Tag.hasUUID("Strike") ? Tag.getUUID("Strike") : null;
        Front = Direction.from2DDataValue(Tag.getInt("Front"));
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
        Tag.putInt("Seed", getSeed());
        if (StrikeId != null) {
            Tag.putUUID("Strike", StrikeId);
        }
        Tag.putInt("Front", Front.get2DDataValue());
    }
}
