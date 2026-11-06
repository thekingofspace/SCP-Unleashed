package net.rai.scp_unleashed.scp007;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.rai.scp_unleashed.control.ControlRefusing;
import net.rai.scp_unleashed.dclass.DClassLike;
import net.rai.scp_unleashed.dclass.DClassRegistry;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animation.RawAnimation;

import java.util.List;

public class Scp007Entity extends DClassLike implements ControlRefusing {
    public static final float Width = 0.6f;
    public static final float Height = 1.8f;
    public static final float EyeHeight = 1.62f;
    public static final EntityDimensions SeatedSize = EntityDimensions.scalable(0.6f, 1.45f).withEyeHeight(1.2f);
    public static final double MaxHealth = 20.0;
    public static final double MoveSpeed = 0.22;
    public static final double StrollSpeed = 0.6;
    public static final double HomeSpeed = 0.7;
    public static final double PanicSpeed = 1.5;
    public static final int StrollInterval = 140;
    public static final int HomeRadius = 16;
    public static final int HomeDrift = 96;
    public static final float LookRange = 8.0f;
    public static final float LookChance = 0.04f;
    public static final float Glance = 0.6f;
    public static final float WaterCost = 8.0f;
    public static final int Lines = 12;
    public static final int DeclineLines = 4;
    public static final int LineCooldown = 50;
    public static final int RiseTicks = 16;
    public static final int GazeShortest = 300;
    public static final int GazeLongest = 700;
    public static final int RestShortest = 400;
    public static final int RestLongest = 1400;
    public static final int ArrivalGaze = 2400;
    public static final int AmbientInterval = 220;
    public static final int WeatherShortest = 50;
    public static final int WeatherLongest = 110;
    public static final double WeatherReach = 5.0;
    public static final double ThreatRange = 8.0;
    public static final byte Standing = 0;
    public static final byte Seated = 1;
    public static final byte Rising = 2;
    public static final RawAnimation SitThenGaze = RawAnimation.begin().thenPlay("sit_down").thenLoop("gaze");
    public static final RawAnimation StandPlay = RawAnimation.begin().thenPlayAndHold("stand_up");
    public static final String Prefix = "message.scp_unleashed.scp007.";
    protected static final EntityDataAccessor<Byte> SeatData = SynchedEntityData.defineId(Scp007Entity.class, EntityDataSerializers.BYTE);

    private int SeatLeft;
    private int RiseLeft;
    private int NextGaze = 200;
    private int LineReady;
    private int LastLine = -1;
    private int LastDecline = -1;
    private int NextWeather = WeatherLongest;

    public Scp007Entity(EntityType<? extends Scp007Entity> Type, Level World) {
        super(Type, World);
        setPathfindingMalus(PathType.WATER, WaterCost);
        setPersistenceRequired();
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MaxHealth)
                .add(Attributes.MOVEMENT_SPEED, MoveSpeed)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder Builder) {
        super.defineSynchedData(Builder);
        Builder.define(SeatData, Standing);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new PanicGoal(this, PanicSpeed));
        goalSelector.addGoal(2, new Scp007Goals.Gaze(this));
        goalSelector.addGoal(3, new MoveTowardsRestrictionGoal(this, HomeSpeed));
        goalSelector.addGoal(4, new Scp007Goals.Wander(this, StrollSpeed, StrollInterval));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, LookRange, LookChance));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor World, DifficultyInstance Difficulty, MobSpawnType Reason, @Nullable SpawnGroupData Group) {
        SpawnGroupData Result = super.finalizeSpawn(World, Difficulty, Reason, Group);
        setPersistenceRequired();
        restrictTo(blockPosition(), HomeRadius);
        return Result;
    }

    @Override
    public float glanceRate() {
        return Glance;
    }

    @Override
    public Component controlRefusal() {
        return Component.translatable(Prefix + "refuses");
    }

    public byte seat() {
        return entityData.get(SeatData);
    }

    public boolean isSeated() {
        return seat() == Seated;
    }

    public boolean isRising() {
        return seat() == Rising;
    }

    public boolean isSettled() {
        return seat() != Standing;
    }

    public void sitDown(int Ticks) {
        if (isSeated()) {
            SeatLeft = Math.max(SeatLeft, Ticks);
            return;
        }
        getNavigation().stop();
        SeatLeft = Ticks;
        entityData.set(SeatData, Seated);
        playLayers(Scp007Registry.SettleLayers, 0.7f, 0.95f + getRandom().nextFloat() * 0.1f);
    }

    public void standUp() {
        if (!isSeated()) {
            return;
        }
        SeatLeft = 0;
        RiseLeft = RiseTicks;
        entityData.set(SeatData, Rising);
        NextGaze = tickCount + RestShortest + getRandom().nextInt(RestLongest - RestShortest);
        playLayers(Scp007Registry.SettleLayers, 0.6f, 1.05f + getRandom().nextFloat() * 0.1f);
    }

    public boolean readyToGaze() {
        return tickCount >= NextGaze;
    }

    public int gazeLength() {
        return GazeShortest + getRandom().nextInt(GazeLongest - GazeShortest);
    }

    public int seatLeft() {
        return SeatLeft;
    }

    public boolean threatened() {
        if (getLastHurtByMob() != null && tickCount - getLastHurtByMobTimestamp() < 200) {
            return true;
        }
        return !level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(ThreatRange), Scp007Entity::isThreat).isEmpty();
    }

    public static boolean isThreat(LivingEntity Other) {
        if (!(Other instanceof Enemy) || !Other.isAlive()) {
            return false;
        }
        return !(Other instanceof NeutralMob Neutral) || Neutral.isAngry();
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose Current) {
        return isSeated() ? SeatedSize.scale(getAgeScale()) : super.getDefaultDimensions(Current);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> Accessor) {
        super.onSyncedDataUpdated(Accessor);
        if (SeatData.equals(Accessor)) {
            refreshDimensions();
        }
    }

    @Override
    protected RawAnimation bodyAnimation() {
        if (!isSwimmingPose() && !isSittingPose() && !isRestingPose()) {
            if (isSeated()) {
                return SitThenGaze;
            }
            if (isRising()) {
                return StandPlay;
            }
        }
        return super.bodyAnimation();
    }

    public Vec3 planetPosition() {
        Vec3 Facing = Vec3.directionFromRotation(0.0f, yBodyRot);
        double Lift = isSeated() ? 0.56 : 1.09;
        return position().add(Facing.x * 0.1, Lift, Facing.z * 0.1);
    }

    @Override
    protected InteractionResult mobInteract(Player User, InteractionHand Hand) {
        ItemStack Held = User.getItemInHand(Hand);
        if (Hand != InteractionHand.MAIN_HAND || User.isSpectator() || Held.is(Items.NAME_TAG) || Held.is(Items.LEAD)) {
            return InteractionResult.PASS;
        }
        if (User instanceof ServerPlayer Speaker) {
            if (isFood(Held)) {
                decline(Speaker);
            } else {
                chat(Speaker);
            }
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    public static boolean isFood(ItemStack Stack) {
        if (Stack.isEmpty()) {
            return false;
        }
        UseAnim Kind = Stack.getUseAnimation();
        return Stack.has(DataComponents.FOOD) || Kind == UseAnim.EAT || Kind == UseAnim.DRINK;
    }

    private void decline(ServerPlayer Speaker) {
        if (tickCount < LineReady) {
            return;
        }
        LineReady = tickCount + LineCooldown;
        int Line = pick(DeclineLines, LastDecline);
        LastDecline = Line;
        say(Speaker, Component.translatable(Prefix + "decline." + Line));
        playLayers(Scp007Registry.DeclineLayers, 0.8f, 1.0f);
        getLookControl().setLookAt(Speaker, 30.0f, 30.0f);
    }

    private void chat(ServerPlayer Speaker) {
        if (tickCount < LineReady) {
            return;
        }
        LineReady = tickCount + LineCooldown;
        int Line = pick(Lines, LastLine);
        LastLine = Line;
        say(Speaker, Component.translatable(Prefix + "line." + Line));
        if (!isSettled()) {
            getNavigation().stop();
            getLookControl().setLookAt(Speaker, 30.0f, 30.0f);
        }
    }

    private int pick(int Count, int Last) {
        int Line = getRandom().nextInt(Count);
        if (Line == Last && Count > 1) {
            Line = (Line + 1 + getRandom().nextInt(Count - 1)) % Count;
        }
        return Line;
    }

    public void say(ServerPlayer Speaker, Component Spoken) {
        Component Name = Component.translatable(Prefix + "speaker");
        Speaker.displayClientMessage(Component.translatable(Prefix + "speech", Name, Spoken).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
    }

    private void playLayers(List<DeferredHolder<SoundEvent, SoundEvent>> Layers, float Volume, float Pitch) {
        Vec3 Where = planetPosition();
        for (DeferredHolder<SoundEvent, SoundEvent> Layer : Layers) {
            level().playSound(null, Where.x, Where.y, Where.z, Layer.get(), SoundSource.NEUTRAL, Volume, Pitch);
        }
    }

    @Override
    public void playAmbientSound() {
        if (isSeated() && getRandom().nextBoolean()) {
            playLayers(Scp007Registry.HumLayers, 0.6f, 0.95f + getRandom().nextFloat() * 0.1f);
            return;
        }
        float Pitch = 0.95f + getRandom().nextFloat() * 0.1f;
        playSound(Scp007Registry.Breathe.get(), 0.6f, Pitch);
        playSound(Scp007Registry.BreatheSoft.get(), 0.6f, Pitch);
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return Scp007Registry.Breathe.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return AmbientInterval;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource Source) {
        return DClassRegistry.Hurt.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return DClassRegistry.Death.get();
    }

    @Override
    public boolean wantsToPickUp(ItemStack Stack) {
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!(level() instanceof ServerLevel World) || !isAlive()) {
            return;
        }
        if (isSeated()) {
            getNavigation().stop();
            if (--SeatLeft <= 0) {
                standUp();
            }
        } else if (isRising() && --RiseLeft <= 0) {
            entityData.set(SeatData, Standing);
        }
        if (!hasRestriction() || getRestrictCenter().distSqr(blockPosition()) > (double) HomeDrift * HomeDrift) {
            restrictTo(blockPosition(), HomeRadius);
        }
        if (--NextWeather <= 0) {
            NextWeather = WeatherShortest + getRandom().nextInt(WeatherLongest - WeatherShortest);
            Vec3 Where = planetPosition();
            if (World.getNearestPlayer(Where.x, Where.y, Where.z, WeatherReach, false) != null) {
                playLayers(Scp007Registry.WeatherLayers, 0.5f, 0.9f + getRandom().nextFloat() * 0.2f);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource Source, float Amount) {
        boolean Hit = super.hurt(Source, Amount);
        if (Hit && !level().isClientSide && isSeated()) {
            standUp();
        }
        return Hit;
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
    protected boolean shouldDropLoot() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag Saved) {
        super.addAdditionalSaveData(Saved);
        if (hasRestriction()) {
            Saved.putLong("Home", getRestrictCenter().asLong());
        }
        Saved.putBoolean("Seated", isSeated());
        Saved.putInt("SeatLeft", SeatLeft);
        Saved.putInt("NextGaze", Math.max(0, NextGaze - tickCount));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag Saved) {
        super.readAdditionalSaveData(Saved);
        if (Saved.contains("Home")) {
            restrictTo(BlockPos.of(Saved.getLong("Home")), HomeRadius);
        }
        if (Saved.getBoolean("Seated")) {
            SeatLeft = Math.max(20, Saved.getInt("SeatLeft"));
            entityData.set(SeatData, Seated);
        }
        NextGaze = tickCount + Saved.getInt("NextGaze");
        setPersistenceRequired();
    }
}
