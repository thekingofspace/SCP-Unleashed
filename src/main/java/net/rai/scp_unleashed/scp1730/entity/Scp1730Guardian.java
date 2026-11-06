package net.rai.scp_unleashed.scp1730.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.rai.scp_unleashed.scp1730.Scp1730Anchors;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.Scp1730Shockwaves;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Scp1730Guardian extends Monster implements GeoEntity {
    public static final float Grown = 1.5f;
    public static final float Width = 3.4f * Grown;
    public static final float Height = 6.0f * Grown;
    public static final float EyeHeight = 5.0f * Grown;
    public static final double Hover = 0.75 * Grown;
    public static final double Glide = 0.11;
    public static final double KeepAway = 5.0 * Grown;
    public static final double Leash = 22.0 * Grown;
    public static final double ArenaReach = 40.0;
    public static final int RegenEvery = 20;
    public static final float RegenAmount = 4.0f;
    public static final int DeathTicks = 60;
    public static final double AuraRadius = 14.0;
    public static final int AuraEvery = 10;
    public static final byte ActionNone = 0;
    public static final byte ActionSwipe = 1;
    public static final byte ActionGrab = 2;
    public static final byte ActionSlam = 3;
    public static final byte ActionPush = 4;
    public static final int SwipeLength = 18;
    public static final int SwipeHit = 9;
    public static final int GrabLength = 26;
    public static final int GrabHold = 10;
    public static final int GrabThrow = 20;
    public static final int SlamLength = 32;
    public static final int SlamImpact = 18;
    public static final int PushLength = 22;
    public static final int PushRelease = 12;
    public static final float SwipeDamage = 6.0f;
    public static final float ThrowDamage = 4.0f;
    public static final float PushDamage = 3.0f;
    public static final double SwipeReach = 5.5 * Grown;
    public static final double GrabReach = 6.5 * Grown;
    public static final double PushReach = 9.0 * Grown;
    public static final double Chest = 3.0 * Grown;
    public static final net.minecraft.resources.ResourceLocation GravityId = Scp1730Registry.location("scp1730_aura_gravity");
    private static final EntityDataAccessor<Boolean> DataAnchored = SynchedEntityData.defineId(Scp1730Guardian.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Byte> DataAction = SynchedEntityData.defineId(Scp1730Guardian.class, EntityDataSerializers.BYTE);
    private static final EntityDataAccessor<Boolean> DataCasting = SynchedEntityData.defineId(Scp1730Guardian.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation Idle = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation Draw = RawAnimation.begin().thenLoop("draw");
    private static final RawAnimation GlideLoop = RawAnimation.begin().thenLoop("glide");
    private static final RawAnimation AnchoredLoop = RawAnimation.begin().thenLoop("anchored");
    private static final RawAnimation SwipeOnce = RawAnimation.begin().thenPlay("swipe");
    private static final RawAnimation GrabOnce = RawAnimation.begin().thenPlay("grab");
    private static final RawAnimation SlamOnce = RawAnimation.begin().thenPlay("slam");
    private static final RawAnimation PushOnce = RawAnimation.begin().thenPlay("push");
    private static final RawAnimation DeathHold = RawAnimation.begin().thenPlayAndHold("death");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent Bar = new ServerBossEvent(Component.translatable("entity.scp_unleashed.scp1730_2"), BossEvent.BossBarColor.WHITE, BossEvent.BossBarOverlay.NOTCHED_10);
    private final Set<UUID> Lightened = new HashSet<>();
    @Nullable
    private BlockPos Home;
    private int ActionTicks;
    private int Cooldown = 40;
    private int Held = -1;

    public Scp1730Guardian(EntityType<? extends Scp1730Guardian> Type, Level World) {
        super(Type, World);
        setNoGravity(true);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 600.0).add(Attributes.ARMOR, 10.0).add(Attributes.FOLLOW_RANGE, 40.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.ATTACK_DAMAGE, 6.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(DataAnchored, false);
        Builder.define(DataAction, ActionNone);
        Builder.define(DataCasting, false);
    }

    public boolean anchored() {
        return entityData.get(DataAnchored);
    }

    public byte action() {
        return entityData.get(DataAction);
    }

    public boolean casting() {
        return entityData.get(DataCasting) || action() != ActionNone;
    }

    public void setHome(BlockPos Pos) {
        Home = Pos.immutable();
    }

    public Vec3 glyphPoint() {
        return position().add(0.0, Height * 0.62, 0.0);
    }

    protected boolean showsBar() {
        return true;
    }

    public boolean barShownTo(ServerPlayer User) {
        return Bar.getPlayers().contains(User);
    }

    public boolean senseAnchor() {
        return Scp1730Anchors.anchored(this);
    }

    protected double leash() {
        return Leash;
    }

    protected boolean swipeable(LivingEntity Each) {
        return Each instanceof Player && Each.isAlive();
    }

    protected boolean pushHurts(LivingEntity Each) {
        return Each instanceof Player;
    }

    protected void regenerate() {
        if (!anchored() && tickCount % RegenEvery == 0 && getHealth() < getMaxHealth()) {
            heal(RegenAmount);
        }
    }

    protected void fell(ServerLevel Server) {
        net.rai.scp_unleashed.scp1730.Scp1730Collapse.guardianFell(Server, this);
    }

    @Override
    protected void actuallyHurt(DamageSource Source, float Amount) {
        float Before = getHealth();
        super.actuallyHurt(Source, Amount);
        net.rai.scp_unleashed.scp1730.guardian.Scp1730GuardianCapture.window(this, Source, Before);
    }

    @Override
    public void startSeenByPlayer(ServerPlayer User) {
        super.startSeenByPlayer(User);
        if (showsBar()) {
            Bar.addPlayer(User);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer User) {
        super.stopSeenByPlayer(User);
        Bar.removePlayer(User);
        lighten(User, false);
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(Source, Amount);
        }
        if (!anchored() || isDeadOrDying()) {
            if (!level().isClientSide && Source.getEntity() != null) {
                level().playSound(null, getX(), getY() + Chest, getZ(), Scp1730Registry.GuardianWard.get(), SoundSource.HOSTILE, 1.0f, 0.8f + random.nextFloat() * 0.3f);
            }
            return false;
        }
        return super.hurt(Source, Amount);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return Scp1730Registry.GuardianChains.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 120;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp1730Registry.GuardianHurt.get();
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return Scp1730Registry.GuardianDeath.get();
    }

    @Override
    protected float getSoundVolume() {
        return 2.0f;
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (deathTime >= DeathTicks && !level().isClientSide && !isRemoved()) {
            level().broadcastEntityEvent(this, (byte) 60);
            remove(RemovalReason.KILLED);
        }
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        entityData.set(DataAction, ActionNone);
        releaseHeld();
        if (level() instanceof ServerLevel Server) {
            for (ServerPlayer User : Server.players()) {
                lighten(User, false);
            }
            fell(Server);
        }
    }

    @Override
    public void remove(RemovalReason Reason) {
        Bar.removeAllPlayers();
        if (level() instanceof ServerLevel Server) {
            for (ServerPlayer User : Server.players()) {
                lighten(User, false);
            }
        }
        super.remove(Reason);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            clientGlyphs();
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide) {
            return;
        }
        Bar.setProgress(getHealth() / getMaxHealth());
        if (tickCount % 5 == 0) {
            boolean Now = senseAnchor();
            if (Now != anchored()) {
                entityData.set(DataAnchored, Now);
                level().playSound(null, getX(), getY() + Chest, getZ(), Scp1730Registry.GuardianCast.get(), SoundSource.HOSTILE, 2.0f, Now ? 0.5f : 1.2f);
            }
        }
        if (isDeadOrDying()) {
            return;
        }
        regenerate();
        if (tickCount % AuraEvery == 0) {
            aura((ServerLevel) level());
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        ServerLevel World = (ServerLevel) level();
        LivingEntity Target = pickTarget(World);
        setTarget(Target);
        entityData.set(DataCasting, Target != null);
        if (Home == null) {
            Home = blockPosition();
        }
        tickAction(World, Target);
        glide(World, Target);
    }

    @Nullable
    protected LivingEntity pickTarget(ServerLevel World) {
        Player Best = null;
        double BestDistance = ArenaReach * ArenaReach;
        for (Player Each : World.getEntitiesOfClass(Player.class, getBoundingBox().inflate(ArenaReach), Each -> Each.isAlive() && !Each.isSpectator() && !Each.isCreative())) {
            double Distance = Each.distanceToSqr(this);
            if (Distance < BestDistance && (Home == null || Each.blockPosition().distSqr(Home) < (Leash + 12.0) * (Leash + 12.0))) {
                Best = Each;
                BestDistance = Distance;
            }
        }
        return Best;
    }

    private double floorBelow(ServerLevel World, Vec3 From) {
        BlockHitResult Hit = World.clip(new ClipContext(From, From.add(0.0, -16.0, 0.0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        return Hit.getType() == HitResult.Type.MISS ? From.y - 16.0 : Hit.getLocation().y;
    }

    private void glide(ServerLevel World, @Nullable LivingEntity Target) {
        Vec3 Here = position();
        double Floor = floorBelow(World, Here.add(0.0, 1.0, 0.0));
        byte Doing = action();
        double Lift = Hover + Math.sin(tickCount * 0.05) * 0.25;
        if (Doing == ActionSlam) {
            Lift = ActionTicks < SlamImpact ? Mth.lerp(ActionTicks / (double) SlamImpact, Hover, 0.05) : Mth.lerp((ActionTicks - SlamImpact) / (double) (SlamLength - SlamImpact), 0.05, Hover);
        }
        double WantY = Floor + Lift;
        Vec3 Goal = Here;
        if (Target != null && Doing == ActionNone) {
            Vec3 Toward = Target.position().subtract(Here);
            double Flat = Math.sqrt(Toward.x * Toward.x + Toward.z * Toward.z);
            if (Flat > KeepAway + 1.0) {
                Goal = Here.add(Toward.x / Flat * Glide, 0.0, Toward.z / Flat * Glide);
            } else if (Flat < KeepAway - 1.5) {
                Goal = Here.subtract(Toward.x / Flat * Glide * 0.6, 0.0, Toward.z / Flat * Glide * 0.6);
            }
            getLookControl().setLookAt(Target, 10.0f, 10.0f);
            setYRot(Mth.approachDegrees(getYRot(), (float) (Mth.atan2(Toward.z, Toward.x) * Mth.RAD_TO_DEG) - 90.0f, 4.0f));
            yBodyRot = getYRot();
        } else if (Target == null && Home != null && Here.distanceToSqr(Vec3.atBottomCenterOf(Home)) > 4.0) {
            Vec3 Back = Vec3.atBottomCenterOf(Home).subtract(Here);
            double Flat = Math.max(0.01, Math.sqrt(Back.x * Back.x + Back.z * Back.z));
            Goal = Here.add(Back.x / Flat * Glide, 0.0, Back.z / Flat * Glide);
        }
        if (Home != null && Goal.distanceToSqr(Vec3.atBottomCenterOf(Home)) > leash() * leash()) {
            Goal = Here;
        }
        if (anchored()) {
            Goal = Here.add(Goal.subtract(Here).scale(0.4));
        }
        Vec3 Step = new Vec3(Goal.x - Here.x, Mth.clamp(WantY - Here.y, -0.25, 0.2), Goal.z - Here.z);
        setDeltaMovement(Step);
        move(MoverType.SELF, Step);
        setDeltaMovement(Vec3.ZERO);
    }

    private void tickAction(ServerLevel World, @Nullable LivingEntity Target) {
        byte Doing = action();
        if (Doing == ActionNone) {
            if (Cooldown > 0) {
                Cooldown--;
                return;
            }
            if (Target == null) {
                return;
            }
            double Distance = Target.distanceTo(this) - Width * 0.5;
            float Roll = random.nextFloat();
            byte Chosen;
            if (Distance < SwipeReach) {
                Chosen = Roll < 0.55f ? ActionSwipe : Roll < 0.85f ? ActionGrab : ActionPush;
            } else if (Distance < PushReach) {
                Chosen = Roll < 0.45f ? ActionSlam : Roll < 0.75f ? ActionPush : ActionNone;
            } else {
                Chosen = Roll < 0.6f ? ActionSlam : ActionNone;
            }
            if (Chosen != ActionNone) {
                begin(World, Chosen);
            } else {
                Cooldown = 10;
            }
            return;
        }
        ActionTicks++;
        switch (Doing) {
            case ActionSwipe -> {
                if (ActionTicks == SwipeHit) {
                    swipe(World);
                }
                if (ActionTicks >= SwipeLength) {
                    finish();
                }
            }
            case ActionGrab -> {
                if (ActionTicks == GrabHold && Target != null && Target.distanceTo(this) - Width * 0.5 < GrabReach) {
                    Held = Target.getId();
                    World.playSound(null, Target.getX(), Target.getY(), Target.getZ(), Scp1730Registry.GuardianGrab.get(), SoundSource.HOSTILE, 1.5f, 1.0f);
                }
                Entity Grabbed = Held >= 0 ? World.getEntity(Held) : null;
                if (Grabbed instanceof LivingEntity Victim && Victim.isAlive() && ActionTicks < GrabThrow) {
                    Vec3 Hand = handPoint();
                    Victim.teleportTo(Hand.x, Hand.y, Hand.z);
                    Victim.setDeltaMovement(Vec3.ZERO);
                    Victim.hurtMarked = true;
                }
                if (ActionTicks == GrabThrow) {
                    throwHeld(World);
                }
                if (ActionTicks >= GrabLength) {
                    finish();
                }
            }
            case ActionSlam -> {
                if (ActionTicks == SlamImpact) {
                    slam(World);
                }
                if (ActionTicks >= SlamLength) {
                    finish();
                }
            }
            case ActionPush -> {
                if (ActionTicks == PushRelease) {
                    push(World);
                }
                if (ActionTicks >= PushLength) {
                    finish();
                }
            }
            default -> finish();
        }
    }

    private void begin(ServerLevel World, byte Chosen) {
        entityData.set(DataAction, Chosen);
        ActionTicks = 0;
        triggerAnim("action", switch (Chosen) {
            case ActionSwipe -> "swipe";
            case ActionGrab -> "grab";
            case ActionSlam -> "slam";
            default -> "push";
        });
        World.playSound(null, getX(), getY() + Chest, getZ(), Scp1730Registry.GuardianCast.get(), SoundSource.HOSTILE, 1.6f, 0.9f + random.nextFloat() * 0.2f);
    }

    private void finish() {
        entityData.set(DataAction, ActionNone);
        ActionTicks = 0;
        releaseHeld();
        Cooldown = anchored() ? 45 + random.nextInt(20) : 28 + random.nextInt(18);
    }

    private void releaseHeld() {
        Held = -1;
    }

    public Vec3 handPoint() {
        Vec3 Forward = Vec3.directionFromRotation(0.0f, yBodyRot);
        return position().add(Forward.scale(Width * 0.5 + 1.2 * Grown)).add(0.0, 3.2 * Grown, 0.0);
    }

    private DamageSource source(ServerLevel World) {
        return new DamageSource(World.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(Scp1730Registry.GuardianDamage), this);
    }

    private void swipe(ServerLevel World) {
        World.playSound(null, getX(), getY() + Chest, getZ(), Scp1730Registry.GuardianSwipe.get(), SoundSource.HOSTILE, 1.8f, 0.9f);
        Vec3 Forward = Vec3.directionFromRotation(0.0f, yBodyRot);
        for (LivingEntity Each : World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(SwipeReach), this::swipeable)) {
            Vec3 Way = Each.position().subtract(position());
            Vec3 Flat = new Vec3(Way.x, 0.0, Way.z);
            if (Flat.length() - Width * 0.5 > SwipeReach || Flat.normalize().dot(Forward) < -0.2) {
                continue;
            }
            if (Each.hurt(source(World), SwipeDamage)) {
                Each.knockback(1.2, -Flat.x, -Flat.z);
            }
        }
    }

    private void throwHeld(ServerLevel World) {
        Entity Grabbed = Held >= 0 ? World.getEntity(Held) : null;
        releaseHeld();
        if (!(Grabbed instanceof LivingEntity Victim) || !Victim.isAlive()) {
            return;
        }
        Vec3 Away = Victim.position().subtract(position());
        Vec3 Flat = new Vec3(Away.x, 0.0, Away.z).normalize();
        Victim.setDeltaMovement(Flat.x * 2.0, 0.8, Flat.z * 2.0);
        Victim.hurtMarked = true;
        Victim.hurt(source(World), ThrowDamage);
        World.playSound(null, Victim.getX(), Victim.getY(), Victim.getZ(), Scp1730Registry.GuardianSwipe.get(), SoundSource.HOSTILE, 1.4f, 0.6f);
    }

    private void slam(ServerLevel World) {
        double Floor = floorBelow(World, position().add(0.0, 1.0, 0.0));
        World.playSound(null, getX(), Floor, getZ(), Scp1730Registry.GuardianSlam.get(), SoundSource.HOSTILE, 3.0f, 0.7f);
        Scp1730Shockwaves.start(World, new Vec3(getX(), Floor, getZ()), this);
        BlockState Ground = World.getBlockState(BlockPos.containing(getX(), Floor - 0.5, getZ()));
        World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Ground), getX(), Floor + 0.2, getZ(), 80, 2.2 * Grown, 0.2, 2.2 * Grown, 0.2);
    }

    private void push(ServerLevel World) {
        World.playSound(null, getX(), getY() + Chest, getZ(), Scp1730Registry.GuardianPush.get(), SoundSource.HOSTILE, 2.0f, 0.8f);
        World.sendParticles(ParticleTypes.SWEEP_ATTACK, getX(), getY() + 2.0 * Grown, getZ(), 16, 3.0 * Grown, 1.0, 3.0 * Grown, 0.0);
        for (LivingEntity Each : World.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(PushReach), Each -> Each != this && Each.isAlive())) {
            Vec3 Way = Each.position().subtract(position());
            double Flat = Math.sqrt(Way.x * Way.x + Way.z * Way.z);
            if (Flat - Width * 0.5 > PushReach || Flat < 1.0e-3) {
                continue;
            }
            Each.setDeltaMovement(Each.getDeltaMovement().add(Way.x / Flat * 2.2, 0.45, Way.z / Flat * 2.2));
            Each.hurtMarked = true;
            if (pushHurts(Each)) {
                Each.hurt(source(World), PushDamage);
            }
        }
    }

    private void aura(ServerLevel World) {
        boolean Active = !anchored();
        for (ServerPlayer User : World.players()) {
            boolean Near = Active && User.distanceTo(this) < AuraRadius && !User.isSpectator();
            lighten(User, Near);
        }
    }

    private void lighten(ServerPlayer User, boolean Near) {
        AttributeInstance Gravity = User.getAttribute(Attributes.GRAVITY);
        if (Gravity == null) {
            return;
        }
        if (Near && !Gravity.hasModifier(GravityId)) {
            Gravity.addTransientModifier(new AttributeModifier(GravityId, -0.45, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            Lightened.add(User.getUUID());
        } else if (!Near && Gravity.hasModifier(GravityId)) {
            Gravity.removeModifier(GravityId);
            Lightened.remove(User.getUUID());
        }
    }

    private void clientGlyphs() {
        if (anchored() || isDeadOrDying()) {
            return;
        }
        int Count = casting() ? 2 : 1;
        for (int Index = 0; Index < Count; Index++) {
            double Angle = random.nextDouble() * Math.PI * 2.0;
            double Reach = (2.5 + random.nextDouble() * 2.0) * Grown;
            level().addParticle(Scp1730Registry.GlyphParticle.get(), getX() + Math.cos(Angle) * Reach, getY() + (2.5 + random.nextDouble() * 3.5) * Grown,
                    getZ() + Math.sin(Angle) * Reach, 0.0, 0.01, 0.0);
        }
        if (random.nextInt(6) == 0) {
            BlockPos Below = BlockPos.containing(getX() + (random.nextDouble() - 0.5) * 8.0, getY() - 1.0, getZ() + (random.nextDouble() - 0.5) * 8.0);
            BlockState Ground = level().getBlockState(Below);
            if (!Ground.isAir()) {
                level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, Ground), Below.getX() + 0.5, Below.getY() + 1.1, Below.getZ() + 0.5, 0.0, 0.25, 0.0);
            }
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        if (Home != null) {
            Tag.put("Home", NbtUtils.writeBlockPos(Home));
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Home = NbtUtils.readBlockPos(Tag, "Home").orElse(null);
        if (hasCustomName()) {
            Bar.setName(getDisplayName());
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "main", 8, Animated -> {
            if (isDeadOrDying()) {
                return Animated.setAndContinue(DeathHold);
            }
            if (anchored()) {
                return Animated.setAndContinue(AnchoredLoop);
            }
            if (Animated.isMoving()) {
                return Animated.setAndContinue(GlideLoop);
            }
            return Animated.setAndContinue(entityData.get(DataCasting) ? Draw : Idle);
        }));
        Controllers.add(new AnimationController<>(this, "action", 4, Animated -> Animated.getController().isPlayingTriggeredAnimation() && !isDeadOrDying()
                ? software.bernie.geckolib.animation.PlayState.CONTINUE : software.bernie.geckolib.animation.PlayState.STOP).receiveTriggeredAnimations()
                .triggerableAnim("swipe", SwipeOnce).triggerableAnim("grab", GrabOnce).triggerableAnim("slam", SlamOnce).triggerableAnim("push", PushOnce));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
