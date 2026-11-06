package net.rai.scp_unleashed.scp610;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.anomaly.InfectionImmunity;
import net.rai.scp_unleashed.anomaly.Targeting;
import net.rai.scp_unleashed.motion.ActionClock;
import net.rai.scp_unleashed.motion.StrideGait;
import net.rai.scp_unleashed.scp268.Scp268;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Scp610Creature extends Monster implements GeoEntity, IEntityWithComplexSpawn, ActionClock.Owner {
    public static final byte ActionNone = 0;
    public static final byte ActionEmerge = 1;
    public static final byte ActionAttack = 2;
    public static final byte ActionSpew = 3;
    public static final byte ActionEvolve = 4;
    public static final byte ActionBlink = 5;
    public static final byte ActionGrow = 6;
    public static final byte ActionTend = 7;
    public static final byte ActionWeave = 8;
    public static final byte ActionBurrow = 9;
    public static final byte ActionSurface = 10;
    public static final byte ActionSpit = 11;
    public static final byte ActionRally = 12;
    public static final byte ActionBirth = 13;
    public static final int EmergeTicks = 30;
    public static final int DeathTicks = 20;
    public static final double StillPace = 0.012;
    public static final double MaxPhaseStep = 0.11;
    public static final int LoopBlend = 3;
    public static final int ActionBlend = 2;
    public static final float LookYawLimit = 45.0f;
    public static final float LookPitchLimit = 30.0f;
    public static final float LookFollow = 0.35f;
    public static final int FlinchGap = 12;
    public static final double PhaseWrap = 64.0;
    public static final double AcquireRange = 20.0;
    public static final DustParticleOptions Gore = new DustParticleOptions(new Vector3f(0.35f, 0.15f, 0.15f), 1.2f);
    private static final EntityDataAccessor<Byte> ActionData = SynchedEntityData.defineId(Scp610Creature.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Integer> ActionStartData = SynchedEntityData.defineId(Scp610Creature.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> LockedYawData = SynchedEntityData.defineId(Scp610Creature.class, EntityDataSerializers.FLOAT);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation DeathPlay = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation HurtPlay = RawAnimation.begin().thenPlay("hurt");
    private final Scp610Form Form;
    private final Scp610Voice Voice;
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final Map<String, RawAnimation> Plays = new HashMap<>();
    private final StrideGait Walker = new StrideGait();
    private final ActionClock Clock = new ActionClock();
    protected int ActionTicks;
    private int TickOrigin;
    private float RenderPartial;
    private int FlinchReady;
    private double IdlePhase;
    private double IdlePhaseO;
    private float BodyYawO;
    private float LookYaw;
    private float LookYawO;
    private float LookPitch;
    private float LookPitchO;
    @Nullable
    private StrideGait.Shape Shape;
    @Nullable
    private Scp610Stride Gait;
    @Nullable
    private String LastLoop;
    private int LastBlend = LoopBlend;
    @Nullable
    private AnimationController<Scp610Creature> BodyController;
    @Nullable
    private LivingEntity Striking;
    @Nullable
    private BlockPos Home;

    protected Scp610Creature(EntityType<? extends Scp610Creature> Type, Level World, Scp610Form Form) {
        super(Type, World);
        this.Form = Form;
        this.Voice = Form.voice();
        setPersistenceRequired();
        xpReward = Form.heavy() ? 12 : 5;
    }

    public static AttributeSupplier.Builder attributes(Scp610Form Form) {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, Form.health())
                .add(Attributes.ATTACK_DAMAGE, Math.max(0.0, Form.damage()))
                .add(Attributes.MOVEMENT_SPEED, Form.speed())
                .add(Attributes.ARMOR, Form.armour())
                .add(Attributes.FOLLOW_RANGE, 32.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, Form.mobile() ? (Form.heavy() ? 0.7 : 0.2) : 1.0)
                .add(Attributes.STEP_HEIGHT, Form.heavy() ? 1.1 : 0.6);
    }

    public Scp610Form form() {
        return Form;
    }

    public Scp610Voice voice() {
        return Voice;
    }

    @Nullable
    public BlockPos home() {
        return Home;
    }

    public void setHome(@Nullable BlockPos Pos) {
        Home = Pos == null ? null : Pos.immutable();
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp610Goals.HoldStill(this));
        goalSelector.addGoal(2, new Scp610Goals.Chase(this, 1.25));
        goalSelector.addGoal(5, new Scp610Goals.ReturnHome(this, 0.9));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0f));
        goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new Scp610Goals.Avenge(this));
        targetSelector.addGoal(2, new Scp610Goals.Hunt(this));
    }

    public static boolean isFlesh(@Nullable Entity Found) {
        return Found instanceof Scp610Creature;
    }

    public static boolean isQuarry(LivingEntity Hunter, LivingEntity Found) {
        if (Found == Hunter || !Found.isAlive() || Found.isInvulnerable() || isFlesh(Found) || Found instanceof ArmorStand || Found instanceof Creeper
                || Found.level() != Hunter.level() || !Targeting.isTargetable(Found) || Scp610Infection.isInfected(Found)) {
            return false;
        }
        if (Found instanceof Player User) {
            return !User.isCreative() && !User.isSpectator() && !Scp268.isNeglected(User);
        }
        if (Found instanceof IronGolem) {
            return true;
        }
        return !Scp610Infection.bloodless(Found) && !InfectionImmunity.isImmune(Found, Scp610Registry.Immune);
    }

    @Override
    public boolean canAttack(LivingEntity Target) {
        return !isFlesh(Target) && super.canAttack(Target);
    }

    @Override
    public void setTarget(@Nullable LivingEntity Target) {
        if (Target != null && (isFlesh(Target) || Scp610Infection.isInfected(Target) && !(Target instanceof Player))) {
            return;
        }
        super.setTarget(Target);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ActionData, ActionNone);
        Builder.define(ActionStartData, 0);
        Builder.define(LockedYawData, 0.0f);
    }

    public byte action() {
        return entityData.get(ActionData);
    }

    public boolean isActing() {
        return action() != ActionNone;
    }

    public boolean isEmerging() {
        byte Current = action();
        return Current == ActionEmerge || Current == ActionGrow;
    }

    public boolean canAct() {
        return !isActing() && isAlive() && !isDeadOrDying();
    }

    public boolean locksMovement() {
        byte Current = action();
        return switch (Current) {
            case ActionEmerge, ActionGrow, ActionEvolve, ActionSpew, ActionTend, ActionWeave, ActionBurrow, ActionSurface, ActionSpit, ActionRally, ActionBirth -> true;
            default -> !Form.mobile();
        };
    }

    public boolean locksYaw() {
        return isEmerging() || action() == ActionEvolve;
    }

    @Nullable
    public String spawnClip() {
        return "emerge";
    }

    public int spawnTicks() {
        return EmergeTicks;
    }

    public void beginEmerge() {
        String Clip = spawnClip();
        if (Clip == null) {
            return;
        }
        setAction("grow".equals(Clip) ? ActionGrow : ActionEmerge, spawnTicks());
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        if (level() instanceof ServerLevel World) {
            playLayers(Scp610Sounds.ConvertLayers, 1.2f, Voice.Pitch());
            World.sendParticles(Gore, getX(), getY() + getBbHeight() * 0.4, getZ(), 24, getBbWidth() * 0.5, getBbHeight() * 0.3, getBbWidth() * 0.5, 0.0);
        }
    }

    @Nullable
    public String actionClip() {
        if (level().isClientSide) {
            return Clock.clip(clockNow());
        }
        return clipOf(action());
    }

    @Nullable
    protected String clipOf(byte Current) {
        return switch (Current) {
            case ActionEmerge -> "emerge";
            case ActionGrow -> "grow";
            case ActionAttack -> "attack";
            case ActionSpew -> "spew";
            case ActionEvolve -> "evolve";
            case ActionBlink -> "blink";
            case ActionTend -> "tend";
            case ActionWeave -> "weave";
            case ActionBurrow -> "burrow";
            case ActionSurface -> "surface";
            case ActionSpit -> "spit";
            case ActionRally -> "rally";
            case ActionBirth -> "birth";
            default -> null;
        };
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
        Clock.begin(Current, clipOf(Current), tickCount, ActionClock.already(Clock.fresh(), tickCount, TickOrigin, entityData.get(ActionStartData)));
    }

    public void setAction(byte Kind, int Ticks) {
        entityData.set(LockedYawData, getYRot());
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
        Striking = null;
    }

    public double actionElapsed() {
        if (!level().isClientSide) {
            return Math.max(0, tickCount - entityData.get(ActionStartData));
        }
        return Clock.elapsed(clockNow());
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
    public void writeSpawnData(RegistryFriendlyByteBuf Buffer) {
        Buffer.writeVarInt(tickCount);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf Buffer) {
        TickOrigin = Buffer.readVarInt() - tickCount;
    }

    public boolean beginAttack(LivingEntity Target) {
        if (!canAct() || Form.attackTicks() <= 0) {
            return false;
        }
        Striking = Target;
        setAction(ActionAttack, Form.attackTicks());
        playLayers(Voice.Attack(), 1.0f, Voice.Pitch());
        return true;
    }

    public boolean keepsMelee() {
        return true;
    }

    public double reachTo(Entity Target) {
        double Dx = Target.getX() - getX();
        double Dz = Target.getZ() - getZ();
        return Math.max(0.0, Math.sqrt(Dx * Dx + Dz * Dz) - (getBbWidth() + Target.getBbWidth()) * 0.5);
    }

    public double attackReach() {
        return Form.heavy() ? 1.6 : 1.0;
    }

    protected void actionTick(byte Kind, int Elapsed) {
        if (Kind == ActionAttack && Elapsed == Form.hitTick()) {
            hit();
        }
    }

    protected void hit() {
        LivingEntity Target = Striking;
        if (Target == null || !Target.isAlive() || reachTo(Target) > attackReach() + 0.7 || Math.abs(Target.getY() - getY()) > getBbHeight() + 1.0) {
            return;
        }
        strike(Target, (float) getAttributeValue(Attributes.ATTACK_DAMAGE), Form.heavy() ? 0.9 : 0.3);
    }

    public boolean strike(LivingEntity Target, float Damage, double Knock) {
        if (!Target.hurt(damageSources().mobAttack(this), Damage)) {
            return false;
        }
        if (Knock > 0.0) {
            Target.knockback(Knock, getX() - Target.getX(), getZ() - Target.getZ());
            Target.hurtMarked = true;
        }
        setLastHurtMob(Target);
        return true;
    }

    protected void finishAction(byte Kind) {
        endAction();
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
        FlinchReady = Math.max(0, FlinchReady - 1);
        if (ActionTicks > 0) {
            byte Current = action();
            actionTick(Current, (int) actionElapsed());
            ActionTicks--;
            if (ActionTicks <= 0 && isAlive() && !isRemoved() && action() == Current) {
                finishAction(Current);
            }
        }
        serverTick();
    }

    protected void serverTick() {
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (locksMovement()) {
            getNavigation().stop();
        }
    }

    private void clientTick() {
        gaitTick();
        lookTick();
        Clock.settle();
    }

    private void lookTick() {
        LookYawO = LookYaw;
        LookPitchO = LookPitch;
        boolean Free = isAlive() && deathTime <= 0 && !locksYaw();
        float WantYaw = Free ? Mth.clamp(Mth.wrapDegrees(yHeadRot - yBodyRot), -LookYawLimit, LookYawLimit) : 0.0f;
        float WantPitch = Free ? Mth.clamp(Mth.wrapDegrees(getXRot()), -LookPitchLimit, LookPitchLimit) : 0.0f;
        LookYaw += (WantYaw - LookYaw) * LookFollow;
        LookPitch += (WantPitch - LookPitch) * LookFollow;
    }

    public float lookYaw(float Partial) {
        return Mth.lerp(Partial, LookYawO, LookYaw);
    }

    public float lookPitch(float Partial) {
        return Mth.lerp(Partial, LookPitchO, LookPitch);
    }

    private StrideGait.Shape shape() {
        if (Shape == null) {
            Scp610Stride Walk = Form.walk();
            Scp610Stride Run = Form.run();
            double Ratio = Form.speed() / 0.25;
            Shape = new StrideGait.Shape(StillPace, 0.135 * Ratio, 0.11 * Ratio, Walk == null ? 1.0 : Walk.distance(), Run == null ? 1.0 : Run.distance(), MaxPhaseStep, LoopBlend);
        }
        return Shape;
    }

    private void gaitTick() {
        IdlePhaseO = IdlePhase;
        IdlePhase += 1.0 / 60.0;
        if (IdlePhase > PhaseWrap) {
            IdlePhase -= PhaseWrap;
            IdlePhaseO -= PhaseWrap;
        }
        if (!Form.mobile()) {
            Gait = null;
            return;
        }
        double Dx = getX() - xo;
        double Dz = getZ() - zo;
        double Turn = Math.abs(Mth.wrapDegrees(yBodyRot - BodyYawO)) * Mth.DEG_TO_RAD * 0.6;
        BodyYawO = yBodyRot;
        boolean Stepped = Walker.tick(Math.sqrt(Dx * Dx + Dz * Dz) + Math.min(Turn, 0.2), isAlive(), shape());
        Gait = Walker.stride() == StrideGait.Idle ? null : Walker.stride() == StrideGait.Run ? Form.run() : Form.walk();
        if (Gait == null || !Stepped || isSilent() || isActing() || !onGround()) {
            return;
        }
        float[] Touch = Gait.touchdowns();
        for (int Limb = 0; Limb < Touch.length; Limb++) {
            if (Scp610Stride.crossed(Walker.from(), Walker.to(), Touch[Limb])) {
                footfall(Limb, Gait == Form.run());
            }
        }
    }

    private void footfall(int Limb, boolean Running) {
        if (!level().hasNearbyAlivePlayer(getX(), getY(), getZ(), 24.0)) {
            return;
        }
        float Volume = Voice.StepVolume() * (Running ? 1.15f : 0.85f) * (0.88f + random.nextFloat() * 0.12f);
        float Pitch = Voice.Pitch() * (0.92f + random.nextFloat() * 0.12f) * (Limb % 2 == 0 ? 1.0f : 0.96f);
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Voice.Step()) {
            level().playLocalSound(getX(), getY(), getZ(), Layer.get(), getSoundSource(), Volume, Pitch, false);
        }
    }

    @Nullable
    public Scp610Stride gait() {
        return Gait;
    }

    @Nullable
    public Scp610Stride strideNamed(String Clip) {
        Scp610Stride Walk = Form.walk();
        Scp610Stride Run = Form.run();
        if (Walk != null && Walk.clip().equals(Clip)) {
            return Walk;
        }
        if (Run != null && Run.clip().equals(Clip)) {
            return Run;
        }
        return null;
    }

    public double gaitCycle(float Partial) {
        return Walker.cycle(Partial);
    }

    public double idleCycle(float Partial) {
        double Value = Mth.lerp(Partial, IdlePhaseO, IdlePhase);
        return Value - Math.floor(Value);
    }

    public void setRenderPartial(float Partial) {
        RenderPartial = Partial;
    }

    public float renderScale() {
        return Form.scale();
    }

    public void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        float Shift = 0.94f + random.nextFloat() * 0.12f;
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            playSound(Layer.get(), Volume, Pitch * Shift);
        }
    }

    public void faceToward(Entity Target, float Step) {
        float Yaw = (float) (Mth.atan2(Target.getZ() - getZ(), Target.getX() - getX()) * Mth.RAD_TO_DEG) - 90.0f;
        setYRot(Mth.wrapDegrees(Mth.approachDegrees(getYRot(), Yaw, Step)));
        yBodyRot = getYRot();
        yHeadRot = getYRot();
    }

    @Override
    public boolean isInvulnerableTo(DamageSource Source) {
        if (!Form.mobile() && (Source.is(DamageTypes.IN_WALL) || Source.is(DamageTypes.CRAMMING) || Source.is(DamageTypes.FALL))) {
            return true;
        }
        return isEmerging() && !Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY) || super.isInvulnerableTo(Source);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hurt = super.hurt(Source, Amount);
        if (Hurt && level() instanceof ServerLevel World && isAlive()) {
            World.sendParticles(Gore, getX(), getY() + getBbHeight() * 0.6, getZ(), 6, getBbWidth() * 0.3, getBbHeight() * 0.2, getBbWidth() * 0.3, 0.0);
            if (FlinchReady <= 0 && !isActing()) {
                FlinchReady = FlinchGap;
                triggerAnim("flinch", "hurt");
            }
        }
        return Hurt;
    }

    @Override
    public void knockback(double Strength, double X, double Z) {
        if (!isEmerging() && Form.mobile()) {
            super.knockback(Strength, X, Z);
        }
    }

    @Override
    public boolean isPushable() {
        return !locksMovement() && super.isPushable();
    }

    public void collapse() {
        if (!isAlive()) {
            return;
        }
        if (level() instanceof ServerLevel World) {
            World.sendParticles(Gore, getX(), getY() + getBbHeight() * 0.5, getZ(), 20, getBbWidth() * 0.4, getBbHeight() * 0.3, getBbWidth() * 0.4, 0.0);
        }
        kill();
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (!(level() instanceof ServerLevel World)) {
            return;
        }
        if (deathTime == DeathTicks - 4) {
            World.sendParticles(Gore, getX(), getY() + getBbHeight() * 0.3, getZ(), 30, getBbWidth() * 0.5, getBbHeight() * 0.3, getBbWidth() * 0.5, 0.0);
            playSound(Scp610Sounds.WetHeavy.get(), 1.0f, 0.8f);
        }
        if (deathTime >= DeathTicks && !isRemoved()) {
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public void die(DamageSource Source) {
        boolean Fresh = !isRemoved() && !dead;
        super.die(Source);
        if (Fresh && level() instanceof ServerLevel) {
            endAction();
            getNavigation().stop();
            List<DeferredHolder<SoundEvent, SoundEvent>> Layers = Voice.Death();
            for (int Index = 1; Index < Layers.size(); Index++) {
                playSound(Layers.get(Index).get(), getSoundVolume(), Voice.Pitch());
            }
        }
    }

    @Override
    public void playAmbientSound() {
        if (isActing() || isSilent()) {
            return;
        }
        playLayers(Voice.Idle(), getSoundVolume(), Voice.Pitch());
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Voice.Idle().get(0).get();
    }

    @Override
    protected void playHurtSound(DamageSource Source) {
        playLayers(Voice.Hurt(), getSoundVolume(), Voice.Pitch());
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Voice.Hurt().get(0).get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Voice.Death().get(0).get();
    }

    @Override
    public float getVoicePitch() {
        return Voice.Pitch() * (0.94f + random.nextFloat() * 0.12f);
    }

    @Override
    protected float getSoundVolume() {
        return Form.heavy() ? 1.5f : 1.0f;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
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
        return super.getBoundingBoxForCulling().inflate(renderScale());
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Key) {
        super.onSyncedDataUpdated(Key);
        if (!level().isClientSide || !Key.equals(ActionData) && !Key.equals(ActionStartData)) {
            return;
        }
        syncClock();
        if (Key.equals(ActionData) && BodyController != null && action() != ActionNone) {
            BodyController.forceAnimationReset();
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        if (isEmerging()) {
            Tag.putInt("Emerge", ActionTicks);
            Tag.putBoolean("Grow", action() == ActionGrow);
        }
        if (Home != null) {
            Tag.put("Home", NbtUtils.writeBlockPos(Home));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        entityData.set(ActionData, ActionNone);
        ActionTicks = 0;
        if (Tag.getInt("Emerge") > 0) {
            setAction(Tag.getBoolean("Grow") ? ActionGrow : ActionEmerge, Tag.getInt("Emerge"));
        }
        Home = NbtUtils.readBlockPos(Tag, "Home").orElse(null);
    }

    protected RawAnimation play(String Clip) {
        return Plays.computeIfAbsent(Clip, Name -> RawAnimation.begin().thenPlayAndHold(Name));
    }

    @Nullable
    protected String loopClip() {
        return null;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Scp610Controller Body = new Scp610Controller(this, "body", Event -> {
            if (deathTime > 0 || isDeadOrDying()) {
                return Event.setAndContinue(choose(Event, "death", DeathPlay));
            }
            String Clip = actionClip();
            if (Clip != null) {
                RawAnimation Wanted = choose(Event, Clip, play(Clip));
                if (Clip.equals(spawnClip())) {
                    Event.getController().transitionLength(0);
                }
                return Event.setAndContinue(Wanted);
            }
            String Loop = loopClip();
            if (Loop != null) {
                return Event.setAndContinue(choose(Event, Loop, RawAnimation.begin().thenLoop(Loop)));
            }
            if (Gait != null) {
                return Event.setAndContinue(choose(Event, Gait.clip(), Gait.loop()));
            }
            return Event.setAndContinue(choose(Event, "idle", IdleLoop));
        });
        BodyController = Body;
        Controllers.add(Body);
        Controllers.add(new AnimationController<>(this, "flinch", 1, Event -> PlayState.STOP).triggerableAnim("hurt", HurtPlay));
    }

    private RawAnimation choose(AnimationState<Scp610Creature> Event, String Clip, RawAnimation Wanted) {
        if (!Clip.equals(LastLoop)) {
            LastBlend = Clip.equals(actionClip()) || "death".equals(Clip) ? ActionBlend : LoopBlend;
            LastLoop = Clip;
        }
        Event.getController().transitionLength(LastBlend);
        return Wanted;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
