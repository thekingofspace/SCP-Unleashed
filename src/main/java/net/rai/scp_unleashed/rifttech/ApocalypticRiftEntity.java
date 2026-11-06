package net.rai.scp_unleashed.rifttech;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.neoforged.neoforge.common.NeoForge;
import net.rai.scp_unleashed.anomaly.FragmentRarity;
import net.rai.scp_unleashed.extraction.RiftExtractorItem;
import net.rai.scp_unleashed.rift.RiftEntity;
import net.rai.scp_unleashed.rift.RiftParticleOptions;
import net.rai.scp_unleashed.rift.RiftRegistry;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApocalypticRiftEntity extends RiftEntity {
    public static final float Height = 2.8f;
    public static final int Colour = 0xFFFFC23C;
    public static final String Label = "apocalyptic";
    public static final float Resistance = 0.4f;
    public static final int Persistent = Integer.MAX_VALUE / 4;
    public static final float MinStep = 0.0025f;
    public static final double VisualReach = 6.0;
    public static final double VisualRise = 10.0;
    public static final int PullGrace = 6;
    public static final int StrikeMin = 20;
    public static final int StrikeSpread = 21;
    public static final double StrikeRadius = 10.0;
    public static final double SplashRadius = 1.25;
    public static final float StrikeDamage = 3.0f;
    public static final int BurnTicks = 60;
    public static final float PullerShare = 0.35f;
    public static final float VictimShare = 0.3f;
    public static final int BoltTicks = 9;

    private static final EntityDataAccessor<Vector3f> DataStrike = SynchedEntityData.defineId(ApocalypticRiftEntity.class, EntityDataSerializers.VECTOR3);
    private static final EntityDataAccessor<Integer> DataStrikes = SynchedEntityData.defineId(ApocalypticRiftEntity.class, EntityDataSerializers.INT);

    private float Carry;
    private long LastPull = Long.MIN_VALUE / 2;
    private int StrikeDelay = StrikeMin;
    private final Map<BlockPos, Long> Collectors = new HashMap<>();
    private int SeenStrikes = -1;
    private final List<Bolt> Bolts = new ArrayList<>();

    public record Bolt(Vec3 Target, int Seed, long Birth) {
    }

    public ApocalypticRiftEntity(EntityType<? extends ApocalypticRiftEntity> Type, Level World) {
        super(Type, World);
        refreshDimensions();
    }

    public static AABB boxAt(Vec3 Bottom) {
        double Half = Height * WidthRatio * 0.5;
        return new AABB(Bottom.x - Half, Bottom.y, Bottom.z - Half, Bottom.x + Half, Bottom.y + Height, Bottom.z + Half);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(DataStrike, new Vector3f());
        Builder.define(DataStrikes, 0);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (!DataStrikes.equals(Key) || !level().isClientSide) {
            return;
        }
        int Count = entityData.get(DataStrikes);
        if (SeenStrikes >= 0 && Count != SeenStrikes) {
            Vec3 Impact = new Vec3(entityData.get(DataStrike));
            Bolts.add(new Bolt(Impact, Count, level().getGameTime()));
            for (int Spark = 0; Spark < 14; Spark++) {
                Vec3 Heading = new Vec3(random.nextGaussian(), Math.abs(random.nextGaussian()) * 0.9, random.nextGaussian()).normalize();
                double Speed = 0.12 + random.nextDouble() * 0.28;
                level().addParticle(new RiftParticleOptions(RiftRegistry.RiftSpark.get(), Colour, -1, 1.0f), Impact.x, Impact.y + 0.1, Impact.z,
                        Heading.x * Speed, Heading.y * Speed, Heading.z * Speed);
            }
            level().addParticle(new RiftParticleOptions(RiftRegistry.RiftShockwave.get(), Colour, -1, 0.7f), Impact.x, Impact.y + 0.2, Impact.z, 0.0, 0.0, 0.0);
        }
        SeenStrikes = Count;
    }

    @Override
    public EntityDimensions getDimensions(Pose Stance) {
        return EntityDimensions.fixed(Height * WidthRatio, Height);
    }

    @Override
    public float getRiftHeight() {
        return Height;
    }

    @Override
    public FragmentRarity getRarity() {
        return FragmentRarity.Abyssal;
    }

    @Override
    public int getRiftColour() {
        return Colour;
    }

    @Override
    public String getRiftLabel() {
        return Label;
    }

    @Override
    public ItemStack createFragment() {
        return new ItemStack(RiftTechRegistry.ApocalypticFragment.get());
    }

    @Override
    public int getLifetime() {
        return Persistent;
    }

    @Override
    public void setExtraction(float Progress) {
        if (!level().isClientSide) {
            LastPull = level().getGameTime();
        }
        float Current = getExtraction();
        float Wanted = Mth.clamp(Progress, 0.0f, 1.0f);
        if (Wanted < Current) {
            Carry = 0.0f;
            super.setExtraction(Wanted);
            return;
        }
        Carry += (Wanted - Current) * Resistance;
        if (Carry >= MinStep) {
            float Next = Math.min(1.0f, Current + Carry);
            Carry = 0.0f;
            super.setExtraction(Next);
        } else {
            super.setExtraction(Current);
        }
    }

    public void notePuller(BlockPos Collector) {
        if (!level().isClientSide) {
            Collectors.put(Collector.immutable(), level().getGameTime());
        }
    }

    public boolean isBeingPulled() {
        return level().getGameTime() - LastPull <= PullGrace;
    }

    public int getStrikeCount() {
        return entityData.get(DataStrikes);
    }

    public List<Bolt> getBolts() {
        return Bolts;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            long Now = level().getGameTime();
            Bolts.removeIf(Item -> Now - Item.Birth() > BoltTicks);
            return;
        }
        if (!(level() instanceof ServerLevel World) || isRemoved() || isCollapsing()) {
            return;
        }
        long Now = World.getGameTime();
        Collectors.values().removeIf(At -> Now - At > PullGrace * 2L);
        if (!isBeingPulled()) {
            StrikeDelay = Math.max(StrikeDelay, StrikeMin / 2);
            return;
        }
        if (--StrikeDelay > 0) {
            return;
        }
        StrikeDelay = StrikeMin + random.nextInt(StrikeSpread);
        strikeAt(chooseTarget(World));
    }

    private Vec3 chooseTarget(ServerLevel World) {
        Vec3 Nexus = getCenter(1.0f);
        List<Vec3> Pullers = new ArrayList<>();
        for (BlockPos At : Collectors.keySet()) {
            Pullers.add(Vec3.atBottomCenterOf(At));
        }
        for (Player User : World.players()) {
            if (!User.isSpectator() && User.distanceToSqr(Nexus) < StrikeRadius * StrikeRadius * 1.5 && User.isUsingItem() && User.getUseItem().getItem() instanceof RiftExtractorItem) {
                Pullers.add(User.position());
            }
        }
        float Roll = random.nextFloat();
        if (Roll < PullerShare && !Pullers.isEmpty()) {
            Vec3 Near = Pullers.get(random.nextInt(Pullers.size()));
            double Angle = random.nextDouble() * Mth.TWO_PI;
            double Gap = 1.2 + random.nextDouble() * 1.4;
            return ground(World, Near.x + Math.cos(Angle) * Gap, Near.z + Math.sin(Angle) * Gap, Near.y + 2.5);
        }
        if (Roll < PullerShare + VictimShare) {
            List<LivingEntity> Nearby = World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(StrikeRadius),
                    Candidate -> Candidate.isAlive() && !Candidate.isSpectator() && Candidate.distanceToSqr(Nexus) <= StrikeRadius * StrikeRadius);
            if (!Nearby.isEmpty()) {
                return Nearby.get(random.nextInt(Nearby.size())).position();
            }
        }
        double Angle = random.nextDouble() * Mth.TWO_PI;
        double Distance = 3.0 + random.nextDouble() * 6.0;
        return ground(World, Nexus.x + Math.cos(Angle) * Distance, Nexus.z + Math.sin(Angle) * Distance, Nexus.y + 3.0);
    }

    private Vec3 ground(ServerLevel World, double X, double Z, double Top) {
        BlockHitResult Hit = World.clip(new ClipContext(new Vec3(X, Top, Z), new Vec3(X, Top - 14.0, Z), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return Hit.getType() == HitResult.Type.MISS ? new Vec3(X, getY(), Z) : Hit.getLocation();
    }

    public void strikeAt(Vec3 Impact) {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        entityData.set(DataStrike, new Vector3f((float) Impact.x, (float) Impact.y, (float) Impact.z));
        entityData.set(DataStrikes, entityData.get(DataStrikes) + 1);
        AABB Splash = new AABB(Impact, Impact).inflate(SplashRadius, SplashRadius + 0.6, SplashRadius);
        for (LivingEntity Victim : World.getEntitiesOfClass(LivingEntity.class, Splash, Candidate -> Candidate.isAlive() && !Candidate.isSpectator())) {
            if (Victim instanceof Player User && User.getAbilities().invulnerable) {
                continue;
            }
            Victim.hurt(damageSources().lightningBolt(), StrikeDamage);
            if (!Victim.fireImmune()) {
                Victim.igniteForTicks(BurnTicks);
            }
        }
        float Pitch = 0.9f + random.nextFloat() * 0.2f;
        World.playSound(null, Impact.x, Impact.y, Impact.z, RiftTechRegistry.GoldenStrike.get(), SoundSource.WEATHER, 3.0f, Pitch);
        World.playSound(null, Impact.x, Impact.y, Impact.z, RiftTechRegistry.GoldenStrikeCrack.get(), SoundSource.WEATHER, 2.0f, Pitch);
        World.playSound(null, Impact.x, Impact.y, Impact.z, RiftTechRegistry.GoldenStrikeImpact.get(), SoundSource.WEATHER, 1.5f, Pitch);
    }

    @Override
    public void collapse() {
        if (level() instanceof ServerLevel World && !isCollapsing() && !isRemoved()) {
            NeoForge.EVENT_BUS.post(new ApocalypticRiftEvent.Collapsed(World, getCenter(1.0f), this));
        }
        super.collapse();
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        AABB Box = getBoundingBox().inflate(VisualReach, 2.0, VisualReach).expandTowards(0.0, VisualRise, 0.0);
        return Bolts.isEmpty() ? Box : Box.inflate(StrikeRadius);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        double Reach = 192.0 * getViewScale();
        return Distance < Reach * Reach;
    }
}
