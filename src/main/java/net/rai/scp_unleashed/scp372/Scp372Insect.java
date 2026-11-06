package net.rai.scp_unleashed.scp372;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.Optional;

public class Scp372Insect extends PathfinderMob implements GeoEntity, IEntityWithComplexSpawn {
    public static final float Width = 0.78f;
    public static final float Height = 0.54f;
    public static final float EyeHeight = 0.372f;
    public static final double Health = 16.0;
    public static final double Armour = 2.0;
    public static final double Speed = 0.2;
    public static final double KnockbackResistance = 1.0;
    public static final double FollowRange = 16.0;
    public static final double WanderModifier = 0.6;
    public static final double SkitterModifier = 1.5;
    public static final double TetherRadius = 4.0;
    public static final double StrollRadius = 3.0;
    public static final double TetherHeight = 3.0;
    public static final int MaterializeTicks = 20;
    public static final int FlickerTicks = 12;
    public static final int SkitterTicks = 14;
    public static final int TiltTicks = 24;
    public static final int TiltMin = 140;
    public static final int TiltMax = 320;
    public static final int SkitterOdds = 600;
    public static final int AnchorCheck = 10;
    public static final double LookRange = 8.0;
    public static final int ChitterInterval = 220;
    public static final float ChitterVolume = 0.32f;
    public static final float HurtVolume = 0.6f;
    public static final float FlickerVolume = 0.9f;
    public static final byte ActionNone = 0;
    public static final byte ActionMaterialize = 1;
    public static final byte ActionFlicker = 2;
    public static final byte ActionSkitter = 3;
    public static final String TiltController = "tilt";
    public static final String TiltAnimation = "tilt";
    public static final Scp372Gait.Shape InsectGait = new Scp372Gait.Shape(
            new Scp372Gait.Profile(18.0, 0.0231, 0.5, 2.0, new double[]{0.398, 0.898}),
            new Scp372Gait.Profile(8.0, 0.0938, 0.6, 2.5, new double[]{0.488, 0.988}),
            0.06, 0.042, Scp372Registry.Step, 0.12f, 1.0f, 12.0);
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp372Insect.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp372Insect.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> HeldData = SynchedEntityData.defineId(Scp372Insect.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WalkLoop = RawAnimation.begin().thenLoop("walk");
    private static final RawAnimation SkitterLoop = RawAnimation.begin().thenLoop("skitter");
    private static final RawAnimation MaterializePlay = RawAnimation.begin().thenPlayAndHold("materialize");
    private static final RawAnimation FlickerPlay = RawAnimation.begin().thenPlayAndHold("flicker_out");
    private static final RawAnimation TiltPlay = RawAnimation.begin().thenPlay(TiltAnimation);
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private BlockPos Anchor;
    @Nullable
    private BlockPos Home;
    private int ActionTicks;
    private int TiltCooldown = TiltMin;
    private int TickOrigin;
    private float RenderPartial;
    @Nullable
    private Scp372Gait Gait;

    public Scp372Insect(EntityType<? extends Scp372Insect> Type, Level World) {
        super(Type, World);
        xpReward = 0;
        setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ARMOR, Armour)
                .add(Attributes.KNOCKBACK_RESISTANCE, KnockbackResistance).add(Attributes.FOLLOW_RANGE, FollowRange).add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(HeldData, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp372Goals.HoldStill(this));
        goalSelector.addGoal(5, new Scp372Goals.Stroll(this, WanderModifier));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, (float) LookRange));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public byte displayAction() {
        return isDeadOrDying() ? ActionFlicker : action();
    }

    public boolean isActing() {
        byte Current = action();
        return Current == ActionMaterialize || Current == ActionFlicker || isDeadOrDying();
    }

    public boolean locksMovement() {
        return isActing();
    }

    public boolean isFlickering() {
        return action() == ActionFlicker || isDeadOrDying();
    }

    public boolean isMaterializing() {
        return action() == ActionMaterialize;
    }

    public boolean isHeld() {
        return entityData.get(HeldData);
    }

    @Nullable
    public String actionClip() {
        return switch (displayAction()) {
            case ActionMaterialize -> "materialize";
            case ActionFlicker -> "flicker_out";
            default -> null;
        };
    }

    public void setAction(byte Kind, int Ticks) {
        entityData.set(ActionStartData, tickCount);
        entityData.set(ActionData, Kind);
        ActionTicks = Ticks;
        if (Kind == ActionMaterialize || Kind == ActionFlicker) {
            getNavigation().stop();
        }
    }

    public double actionElapsed() {
        if (isDeadOrDying()) {
            return deathTime + (level().isClientSide ? RenderPartial : 0.0f);
        }
        int Start = entityData.get(ActionStartData);
        if (!level().isClientSide) {
            return Math.max(0, tickCount - Start);
        }
        return Math.max(0.0, tickCount + TickOrigin - Start + RenderPartial);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf Buffer) {
        Buffer.writeVarInt(tickCount);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf Buffer) {
        TickOrigin = Buffer.readVarInt() - tickCount;
    }

    @Nullable
    public BlockPos anchor() {
        return Anchor;
    }

    @Nullable
    public BlockPos home() {
        return Home;
    }

    public void bindTo(BlockPos AnchorPos) {
        Anchor = AnchorPos.immutable();
        Home = null;
        entityData.set(HeldData, true);
    }

    public void materialize() {
        setAction(ActionMaterialize, MaterializeTicks);
    }

    @Nullable
    public Vec3 tetherCentre() {
        if (Anchor != null) {
            return Vec3.atBottomCenterOf(Anchor);
        }
        return Home != null ? Vec3.atBottomCenterOf(Home) : null;
    }

    @Override
    public void tick() {
        super.tick();
        if (level() instanceof ServerLevel World) {
            serverTick(World);
        } else {
            Scp372Gait Current = gait();
            if (Current != null) {
                Current.tick(this);
            }
        }
    }

    private void serverTick(ServerLevel World) {
        if (Home == null && Anchor == null && !isHeld() && !isFlickering()) {
            Home = blockPosition();
        }
        if (ActionTicks > 0 && --ActionTicks == 0) {
            byte Finished = action();
            entityData.set(ActionData, ActionNone);
            if (Finished == ActionFlicker) {
                discard();
                return;
            }
        }
        if (!isAlive() || isFlickering()) {
            return;
        }
        tether(World);
        if (tickCount % AnchorCheck == 0) {
            checkAnchor(World);
        }
        if (isActing()) {
            return;
        }
        if (--TiltCooldown <= 0) {
            TiltCooldown = TiltMin + random.nextInt(TiltMax - TiltMin + 1);
            if (World.getNearestPlayer(this, LookRange) != null) {
                triggerAnim(TiltController, TiltAnimation);
            }
        }
        if (action() == ActionNone && random.nextInt(SkitterOdds) == 0) {
            skitter();
        }
    }

    private void tether(ServerLevel World) {
        Vec3 Centre = tetherCentre();
        if (Centre == null) {
            return;
        }
        double Dx = getX() - Centre.x;
        double Dz = getZ() - Centre.z;
        if (Math.abs(getY() - Centre.y) > TetherHeight) {
            Optional<Vec3> Back = Scp372Anchoring.standingSpot(World, BlockPos.containing(Centre), getType().getDimensions());
            Back.ifPresent(Spot -> teleportTo(Spot.x, Spot.y, Spot.z));
            getNavigation().stop();
            return;
        }
        double Distance = Math.sqrt(Dx * Dx + Dz * Dz);
        if (Distance <= TetherRadius) {
            return;
        }
        double Scale = TetherRadius / Distance;
        setPos(Centre.x + Dx * Scale, getY(), Centre.z + Dz * Scale);
        Vec3 Motion = getDeltaMovement();
        setDeltaMovement(0.0, Motion.y, 0.0);
        getNavigation().stop();
    }

    private void checkAnchor(ServerLevel World) {
        if (Anchor == null || !World.isLoaded(Anchor)) {
            return;
        }
        boolean Held = Scp372State.get(World.getServer()).isHeldBy(getUUID()) && World.getBlockEntity(Anchor) instanceof Scp372AnchorBlockEntity Device && Device.holds(getUUID());
        if (!Held) {
            flickerOut();
        }
    }

    public void skitter() {
        if (level().isClientSide || isActing()) {
            return;
        }
        Optional<Vec3> Target = Scp372Goals.strollTarget(this, StrollRadius);
        if (Target.isEmpty()) {
            return;
        }
        setAction(ActionSkitter, SkitterTicks);
        getNavigation().moveTo(Target.get().x, Target.get().y, Target.get().z, SkitterModifier);
        playLayers(Scp372Registry.SkitterLayers, 0.5f, 1.15f);
    }

    public void flickerOut() {
        if (level().isClientSide || isFlickering()) {
            return;
        }
        Anchor = null;
        Home = null;
        entityData.set(HeldData, false);
        setAction(ActionFlicker, FlickerTicks);
        setInvulnerable(true);
        burst((ServerLevel) level());
        playLayers(Scp372Registry.FlickerLayers, FlickerVolume, 1.0f);
    }

    public void burst(ServerLevel World) {
        World.sendParticles(Scp372Registry.Flare.get(), getX(), getY() + 0.24, getZ(), 26, 0.54, 0.15, 0.54, 0.048);
    }

    public void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        float Shifted = Pitch * (0.94f + random.nextFloat() * 0.12f);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            level().playSound(null, getX(), getY() + 0.18, getZ(), Layer.get(), getSoundSource(), Volume, Shifted);
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (isFlickering()) {
            return false;
        }
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && isAlive() && !level().isClientSide && !isActing()) {
            skitter();
        }
        return Hurt;
    }

    @Override
    public void die(DamageSource Source) {
        boolean First = !isRemoved() && !dead;
        super.die(Source);
        if (First && level() instanceof ServerLevel World) {
            entityData.set(HeldData, false);
            burst(World);
            playLayers(Scp372Registry.FlickerLayers, FlickerVolume, 1.0f);
            Scp372Anchoring.creatureDied(World, this);
            Anchor = null;
        }
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= FlickerTicks && !level().isClientSide() && !isRemoved()) {
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return ChitterInterval;
    }

    @Override
    public void playAmbientSound() {
        if (isActing()) {
            return;
        }
        playLayers(Scp372Registry.ChitterLayers, ChitterVolume, 1.0f);
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp372Registry.Hurt.get();
    }

    @Override
    protected float getSoundVolume() {
        return HurtVolume;
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState State) {
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.HOSTILE;
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity Other) {
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
        return super.getBoundingBoxForCulling().inflate(1.32, 0.6, 1.32);
    }

    @Nullable
    public Scp372Gait gait() {
        if (Gait == null && level().isClientSide) {
            Gait = new Scp372Gait(InsectGait);
        }
        return Gait;
    }

    public boolean isStriding() {
        Scp372Gait Current = gait();
        return Current != null && Current.striding();
    }

    public boolean isSkittering() {
        Scp372Gait Current = gait();
        return Current != null && Current.striding() && Current.skittering();
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
        Scp372Gait Current = gait();
        if (Current != null) {
            Current.setPartial(Partial);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        if (Anchor != null) {
            Tag.put("Anchor", NbtUtils.writeBlockPos(Anchor));
        }
        if (Home != null) {
            Tag.put("Home", NbtUtils.writeBlockPos(Home));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Anchor = NbtUtils.readBlockPos(Tag, "Anchor").orElse(null);
        Home = NbtUtils.readBlockPos(Tag, "Home").orElse(null);
        entityData.set(HeldData, Anchor != null);
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
        setInvulnerable(false);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new Scp372GaitController(this, "body", 0, Event -> {
            byte Current = displayAction();
            Event.getController().transitionLength(Scp372GaitController.blendFor(Current));
            if (Current == ActionMaterialize) {
                return Event.setAndContinue(MaterializePlay);
            }
            if (Current == ActionFlicker) {
                return Event.setAndContinue(FlickerPlay);
            }
            if (isStriding()) {
                return Event.setAndContinue(isSkittering() ? SkitterLoop : WalkLoop);
            }
            return Event.setAndContinue(IdleLoop);
        }));
        Controllers.add(new AnimationController<>(this, TiltController, 4, Event -> PlayState.STOP).triggerableAnim(TiltAnimation, TiltPlay));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
