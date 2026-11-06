package net.rai.scp_unleashed.daevite.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidType;
import net.rai.scp_unleashed.daevite.DaeviteCollapse;
import net.rai.scp_unleashed.daevite.DaeviteRegistry;
import net.rai.scp_unleashed.daevite.DaeviteTargets;
import net.rai.scp_unleashed.dclass.DClassLike;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.Optional;

public abstract class DaeviteMob extends DClassLike {
    public static final int ScanEvery = 10;
    public static final double SenseRange = 10.0;
    public static final double LoseRange = 56.0;
    public static final int LoseSight = 300;
    public static final double HomeLeash = 18.0;
    public static final int DeathTicks = 34;
    public static final byte MoveNone = 0;
    public static final RawAnimation CollapsePlay = RawAnimation.begin().thenPlayAndHold("collapse");
    protected static final EntityDataAccessor<Byte> MoveData = SynchedEntityData.defineId(DaeviteMob.class, EntityDataSerializers.BYTE);
    protected static final EntityDataAccessor<Integer> SerialData = SynchedEntityData.defineId(DaeviteMob.class, EntityDataSerializers.INT);

    @Nullable
    private BlockPos Home;
    @Nullable
    private Vec3 Patrol;
    private long PatrolUntil;
    private int MoveStart;
    private int LocalMoveStart;
    private float RenderPartial;
    private int LastSeen;
    private int Doom = -1;
    private boolean Unmade;
    @Nullable
    private DaeviteMoveController<DaeviteMob> MoveController;

    protected DaeviteMob(EntityType<? extends DaeviteMob> Type, Level World) {
        super(Type, World);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(MoveData, MoveNone);
        Builder.define(SerialData, 0);
    }

    public abstract String clipFor(byte Move);

    public abstract boolean loopingMove();

    protected abstract boolean seesThroughRobes();

    public abstract String roleName();

    protected abstract void serverCombat(ServerLevel World);

    public abstract void fight(DaeviteGoals.Fight Pursuit, LivingEntity Prey);

    public byte move() {
        return entityData.get(MoveData);
    }

    public boolean moving(byte Kind) {
        return move() == Kind;
    }

    protected void startMove(byte Kind) {
        MoveStart = tickCount;
        entityData.set(MoveData, Kind);
        entityData.set(SerialData, entityData.get(SerialData) + 1);
    }

    protected void endMove() {
        if (move() != MoveNone) {
            entityData.set(MoveData, MoveNone);
        }
    }

    public int moveTicks() {
        return tickCount - MoveStart;
    }

    public double moveElapsed() {
        if (!level().isClientSide) {
            return tickCount - MoveStart;
        }
        return Math.max(0.0, tickCount - LocalMoveStart + RenderPartial);
    }

    @Nullable
    public String moveClip() {
        byte Current = move();
        return Current == MoveNone ? null : clipFor(Current);
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    public float renderPartial() {
        return RenderPartial;
    }

    public boolean isPrey(@Nullable Entity Found) {
        return DaeviteTargets.isPrey(this, Found, seesThroughRobes());
    }

    @Nullable
    public BlockPos home() {
        return Home;
    }

    public void setHome(BlockPos Pos) {
        Home = Pos.immutable();
    }

    @Nullable
    public Vec3 patrolTarget() {
        if (Patrol != null && level().getGameTime() > PatrolUntil) {
            Patrol = null;
        }
        return Patrol;
    }

    public void setPatrol(Vec3 Target, long Until) {
        Patrol = Target;
        PatrolUntil = Until;
    }

    public void clearPatrol() {
        Patrol = null;
    }

    public boolean onPatrol() {
        return patrolTarget() != null;
    }

    public void doom(int Delay) {
        if (Doom < 0 && !isDeadOrDying()) {
            Doom = Math.max(1, Delay);
        }
    }

    public boolean doomed() {
        return Doom >= 0 || Unmade;
    }

    public boolean unmade() {
        return Unmade;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Accessor) {
        super.onSyncedDataUpdated(Accessor);
        if (level().isClientSide && SerialData.equals(Accessor)) {
            LocalMoveStart = tickCount;
            if (MoveController != null) {
                MoveController.forceAnimationReset();
            }
        }
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Group);
        if (Home == null) {
            Home = blockPosition();
        }
        return Result;
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (Doom >= 0) {
            getNavigation().stop();
            endMove();
            if (Doom % 6 == 0) {
                World.sendParticles(DaeviteCollapse.Cinder, getX(), getY() + getBbHeight() * 0.6, getZ(), 4, 0.25, 0.4, 0.25, 0.02);
            }
            if (--Doom <= 0) {
                Unmade = true;
                kill();
            }
            return;
        }
        scan(World);
        setAggressive(getTarget() != null);
        serverCombat(World);
    }

    protected void scan(ServerLevel World) {
        LivingEntity Current = getTarget();
        if (Current != null) {
            if (!isPrey(Current) || Current.level() != level() || distanceToSqr(Current) > LoseRange * LoseRange) {
                setTarget(null);
            } else if (getSensing().hasLineOfSight(Current) || distanceToSqr(Current) < SenseRange * SenseRange) {
                LastSeen = tickCount;
            } else if (tickCount - LastSeen > LoseSight) {
                setTarget(null);
            }
        }
        if (getTarget() != null || (tickCount + getId()) % ScanEvery != 0) {
            return;
        }
        double Range = getAttributeValue(Attributes.FOLLOW_RANGE);
        LivingEntity Best = null;
        double BestGap = Double.MAX_VALUE;
        for (LivingEntity Found : World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(Range, Range * 0.5, Range), this::isPrey)) {
            double Gap = distanceToSqr(Found);
            if (Gap < BestGap && (Gap < SenseRange * SenseRange || hasLineOfSight(Found))) {
                BestGap = Gap;
                Best = Found;
            }
        }
        if (Best != null) {
            pursue(Best);
        }
    }

    public void pursue(LivingEntity Prey) {
        setTarget(Prey);
        LastSeen = tickCount;
    }

    protected void turnTo(Entity Prey, float Step) {
        double Dx = Prey.getX() - getX();
        double Dz = Prey.getZ() - getZ();
        float Yaw = (float) (Mth.atan2(Dz, Dx) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Mth.approachDegrees(getYRot(), Yaw, Step));
        setYBodyRot(getYRot());
        setYHeadRot(getYRot());
    }

    protected boolean facing(Entity Prey, double Cosine) {
        Vec3 Toward = Prey.position().subtract(position()).multiply(1.0, 0.0, 1.0);
        Vec3 Look = Vec3.directionFromRotation(0.0f, getYRot());
        return Toward.lengthSqr() < 1.0e-4 || Toward.normalize().dot(Look) >= Cosine;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        if (DaeviteTargets.isAlly(Source.getEntity()) || DaeviteTargets.isAlly(Source.getDirectEntity())) {
            return true;
        }
        return super.isInvulnerableTo(Source);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && !level().isClientSide && isAlive() && Doom < 0) {
            Entity Attacker = Source.getEntity();
            if (Attacker instanceof Player User && !User.isCreative()) {
                DaeviteTargets.provoke(User);
            }
            if (Attacker instanceof LivingEntity Body && isPrey(Body)) {
                LivingEntity Current = getTarget();
                if (Current == null || distanceToSqr(Body) < distanceToSqr(Current) * 0.6) {
                    pursue(Body);
                }
            }
        }
        return Hurt;
    }

    @Override
    public boolean isAlliedTo(Entity Other) {
        return DaeviteTargets.isAlly(Other) || super.isAlliedTo(Other);
    }

    @Override
    protected boolean shouldDropLoot() {
        return !Unmade && super.shouldDropLoot();
    }

    @Override
    public boolean shouldDropExperience() {
        return !Unmade && super.shouldDropExperience();
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        AABB Box = getBoundingBox();
        double Top = Box.maxY - (Box.maxY - Box.minY) * Math.min(1.0, deathTime / (double) DeathTicks) * 0.85;
        if (deathTime == 1) {
            World.playSound(null, getX(), getY(), getZ(), DaeviteRegistry.Collapse.get(), SoundSource.HOSTILE, 1.3f, 0.75f + random.nextFloat() * 0.15f);
        }
        World.sendParticles(DaeviteCollapse.Ash, getX(), (Box.minY + Top) * 0.5, getZ(), 3 + deathTime / 5, 0.28, (Top - Box.minY) * 0.3, 0.28, 0.02);
        World.sendParticles(DaeviteCollapse.Cinder, getX(), Top, getZ(), 2, 0.25, 0.15, 0.25, 0.03);
        if (deathTime % 5 == 0) {
            World.sendParticles(ParticleTypes.SMALL_FLAME, getX(), Box.minY + 0.3, getZ(), 2, 0.3, 0.1, 0.3, 0.01);
            World.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), Box.minY + 0.5, getZ(), 1, 0.3, 0.1, 0.3, 0.01);
        }
        if (deathTime >= DeathTicks && !isRemoved()) {
            World.sendParticles(DaeviteCollapse.Ash, getX(), getY() + 0.3, getZ(), 36, 0.45, 0.2, 0.45, 0.03);
            World.sendParticles(DaeviteCollapse.Cinder, getX(), getY() + 0.3, getZ(), 14, 0.4, 0.2, 0.4, 0.05);
            remove(RemovalReason.KILLED);
        }
    }

    protected abstract RawAnimation liveBody();

    @Override
    protected RawAnimation bodyAnimation() {
        if (isDeadOrDying()) {
            return CollapsePlay;
        }
        return liveBody();
    }

    protected abstract RawAnimation movePlay(byte Move);

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        super.registerControllers(Controllers);
        MoveController = new DaeviteMoveController<>(this, "move", 2, Event -> {
            byte Current = move();
            if (Current == MoveNone || isDeadOrDying()) {
                return PlayState.STOP;
            }
            RawAnimation Play = movePlay(Current);
            return Play == null ? PlayState.STOP : Event.setAndContinue(Play);
        });
        Controllers.add(MoveController);
    }

    @Override
    public boolean eyesShut() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean canChangeDimensions(Level From, Level To) {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean Passengers) {
        return false;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    @Override
    public boolean canDrownInFluidType(FluidType Kind) {
        return false;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(1.6, 0.6, 1.6);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        if (Home != null) {
            Saved.put("Home", NbtUtils.writeBlockPos(Home));
        }
        if (Patrol != null) {
            Saved.putDouble("PatrolX", Patrol.x);
            Saved.putDouble("PatrolY", Patrol.y);
            Saved.putDouble("PatrolZ", Patrol.z);
            Saved.putLong("PatrolUntil", PatrolUntil);
        }
        if (Doom >= 0) {
            Saved.putInt("Doom", Doom);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        Optional<BlockPos> Found = NbtUtils.readBlockPos(Saved, "Home");
        Home = Found.orElse(null);
        if (Saved.contains("PatrolX")) {
            Patrol = new Vec3(Saved.getDouble("PatrolX"), Saved.getDouble("PatrolY"), Saved.getDouble("PatrolZ"));
            PatrolUntil = Saved.getLong("PatrolUntil");
        }
        if (Saved.contains("Doom")) {
            Doom = Saved.getInt("Doom");
        }
    }
}
