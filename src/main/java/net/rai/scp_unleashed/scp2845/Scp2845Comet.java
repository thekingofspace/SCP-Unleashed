package net.rai.scp_unleashed.scp2845;

import net.minecraft.core.BlockPos;
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
import net.rai.scp_unleashed.scp002.Scp002Strikes;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.UUID;
import java.util.function.Consumer;

public class Scp2845Comet extends Entity {
    public static final int FlightTicks = 64;
    public static final int GlowTicks = 600;
    public static final double LaunchHeight = 128.0;
    public static final double LaunchReach = 96.0;
    public static final float Easing = 1.45f;
    public static final int PhaseFlight = 0;
    public static final int PhaseGlow = 1;

    private static final EntityDataAccessor<Vector3f> DataStart = SynchedEntityData.defineId(Scp2845Comet.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Long> DataLaunch = SynchedEntityData.defineId(Scp2845Comet.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DataPhase = SynchedEntityData.defineId(Scp2845Comet.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> DataLanded = SynchedEntityData.defineId(Scp2845Comet.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> DataSeed = SynchedEntityData.defineId(Scp2845Comet.class, EntityDataSerializers.INT);

    private static Consumer<Scp2845Comet> ClientHook = Comet -> {
    };

    @Nullable
    private UUID CaseId;
    private Vec3 From = Vec3.ZERO;
    private Vec3 LastHead;
    private boolean Sounding;

    public Scp2845Comet(EntityType<? extends Scp2845Comet> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
    }

    @Nullable
    public static Scp2845Comet launch(ServerLevel World, BlockPos Ground, Vec3 ApproachFrom, @Nullable UUID CaseId) {
        if (!Scp002Strikes.fallsIn(World)) {
            return null;
        }
        Scp2845Comet Comet = new Scp2845Comet(Scp2845Registry.CometType.get(), World);
        RandomSource Random = World.random;
        Vec3 Impact = Vec3.atBottomCenterOf(Ground.above());
        double Dx = ApproachFrom.x - Impact.x;
        double Dz = ApproachFrom.z - Impact.z;
        double Angle = Dx * Dx + Dz * Dz < 1.0 ? Random.nextDouble() * Math.PI * 2.0 : Math.atan2(Dz, Dx);
        Angle += (Random.nextDouble() - 0.5) * Math.toRadians(60.0);
        double Reach = LaunchReach * (0.9 + Random.nextDouble() * 0.2);
        double Height = LaunchHeight * (0.92 + Random.nextDouble() * 0.16);
        Comet.moveTo(Impact.x, Impact.y, Impact.z, 0.0f, 0.0f);
        Comet.entityData.set(DataStart, new Vector3f((float) (Math.cos(Angle) * Reach), (float) Height, (float) (Math.sin(Angle) * Reach)));
        Comet.entityData.set(DataLaunch, World.getGameTime());
        Comet.entityData.set(DataSeed, Random.nextInt(1 << 20));
        Comet.CaseId = CaseId;
        Comet.From = ApproachFrom;
        World.addFreshEntity(Comet);
        return Comet;
    }

    public static void setClientHook(Consumer<Scp2845Comet> Hook) {
        ClientHook = Hook;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(DataStart, new Vector3f(0.0f, (float) LaunchHeight, 0.0f));
        Builder.define(DataLaunch, 0L);
        Builder.define(DataPhase, PhaseFlight);
        Builder.define(DataLanded, 0L);
        Builder.define(DataSeed, 0);
    }

    public boolean claimSounds() {
        if (Sounding) {
            return false;
        }
        Sounding = true;
        return true;
    }

    public boolean isLaunched() {
        return entityData.get(DataLaunch) > 0L;
    }

    public int getPhase() {
        return entityData.get(DataPhase);
    }

    public int getSeed() {
        return entityData.get(DataSeed);
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
        return Mth.clamp(Age / FlightTicks, 0.0f, 1.0f);
    }

    public Vec3 getHead(float PartialTick) {
        Vector3f Offset = entityData.get(DataStart);
        double Remaining = 1.0 - Math.pow(getProgress(PartialTick), Easing);
        return position().add(Offset.x() * Remaining, Offset.y() * Remaining, Offset.z() * Remaining);
    }

    public Vec3 getHeading() {
        Vector3f Offset = entityData.get(DataStart);
        return new Vec3(-Offset.x(), -Offset.y(), -Offset.z()).normalize();
    }

    public int ticksToImpact() {
        return (int) (entityData.get(DataLaunch) + FlightTicks - level().getGameTime());
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
                glow();
            }
            ClientHook.accept(this);
            return;
        }
        ServerLevel World = (ServerLevel) level();
        long Now = World.getGameTime();
        if (getPhase() == PhaseFlight) {
            Scp2845Arrival.keepLoaded(World, blockPosition());
            if (Now - entityData.get(DataLaunch) >= FlightTicks) {
                strike(World, Now);
            }
            return;
        }
        if (Now - getLanded() > GlowTicks || Now < getLanded()) {
            discard();
        }
    }

    private void strike(ServerLevel World, long Now) {
        entityData.set(DataPhase, PhaseGlow);
        entityData.set(DataLanded, Now);
        Scp2845Arrival.impact(World, blockPosition().below(), From, CaseId);
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
            World.addParticle(ParticleTypes.END_ROD, true, At.x + Random.nextGaussian() * 0.35, At.y + Random.nextGaussian() * 0.35, At.z + Random.nextGaussian() * 0.35,
                    Back.x * 0.08, Back.y * 0.08, Back.z * 0.08);
            if (Random.nextInt(2) == 0) {
                World.addParticle(ParticleTypes.CHERRY_LEAVES, true, At.x + Random.nextGaussian() * 0.6, At.y + Random.nextGaussian() * 0.6, At.z + Random.nextGaussian() * 0.6,
                        0.0, 0.0, 0.0);
            }
            if (Random.nextInt(3) == 0) {
                World.addParticle(ParticleTypes.CLOUD, true, At.x + Random.nextGaussian() * 0.5, At.y + Random.nextGaussian() * 0.5, At.z + Random.nextGaussian() * 0.5,
                        Back.x * 0.03, 0.01, Back.z * 0.03);
            }
        }
    }

    private void glow() {
        long Since = level().getGameTime() - getLanded();
        if (Since < 0L || Since > GlowTicks) {
            return;
        }
        float Fade = 1.0f - (float) Since / GlowTicks;
        if (random.nextFloat() < 0.25f + Fade * 0.6f) {
            double Angle = random.nextDouble() * Math.PI * 2.0;
            double Distance = Math.sqrt(random.nextDouble()) * 6.5;
            level().addParticle(random.nextInt(3) == 0 ? ParticleTypes.END_ROD : ParticleTypes.CAMPFIRE_COSY_SMOKE, true, getX() + Math.cos(Angle) * Distance,
                    getY() - 2.5 + random.nextDouble(), getZ() + Math.sin(Angle) * Distance, 0.0, 0.02 + random.nextDouble() * 0.02, 0.0);
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
        entityData.set(DataPhase, Tag.getInt("Phase"));
        entityData.set(DataLanded, Tag.getLong("Landed"));
        entityData.set(DataSeed, Tag.getInt("Seed"));
        CaseId = Tag.hasUUID("Case") ? Tag.getUUID("Case") : null;
        From = new Vec3(Tag.getDouble("FromX"), Tag.getDouble("FromY"), Tag.getDouble("FromZ"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
        Vector3f Offset = entityData.get(DataStart);
        Tag.putFloat("StartX", Offset.x());
        Tag.putFloat("StartY", Offset.y());
        Tag.putFloat("StartZ", Offset.z());
        Tag.putLong("Launch", entityData.get(DataLaunch));
        Tag.putInt("Phase", getPhase());
        Tag.putLong("Landed", getLanded());
        Tag.putInt("Seed", getSeed());
        if (CaseId != null) {
            Tag.putUUID("Case", CaseId);
        }
        Tag.putDouble("FromX", From.x);
        Tag.putDouble("FromY", From.y);
        Tag.putDouble("FromZ", From.z);
    }
}
