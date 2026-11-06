package net.rai.scp_unleashed.scp4666.christmas;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp4666.Scp4666Registry;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public class Scp4666Sighting extends Entity implements GeoEntity {
    public static final byte StateWave = 0;
    public static final byte StateDive = 1;
    public static final int WaveTicks = 110;
    public static final int DiveTicks = 34;
    public static final int BurstTick = 17;
    public static final int MaxTicks = 600;
    public static final int StareLimit = 30;
    public static final double StareDot = 0.995;
    public static final double ApproachRange = 40.0;
    public static final double DiveReach = 1.1;
    public static final double RenderReach = 192.0;
    public static final float SoundVolume = 6.0f;
    private static final EntityDataAccessor<Byte> StateData = SynchedEntityData.defineId(Scp4666Sighting.class, EntityDataSerializers.BYTE);
    private static final RawAnimation WaveLoop = RawAnimation.begin().thenLoop("wave");
    private static final RawAnimation DivePlay = RawAnimation.begin().thenPlayAndHold("dive");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID Witness;
    @Nullable
    private ServerPlayer Watcher;
    private int Age;
    private int DiveAge;
    private int Stared;

    public Scp4666Sighting(EntityType<? extends Scp4666Sighting> Type, Level World) {
        super(Type, World);
        noPhysics = true;
        setNoGravity(true);
        setInvulnerable(true);
    }

    public static Scp4666Sighting spawn(ServerLevel World, Vec3 Spot, ServerPlayer Viewer) {
        Scp4666Sighting Figure = new Scp4666Sighting(Scp4666Registry.Sighting.get(), World);
        Figure.Witness = Viewer.getUUID();
        Figure.Watcher = Viewer;
        float Yaw = (float) (Mth.atan2(Viewer.getZ() - Spot.z, Viewer.getX() - Spot.x) * Mth.RAD_TO_DEG) - 90.0f;
        Figure.moveTo(Spot.x, Spot.y, Spot.z, Yaw, 0.0f);
        World.addFreshEntity(Figure);
        return Figure;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        Builder.define(StateData, StateWave);
    }

    public byte state() {
        return entityData.get(StateData);
    }

    public boolean diving() {
        return state() == StateDive;
    }

    @Nullable
    public UUID witness() {
        return Witness;
    }

    public int age() {
        return Age;
    }

    @Nullable
    private ServerPlayer viewer() {
        ServerPlayer Found = Watcher;
        if ((Found == null || Found.isRemoved()) && Witness != null && getServer() != null) {
            Found = getServer().getPlayerList().getPlayer(Witness);
            Watcher = Found;
        }
        return Found != null && !Found.isRemoved() && Found.level() == level() && Found.isAlive() ? Found : null;
    }

    public static boolean staring(ServerPlayer Viewer, Vec3 Target) {
        Vec3 Toward = Target.subtract(Viewer.getEyePosition()).normalize();
        return Viewer.getViewVector(1.0f).dot(Toward) >= StareDot;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            return;
        }
        Age++;
        ServerPlayer Viewer = viewer();
        if (Viewer == null || Age > MaxTicks) {
            discard();
            return;
        }
        if (!diving()) {
            double Dx = Viewer.getX() - getX();
            double Dz = Viewer.getZ() - getZ();
            setYRot((float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f);
            Stared = staring(Viewer, position().add(0.0, getBbHeight() * 0.8, 0.0)) ? Stared + 1 : Math.max(0, Stared - 1);
            if (Age >= WaveTicks || Stared >= StareLimit || Viewer.distanceToSqr(this) < ApproachRange * ApproachRange) {
                dive();
            }
            return;
        }
        DiveAge++;
        if (DiveAge == BurstTick) {
            burst(Viewer);
        }
        if (DiveAge >= DiveTicks) {
            discard();
        }
    }

    public void dive() {
        if (!diving()) {
            entityData.set(StateData, StateDive);
            DiveAge = 0;
        }
    }

    public Vec3 burstPoint() {
        float Radians = getYRot() * Mth.DEG_TO_RAD;
        return position().add(-Mth.sin(Radians) * DiveReach, 0.1, Mth.cos(Radians) * DiveReach);
    }

    private void burst(ServerPlayer Viewer) {
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        Vec3 Point = burstPoint();
        BlockParticleOption Snow = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SNOW_BLOCK.defaultBlockState());
        World.sendParticles(Viewer, Snow, true, Point.x, Point.y + 0.3, Point.z, 70, 0.6, 0.5, 0.6, 0.25);
        World.sendParticles(Viewer, ParticleTypes.CLOUD, true, Point.x, Point.y + 0.4, Point.z, 14, 0.5, 0.3, 0.5, 0.06);
        World.sendParticles(Viewer, ParticleTypes.SNOWFLAKE, true, Point.x, Point.y + 0.6, Point.z, 30, 0.8, 0.6, 0.8, 0.05);
        long Seed = World.getRandom().nextLong();
        Viewer.connection.send(new ClientboundSoundPacket(Scp4666Registry.SightingDive, SoundSource.HOSTILE, Point.x, Point.y, Point.z, SoundVolume, 0.8f, Seed));
    }

    @Override
    public boolean broadcastToPlayer(ServerPlayer Viewer) {
        return Witness != null && Witness.equals(Viewer.getUUID());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < RenderReach * RenderReach;
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
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        return false;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag Tag) {
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "body", 4, Event -> Event.setAndContinue(diving() ? DivePlay : WaveLoop)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
