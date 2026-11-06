package net.rai.scp_unleashed.scp1730.pit;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.EventHooks;
import net.rai.scp_unleashed.scp1730.Scp1730Anchors;
import net.rai.scp_unleashed.scp1730.Scp1730Registry;
import net.rai.scp_unleashed.scp1730.entity.Scp1730Leech;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.PlayState;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public class Scp1730Mass extends Monster implements GeoEntity {
    public static final float Width = 6.0f;
    public static final float Height = 3.0f;
    public static final float EyeHeight = 2.0f;
    public static final float MaxHealth = 420.0f;
    public static final float DamageShare = 0.3f;
    public static final int CheckEvery = 10;
    public static final int GraceTicks = 1200;
    public static final int RestlessTicks = 400;
    public static final int DigTicks = 200;
    public static final int DigLayers = 3;
    public static final int RevealTicks = 60;
    public static final int DeathTicks = 100;
    public static final int SheddingCap = 2;
    public static final double NoticeRange = 32.0;
    public static final double Roam = 4.0;
    public static final double CrawlSpeed = 0.07;
    public static final int SurgeTicks = 16;
    private static final EntityDataAccessor<Boolean> DataContained = SynchedEntityData.defineId(Scp1730Mass.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DataRestless = SynchedEntityData.defineId(Scp1730Mass.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DataDigging = SynchedEntityData.defineId(Scp1730Mass.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DataReveal = SynchedEntityData.defineId(Scp1730Mass.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DataCrawling = SynchedEntityData.defineId(Scp1730Mass.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation RestlessLoop = RawAnimation.begin().thenLoop("restless");
    private static final RawAnimation CrawlLoop = RawAnimation.begin().thenLoop("crawl");
    private static final RawAnimation DigLoop = RawAnimation.begin().thenPlay("dig_start").thenLoop("dig");
    private static final RawAnimation RevealOnce = RawAnimation.begin().thenPlay("reveal").thenLoop("idle");
    private static final RawAnimation DeathHold = RawAnimation.begin().thenPlayAndHold("death");
    private static final RawAnimation LeechLoop = RawAnimation.begin().thenLoop("leeches");
    private static final RawAnimation FlinchOnce = RawAnimation.begin().thenPlay("flinch");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    private int Unrest;
    private int DigProgress;
    private double DigOrigin;
    private int Reveal;
    @Nullable
    private Vec3 Home;
    @Nullable
    private Vec3 Goal;
    private int GoalTicks;

    public Scp1730Mass(EntityType<? extends Scp1730Mass> Type, Level World) {
        super(Type, World);
        setPersistenceRequired();
        xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, MaxHealth).add(Attributes.ARMOR, 6.0).add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0).add(Attributes.FOLLOW_RANGE, 8.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(DataContained, false);
        Builder.define(DataRestless, false);
        Builder.define(DataDigging, false);
        Builder.define(DataReveal, 0);
        Builder.define(DataCrawling, false);
    }

    @Override
    protected void registerGoals() {
    }

    public boolean contained() {
        return entityData.get(DataContained);
    }

    public boolean restless() {
        return entityData.get(DataRestless);
    }

    public boolean digging() {
        return entityData.get(DataDigging);
    }

    public boolean crawling() {
        return entityData.get(DataCrawling);
    }

    public void setHome(Vec3 Point) {
        Home = Point;
        Goal = null;
    }

    @Nullable
    public Vec3 home() {
        return Home;
    }

    public boolean revealing() {
        return entityData.get(DataReveal) > 0;
    }

    public int unrest() {
        return Unrest;
    }

    public void setUnrest(int Value) {
        Unrest = Math.max(0, Value);
    }

    public int digProgress() {
        return DigProgress;
    }

    public void released(ServerLevel World) {
        Unrest = 0;
        Home = position();
        Goal = null;
        Reveal = RevealTicks;
        entityData.set(DataReveal, RevealTicks);
        World.playSound(null, getX(), getY() + 2.0, getZ(), Scp1730PitRegistry.Roar.get(), SoundSource.HOSTILE, 2.4f, 0.75f);
        World.playSound(null, getX(), getY() + 2.0, getZ(), Scp1730PitRegistry.MassIdle.get(), SoundSource.HOSTILE, 2.0f, 0.8f);
        World.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 2.0, getZ(), 40, 2.0, 1.5, 2.0, 0.05);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean removeWhenFarAway(double Distance) {
        return false;
    }

    @Override
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double Distance) {
        return Distance < 80.0 * 80.0;
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        if (Source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return super.hurt(Source, Amount);
        }
        if (isDeadOrDying()) {
            return false;
        }
        boolean Hurt = super.hurt(Source, Amount * DamageShare);
        if (Hurt && level() instanceof ServerLevel World) {
            triggerAnim("action", "flinch");
            if (Source.getEntity() instanceof Player && random.nextInt(3) == 0) {
                shed(World);
            }
        }
        return Hurt;
    }

    private void shed(ServerLevel World) {
        int Near = World.getEntitiesOfClass(Scp1730Leech.class, getBoundingBox().inflate(16.0), Scp1730Leech::isAlive).size();
        if (Near >= SheddingCap) {
            return;
        }
        Scp1730Leech Leech = Scp1730Registry.LeechType.get().create(World);
        if (Leech == null) {
            return;
        }
        double Angle = random.nextDouble() * Math.PI * 2.0;
        Leech.moveTo(getX() + Math.cos(Angle) * 2.4, getY() + 1.0, getZ() + Math.sin(Angle) * 2.4, random.nextFloat() * 360.0f, 0.0f);
        Leech.setDeltaMovement(Math.cos(Angle) * 0.25, 0.3, Math.sin(Angle) * 0.25);
        Leech.finalizeSpawn(World, World.getCurrentDifficultyAt(blockPosition()), MobSpawnType.MOB_SUMMONED, null);
        World.addFreshEntity(Leech);
        World.playSound(null, Leech.getX(), Leech.getY(), Leech.getZ(), Scp1730Registry.LeechEmerge.get(), SoundSource.HOSTILE, 1.0f, 1.0f);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return restless() || digging() ? Scp1730PitRegistry.MassRestless.get() : Scp1730PitRegistry.MassIdle.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return restless() ? 90 : 220;
    }

    @Nullable
    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp1730PitRegistry.MassHurt.get();
    }

    @Nullable
    @Override
    protected SoundEvent getDeathSound() {
        return Scp1730PitRegistry.MassDeath.get();
    }

    @Override
    protected float getSoundVolume() {
        return 1.8f;
    }

    @Override
    protected void tickDeath() {
        deathTime++;
        if (level() instanceof ServerLevel World) {
            if (deathTime % 5 == 0) {
                World.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 1.0, getZ(), 10, 2.0, 0.6, 2.0, 0.02);
            }
            if (deathTime >= DeathTicks && !isRemoved()) {
                World.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.5, getZ(), 30, 2.0, 0.4, 2.0, 0.02);
                remove(RemovalReason.KILLED);
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || isDeadOrDying()) {
            return;
        }
        ServerLevel World = (ServerLevel) level();
        if (Reveal > 0) {
            Reveal--;
            if (Reveal == 0) {
                entityData.set(DataReveal, 0);
            }
        }
        if (tickCount % CheckEvery == 0) {
            check(World);
        }
        if (digging()) {
            tickDig(World);
        }
        boolean Moving = contained() && !digging() && Reveal <= 0 && wander(World);
        if (Moving != crawling()) {
            entityData.set(DataCrawling, Moving);
        }
    }

    private boolean wander(ServerLevel World) {
        if (Home == null) {
            Home = position();
        }
        if (Goal != null) {
            GoalTicks++;
            Vec3 Way = new Vec3(Goal.x - getX(), 0.0, Goal.z - getZ());
            if (Way.lengthSqr() < 1.0 || GoalTicks > 240 || horizontalCollision && GoalTicks > 20) {
                Goal = null;
                return false;
            }
            float Want = (float) (Math.atan2(Way.z, Way.x) * (180.0 / Math.PI)) - 90.0f;
            float Turned = net.minecraft.util.Mth.approachDegrees(getYRot(), Want, 2.5f);
            setYRot(Turned);
            yBodyRot = Turned;
            yHeadRot = Turned;
            double Phase = Math.sin(tickCount * Math.PI * 2.0 / SurgeTicks);
            double Surge = 0.2 + 0.8 * Math.max(0.0, Phase) * Math.max(0.0, Phase);
            Vec3 Forward = Vec3.directionFromRotation(0.0f, Turned);
            double Facing = Math.max(0.0, Forward.dot(Way.normalize()));
            move(net.minecraft.world.entity.MoverType.SELF, new Vec3(Forward.x * CrawlSpeed * Surge * Facing, 0.0, Forward.z * CrawlSpeed * Surge * Facing));
            return true;
        }
        if (random.nextInt(80) == 0) {
            double Angle = random.nextDouble() * Math.PI * 2.0;
            double Reach = 1.5 + random.nextDouble() * (Roam - 1.5);
            Goal = new Vec3(Home.x + Math.cos(Angle) * Reach, getY(), Home.z + Math.sin(Angle) * Reach);
            GoalTicks = 0;
        }
        return false;
    }

    public void check(ServerLevel World) {
        boolean Held = Scp1730Anchors.folded(World, getBoundingBox());
        entityData.set(DataContained, Held);
        if (Held) {
            Unrest = Math.max(0, Unrest - CheckEvery * 2);
            if (digging()) {
                stopDig(World);
            }
        } else if (!digging()) {
            Unrest += CheckEvery;
            if (Unrest >= GraceTicks) {
                startDig(World);
            }
        }
        entityData.set(DataRestless, !Held && Unrest >= RestlessTicks);
    }

    public void startDig(ServerLevel World) {
        entityData.set(DataDigging, true);
        DigProgress = 0;
        DigOrigin = getY();
        noPhysics = true;
        setNoGravity(true);
        World.playSound(null, getX(), getY() + 2.0, getZ(), Scp1730PitRegistry.Roar.get(), SoundSource.HOSTILE, 2.2f, 0.65f);
    }

    public void stopDig(ServerLevel World) {
        entityData.set(DataDigging, false);
        noPhysics = false;
        setNoGravity(false);
        if (getY() < DigOrigin) {
            setPos(getX(), DigOrigin, getZ());
        }
        DigProgress = 0;
        World.playSound(null, getX(), getY() + 2.0, getZ(), Scp1730PitRegistry.MassIdle.get(), SoundSource.HOSTILE, 2.0f, 0.6f);
    }

    private void tickDig(ServerLevel World) {
        DigProgress++;
        int Layer = Math.min(DigLayers - 1, DigProgress * DigLayers / DigTicks);
        int Floor = (int) Math.floor(DigOrigin) - 1 - Layer;
        boolean Blocked = false;
        if (DigProgress % 10 == 0) {
            Blocked = !carve(World, Floor);
            World.playSound(null, getX(), getY() + 0.5, getZ(), Scp1730PitRegistry.MassDig.get(), SoundSource.HOSTILE, 1.8f, 0.7f + random.nextFloat() * 0.2f);
            World.sendParticles(ParticleTypes.SQUID_INK, getX(), getY() + 0.5, getZ(), 8, 1.8, 0.3, 1.8, 0.03);
        }
        if (Blocked) {
            DigProgress = Math.max(0, DigProgress - 10);
            return;
        }
        double Target = DigOrigin - (DigProgress / (double) DigTicks) * (DigLayers + Height * 0.6);
        if (getY() > Target && open(World, (int) Math.floor(Target))) {
            setPos(getX(), Math.max(Target, getY() - 0.06), getZ());
        }
        if (DigProgress >= DigTicks) {
            escape(World);
        }
    }

    private boolean open(ServerLevel World, int Level) {
        BlockPos Centre = BlockPos.containing(getX(), Level, getZ());
        BlockState State = World.getBlockState(Centre);
        return State.isAir() || State.getCollisionShape(World, Centre).isEmpty() || World.getBlockState(Centre).canBeReplaced();
    }

    private boolean carve(ServerLevel World, int Floor) {
        int Radius = (int) Math.ceil(Width / 2.0) - 1;
        BlockPos Centre = BlockPos.containing(getX(), Floor, getZ());
        boolean Any = false;
        boolean Core = false;
        if (!EventHooks.canEntityGrief(World, this)) {
            return false;
        }
        for (int Dx = -Radius; Dx <= Radius; Dx++) {
            for (int Dz = -Radius; Dz <= Radius; Dz++) {
                if (Dx * Dx + Dz * Dz > Radius * Radius + 1) {
                    continue;
                }
                BlockPos Pos = Centre.offset(Dx, 0, Dz);
                BlockState State = World.getBlockState(Pos);
                if (State.isAir()) {
                    Any = true;
                    if (Dx == 0 && Dz == 0) {
                        Core = true;
                    }
                    continue;
                }
                if (!breakable(World, Pos, State)) {
                    continue;
                }
                World.levelEvent(2001, Pos, net.minecraft.world.level.block.Block.getId(State));
                World.destroyBlock(Pos, false, this);
                Any = true;
                if (Dx == 0 && Dz == 0) {
                    Core = true;
                }
            }
        }
        return Any && Core;
    }

    public boolean breakable(ServerLevel World, BlockPos Pos, BlockState State) {
        if (State.getDestroySpeed(World, Pos) < 0.0f || State.is(BlockTags.WITHER_IMMUNE) || State.is(BlockTags.DRAGON_IMMUNE)) {
            return false;
        }
        if (World.getBlockEntity(Pos) instanceof net.rai.scp_unleashed.scp1730.block.RealityFolderBlockEntity) {
            return false;
        }
        return EventHooks.onEntityDestroyBlock(this, Pos, State);
    }

    private void escape(ServerLevel World) {
        Vec3 Hole = new Vec3(getX(), DigOrigin, getZ());
        World.playSound(null, Hole.x, Hole.y, Hole.z, Scp1730PitRegistry.MassBurrow.get(), SoundSource.HOSTILE, 3.0f, 0.7f);
        World.sendParticles(ParticleTypes.SQUID_INK, Hole.x, Hole.y, Hole.z, 60, 2.0, 0.5, 2.0, 0.08);
        BlockState Below = World.getBlockState(BlockPos.containing(Hole.x, DigOrigin - DigLayers - 1, Hole.z));
        if (!Below.isAir()) {
            World.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Below), Hole.x, Hole.y, Hole.z, 40, 1.5, 0.5, 1.5, 0.2);
        }
        Component Notice = Component.translatable("message.scp_unleashed.scp1730_3.burrowed").withStyle(ChatFormatting.RED);
        for (ServerPlayer User : World.players()) {
            if (User.distanceToSqr(Hole) < NoticeRange * NoticeRange) {
                User.displayClientMessage(Notice, true);
            }
        }
        discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        Tag.putInt("Unrest", Unrest);
        Tag.putBoolean("Digging", digging());
        Tag.putInt("DigProgress", DigProgress);
        Tag.putDouble("DigOrigin", DigOrigin);
        if (Home != null) {
            Tag.putDouble("HomeX", Home.x);
            Tag.putDouble("HomeY", Home.y);
            Tag.putDouble("HomeZ", Home.z);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Unrest = Tag.getInt("Unrest");
        DigProgress = Tag.getInt("DigProgress");
        DigOrigin = Tag.getDouble("DigOrigin");
        Home = Tag.contains("HomeX") ? new Vec3(Tag.getDouble("HomeX"), Tag.getDouble("HomeY"), Tag.getDouble("HomeZ")) : null;
        if (Tag.getBoolean("Digging")) {
            entityData.set(DataDigging, true);
            noPhysics = true;
            setNoGravity(true);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "main", 8, Animated -> {
            if (isDeadOrDying()) {
                return Animated.setAndContinue(DeathHold);
            }
            if (digging()) {
                return Animated.setAndContinue(DigLoop);
            }
            if (revealing()) {
                return Animated.setAndContinue(RevealOnce);
            }
            if (crawling()) {
                return Animated.setAndContinue(CrawlLoop);
            }
            return Animated.setAndContinue(restless() ? RestlessLoop : IdleLoop);
        }));
        Controllers.add(new AnimationController<>(this, "leeches", 0, Animated -> isDeadOrDying() || digging() ? PlayState.STOP : Animated.setAndContinue(LeechLoop)));
        Controllers.add(new AnimationController<>(this, "action", 2, Animated -> Animated.getController().isPlayingTriggeredAnimation() ? PlayState.CONTINUE : PlayState.STOP)
                .receiveTriggeredAnimations().triggerableAnim("flinch", FlinchOnce));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
