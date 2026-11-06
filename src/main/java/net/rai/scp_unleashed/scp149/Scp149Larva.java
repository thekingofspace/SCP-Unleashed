package net.rai.scp_unleashed.scp149;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.animation.AnimatableManager;
import software.bernie.geckolib.animation.AnimationController;
import software.bernie.geckolib.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.Optional;
import java.util.UUID;

public class Scp149Larva extends Monster implements GeoEntity {
    public static final float Width = 0.35f;
    public static final float Height = 0.2f;
    public static final float EyeHeight = 0.12f;
    public static final float ModelScale = 0.7f;
    public static final double Health = 3.0;
    public static final double Speed = 0.17;
    public static final double WanderModifier = 0.8;
    public static final double FleeModifier = 1.0;
    public static final float FleeRange = 5.0f;
    public static final int CrawlMin = 200;
    public static final int CrawlMax = 400;
    public static final int GatherLimit = 900;
    public static final int LonelyLimit = 450;
    public static final int DrownLimit = 100;
    public static final float WaterMalus = 16.0f;
    public static final float BorderMalus = 8.0f;
    public static final double LonelyRange = 24.0;
    public static final int NestRoam = 6;
    public static final double SiblingRange = 8.0;
    public static final byte ModeCrawl = 0;
    public static final byte ModeGather = 1;
    private static final EntityDataAccessor<Byte> ModeData = SynchedEntityData.defineId(Scp149Larva.class, EntityDataSerializers.BYTE);
    private static final RawAnimation IdleLoop = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation CrawlLoop = RawAnimation.begin().thenLoop("crawl");
    private final AnimatableInstanceCache Cache = GeckoLibUtil.createInstanceCache(this);
    @Nullable
    private UUID Brood;
    @Nullable
    private BlockPos Nest;
    private long Born = Long.MIN_VALUE;
    private int Crawl;
    private int Gathered;
    private int Lonely;
    private int Soaked;
    @Nullable
    private Scp149Crawl Gait;

    public Scp149Larva(EntityType<? extends Scp149Larva> Type, Level World) {
        super(Type, World);
        xpReward = 1;
        setPersistenceRequired();
        setPathfindingMalus(PathType.WATER, WaterMalus);
        setPathfindingMalus(PathType.WATER_BORDER, BorderMalus);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, Health).add(Attributes.MOVEMENT_SPEED, Speed).add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(ModeData, ModeCrawl);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new Scp149Goals.Gather(this));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, FleeRange, FleeModifier, FleeModifier));
        goalSelector.addGoal(3, new WaterAvoidingRandomStrollGoal(this, WanderModifier, 0.02f));
    }

    public void adopt(UUID Owner, BlockPos Home, long Now) {
        Brood = Owner;
        Nest = Home.immutable();
        Born = Now;
        Crawl = CrawlMin + getRandom().nextInt(CrawlMax - CrawlMin + 1);
        restrictTo(Nest, NestRoam);
    }

    public UUID brood() {
        if (Brood == null) {
            Brood = UUID.randomUUID();
        }
        return Brood;
    }

    public BlockPos nest() {
        if (Nest == null) {
            Nest = blockPosition();
        }
        return Nest;
    }

    public boolean gathering() {
        return entityData.get(ModeData) == ModeGather;
    }

    public int lonely() {
        return Lonely;
    }

    public void setLonely(int Value) {
        Lonely = Value;
    }

    public int gathered() {
        return Gathered;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) {
            Scp149Crawl Current = gait();
            if (Current != null) {
                Current.tick(this);
            }
            return;
        }
        if (!isAlive() || !(level() instanceof ServerLevel World)) {
            return;
        }
        Soaked = isInWater() ? Soaked + 1 : Math.max(0, Soaked - 2);
        if (Soaked >= DrownLimit) {
            wither(World);
            return;
        }
        long Now = Scp149World.now(World.getServer());
        if (Born == Long.MIN_VALUE || Nest == null) {
            Scp149Larva Sibling = sibling(World);
            if (Sibling != null) {
                adopt(Sibling.brood(), Sibling.nest(), Now);
            } else {
                adopt(brood(), blockPosition(), Now);
            }
        }
        if (!gathering()) {
            if (Now - Born >= Crawl) {
                entityData.set(ModeData, ModeGather);
                clearRestriction();
            }
            return;
        }
        Gathered++;
        if (Gathered >= GatherLimit || Lonely >= LonelyLimit) {
            wither(World);
        }
    }

    @Nullable
    private Scp149Larva sibling(ServerLevel World) {
        Scp149Larva Best = null;
        double Closest = SiblingRange * SiblingRange;
        for (Scp149Larva Other : World.getEntitiesOfClass(Scp149Larva.class, getBoundingBox().inflate(SiblingRange), Found -> Found != this && Found.isAlive() && Found.Born != Long.MIN_VALUE)) {
            double Distance = Other.distanceToSqr(this);
            if (Distance < Closest) {
                Closest = Distance;
                Best = Other;
            }
        }
        return Best;
    }

    public void wither(ServerLevel World) {
        World.sendParticles(Scp149Registry.Mote.get(), getX(), getY() + 0.1, getZ(), 6, 0.12, 0.05, 0.12, 0.01);
        World.playSound(null, getX(), getY(), getZ(), Scp149Registry.Wither.get(), SoundSource.HOSTILE, 0.7f, 1.0f + getRandom().nextFloat() * 0.2f);
        discard();
    }

    @Override
    public void die(DamageSource Source) {
        super.die(Source);
        if (level() instanceof ServerLevel World) {
            World.sendParticles(Scp149Registry.Mote.get(), getX(), getY() + 0.15, getZ(), 8, 0.12, 0.08, 0.12, 0.04);
        }
    }

    @Override
    protected void playStepSound(BlockPos Pos, BlockState Below) {
        if (getRandom().nextInt(3) == 0) {
            playSound(Scp149Registry.LarvaSkitter.get(), 0.25f, 1.0f + getRandom().nextFloat() * 0.2f);
        }
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return Scp149Registry.LarvaSquelch.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.4f;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return Scp149Registry.LarvaHurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return Scp149Registry.LarvaDeath.get();
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
    public boolean causeFallDamage(float Distance, float Multiplier, DamageSource Source) {
        return false;
    }

    @Override
    public boolean isPreventingPlayerRest(Player Sleeper) {
        return false;
    }

    @Nullable
    public Scp149Crawl gait() {
        if (Gait == null && level().isClientSide) {
            Gait = new Scp149Crawl();
        }
        return Gait;
    }

    public boolean isCrawling() {
        Scp149Crawl Current = gait();
        return Current != null && Current.crawling();
    }

    public void setRenderPartial(float Partial) {
        Scp149Crawl Current = gait();
        if (Current != null) {
            Current.setPartial(Partial);
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Tag) {
        super.addAdditionalSaveData(Tag);
        if (Brood != null) {
            Tag.putUUID("Brood", Brood);
        }
        if (Nest != null) {
            Tag.put("Nest", NbtUtils.writeBlockPos(Nest));
        }
        if (Born != Long.MIN_VALUE) {
            Tag.putLong("Born", Born);
        }
        Tag.putInt("Crawl", Crawl);
        Tag.putInt("Gathered", Gathered);
        Tag.putInt("Lonely", Lonely);
        Tag.putInt("Soaked", Soaked);
        Tag.putBoolean("Gathering", gathering());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Tag) {
        super.readAdditionalSaveData(Tag);
        Brood = Tag.hasUUID("Brood") ? Tag.getUUID("Brood") : null;
        Optional<BlockPos> Saved = NbtUtils.readBlockPos(Tag, "Nest");
        Nest = Saved.orElse(null);
        Born = Tag.contains("Born") ? Tag.getLong("Born") : Long.MIN_VALUE;
        Crawl = Tag.contains("Crawl") ? Tag.getInt("Crawl") : CrawlMin;
        Gathered = Tag.getInt("Gathered");
        Lonely = Tag.getInt("Lonely");
        Soaked = Tag.getInt("Soaked");
        entityData.set(ModeData, Tag.getBoolean("Gathering") ? ModeGather : ModeCrawl);
        if (Nest != null && !gathering()) {
            restrictTo(Nest, NestRoam);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar Controllers) {
        Controllers.add(new AnimationController<>(this, "body", 3, Event -> Event.setAndContinue(isCrawling() ? CrawlLoop : IdleLoop)) {
            @Override
            protected double adjustTick(double Tick) {
                double Adjusted = super.adjustTick(Tick);
                if (animationState != State.RUNNING || currentAnimation == null || !"crawl".equals(currentAnimation.animation().name())) {
                    return Adjusted;
                }
                Scp149Crawl Current = gait();
                return Current == null ? Adjusted : Current.cycle() * currentAnimation.animation().length();
            }
        });
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return Cache;
    }
}
