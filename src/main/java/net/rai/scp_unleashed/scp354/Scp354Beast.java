package net.rai.scp_unleashed.scp354;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.motion.ActionClock;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;

public abstract class Scp354Beast extends Monster implements GeoEntity, Scp354Emergent, Scp354Swimmer, IEntityWithComplexSpawn, ActionClock.Owner {
    public static final int DripTicks = 600;
    public static final int DripSync = 20;
    public static final int CollapseTicks = 40;
    public static final int FadeTicks = 16;
    public static final byte ActionNone = 0;
    public static final byte ActionEmerge = 1;
    public static final int SwimBlend = 6;
    public static final int ActionBlend = 2;
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp354Beast.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp354Beast.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LockedYawData = SynchedEntityData.defineId(Scp354Beast.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DripData = SynchedEntityData.defineId(Scp354Beast.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HaulData = SynchedEntityData.defineId(Scp354Beast.class, EntityDataSerializers.BOOLEAN);
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final ActionClock Clock = new ActionClock();
    private int ActionTicks;
    private int TickOrigin;
    private float RenderPartial;
    private int ClientDrip;
    private int HaulLeft;
    @Nullable
    private String LastClip;
    private int LastBlend;
    private final Scp354Swim.Course Swimming = new Scp354Swim.Course();
    @Nullable
    private Scp354Gait Gait;

    protected Scp354Beast(EntityType<? extends Scp354Beast> Type, Level World) {
        super(Type, World);
        setPersistenceRequired();
        setPathfindingMalus(PathType.DANGER_OTHER, Scp354Swim.ShoreMalus);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(LockedYawData, 0.0f);
        Builder.define(DripData, 0);
        Builder.define(HaulData, false);
    }

    @Override
    protected PathNavigation createNavigation(Level World) {
        return new Scp354Swim.DryNavigation(this, World);
    }

    @Override
    public boolean swimsOut() {
        return false;
    }

    @Override
    public double swimSpeed() {
        return 0.12;
    }

    @Override
    public double draft() {
        return getBbHeight() * 0.5;
    }

    @Override
    public double haulReach() {
        return 1.5;
    }

    @Override
    public int haulTicks() {
        return 14;
    }

    @Override
    public Scp354Swim.Course course() {
        return Swimming;
    }

    @Override
    public void beginHaul() {
        HaulLeft = haulTicks();
        entityData.set(HaulData, true);
    }

    @Override
    public boolean isHauling() {
        return entityData.get(HaulData);
    }

    public abstract int emergeTicks();

    @Nullable
    public abstract String actionClip();

    @Nullable
    protected abstract Scp354Gait.Shape gaitShape();

    protected abstract void actionTick(byte Kind, int Elapsed);

    protected abstract List<DeferredHolder<SoundEvent, SoundEvent>> deathLayers();

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean isActing() {
        return action() != ActionNone;
    }

    @Override
    public boolean isEmerging() {
        return action() == ActionEmerge;
    }

    public boolean locksMovement() {
        return isEmerging();
    }

    public boolean locksYaw() {
        return isEmerging();
    }

    public boolean isStalking() {
        return false;
    }

    public int dripTicks() {
        return level().isClientSide ? ClientDrip : entityData.get(DripData);
    }

    public void setAction(byte Kind, int Ticks) {
        entityData.set(LockedYawData, Mth.wrapDegrees(getYRot()));
        entityData.set(ActionStartData, tickCount);
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        if (locksMovement()) {
            getNavigation().stop();
        }
        applyLockedYaw();
    }

    public void endAction() {
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
    }

    public double actionElapsed() {
        if (!level().isClientSide) {
            return Math.max(0, tickCount - entityData.get(ActionStartData));
        }
        return Clock.elapsed(clockNow());
    }

    public byte shownAction() {
        return level().isClientSide ? Clock.kind(clockNow()) : action();
    }

    @Nullable
    public String shownClip() {
        return level().isClientSide ? Clock.clip(clockNow()) : actionClip();
    }

    @Override
    public ActionClock actionClock() {
        return Clock;
    }

    private double clockNow() {
        return tickCount + RenderPartial;
    }

    private void syncClock() {
        byte Current = action();
        if (Current == ActionNone) {
            Clock.finish();
            return;
        }
        Clock.begin(Current, actionClip(), tickCount, ActionClock.already(Clock.fresh(), tickCount, TickOrigin, entityData.get(ActionStartData)));
    }

    public void applyLockedYaw() {
        if (!locksYaw()) {
            return;
        }
        float Yaw = entityData.get(LockedYawData);
        setYRot(Yaw);
        yRotO = Yaw;
        yBodyRot = Yaw;
        yBodyRotO = Yaw;
        yHeadRot = Yaw;
        yHeadRotO = Yaw;
    }

    @Override
    public void beginEmerge() {
        setAction(ActionEmerge, emergeTicks());
        entityData.set(DripData, DripTicks);
        if (level() instanceof ServerLevel World) {
            Scp354Sounds.play(World, position(), Scp354Registry.EmergeLayers, 1.6f, 0.8f + random.nextFloat() * 0.15f);
            World.sendParticles(Scp354Registry.Splash.get(), getX(), getY() + 0.9, getZ(), 30, getBbWidth() * 0.5, 0.2, getBbWidth() * 0.5, 0.25);
        }
    }

    public int blendTo(String Clip, int Usual) {
        String From = LastClip;
        if (Clip.equals(From)) {
            return LastBlend;
        }
        LastClip = Clip;
        LastBlend = Usual;
        if (From != null && Usual > ActionBlend && ("swim".equals(From) || "haulout".equals(From) || "swim".equals(Clip))) {
            LastBlend = SwimBlend;
        }
        return LastBlend;
    }

    public void turnToward(LivingEntity Target, float Step) {
        float Yaw = (float) (Mth.atan2(Target.getZ() - getZ(), Target.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Mth.wrapDegrees(Mth.approachDegrees(getYRot(), Yaw, Step)));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    public double reachTo(LivingEntity Target) {
        double Dx = Target.getX() - getX();
        double Dz = Target.getZ() - getZ();
        return Math.sqrt(Dx * Dx + Dz * Dz) - Target.getBbWidth() * 0.5 - getBbWidth() * 0.5;
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf Buffer) {
        Buffer.writeVarInt(tickCount);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf Buffer) {
        TickOrigin = Buffer.readVarInt() - tickCount;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (DripData.equals(Key)) {
            ClientDrip = entityData.get(DripData);
        }
        if (level().isClientSide && (ActionData.equals(Key) || ActionStartData.equals(Key))) {
            syncClock();
        }
    }

    @Override
    public void tick() {
        super.tick();
        applyLockedYaw();
        if (level().isClientSide) {
            clientTick();
            return;
        }
        if (!isAlive()) {
            return;
        }
        if (HaulLeft > 0 && --HaulLeft == 0) {
            entityData.set(HaulData, false);
        }
        int Drip = entityData.get(DripData);
        if (Drip > 0 && tickCount % DripSync == 0) {
            entityData.set(DripData, Math.max(0, Drip - DripSync));
        }
        if (ActionTicks > 0) {
            byte Current = action();
            actionTick(Current, (int) actionElapsed());
            ActionTicks--;
            if (ActionTicks <= 0 && isAlive() && !isRemoved() && action() == Current) {
                entityData.set(ActionData, ActionNone);
                actionEnded(Current);
            }
        }
    }

    protected void actionEnded(byte Kind) {
    }

    protected void clientTick() {
        Scp354Gait Current = gait();
        if (Current != null) {
            Current.tick(this);
        }
        Clock.settle();
        if (ClientDrip > 0) {
            ClientDrip--;
            if (random.nextInt(Math.max(1, 2 + (DripTicks - ClientDrip) / 60)) == 0) {
                double Spread = getBbWidth() * 0.45;
                level().addParticle(Scp354Registry.Drip.get(), getX() + (random.nextDouble() - 0.5) * 2.0 * Spread, getY() + getBbHeight() * (0.25 + random.nextDouble() * 0.6),
                        getZ() + (random.nextDouble() - 0.5) * 2.0 * Spread, 0.0, 0.0, 0.0);
            }
        }
        if (deathTime > 0 && isDeadOrDying()) {
            int Count = 1 + deathTime / 10;
            for (int Index = 0; Index < Count; Index++) {
                double Spread = getBbWidth() * 0.6;
                level().addParticle(Scp354Registry.Drip.get(), getX() + (random.nextDouble() - 0.5) * 2.0 * Spread, getY() + getBbHeight() * random.nextDouble() * 0.5,
                        getZ() + (random.nextDouble() - 0.5) * 2.0 * Spread, 0.0, 0.0, 0.0);
            }
        }
    }

    @Nullable
    public Scp354Gait gait() {
        Scp354Gait.Shape Form = gaitShape();
        if (Gait == null && Form != null && level().isClientSide) {
            Gait = new Scp354Gait(Form);
        }
        return Gait;
    }

    public boolean isStriding() {
        Scp354Gait Current = gait();
        return Current != null && Current.striding();
    }

    public boolean isRunning() {
        Scp354Gait Current = gait();
        return Current != null && Current.striding() && Current.running();
    }

    public boolean isStalkStriding() {
        Scp354Gait Current = gait();
        return Current != null && Current.striding() && Current.stalking();
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
        Scp354Gait Current = gait();
        if (Current != null) {
            Current.setPartial(Partial);
        }
    }

    public float fade(float Partial) {
        if (!isDeadOrDying()) {
            return 1.0f;
        }
        float Time = deathTime + Partial;
        return Mth.clamp((CollapseTicks - Time) / FadeTicks, 0.0f, 1.0f);
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (level() instanceof ServerLevel World) {
            if (deathTime == 1) {
                Scp354Sounds.play(World, position(), Scp354Registry.CollapseLayers, 1.2f, 0.8f + random.nextFloat() * 0.2f);
            }
            if (deathTime % 6 == 0) {
                World.sendParticles(Scp354Registry.Splash.get(), getX(), getY() + 0.2, getZ(), 6, getBbWidth() * 0.4, 0.1, getBbWidth() * 0.4, 0.1);
            }
            if (deathTime >= CollapseTicks && !isRemoved()) {
                World.sendParticles(Scp354Registry.Splash.get(), getX(), getY() + 0.15, getZ(), 30, getBbWidth() * 0.5, 0.1, getBbWidth() * 0.5, 0.18);
                World.sendParticles(Scp354Registry.Ripple.get(), getX(), getY() + 0.05, getZ(), 4, getBbWidth() * 0.3, 0.0, getBbWidth() * 0.3, 0.0);
                remove(Entity.RemovalReason.KILLED);
            }
        }
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        endAction();
        if (level() instanceof ServerLevel World) {
            Scp354Sounds.play(World, position(), deathLayers(), 1.5f, 0.9f + random.nextFloat() * 0.1f);
        }
    }

    @Override
    public boolean canAttack(LivingEntity Target) {
        return !Target.getType().is(Scp354Registry.Spawns) && super.canAttack(Target);
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return !locksMovement() && super.isPushable();
    }

    @Override
    public void makeStuckInBlock(BlockState State, Vec3 Multiplier) {
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
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
    public AABB getBoundingBoxForCulling() {
        return super.getBoundingBoxForCulling().inflate(isActing() ? 3.0 : 1.5);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("Scp354Drip", entityData.get(DripData));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        entityData.set(DripData, Tag.getInt("Scp354Drip"));
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
